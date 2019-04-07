package kin.sdk.migration.common.exception;

public class AccountNotFoundLocallyException extends MigrationFailedException {

	public AccountNotFoundLocallyException() {
		super("Account was not found in the list of accounts on this device");
	}

}
