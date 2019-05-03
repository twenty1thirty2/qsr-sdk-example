package com.closecomms.qsrsdkexample;

import android.app.Application;

import timber.log.Timber;

public class QSRSDKExampleApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }

}
