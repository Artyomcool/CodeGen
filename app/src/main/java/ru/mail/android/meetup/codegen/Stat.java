package ru.mail.android.meetup.codegen;

import android.util.Log;

import com.flurry.android.FlurryAgent;
import com.google.android.gms.analytics.HitBuilders;

import java.util.HashMap;
import java.util.Map;

public class Stat {

    private static void flurry(String category, String action) {
        FlurryAgent.logEvent(category + "_" + action);
    }

    private static void ga(String category, String action) {
        GaTracker.get().send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .build());
    }

    private static void all(String category, String action) {
        flurry(category, action);
        ga(category, action);
    }

    private static void all() {
        Throwable throwable = new Throwable();
        int depth = 2;      //because of synthetic accessor
        StackTraceElement element = throwable.getStackTrace()[depth];

        String className = element.getClassName();
        String methodName = element.getMethodName();

        className = className.substring(className.lastIndexOf("$") + 1);
        all(className, methodName);
    }

    public static class Main {

        public static void fabClicked(int length) {
            Map<String, String> params = new HashMap<>();
            params.put("length", String.valueOf(length));
            FlurryAgent.logEvent("Main_fabClicked", params);

            GaTracker.get().send(new HitBuilders.EventBuilder()
                    .setCategory("Main")
                    .setAction("fabClicked")
                    .setValue(length)
                    .build());
        }

        public static void settingsClicked() {
            all();
        }

    }

}