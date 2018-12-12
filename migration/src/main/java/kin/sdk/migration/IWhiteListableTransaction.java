package kin.sdk.migration;

public interface IWhiteListableTransaction {
    String getTransactionPayload();
    String getNetworkPassphrase();
}
