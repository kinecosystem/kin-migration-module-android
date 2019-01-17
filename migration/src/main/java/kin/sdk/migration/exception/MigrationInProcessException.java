package kin.sdk.migration.exception;

public class MigrationInProcessException extends OperationFailedException {

    public MigrationInProcessException(Throwable cause) {
        super(cause);
    }

    public MigrationInProcessException(String message) {
        super(message);
    }

    public MigrationInProcessException(String message, Throwable cause) {
        super(message, cause);
    }
}
