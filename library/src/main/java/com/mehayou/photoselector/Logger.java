package com.mehayou.photoselector;

import android.util.Log;

class Logger {

    static void i(String message) {
        if (BuildConfig.DEBUG) {
            Log.i(PhotoSelector.class.getSimpleName(), message);
        }
    }
}
