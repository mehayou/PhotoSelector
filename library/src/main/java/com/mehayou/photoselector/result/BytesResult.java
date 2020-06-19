package com.mehayou.photoselector.result;

import com.mehayou.photoselector.PhotoSelector;
import com.mehayou.photoselector.tools.Tools;

import java.io.File;

class BytesResult extends Result<byte[]> {

    BytesResult(PhotoSelector.ResultCallback<byte[]> callback) {
        super(callback);
    }

    @Override
    byte[] onImageResult(File file) {
        return Tools.readBytesFromFile(file);
    }

}
