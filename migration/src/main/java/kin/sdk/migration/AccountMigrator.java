package kin.sdk.migration;

import static kin.sdk.migration.Commons.MAX_RETRIES;

import android.support.annotation.NonNull;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import kin.sdk.Logger;
import kin.sdk.migration.bi.IMigrationEventsListener.RequestAccountMigrationSuccessReason;
import kin.sdk.migration.bi.IMigrationEventsListener.SelectedSdkReason;
import kin.sdk.migration.common.KinSdkVersion;
import kin.sdk.migration.common.exception.MigrationFailedException;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

class AccountMigrator {

	private static final int TIMEOUT = 30;

	private final MigrationEventsNotifier eventsNotifier;
	private final MigrationNetworkInfo migrationNetworkInfo;
	private OkHttpClient okHttpClient;


	AccountMigrator(MigrationEventsNotifier eventsNotifier, MigrationNetworkInfo migrationNetworkInfo) {
		this.eventsNotifier = eventsNotifier;
		this.migrationNetworkInfo = migrationNetworkInfo;
		createHttpClient();
	}

	void migrateToNewKin(final String publicAddress) throws Exception {
		eventsNotifier.onRequestAccountMigrationStarted(publicAddress);
		try {
			Response response = sendRequest(migrationNetworkInfo.getMigrationServiceUrl() + publicAddress);
			if (response.isSuccessful()) {
				eventsNotifier
					.onRequestAccountMigrationSucceeded(publicAddress, RequestAccountMigrationSuccessReason.MIGRATED);
				eventsNotifier
					.onCallbackReady(KinSdkVersion.NEW_KIN_SDK, SelectedSdkReason.MIGRATED);
			} else {
				handleMigrationException(response, publicAddress);
			}
		} catch (IOException e) {
			eventsNotifier.onRequestAccountMigrationFailed(publicAddress, e);
			throw e;
		}
	}// Handle the migration exceptions, if an exception that is not solvable then throw it, else just handle it.

	void handleMigrationException(Response response, String publicAddress) throws MigrationFailedException {
		// check if account has been migrated successfully and if yes then complete the process and update the persistent state.
		boolean handled = false;
		MigrationFailedException exception = new MigrationFailedException(
			"Migration not completed due to an unexpected exception");
		ResponseBody body = response.body();
		if (body != null) {
			try {
				Type type = new TypeToken<Map<String, String>>() {
				}.getType();
				Map<String, String> error = new Gson().fromJson(body.string(), type);
				final String code = error.get("code");
				String message = error.get("message");
				if (code != null) {
					switch (code) {
						case "4001":  // account not burned
							exception = new MigrationFailedException(message + ", code = " + code);
							break;
						case "4002":  // account was already migrated
							eventsNotifier.onRequestAccountMigrationSucceeded(publicAddress,
								RequestAccountMigrationSuccessReason.ALREADY_MIGRATED);
							eventsNotifier
								.onCallbackReady(KinSdkVersion.NEW_KIN_SDK, SelectedSdkReason.ALREADY_MIGRATED);
							handled = true;
							break;
						case "4003":  // public address is not valid, meaning the format of it is not valid.
							exception = new MigrationFailedException(message + ", code = " + code);
							break;
						case "4041":  // account was not found
							eventsNotifier.onRequestAccountMigrationSucceeded(publicAddress,
								RequestAccountMigrationSuccessReason.ACCOUNT_NOT_FOUND);
							eventsNotifier
								.onCallbackReady(KinSdkVersion.NEW_KIN_SDK, SelectedSdkReason.NO_ACCOUNT_TO_MIGRATE);
							handled = true;
							break;
						default:
							exception = new MigrationFailedException(
								"Got an unexpected migration exception with message: " + message + ", and code: "
									+ code);
							break;
					}
				}
			} catch (IOException e) {
				exception = new MigrationFailedException("Json parsing failed", e);
			}
		} else {
			exception = new MigrationFailedException("Body is null, response code is = " + response.code());
		}
		if (!handled) {
			eventsNotifier.onRequestAccountMigrationFailed(publicAddress, exception);
			throw (exception);
		}
	}

	private void createHttpClient() {
		RetryInterceptor retryInterceptor = new RetryInterceptor();
		okHttpClient = new OkHttpClient.Builder()
			.connectTimeout(TIMEOUT, TimeUnit.SECONDS)
			.readTimeout(TIMEOUT, TimeUnit.SECONDS)
			.addInterceptor(retryInterceptor)
			.build();
	}

	private Response sendRequest(String url) throws IOException {
		Request request = new Request.Builder()
			.url(url)
			.post(RequestBody.create(null, ""))
			.build();
		return okHttpClient.newCall(request).execute();
	}

	private static class RetryInterceptor implements Interceptor {

		private int retryCounter = 1;

		@Override
		public Response intercept(@NonNull Chain chain) throws IOException {
			Logger.d("RetryInterceptor, intercept: ");
			Request request = chain.request();
			Response response = chain.proceed(request);
			if (response.code() >= 500) {
				response = retryRequest(request, chain);
			}
			retryCounter = 1;
			Logger.d("RetryInterceptor, intercepted: " + response.toString());
			return response;
		}

		private Response retryRequest(Request req, Chain chain) throws IOException {
			Logger.d("RetryInterceptor, retrying new request");
			Request newRequest = req.newBuilder().build();
			Response another = chain.proceed(newRequest);
			while (retryCounter < MAX_RETRIES && another.code() >= 500) {
				retryCounter++;
				another = retryRequest(newRequest, chain); // recursive call
			}
			return another;
		}
	}


}