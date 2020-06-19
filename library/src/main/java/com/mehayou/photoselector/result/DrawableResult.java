package com.mehayou.photoselector.result;

import android.graphics.drawable.Drawable;
import android.support.v7.graphics.drawable.DrawableWrapper;

import com.mehayou.photoselector.PhotoSelector;

import java.io.File;

class DrawableResult extends Result<Drawable> {

    DrawableResult(PhotoSelector.ResultCallback<Drawable> callback) {
        super(callback);
    }

    @Override
    Drawable onImageResult(File file) {
        return DrawableWrapper.createFromPath(file.getPath());
    }

}
