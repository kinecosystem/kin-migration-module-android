package kin.sdk.migration.sdk_related;

import kin.sdk.TransactionId;
import kin.sdk.migration.interfaces.ITransactionId;

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