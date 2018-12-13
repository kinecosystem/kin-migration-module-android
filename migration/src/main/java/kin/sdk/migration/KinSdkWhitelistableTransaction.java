package kin.sdk.migration;

import kin.sdk.WhitelistableTransaction;

public class KinSdkWhitelistableTransaction implements IWhitelistableTransaction {

    private final WhitelistableTransaction whitelistableTransaction;

    KinSdkWhitelistableTransaction(WhitelistableTransaction whitelistableTransaction) {
        this.whitelistableTransaction = whitelistableTransaction;
    }

    @Override
    public String getTransactionPayload() {
        return whitelistableTransaction.getTransactionPayload();
    }

    @Override
    public String getNetworkPassphrase() {
        return whitelistableTransaction.getNetworkPassphrase();
    }
}
