package kin.sdk.migration.common.exception;

public class AccountNotFoundInListOfAccounts extends MigrationFailedException {

	public AccountNotFoundInListOfAccounts() {
		super("Account was not found in the list of accounts on this device");
	}

}
