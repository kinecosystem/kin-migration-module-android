package kin.sdk.migration.interfaces;

import org.json.JSONException;

import java.io.IOException;

public interface IWhitelistService {

    void whitelistTransaction(IWhitelistableTransaction whitelistableTransaction, IWhitelistServiceCallbacks callbacks) throws JSONException;

    String whitelistTransactionSync(IWhitelistableTransaction whitelistableTransaction) throws IOException, JSONException;
}
