package com.mehayou.photoselector.result;

import com.mehayou.photoselector.PhotoSelector;

import java.io.File;

class FileResult extends Result<File> {

    FileResult(PhotoSelector.ResultCallback<File> callback) {
        super(callback);
    }

    @Override
    File onImageResult(File file) {
        return file;
    }

}
