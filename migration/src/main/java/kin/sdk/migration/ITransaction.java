package kin.sdk.migration;

import java.math.BigDecimal;

import kin.base.KeyPair;
import kin.sdk.TransactionId;

public interface ITransaction {

    KeyPair getDestination();

    KeyPair getSource();

    BigDecimal getAmount();

    int getFee();

    String getMemo();

    TransactionId getId();

//    kin.base.Transaction getStellarTransaction();

    IWhitelistableTransaction getWhitelistableTransaction();


}
