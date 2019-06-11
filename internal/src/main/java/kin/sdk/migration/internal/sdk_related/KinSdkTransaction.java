package kin.sdk.migration.internal.sdk_related;

import java.math.BigDecimal;

import kin.sdk.PaymentTransaction;
import kin.sdk.migration.common.interfaces.ITransaction;
import kin.sdk.migration.common.interfaces.ITransactionId;
import kin.sdk.migration.common.interfaces.IWhitelistableTransaction;

public class KinSdkTransaction implements ITransaction {

    private final PaymentTransaction transaction;

    KinSdkTransaction(PaymentTransaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public BigDecimal getAmount() {
        return transaction.amount();
    }

    @Override
    public String getMemo() {
        return transaction.memo();
    }

    @Override
    public ITransactionId getId() {
        return new KinSdkTransactionId(transaction.id());
    }

    @Override
    public IWhitelistableTransaction getWhitelistableTransaction() {
        return new KinSdkWhitelistableTransaction(transaction.whitelistableTransaction());
    }
}
