package tar.eof.ext6.utils;

import android.content.Context;
import android.content.SharedPreferences;

import tar.eof.ext6.AppContext;


/**
 * Created by alex_strange on 11/25/2020.
 */
public class PreferenceStorage {
    static String preferencesIdentifier = "tar.io.preferences";

    public static void storeBaseDrive(String drive) {
        SharedPreferences.Editor editor;
        editor = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE).
                edit();
        editor.putString("basedrive", drive);
        editor.apply();
    }

    public static void storeCurrentDrive(String drive) {
        SharedPreferences.Editor editor;
        editor = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE).
                edit();
        editor.putString("currentdrive", drive);
        editor.apply();
    }

    public static void storeExpiration(String dateString) {
        SharedPreferences.Editor editor;
        editor = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE).
                edit();
        editor.putString("exp", dateString);
        editor.apply();
    }

    public static void setLock(boolean lock) {
        SharedPreferences.Editor editor;
        editor = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE).
                edit();
        editor.putBoolean("lock", lock);
        editor.apply();
    }

    public static boolean getLock() {
        SharedPreferences prefs;
        prefs = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE);
        boolean lock = prefs.getBoolean("lock", false);
        return lock;
    }

    public static void setHack(boolean lock) {
        SharedPreferences.Editor editor;
        editor = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE).
                edit();
        editor.putBoolean("hack", lock);
        editor.apply();
    }

    public static boolean getHack() {
        SharedPreferences prefs;
        prefs = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE);
        boolean lock = prefs.getBoolean("hack", false);
        return lock;
    }

    public static String getBaseDrive() {
        SharedPreferences prefs;
        prefs = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE);
        String basedrive = prefs.getString("basedrive", "maria");
        return basedrive;
    }

    public static String getCurrentDrive() {
        SharedPreferences prefs;
        prefs = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE);
        String currentdrive = prefs.getString("currentdrive", "maria");
        return currentdrive;
    }

    public static String getExpiration() {
        SharedPreferences prefs;
        prefs = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE);
        String basedrive = prefs.getString("exp", "F5886C");
        return basedrive;
    }

}
