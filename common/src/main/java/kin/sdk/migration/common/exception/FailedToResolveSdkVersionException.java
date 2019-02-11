package kin.sdk.migration.common.exception;

public class FailedToResolveSdkVersionException extends Exception {

    public FailedToResolveSdkVersionException() {
        super("Failed to resolve the current sdk version");
    }

    public FailedToResolveSdkVersionException(Throwable t) {
        super(t);
    }
}
