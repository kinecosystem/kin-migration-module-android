package kin.sdk.migration.sdk_related;

public class WhitelistResult {

    /**
     * represent a transaction which is whitelisted.
     * In blockchain terms it is basically  a transaction envelop as string.
     */
    private String whitelistedTransaction;

    /**
     * true if this transaction should be sent, false otherwise
     */
    private boolean shouldSendTransaction;

    public WhitelistResult(String whitelistedTransaction, boolean shouldSendTransaction) {
        this.whitelistedTransaction = whitelistedTransaction;
        this.shouldSendTransaction = shouldSendTransaction;
    }

    public String getWhitelistedTransaction() {
        return whitelistedTransaction;
    }

    public boolean shouldSendTransaction() {
        return shouldSendTransaction;
    }
}
