package kin.sdk.migration.sdk_related;

import kin.sdk.TransactionId;
import kin.sdk.migration.interfaces.ITransactionId;

public class KinSdkTransactionId implements ITransactionId {

    private final String id;

    KinSdkTransactionId(TransactionId transactionId) {
        this.id = transactionId != null ? transactionId.id() : null;
    }

    @Override
    public String id() {
        return id;
    }
}