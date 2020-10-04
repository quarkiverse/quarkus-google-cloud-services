package io.quarkiverse.googlecloudservices.secretmanager.runtime;

public class SecretManagerException extends RuntimeException {
    public SecretManagerException() {
    }

    public SecretManagerException(String message) {
        super(message);
    }

    public SecretManagerException(String message, Throwable cause) {
        super(message, cause);
    }

    public SecretManagerException(Throwable cause) {
        super(cause);
    }

    public SecretManagerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
