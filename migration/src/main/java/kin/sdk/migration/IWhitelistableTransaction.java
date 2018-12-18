package kin.sdk.migration;

public interface IWhitelistableTransaction {
    String getTransactionPayload();
    String getNetworkPassphrase();
}
