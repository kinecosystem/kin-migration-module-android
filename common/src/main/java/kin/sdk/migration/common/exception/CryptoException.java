package kin.sdk.migration.common.exception;

/**
 * Decryption/Encryption error when importing - KinClient.importAccount(String, String)} or
 * exporting KinAccount.export(String)} an account.
 */
public class CryptoException extends Exception {

    public CryptoException(Throwable e) {
        super(e);
    }

    public CryptoException(String msg) {
        super(msg);
    }

    public CryptoException(String msg, Throwable e) {
        super(msg, e);
    }
}
