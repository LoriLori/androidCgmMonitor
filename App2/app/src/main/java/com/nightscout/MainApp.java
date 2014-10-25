package com.nightscout;

import android.app.Application;

import com.squareup.otto.Bus;

public class MainApp extends Application {

    private static MainThreadBus sBus;

    @Override
    public void onCreate() {
        super.onCreate();
        sBus = new MainThreadBus();
    }

    public static Bus bus() {
        return sBus;
    }

}
