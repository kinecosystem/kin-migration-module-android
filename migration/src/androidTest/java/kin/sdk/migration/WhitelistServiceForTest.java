package kin.sdk.migration;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import kin.sdk.WhitelistableTransaction;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WhitelistServiceForTest {

    private static final String URL_WHITELISTING_SERVICE = "http://18.206.35.110:3000/whitelist";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient okHttpClient;

    WhitelistServiceForTest() {
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
    }

    String whitelistTransaction(WhitelistableTransaction whitelistableTransaction) throws JSONException, IOException {
        RequestBody requestBody = RequestBody.create(JSON, toJson(whitelistableTransaction));
        Request request = new Request.Builder()
                .url(URL_WHITELISTING_SERVICE)
                .post(requestBody)
                .build();
        Response response = okHttpClient.newCall(request).execute();
        String whitelist = null;
        if (response.body() != null) {
            whitelist = response.body().string();
        }
        return whitelist;
    }

    private String toJson(WhitelistableTransaction whitelistableTransaction) throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("envelop", whitelistableTransaction.getTransactionPayload());
        jo.put("network_id", whitelistableTransaction.getNetworkPassphrase());
        return jo.toString();
    }

}
