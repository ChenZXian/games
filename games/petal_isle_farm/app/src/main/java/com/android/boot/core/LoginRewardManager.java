package com.android.boot.core;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LoginRewardManager {
    private final SharedPreferences pref;

    public LoginRewardManager(Context context) {
        pref = context.getSharedPreferences("login", Context.MODE_PRIVATE);
    }

    public int claimableDay() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String last = pref.getString("last", "");
        int day = pref.getInt("day", 1);
        if (!today.equals(last)) {
            return day;
        }
        return 0;
    }

    public void claim() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        int day = pref.getInt("day", 1);
        day = day >= 7 ? 1 : day + 1;
        pref.edit().putString("last", today).putInt("day", day).apply();
    }
}
