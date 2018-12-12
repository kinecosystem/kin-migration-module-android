package kin.sdk.migration;

import java.math.BigDecimal;

import kin.core.PaymentInfo;
import kin.core.TransactionId;

public class KinCorePaymentInfo implements IPaymentInfo {

    private final PaymentInfo paymentInfo;

    KinCorePaymentInfo(PaymentInfo paymentInfo) {
        this.paymentInfo = paymentInfo;
    }

    @Override
    public String createdAt() {
        return paymentInfo.createdAt();
    }

    @Override
    public String destinationPublicKey() {
        return paymentInfo.destinationPublicKey();
    }

    @Override
    public String sourcePublicKey() {
        return paymentInfo.sourcePublicKey();
    }

    @Override
    public BigDecimal amount() {
        return paymentInfo.amount();
    }

    @Override
    public ITransactionId hash() {
        TransactionId transactionId = paymentInfo.hash();
        return new KinCoreTransactionId(transactionId);
    }

    @Override
    public String memo() {
        return paymentInfo.memo();
    }
}
