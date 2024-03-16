package dk.mada.action.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Runs external commands.
 */
public final class ExternalCmdRunner {
    /** Default timeout for a command. */
    private static final int CMD_DEFAULT_TIMEOUT_SECONDS = 30;
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
        public static CmdInput ofCmd(List<String> command) {
            return new CmdInput(command, null, null, null, CMD_DEFAULT_TIMEOUT_SECONDS);
        }

        public static CmdInput ofCmd(List<String> command, Map<String, String> env) {
            return new CmdInput(command, null, null, env, CMD_DEFAULT_TIMEOUT_SECONDS);
        }
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

            if (stdin != null) {
                p.outputWriter(StandardCharsets.UTF_8).write(stdin);
            }
            BufferedReader outputReader = p.inputReader(StandardCharsets.UTF_8);

            if (!p.waitFor(input.timeout(), TimeUnit.SECONDS)) {
                throw new IllegalStateException("Command timed out!");
            }

            String output = outputReader.lines().collect(Collectors.joining("\n"));
            return new CmdResult(p.exitValue(), output);
        } catch (IOException e) {
            throw new IllegalStateException("Failed running command", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Waited for command to complete, but was interruped");
        }
    }
}
