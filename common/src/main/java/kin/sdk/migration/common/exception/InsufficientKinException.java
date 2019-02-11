package kin.sdk.migration.common.exception;

/**
 * Transaction failed due to insufficient kin balance.
 */
public class InsufficientKinException extends OperationFailedException {

    public InsufficientKinException() {
        super("Not enough kin to perform the transaction.");
    }
}
