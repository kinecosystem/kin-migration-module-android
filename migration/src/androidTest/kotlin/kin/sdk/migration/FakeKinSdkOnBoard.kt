package kin.sdk.migration

import android.util.Log
import kin.sdk.migration.IntegConsts.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Fake on board for integration test, support creating and funding accounts on stellar test net
 */
internal class FakeKinSdkOnBoard @Throws(IOException::class)
constructor() {

    @Throws(Exception::class)
    fun createAccount(destinationAccount: String) {
        val client = OkHttpClient()
        val request = Request.Builder()
                .url(TEST_SDK_URL_CREATE_ACCOUNT + destinationAccount).build()
        client.newCall(request).execute()?.let {
            if (it.body() == null || it.code() != 200) {
                Log.d("test", "createAccount error, error code = ${it.code()}, message = ${it.message()}")
            }
        }
    }

    fun fundWithKin(destinationAccount: String, amount: String) {
        val client = OkHttpClient()
        val request = Request.Builder()
                .url(String.format(TEST_SDK_URL_FUND + amount, destinationAccount))
                .get()
                .build()
        client.newCall(request).execute()?.let {
            if (it.body() == null || it.code() != 200) {
                Log.d("test", "fundWithKin error, error code = ${it.code()}, message = ${it.message()}")
            }
        }

    }
}

