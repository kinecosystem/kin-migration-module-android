package kin.sdk.migration.internal.core_related;

import java.math.BigDecimal;
import kin.core.Balance;
import kin.sdk.migration.common.interfaces.IBalance;

public class KinCoreBalance implements IBalance {

    private final Balance balance;

    KinCoreBalance(Balance balance) {
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