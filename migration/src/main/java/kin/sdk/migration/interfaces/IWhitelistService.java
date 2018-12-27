package kin.sdk.migration.interfaces;

import kin.sdk.migration.exception.WhitelistTransactionFailedException;

public interface IWhitelistService {

    String whitelistTransaction(IWhitelistableTransaction whitelistableTransaction) throws WhitelistTransactionFailedException;
}
