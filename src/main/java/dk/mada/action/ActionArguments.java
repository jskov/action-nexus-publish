package dk.mada.action;

/**
 * These are the arguments accepted by the action.
 * Arguments are provided via environment variables.
 */
public record ActionArguments(String gpgPrivateKey, String gpgPrivateKeySecret
        ) {
}
