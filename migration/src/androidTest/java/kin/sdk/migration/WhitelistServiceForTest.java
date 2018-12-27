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
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class WhitelistServiceForTest implements IWhitelistService {

    private static final String URL_WHITELISTING_SERVICE = "http://10.4.59.1:3003/whitelist";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient okHttpClient;

    WhitelistServiceForTest() {
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String whitelistTransaction(IWhitelistableTransaction whitelistableTransaction) throws WhitelistTransactionFailedException {
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
        return whitelistTransaction;
    }

    private String toJson(IWhitelistableTransaction whitelistableTransaction) throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("envelop", whitelistableTransaction.getTransactionPayload());
        jo.put("network_id", whitelistableTransaction.getNetworkPassphrase());
        return jo.toString();
    }

    private void handleResponse(@NonNull Response response, IWhitelistServiceCallbacks callbacks) throws IOException {
        if (callbacks != null) {
            if (response.body() != null) {
                callbacks.onSuccess(response.body().string());
            } else {
                Exception exception = new Exception("Whitelist - no body, response code is " + response.code());
                callbacks.onFailure(exception);
            }
            int code = response.code();
            response.close();
            if (code != 200) {
                Exception exception = new Exception("Whitelist - response code is " + response.code());
                callbacks.onFailure(exception);
            }
        }
    }

}
