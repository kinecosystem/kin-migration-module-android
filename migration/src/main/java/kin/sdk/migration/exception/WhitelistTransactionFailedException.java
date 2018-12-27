package kin.sdk.migration.exception;

public class WhitelistTransactionFailedException  extends OperationFailedException {

    public WhitelistTransactionFailedException(Throwable cause) {
        super(cause);
    }

    public WhitelistTransactionFailedException(String message) {
        super(message);
    }

    public WhitelistTransactionFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
