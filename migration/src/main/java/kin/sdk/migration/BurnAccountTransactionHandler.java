package kin.sdk.migration;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import org.stellar.sdk.Asset;
import org.stellar.sdk.AssetTypeCreditAlphaNum;
import org.stellar.sdk.ChangeTrustOperation;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Server;
import org.stellar.sdk.SetOptionsOperation;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.HttpResponseException;
import org.stellar.sdk.responses.SubmitTransactionResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import kin.sdk.migration.core_related.KinCoreTransactionId;
import kin.sdk.migration.exception.AccountNotActivatedException;
import kin.sdk.migration.exception.AccountNotFoundException;
import kin.sdk.migration.exception.InsufficientKinException;
import kin.sdk.migration.exception.OperationFailedException;
import kin.sdk.migration.exception.TransactionFailedException;
import kin.sdk.migration.interfaces.ITransactionId;

public class BurnAccountTransactionHandler { // TODO: 27/12/2018 it is such a waist but delete before creating PR

    private static final String KIN_ASSET_CODE = "KIN";
    private static final String INSUFFICIENT_KIN_RESULT_CODE = "op_underfunded";
    private static final int TRANSACTIONS_TIMEOUT = 30;

    private final Server server;
    private KinAsset kinAsset;

    BurnAccountTransactionHandler(@NonNull String issuer, @NonNull String networkUrl) {
        kinAsset = new KinAsset(KIN_ASSET_CODE, issuer);
        server = new Server(networkUrl, TRANSACTIONS_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * Check if the account has been "burned".
     * @param accountId the account ID to check if it is "burned"
     * @return true if the account is "burned", false otherwise.
     * @throws AccountNotFoundException if account not created yet
     * @throws AccountNotActivatedException if account has no Kin trust
     * @throws OperationFailedException any other error
     */
    boolean isAccountBurned(@NonNull String accountId) throws OperationFailedException {
        Utils.checkNotNull(accountId, "account");
        boolean isBurned;
        try {
            AccountResponse accountResponse = server.accounts().account(KeyPair.fromAccountId(accountId));
            if (accountResponse == null) {
                throw new OperationFailedException("can't retrieve data for account " + accountId);
            }
            AccountResponse.Signer signer = accountResponse.getSigners()[0];
            isBurned = signer.getWeight() == 0;
        } catch (HttpResponseException httpError) {
            if (httpError.getStatusCode() == 404) {
                throw new AccountNotFoundException(accountId);
            } else {
                throw new OperationFailedException(httpError);
            }
        } catch (IOException e) {
            throw new OperationFailedException(e);
        }

        return isBurned;
    }

    ITransactionId burnAccount(@NonNull KeyPair from, @NonNull BigDecimal balance) throws OperationFailedException {
        AccountResponse sourceAccount = loadSourceAccount(from);
        Transaction transaction = buildBurnTransaction(from, sourceAccount, balance);
        return sendBurnAccountTransaction(transaction);
    }

    private AccountResponse loadSourceAccount(@NonNull KeyPair from) throws OperationFailedException {
        AccountResponse sourceAccount;
        sourceAccount = loadAccount(from);
        checkKinTrust(sourceAccount);
        return sourceAccount;
    }

    private AccountResponse loadAccount(@NonNull KeyPair from) throws OperationFailedException {
        AccountResponse sourceAccount;
        try {
            sourceAccount = server.accounts().account(from);
        } catch (HttpResponseException httpError) {
            if (httpError.getStatusCode() == 404) {
                throw new AccountNotFoundException(from.getAccountId());
            } else {
                throw new OperationFailedException(httpError);
            }
        } catch (IOException e) {
            throw new OperationFailedException(e);
        }
        if (sourceAccount == null) {
            throw new OperationFailedException("can't retrieve data for account " + from.getAccountId());
        }
        return sourceAccount;
    }

    private void checkKinTrust(AccountResponse accountResponse) throws AccountNotActivatedException {
        if (!kinAsset.hasKinTrust(accountResponse)) {
            throw new AccountNotActivatedException(accountResponse.getKeypair().getAccountId());
        }
    }

    @NonNull
    private Transaction buildBurnTransaction(@NonNull KeyPair from, AccountResponse sourceAccount, BigDecimal balance) {
        Transaction.Builder transactionBuilder = new Transaction.Builder(sourceAccount)
                .addOperation(new ChangeTrustOperation.Builder(kinAsset.getStellarAsset(), balance.toString()).build())
                .addOperation(new SetOptionsOperation.Builder().setMasterKeyWeight(0).build());
        Transaction transaction = transactionBuilder.build();
        transaction.sign(from);
        return transaction;
    }

    @NonNull
    private ITransactionId sendBurnAccountTransaction(Transaction transaction) throws OperationFailedException {
        try {
            SubmitTransactionResponse response = server.submitTransaction(transaction);
            if (response == null) {
                throw new OperationFailedException("can't get transaction response");
            }
            if (response.isSuccess()) {
                return new KinCoreTransactionId(response.getHash());
            } else {
                return createFailureException(response);
            }
        } catch (IOException e) {
            throw new OperationFailedException(e);
        }
    }

    private ITransactionId createFailureException(SubmitTransactionResponse response) throws TransactionFailedException, InsufficientKinException {
        TransactionFailedException transactionException = Utils.createTransactionException(response);
        if (isInsufficientKinException(transactionException)) {
            throw new InsufficientKinException();
        } else {
            throw transactionException;
        }
    }

    private boolean isInsufficientKinException(TransactionFailedException transactionException) {
        List<String> resultCodes = transactionException.getOperationsResultCodes();
        return resultCodes != null && resultCodes.size() > 0 && INSUFFICIENT_KIN_RESULT_CODE.equals(resultCodes.get(0));
    }


    private static class KinAsset {

        private final AssetTypeCreditAlphaNum stellarKinAsset;

        KinAsset(String assetCode, String kinIssuerAccountId) {
            KeyPair issuerKeyPair = KeyPair.fromAccountId(kinIssuerAccountId);
            this.stellarKinAsset = (AssetTypeCreditAlphaNum) Asset.createNonNativeAsset(assetCode, issuerKeyPair);
        }

        boolean isKinAsset(@Nullable Asset asset) {
            return asset != null && stellarKinAsset.equals(asset);
        }

        boolean hasKinTrust(@NonNull AccountResponse addresseeAccount) {
            AccountResponse.Balance balances[] = addresseeAccount.getBalances();
            boolean hasTrust = false;
            for (AccountResponse.Balance balance : balances) {
                if (isKinAsset(balance.getAsset())) {
                    hasTrust = true;
                }
            }
            return hasTrust;
        }

        @NonNull
        Asset getStellarAsset() {
            return stellarKinAsset;
        }
    }



}
