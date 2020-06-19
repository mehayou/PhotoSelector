package com.mehayou.photoselector.result;

import com.mehayou.photoselector.PhotoSelector;
import com.mehayou.photoselector.tools.Tools;

import java.io.File;

class Base64Result extends Result<String> {

    Base64Result(PhotoSelector.ResultCallback<String> callback) {
        super(callback);
    }

    @Override
    String onImageResult(File file) {
        return Tools.readBase64FromFile(file);
    }

}
