package kin.sdk.migration.exception;

public class FailedToResolveSdkVersionException extends Exception {

    public FailedToResolveSdkVersionException() {
        super("Failed to resolve the current sdk version");
    }
}
