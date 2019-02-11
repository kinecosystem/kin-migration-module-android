package kin.sdk.migration;

import static kin.sdk.migration.Commons.MAX_RETRIES;

import android.support.annotation.NonNull;
import kin.sdk.Logger;
import kin.sdk.migration.bi.IMigrationEventsListener.BurnReason;
import kin.sdk.migration.bi.IMigrationEventsListener.CheckBurnReason;
import kin.sdk.migration.common.exception.AccountNotActivatedException;
import kin.sdk.migration.common.exception.AccountNotFoundException;
import kin.sdk.migration.common.exception.MigrationFailedException;
import kin.sdk.migration.common.exception.OperationFailedException;
import kin.sdk.migration.common.interfaces.ITransactionId;
import kin.sdk.migration.internal.core_related.KinAccountCoreImpl;
import org.stellar.sdk.responses.HttpResponseException;

class AccountBurner {

	private final MigrationEventsNotifier eventsNotifier;

	AccountBurner(MigrationEventsNotifier eventsNotifier) {
		this.eventsNotifier = eventsNotifier;
	}

	/**
	 * Start the burn account process which checks if the account was already burned and if not then burned it.
	 *
	 * @param account is the kin account.
	 * @return the reason.
	 */
	@NonNull
	BurnReason start(final KinAccountCoreImpl account)
		throws MigrationFailedException {
		String publicAddress = account.getPublicAddress();
		if (publicAddress != null) {
			CheckBurnReason state = checkAccountBurnedState(account);
			switch (state) {
				case NOT_BURNED:
					return burnAccount(publicAddress, account);
				case ALREADY_BURNED:
					return BurnReason.ALREADY_BURNED;
				case NO_ACCOUNT:
					return BurnReason.NO_ACCOUNT;
				case NO_TRUSTLINE:
					return BurnReason.NO_TRUSTLINE;
				default:
					throw new MigrationFailedException("checkAccountBurnedState returned unexpected result");
			}
		} else {
			throw new MigrationFailedException("account not valid - public address is null");
		}
	}

	@NonNull
	private CheckBurnReason checkAccountBurnedState(final KinAccountCoreImpl kinAccountCore)
		throws MigrationFailedException {
		String publicAddress = kinAccountCore.getPublicAddress();
		eventsNotifier.onCheckBurnStarted(publicAddress);
		int retryCounter = 0;
		while (true) {
			try {
				if (kinAccountCore.isAccountBurned()) {
					eventsNotifier
						.onCheckBurnSucceeded(publicAddress, CheckBurnReason.ALREADY_BURNED);
					return CheckBurnReason.ALREADY_BURNED;
				} else {
					eventsNotifier
						.onCheckBurnSucceeded(publicAddress, CheckBurnReason.NOT_BURNED);
					return CheckBurnReason.NOT_BURNED;
				}
			} catch (AccountNotFoundException e) {
				eventsNotifier.onCheckBurnSucceeded(publicAddress, CheckBurnReason.NO_ACCOUNT);
				return CheckBurnReason.NO_ACCOUNT;
			} catch (AccountNotActivatedException e) {
				eventsNotifier.onCheckBurnSucceeded(publicAddress, CheckBurnReason.NO_TRUSTLINE);
				return CheckBurnReason.NO_TRUSTLINE;
			} catch (OperationFailedException e) {
				if (shouldRetry(retryCounter, e)) {
					Logger.d("checkAccountBurnedState: retry number " + retryCounter);
					retryCounter++;
					continue;
				}
				eventsNotifier.onCheckBurnFailed(publicAddress, e);
				throw new MigrationFailedException("Checking if the old account is burned has failed", e);
			}
		}
	}

	private BurnReason burnAccount(String publicAddress, KinAccountCoreImpl account) throws MigrationFailedException {
		eventsNotifier.onBurnStarted(publicAddress);
		int retryCounter = 0;
		while (true) {
			try {
				ITransactionId transactionId = account.sendBurnTransactionSync(publicAddress);
				if (transactionId.id() == null || transactionId.id().isEmpty()) {
					MigrationFailedException exception =
						new MigrationFailedException(
							"Burning the account could not succeed due to an unexpected error, transaction id is empty");
					eventsNotifier.onBurnFailed(publicAddress, exception);
					throw exception;
				} else {
					eventsNotifier.onBurnSucceeded(publicAddress, BurnReason.BURNED);
					return BurnReason.BURNED;
				}
			} catch (AccountNotFoundException e) {
				eventsNotifier.onBurnSucceeded(publicAddress, BurnReason.NO_ACCOUNT);
				return BurnReason.NO_ACCOUNT;
			} catch (AccountNotActivatedException e) {
				eventsNotifier.onBurnSucceeded(publicAddress, BurnReason.NO_TRUSTLINE);
				return BurnReason.NO_TRUSTLINE;
			} catch (OperationFailedException e) {
				if (shouldRetry(retryCounter, e)) {
					Logger.d("burnAccount: retry number " + retryCounter);
					retryCounter++;
					continue;
				}
				eventsNotifier.onBurnFailed(publicAddress, e);
				throw new MigrationFailedException("Burning the old account failed", e);
			}
		}
	}

	private boolean shouldRetry(int retryCounter, Throwable e) {
		boolean shouldRetry = false;
		Throwable cause = e.getCause();
		if (cause instanceof HttpResponseException) {
			HttpResponseException httpException = (HttpResponseException) cause;
			if (httpException.getStatusCode() >= 500 && retryCounter < MAX_RETRIES) {
				shouldRetry = true;
			}
		}
		return shouldRetry;
	}

}