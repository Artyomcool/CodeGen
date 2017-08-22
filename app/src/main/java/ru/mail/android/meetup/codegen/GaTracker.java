package ru.mail.android.meetup.codegen;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;

public class GaTracker {

    private static Tracker tracker;

    static void init(Context context) {
        tracker = GoogleAnalytics.getInstance(context).newTracker(R.xml.global_tracker);
    }

    public static Tracker get() {
        return tracker;
    }

}
