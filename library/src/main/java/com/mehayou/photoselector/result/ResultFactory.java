package com.mehayou.photoselector.result;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.mehayou.photoselector.PhotoSelector;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ResultFactory {

    public static <T> Result callback(PhotoSelector.ResultCallback<T> callback) {
        Type type = getGenericType(callback);
        Class<?> tClass = getTypeClass(type);
        if (File.class.equals(tClass)) {
            return new FileResult((PhotoSelector.ResultCallback<File>) callback);
        } else if (byte[].class.equals(tClass)) {
            return new BytesResult((PhotoSelector.ResultCallback<byte[]>) callback);
        } else if (Bitmap.class.equals(tClass)) {
            return new BitmapResult((PhotoSelector.ResultCallback<Bitmap>) callback);
        } else if (Drawable.class.equals(tClass)) {
            return new DrawableResult((PhotoSelector.ResultCallback<Drawable>) callback);
        } else if (String.class.equals(tClass)) {
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

    private static Class<?> getTypeClass(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            Type innerType = ((ParameterizedType) type).getRawType();
            return (Class<?>) innerType;
        } else if (type instanceof GenericArrayType) {
            Type compType = ((GenericArrayType) type).getGenericComponentType();
            Class<?> typeClass = getTypeClass(compType);
            if (typeClass != null) {
                return Array.newInstance(typeClass, 0).getClass();
            }
        }
        return null;
    }

    public static boolean isFileResult(Result result) {
        return result instanceof FileResult;
    }
}
