package kin.sdk.migration;

import java.math.BigDecimal;

import kin.sdk.Balance;

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
