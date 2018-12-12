package kin.sdk.migration.exception;

/**
 * Account was deleted using KinClient.deleteAccount(int)}, and cannot be used any more.
 */
public class AccountDeletedException extends OperationFailedException {

    public AccountDeletedException() {
        super("Account deleted, Create new account");
    }
}
