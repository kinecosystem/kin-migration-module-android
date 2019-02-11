package kin.sdk.migration.common.interfaces;

import android.support.annotation.NonNull;
import kin.sdk.migration.common.exception.CorruptedDataException;
import kin.sdk.migration.common.exception.CreateAccountException;
import kin.sdk.migration.common.exception.CryptoException;
import kin.sdk.migration.common.exception.DeleteAccountException;

public interface IKinClient {

    IEnvironment getEnvironment();

    /**
     * Creates and adds an account.
     * <p>Once created, the account information will be stored securely on the device and can
     * be accessed again via the getAccount(int)} method.</p>
     *
     * @return KinAccount the account created store the key.
     */
    @NonNull
    IKinAccount addAccount() throws CreateAccountException;

    /**
     * Returns an account at input index.
     *
     * @return the account at the input index or null if there is no such account
     */
    IKinAccount getAccount(int index);

    /**
     * @return true if there is an existing account
     */
    boolean hasAccount();

    /**
     * Returns the number of existing accounts
     */
    @SuppressWarnings("WeakerAccess")
    int getAccountCount();

    /**
     * Deletes the account at input index (if it exists)
     */
    void deleteAccount(int index) throws DeleteAccountException;

    /**
     * Deletes all accounts.
     */
    @SuppressWarnings("WeakerAccess")
    void clearAllAccounts();

    /**
     * Import an account from a JSON-formatted string.
     *
     * @param exportedJson The exported JSON-formatted string.
     * @param passphrase The passphrase to decrypt the secret key.
     * @return The imported account
     */
    @NonNull
    IKinAccount importAccount(@NonNull String exportedJson, @NonNull String passphrase) throws CryptoException, CreateAccountException, CorruptedDataException;

}
