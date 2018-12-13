package kin.sdk.migration;

import android.support.annotation.NonNull;

import kin.sdk.migration.exception.CorruptedDataException;
import kin.sdk.migration.exception.CreateAccountException;
import kin.sdk.migration.exception.CryptoException;
import kin.sdk.migration.exception.DeleteAccountException;

public interface IKinClient {

    IEnvironment getEnvironment();

    /**
     * Creates and adds an account.
     * <p>Once created, the account information will be stored securely on the device and can
     * be accessed again via the getAccount(int)} method.</p>
     *
     * @return KinAccount the account created store the key.
     */
    public @NonNull
    IKinAccount addAccount() throws CreateAccountException;

    /**
     * Returns an account at input index.
     *
     * @return the account at the input index or null if there is no such account
     */
    public IKinAccount getAccount(int index);

    /**
     * @return true if there is an existing account
     */
    public boolean hasAccount();

    /**
     * Returns the number of existing accounts
     */
    @SuppressWarnings("WeakerAccess")
    public int getAccountCount();

    /**
     * Deletes the account at input index (if it exists)
     */
    public void deleteAccount(int index) throws DeleteAccountException;

    /**
     * Deletes all accounts.
     */
    @SuppressWarnings("WeakerAccess")
    public void clearAllAccounts();

    /**
     * Get the current minimum fee that the network charges per operation.
     * This value is expressed in stroops.
     *
     * @return Request<Integer> - the minimum fee.
     */
    public IRequest<Long> getMinimumFee();

    /**
     * Import an account from a JSON-formatted string.
     *
     * @param exportedJson The exported JSON-formatted string.
     * @param passphrase The passphrase to decrypt the secret key.
     * @return The imported account
     */
    public @NonNull
    IKinAccount importAccount(@NonNull String exportedJson, @NonNull String passphrase) throws CryptoException, CreateAccountException, CorruptedDataException;

}
