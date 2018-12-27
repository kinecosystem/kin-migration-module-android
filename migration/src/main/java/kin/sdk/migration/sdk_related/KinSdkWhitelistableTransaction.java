package kin.sdk.migration.sdk_related;

import kin.sdk.WhitelistableTransaction;
import kin.sdk.migration.interfaces.IWhitelistableTransaction;

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
