package kin.sdk.migration.exception;

import kin.sdk.exception.OperationFailedException;

public class InsufficientFeeException extends OperationFailedException {

    public InsufficientFeeException() {
        super("Not enough fee to perform the transaction.");
    }
}
