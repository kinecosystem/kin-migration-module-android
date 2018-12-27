package kin.sdk.migration.exception;

public class MigrationFailedException extends OperationFailedException {

    public MigrationFailedException() {
        this("Migration process failed due to an unexpected exception");
    }

    public MigrationFailedException(Throwable cause) {
        super(cause);
    }

    public MigrationFailedException (String message) {
        super(message);
    }

    public MigrationFailedException (String message, Throwable cause) {
        super(message, cause);
    }
}
