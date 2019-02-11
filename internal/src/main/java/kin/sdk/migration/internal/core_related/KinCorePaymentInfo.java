package kin.sdk.migration.internal.core_related;

import java.math.BigDecimal;
import kin.core.PaymentInfo;
import kin.core.TransactionId;
import kin.sdk.migration.common.interfaces.IPaymentInfo;
import kin.sdk.migration.common.interfaces.ITransactionId;

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
