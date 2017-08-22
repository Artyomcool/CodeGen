package ru.mail.android.meetup.codegen;

import android.app.Application;

import com.flurry.android.FlurryAgent;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        FlurryAgent.init(this, "7VVG4RYZV4J9B9J2Z4VP");
        GaTracker.init(this);
    }
}
