package kin.sdk.migration.common.interfaces;

public interface IWhitelistableTransaction {
    String getTransactionPayload();
    String getNetworkPassphrase();
}
