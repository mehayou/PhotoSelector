package com.mehayou.photoselector.result;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.mehayou.photoselector.PhotoSelector;

import java.io.File;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ResultFactory {

    public static <T> Result callback(PhotoSelector.ResultCallback<T> callback) {
        Type type = getGenericType(callback);
        if (File.class.equals(type)) {
            return new FileResult((PhotoSelector.ResultCallback<File>) callback);
        } else if (byte[].class.equals(type)) {
            return new BytesResult((PhotoSelector.ResultCallback<byte[]>) callback);
        } else if (Bitmap.class.equals(type)) {
            return new BitmapResult((PhotoSelector.ResultCallback<Bitmap>) callback);
        } else if (Drawable.class.equals(type)) {
            return new DrawableResult((PhotoSelector.ResultCallback<Drawable>) callback);
        } else if (String.class.equals(type)) {
            return new Base64Result((PhotoSelector.ResultCallback<String>) callback);
        } else {
            return null;
        }
    }

    private static <T> Type getGenericType(PhotoSelector.ResultCallback<T> callback) {
        Type[] types = callback.getClass().getGenericInterfaces();
        for (Type type : types) {
            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Type rawType = parameterizedType.getRawType();
                if (PhotoSelector.ResultCallback.class.equals(rawType)) {
                    type = parameterizedType.getActualTypeArguments()[0];
                    return type;
                }
            }
        }
        return null;
    }

    public static boolean isFileResult(Result result) {
        return result instanceof FileResult;
    }
}
