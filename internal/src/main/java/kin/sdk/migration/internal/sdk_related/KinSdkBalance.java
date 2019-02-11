package kin.sdk.migration.internal.sdk_related;

import java.math.BigDecimal;
import kin.sdk.Balance;
import kin.sdk.migration.common.interfaces.IBalance;

public class KinSdkBalance implements IBalance {

    private final Balance balance;

    KinSdkBalance(Balance balance) {
        this.balance = balance;
    }

    @Override
    public BigDecimal value() {
        return balance.value();
    }

    @Override
    public String value(int precision) {
        return balance.value(precision);
    }
}
