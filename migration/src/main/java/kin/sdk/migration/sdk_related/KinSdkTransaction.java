package kin.sdk.migration.sdk_related;

import java.math.BigDecimal;

import kin.base.KeyPair;
import kin.sdk.Transaction;
import kin.sdk.TransactionId;
import kin.sdk.migration.interfaces.ITransaction;
import kin.sdk.migration.interfaces.IWhitelistableTransaction;

public class KinSdkTransaction implements ITransaction {

    private final Transaction transaction;

    KinSdkTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public KeyPair getDestination() {
        return transaction.getDestination();
    }

    @Override
    public KeyPair getSource() {
        return transaction.getSource();
    }

    @Override
    public BigDecimal getAmount() {
        return transaction.getAmount();
    }

    @Override
    public String getMemo() {
        return transaction.getMemo();
    }

    @Override
    public TransactionId getId() {
        return transaction.getId();
    }

    @Override
    public IWhitelistableTransaction getWhitelistableTransaction() {
        return new KinSdkWhitelistableTransaction(transaction.getWhitelistableTransaction());
    }
}
