package kin.sdk.migration;

import java.math.BigDecimal;

import kin.core.Balance;

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