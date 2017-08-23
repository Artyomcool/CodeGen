package ru.mail.android.meetup.codegen;

import com.flurry.android.FlurryAgent;
import com.google.android.gms.analytics.HitBuilders;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Stat {

    private static void flurry(String category, String action, Map<String, String> params) {
        FlurryAgent.logEvent(category + "_" + action, params);
    }

    private static void ga(String category, String action, long value) {
        GaTracker.get().send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setValue(value)
                .build());
    }

    private static void all(Params params) {
        Throwable throwable = new Throwable();
        int depth = 2;      //because of synthetic accessor
        StackTraceElement element = throwable.getStackTrace()[depth];

        String className = element.getClassName();
        String methodName = element.getMethodName();

        className = className.substring(className.lastIndexOf("$") + 1);

        ga(className, methodName, params.value);
        flurry(className, methodName, params.extraParams());
    }

    private static class Params {

        private long value;

        private Map<String, String> extraParams;

        static Params create() {
            return new Params();
        }

        private Map<String, String> extra() {
            if (extraParams == null) {
                extraParams = new HashMap<>();
            }
            return extraParams;
        }

        Map<String, String> extraParams() {
            return extraParams == null ? Collections.<String, String>emptyMap() : extraParams;
        }

        Params value(long value) {
            this.value = value;
            return this;
        }

        Params value(String name, long value) {
            this.value = value;
            return extra(name, String.valueOf(value));
        }

        Params extra(String name, String value) {
            extra().put(name, value);
            return this;
        }

    }

    public static class Main {

        public static void fabClicked(int length) {
            all(Params.create().value("length", length));
        }

        public static void settingsClicked() {
            all(Params.create());
        }

        public static void someMoreComplexEvent(String type, long duration, boolean important) {
            all(Params.create()
                    .value("duration", duration)
                    .extra("type", type)
                    .extra("important", String.valueOf(important))
            );
        }

    }

}