package io.quarkiverse.googlecloudservices.secretmanager.runtime;

public class SecretManagerCredentialProviderException extends RuntimeException {
    public SecretManagerCredentialProviderException() {
    }

    public SecretManagerCredentialProviderException(String message) {
        super(message);
    }

    public SecretManagerCredentialProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public SecretManagerCredentialProviderException(Throwable cause) {
        super(cause);
    }

    public SecretManagerCredentialProviderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
