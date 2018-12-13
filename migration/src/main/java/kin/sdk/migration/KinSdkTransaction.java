package kin.sdk.migration;

import java.math.BigDecimal;

import kin.base.KeyPair;
import kin.sdk.Transaction;
import kin.sdk.TransactionId;
import kin.sdk.WhitelistableTransaction;

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
    public int getFee() {
        return transaction.getFee();
    }

    @Override
    public String getMemo() {
        return transaction.getMemo();
    }

    @Override
    public TransactionId getId() {
        return transaction.getId();
    }

//    @Override
//    public kin.base.Transaction getStellarTransaction() {
//        return transaction.get;
//    }

    @Override
    public IWhitelistableTransaction getWhitelistableTransaction() {
        return new KinSdkWhitelistableTransaction(transaction.getWhitelistableTransaction());
    }
}
