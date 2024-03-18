package dk.mada.action.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Runs external commands.
 */
public final class ExternalCmdRunner {
    /** The temp directory path */
    private static final Path TEMP_DIR = Paths.get(System.getProperty("java.io.tmpdir"));

    private ExternalCmdRunner() {
    }

    /**
     * Input for a command execution.
     *
     * @param command the command and its arguments
     * @param execDir the execution directory. If null, the system property java.io.tmpdir will be used
     * @param stdin   text to be provided to the command's stdin, or null
     * @param env     the environment variables, or null
     * @param timeout the max runtime (in seconds) for the command. If exceeded, IllegalStateException is thrown
     */
    public record CmdInput(List<String> command, Path execDir, String stdin, Map<String, String> env, int timeout) {
    }

    /**
     * The result from a command execution.
     *
     * @param status the command exit code
     * @param output the combined stdout/stderr output
     */
    public record CmdResult(int status, String output) {
    }

    /**
     * Runs an external command.
     *
     * If the command fails (returns non-zero) or times out, a IllegalStateException is thrown.
     *
     * @param input the command input
     * @return the command result
     */
    public static CmdResult runCmd(CmdInput input) {
        try {
            Path execDir = input.execDir();
            if (execDir == null) {
                execDir = TEMP_DIR;
            }
            String stdin = input.stdin();
            Map<String, String> env = input.env();

            ProcessBuilder pb = new ProcessBuilder()
                    .command(input.command())
                    .redirectErrorStream(true)
                    .directory(execDir.toFile());
            if (env != null) {
                pb.environment().putAll(env);
            }
            Process p = pb.start();

            System.out.println("Command: " + input.command());

            if (stdin != null) {
                p.outputWriter(StandardCharsets.UTF_8).write(stdin);
                p.outputWriter().close();
            }

//            BufferedReader outputReader = p.inputReader(StandardCharsets.UTF_8);
/*
            if (stdin != null) {
                new Thread(() -> {
                    System.out.println("WRITER THREAD");
                    try {
                        p.outputWriter(StandardCharsets.UTF_8).write(stdin);
                        p.outputWriter().close();
                    } catch (IOException e) {
                        throw new UncheckedIOException("Failed to write to external process", e);
                    }
                    System.out.println("WRITER DONE!");
                }).start();
            }
*/
            ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();

            Thread readerThread = new Thread(() -> {
                System.out.println("READER THREAD");
                int total = 0;
                byte[] buf = new byte[1024];
                try (InputStream is = p.getInputStream()) {
                    int read;
                    while ((read = is.read(buf)) != -1) {
                        outputBuffer.write(buf, 0, read);
                        total += read;
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to write to external process", e);
                }
                System.out.println("READER DONE after " + total + " bytes!");
            });
            readerThread.start();
            readerThread.join();
            
            // FIXME: does timeout here matter?
            // FIXME: Surely the join above will hang anyway?
            // FIXME: Indeed that is what happens - need separate thread to interrupt
            if (!p.waitFor(input.timeout(), TimeUnit.SECONDS)) {
                throw new IllegalStateException("Command timed out!");
            }

            // FIXME: handle closing thread streams
            
//            String output = outputReader.lines().collect(Collectors.joining("\n"));
            String output = outputBuffer.toString(StandardCharsets.UTF_8);
            System.out.println("Output: " + output);
            return new CmdResult(p.exitValue(), output);
        } catch (IOException e) {
            throw new IllegalStateException("Failed running command", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Waited for command to complete, but was interruped");
        }
    }
}
