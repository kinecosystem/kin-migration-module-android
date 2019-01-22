package kin.sdk;

import android.text.TextUtils;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Logger {

    private static final String TAG = "KinMigrationModule";
    private static boolean shouldLog;

    public static void enable(boolean enable) {
        shouldLog = enable;
    }

    public static void d(String message) {
        if (shouldLog) {
            Log.d(TAG, message);
        }
    }

    public static void e(String message, Exception e) {
        if (shouldLog) {
            Log.e(TAG, message + "\n" + getPrintableStackTrace(e));
        }
    }

    private static String getPrintableStackTrace(Throwable t) {
        String stackTrace = Log.getStackTraceString(t);
        // print UnknownHostException as well, as Android filtering it out
        if (TextUtils.isEmpty(stackTrace)) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            stackTrace = sw.toString();
        }
        return stackTrace;
    }
}
