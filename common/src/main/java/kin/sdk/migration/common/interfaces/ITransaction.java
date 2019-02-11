package kin.sdk.migration.common.interfaces;

import java.math.BigDecimal;

public interface ITransaction {

    BigDecimal getAmount();

    String getMemo();

    ITransactionId getId();

    IWhitelistableTransaction getWhitelistableTransaction();


}
