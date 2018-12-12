package kin.sdk.migration;


import kin.sdk.WhitelistableTransaction;

public interface IWhitelistService {

    void whitelistTransaction(WhitelistableTransaction whitelistableTransaction, WhitelistServiceCallbacks callbacks);

    String whitelistTransactionSync(WhitelistableTransaction whitelistableTransaction);
}
