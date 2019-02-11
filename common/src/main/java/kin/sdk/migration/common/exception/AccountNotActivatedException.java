package kin.sdk.migration.common.exception;


import android.support.annotation.NonNull;

/**
 * Account is not activated, use KinAccountactivate() to activate the account
 */
public class AccountNotActivatedException extends OperationFailedException {

    private final String accountId;

    public AccountNotActivatedException(@NonNull String accountId) {
        super("Account " + accountId + " is not activated");

        this.accountId = accountId;
    }

    @NonNull
    public String getAccountId() {
        return accountId;
    }
}
