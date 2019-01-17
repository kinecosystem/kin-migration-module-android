package kin.sdk.migration;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import kin.sdk.migration.exception.WhitelistTransactionFailedException;
import kin.sdk.migration.interfaces.IWhitelistService;
import kin.sdk.migration.interfaces.IWhitelistServiceCallbacks;
import kin.sdk.migration.interfaces.IWhitelistableTransaction;
import kin.sdk.migration.sdk_related.WhitelistResult;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class WhitelistServiceForTest implements IWhitelistService {

    private static final String URL_WHITELISTING_SERVICE = "http://34.239.111.38:3000/whitelist";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient okHttpClient;

    WhitelistServiceForTest() {
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public WhitelistResult onWhitelistableTransactionReady(IWhitelistableTransaction whitelistableTransaction) throws WhitelistTransactionFailedException {
        String whitelistTransaction = null;
        RequestBody requestBody;
        try {
            requestBody = RequestBody.create(JSON, toJson(whitelistableTransaction));
        } catch (JSONException e) {
            throw new WhitelistTransactionFailedException(e);
        }
        Request request = new Request.Builder()
                .url(URL_WHITELISTING_SERVICE)
                .post(requestBody)
                .build();
        Response response;
        try {
            response = okHttpClient.newCall(request).execute();
            if (response != null) {
                ResponseBody body = response.body();
                if (body != null) {
                    whitelistTransaction = body.string();
                }
            }
        } catch (IOException e) {
            throw new WhitelistTransactionFailedException(e);
        }
        return new WhitelistResult(whitelistTransaction, true);
    }

    private String toJson(IWhitelistableTransaction whitelistableTransaction) throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("envelope", whitelistableTransaction.getTransactionPayload());
        jo.put("network_id", whitelistableTransaction.getNetworkPassphrase());
        return jo.toString();
    }

}
