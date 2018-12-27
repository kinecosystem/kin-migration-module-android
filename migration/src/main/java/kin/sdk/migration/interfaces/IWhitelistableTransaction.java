package kin.sdk.migration.interfaces;

public interface IWhitelistableTransaction {
    String getTransactionPayload();
    String getNetworkPassphrase();
}
