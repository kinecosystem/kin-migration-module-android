package kin.sdk.migration;

import kin.core.TransactionId;

public class KinCoreTransactionId implements ITransactionId {

    private final String id;

    KinCoreTransactionId(TransactionId transactionId) {
        this.id = transactionId.id();
    }

    @Override
    public String id() {
        return id;
    }
}
