package kin.sdk.migration;

import kin.sdk.TransactionId;

public class KinSdkTransactionId implements ITransactionId {

    private final String id;

    KinSdkTransactionId(TransactionId transactionId) {
        this.id = transactionId.id();
    }

    @Override
    public String id() {
        return id;
    }
}