package kin.sdk.migration.exception;

public class MigrationFailedException extends OperationFailedException {


    public MigrationFailedException (String message) {
        super(message);
    }

    public MigrationFailedException (String message, Throwable cause) {
        super(message, cause);
    }
}
