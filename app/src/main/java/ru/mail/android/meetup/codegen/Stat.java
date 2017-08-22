package ru.mail.android.meetup.codegen;

import com.flurry.android.FlurryAgent;
import com.google.android.gms.analytics.HitBuilders;

public class Stat {

    public static class Main {

        public static void fabClicked() {
            FlurryAgent.logEvent("Main_fabClicked");
            GaTracker.get().send(new HitBuilders.EventBuilder()
                    .setCategory("Main")
                    .setAction("fabClicked")
                    .build());
        }

        public static void settingsClicked() {
            FlurryAgent.logEvent("Main_settingsClicked");
            GaTracker.get().send(new HitBuilders.EventBuilder()
                    .setCategory("Main")
                    .setAction("settingsClicked")
                    .build());
        }

    }

}