package kin.sdk.migration.core_related;

import kin.core.TransactionId;
import kin.sdk.migration.interfaces.ITransactionId;

public class KinCoreTransactionId implements ITransactionId {

    private final String id;

    public KinCoreTransactionId(TransactionId transactionId) {
        this.id = transactionId.id();
    }

    public KinCoreTransactionId(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }
}
