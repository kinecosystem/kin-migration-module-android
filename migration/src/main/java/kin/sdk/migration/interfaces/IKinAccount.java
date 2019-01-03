package kin.sdk.migration.interfaces;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.io.IOException;
import java.math.BigDecimal;
import kin.sdk.migration.exception.AccountNotActivatedException;
import kin.sdk.migration.exception.CryptoException;
import kin.sdk.migration.exception.OperationFailedException;
import kin.sdk.migration.exception.AccountNotFoundException;
import kin.sdk.migration.exception.TransactionFailedException;
import kin.sdk.migration.exception.InsufficientKinException;
import kin.utils.Request;

public interface IKinAccount {

    /**
     * @return String the public address of the account, or null if deleted
     */
    @Nullable
    String getPublicAddress();

    /**
     * Create a request for signing and sending a transaction of the given amount in kin, to the specified public
     * address
     *
     * @param publicAddress the account address to send the specified kin amount
     * @param amount the amount of kin to transfer
     * @param whitelistService is a service which can be used in order to whitelist the transaction before sending it.
     * @return {@code Request<TransactionId>}, TransactionId - the transaction identifier
     */
    @NonNull
    Request<ITransactionId> sendTransaction(@NonNull String publicAddress, @NonNull BigDecimal amount, IWhitelistService whitelistService);

    /**
     * Create Request for signing and sending a transaction of the given amount in kin, to the specified public
     * address
     * <p> See KinAccount.sendTransactionSync(String, BigDecimal) for possibles errors</p>
     *
     * @param publicAddress the account address to send the specified kin amount
     * @param amount the amount of kin to transfer
     * @param whitelistService is a service which can be used in order to whitelist the transaction before sending it.
     * @param memo An optional string, can contain a utf-8 string up to 28 bytes in length, included on the transaction
     * record.
     * @return {@code Request<TransactionId>}, TransactionId - the transaction identifier
     */
    @NonNull
    Request<ITransactionId> sendTransaction(@NonNull String publicAddress, @NonNull BigDecimal amount, IWhitelistService whitelistService, @Nullable String memo);

    /**
     * Create, sign and send a transaction of the given amount in kin to the specified public address
     * <p><b>Note:</b> This method accesses the network, and should not be called on the android main thread.</p>
     *
     * @param publicAddress the account address to send the specified kin amount
     * @param amount the amount of kin to transfer
     * @param whitelistService is a service which can be used in order to whitelist the transaction before sending it.
     * @return TransactionId the transaction identifier
     * @throws AccountNotFoundException if the sender or destination account was not created
     * @throws AccountNotActivatedException if the sender or destination account is not activated
     * @throws InsufficientKinException if account balance has not enough kin
     * @throws TransactionFailedException if transaction failed, contains blockchain failure details
     * @throws OperationFailedException other error occurred
     */
    @NonNull
    ITransactionId sendTransactionSync(@NonNull String publicAddress, @NonNull BigDecimal amount, IWhitelistService whitelistService)
            throws OperationFailedException;

    /**
     * Create, sign and send a transaction of the given amount in kin to the specified public address
     * <p><b>Note:</b> This method accesses the network, and should not be called on the android main thread.</p>
     *
     * @param publicAddress the account address to send the specified kin amount
     * @param amount the amount of kin to transfer
     * @param whitelistService is a service which can be used in order to whitelist the transaction before sending it.
     * @param memo An optional string, can contain a utf-8 string up to 28 bytes in length, included on the transaction
     * record.
     * @return TransactionId the transaction identifier
     * @throws AccountNotFoundException if the sender or destination account was not created
     * @throws AccountNotActivatedException if the sender or destination account is not activated
     * @throws InsufficientKinException if account balance has not enough kin
     * @throws TransactionFailedException if transaction failed, contains blockchain failure details
     * @throws OperationFailedException other error occurred
     */
    @NonNull
    ITransactionId sendTransactionSync(@NonNull String publicAddress, @NonNull BigDecimal amount, IWhitelistService whitelistService, @Nullable String memo)
            throws OperationFailedException;


    /**
     * Create request for getting the current confirmed balance in kin
     *
     * @return {@code Request<Balance>} Balance - the balance in kin
     */
    @NonNull
    Request<IBalance> getBalance();

    /**
     * Get the current confirmed balance in kin
     * <p><b>Note:</b> This method accesses the network, and should not be called on the android main thread.</p>
     *
     * @return the balance in kin
     * @throws AccountNotFoundException if account was not created
     * @throws AccountNotActivatedException if account is not activated
     * @throws OperationFailedException any other error
     */
    @NonNull
    IBalance getBalanceSync() throws OperationFailedException;

    /**
     * Create Request for allowing an account to receive kin.
     * <p> See KinAccount.activateSync() for possibles errors</p>
     *
     * @return {@code Request<Void>}
     */
    @NonNull
    Request<Void> activate();

    /**
     * Allow an account to receive kin.
     * <p><b>Note:</b> This method accesses the network, and should not be called on the android main thread.</p>
     * @throws AccountNotFoundException if account is not created
     * @throws TransactionFailedException if activation transaction failed, contains blockchain failure details
     * @throws OperationFailedException any other error
     */
    void activateSync() throws OperationFailedException;

    /**
     * Get current account status on blockchain network.
     *
     * @return account status, either AccountStatus.NOT_CREATED, AccountStatus.NOT_ACTIVATED or
     * AccountStatus.ACTIVATED
     * @throws OperationFailedException any other error
     */
    int getStatusSync() throws OperationFailedException;

    /**
     * Create Request for getting current account status on blockchain network.
     * <p> See KinAccount.getStatusSync() for possibles errors</p>
     *
     * @return account status, either AccountStatus.NOT_CREATED, AccountStatus.NOT_ACTIVATED or
     * AccountStatus.ACTIVATED
     */
    Request<Integer> getStatus();

    /**
     * Export the account data as a JSON string. The seed is encrypted.
     *
     * @param passphrase The passphrase with which to encrypt the seed
     * @return A JSON representation of the data as a string
     */
    String export(@NonNull String passphrase) throws CryptoException;

    /**
     * Creates and adds listener for balance changes of this account, use returned {@link IListenerRegistration} to
     * stop listening. <p><b>Note:</b> Events will be fired on background thread.</p>
     *
     * @param listener listener object for payment events
     */
    IListenerRegistration addBalanceListener(@NonNull final IEventListener<IBalance> listener);

    /**
     * Creates and adds listener for payments concerning this account, use returned {@link IListenerRegistration} to
     * stop listening. <p><b>Note:</b> Events will be fired on background thread.</p>
     *
     * @param listener listener object for payment events
     */
    IListenerRegistration addPaymentListener(@NonNull final IEventListener<IPaymentInfo> listener);

    /**
     * Creates and adds listener for account creation event, use returned {@link IListenerRegistration} to stop
     * listening. <p><b>Note:</b> Events will be fired on background thread.</p>
     *
     * @param listener listener object for payment events
     */
    IListenerRegistration addAccountCreationListener(final IEventListener<Void> listener);

}
