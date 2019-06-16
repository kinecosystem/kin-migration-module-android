package kin.sdk.migration.internal.sdk_related;

import kin.sdk.WhitelistableTransaction;
import kin.sdk.migration.common.interfaces.IWhitelistableTransaction;

public class KinSdkWhitelistableTransaction implements IWhitelistableTransaction {

    private final WhitelistableTransaction whitelistableTransaction;

    KinSdkWhitelistableTransaction(WhitelistableTransaction whitelistableTransaction) {
        this.whitelistableTransaction = whitelistableTransaction;
    }

    @Override
    public String getTransactionPayload() {
        return whitelistableTransaction.transactionPayload();
    }

    @Override
    public String getNetworkPassphrase() {
        return whitelistableTransaction.networkPassphrase();
    }
}
