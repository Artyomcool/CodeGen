package ru.mail.android.meetup.codegen;

import com.flurry.android.FlurryAgent;
import com.google.android.gms.analytics.HitBuilders;

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

    public static class Main {

        public static void fabClicked() {
            all("Main", "fabClicked");
        }

        public static void settingsClicked() {
            all("Main", "settingsClicked");
        }

    }

}