package kin.sdk.migration.internal.sdk_related;

import java.math.BigDecimal;
import kin.sdk.Transaction;
import kin.sdk.migration.common.interfaces.ITransaction;
import kin.sdk.migration.common.interfaces.ITransactionId;
import kin.sdk.migration.common.interfaces.IWhitelistableTransaction;

public class KinSdkTransaction implements ITransaction {

    private final Transaction transaction;

    KinSdkTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public BigDecimal getAmount() {
        return new BigDecimal(0);
    }

    @Override
    public String getMemo() {
        return transaction.getMemo().toString();
    }

    @Override
    public ITransactionId getId() {
        return new KinSdkTransactionId(transaction.getId());
    }

    @Override
    public IWhitelistableTransaction getWhitelistableTransaction() {
        return new KinSdkWhitelistableTransaction(transaction.getWhitelistableTransaction());
    }
}
