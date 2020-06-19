package com.mehayou.photoselector.result;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.mehayou.photoselector.PhotoSelector;

import java.io.File;

public class ResultFactory {

    public static Result callbackBytes(PhotoSelector.ResultCallback<byte[]> callback) {
        return new BytesResult(callback);
    }

    public static Result callbackFile(PhotoSelector.ResultCallback<File> callback) {
        return new FileResult(callback);
    }

    public static Result callbackBase64(PhotoSelector.ResultCallback<String> callback) {
        return new Base64Result(callback);
    }

    public static Result callbackBitmap(PhotoSelector.ResultCallback<Bitmap> callback) {
        return new BitmapResult(callback);
    }

    public static Result callbackDrawable(PhotoSelector.ResultCallback<Drawable> callback) {
        return new DrawableResult(callback);
    }

    public static boolean isFileResult(Result result) {
        return result instanceof FileResult;
    }
}
