package kin.sdk.migration.internal.sdk_related;

import java.math.BigDecimal;
import kin.sdk.PaymentInfo;
import kin.sdk.TransactionId;
import kin.sdk.migration.common.interfaces.IPaymentInfo;
import kin.sdk.migration.common.interfaces.ITransactionId;

public class KinSdkPaymentInfo  implements IPaymentInfo {

    private final PaymentInfo paymentInfo;

    KinSdkPaymentInfo(PaymentInfo paymentInfo) {
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
        return new KinSdkTransactionId(transactionId);
    }

    @Override
    public String memo() {
        return paymentInfo.memo();
    }
}
