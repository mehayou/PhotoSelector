package com.mehayou.photoselector.compress;

import android.graphics.Bitmap;

import java.io.File;

public class Compress {

    public static void run(File srcFile, File outFile,
                           Integer pxSize, Long byteSize,
                           Bitmap.CompressFormat format,
                           Callback callback) {
        new CompressAsyncTask(srcFile, outFile, pxSize, byteSize, format, callback).execute();
    }

    public interface Callback {

        void onCompressStart(File srcFile);

        void onCompressComplete(File srcFile, File outFile);
    }
}
