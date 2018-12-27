package kin.sdk.migration.interfaces;

import java.math.BigDecimal;

import kin.base.KeyPair;
import kin.sdk.TransactionId;

public interface ITransaction {

    KeyPair getDestination();

    KeyPair getSource();

    BigDecimal getAmount();

    String getMemo();

    TransactionId getId();

    IWhitelistableTransaction getWhitelistableTransaction();


}
