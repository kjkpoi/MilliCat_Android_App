package kr.ac.kaist.mobilecs;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

/**
 * Created by william on 24/03/15.
 */
public class SharedPreferencesManager {

    public static final String MAIN_PREFERENCES = "MainPreferences";
    private static SharedPreferences sharedPreferences;

    public static boolean isLoggedIn(@NonNull Context context) {
        return getSharedPreferences(context).getBoolean("isLoggedIn", false);
    }

    public static void isLoggedIn(@NonNull Context context, boolean isLoggedIn) {
        getSharedPreferences(context).edit().putBoolean("isLoggedIn", isLoggedIn).apply();
    }

    public static String getName(@NonNull Context context) {
        return getSharedPreferences(context).getString("name", null);
    }

    public static void setName(@NonNull Context context, String name) {
        getSharedPreferences(context).edit().putString("name", name).apply();
    }

    public static SharedPreferences getSharedPreferences(@NonNull Context context) {
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(MAIN_PREFERENCES, Context.MODE_PRIVATE);

        return sharedPreferences;
    }
}
