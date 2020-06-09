package com.mehayou.photoselector;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PhotoSelector {

    private static final String TAG = PhotoSelector.class.getSimpleName();

    private static void logger(String message) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, message);
        }
    }

    private Tools mTools;
    // 拍照的Uri
    private Uri mCameraUri;
    // 相册的Uri
    private Uri mGalleryUri;
    // 裁剪的Uri
    private Uri mCropUri;
    //压缩生成的文件
    private File mCompressFile;

    private boolean mCompressing;

    private Builder mBuilder;

    private PhotoSelector(Builder builder) {
        this.mTools = new Tools();
        this.mBuilder = builder;
    }

    private Context getContext() {
        return this.mBuilder.context;
    }

    /**
     * @return 是否正在压缩图片
     */
    public boolean isCompressing() {
        return this.mCompressing;
    }

    private boolean isCrop() {
        return this.mBuilder.crop;
    }

    /**
     * 去拍照
     */
    public void toCamera() {
        ResultCallback resultCallback = this.mBuilder.resultCallback;
        if (resultCallback != null) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                this.mGalleryUri = null;
                this.mCameraUri = this.mTools.getCameraUri(getContext(), new File(this.mBuilder.directory, getFileName()));
                intent.putExtra(MediaStore.EXTRA_OUTPUT, this.mCameraUri);
            }
            startActivityForResult(intent, this.mBuilder.camera_request_code);
            logger("-> toCamera");
        }
    }

    /**
     * 去相册
     */
    public void toGallery() {
        ResultCallback resultCallback = this.mBuilder.resultCallback;
        if (resultCallback != null) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
            startActivityForResult(intent, this.mBuilder.gallery_request_code);
            logger("-> toGallery");
        }
    }

    /**
     * @return 随机生成文件名
     */
    private String getFileName() {
        String fileName = null;
        String format = this.mBuilder.fileNameFormat;
        if (format != null && !format.isEmpty()) {
            try {
                fileName = new SimpleDateFormat(format).format(new Date());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (fileName == null || fileName.isEmpty()) {
            fileName = String.valueOf(System.currentTimeMillis());
        }
        return fileName.concat(this.mBuilder.getFileSuffix());
    }

    /**
     * 回收裁剪图片文件
     */
    private void recycleCrop(boolean delete) {
        if (this.mCropUri != null) {
            if (delete) {
                try {
                    File cropFile = new File(new URI(this.mCropUri.toString()));
                    logger("-How much CropFile size and path? -Is "
                            + (cropFile.length() / 1024)
                            + "KB, and absolute path is "
                            + cropFile.getAbsolutePath());
                    boolean isDeleteCropFile = cropFile.delete();
                    logger("-CropFile is delete? -" + isDeleteCropFile);
                    // 刷新扫描裁剪图片文件
                    this.mTools.scanFileAsync(getContext(), this.mCropUri);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
            this.mCropUri = null;
        }
    }

    /**
     * 回收压缩图片文件
     */
    private void recycleCompress(boolean delete) {
        if (delete) {
            if (this.mCompressFile != null && this.mCompressFile.exists()) {
                logger("-How much CompressFile size and path? -Is "
                        + (this.mCompressFile.length() / 1024)
                        + "KB, and absolute path is "
                        + this.mCompressFile.getAbsolutePath());
                Uri uri = this.mTools.getImageUriFromFile(getContext(), this.mCompressFile);
                boolean isDeleteCompressFile = this.mCompressFile.delete();
                logger("-CompressFile is delete? -" + isDeleteCompressFile);
                // 刷新扫描压缩图片文件
                this.mTools.scanFileAsync(getContext(), uri);
            }
        }
        this.mCompressFile = null;
    }

    /**
     * 回收拍照图片文件
     */
    private void recycleCamera(boolean delete) {
        if (this.mCameraUri != null) {
            if (delete) {
                File file = this.mTools.uriToFile(getContext(), this.mCameraUri);
                if (file != null && file.exists()) {
                    logger("-How much CameraFile size and path? -Is "
                            + (file.length() / 1024)
                            + "KB, and absolute path is "
                            + file.getAbsolutePath());
                    boolean isDeleteCameraFile = file.delete();
                    logger("-CameraFile is delete? -" + isDeleteCameraFile);
                    // 刷新扫描压缩图片文件
                    this.mTools.scanFileAsync(getContext(), this.mCameraUri);
                }
            }
            this.mCameraUri = null;
        }
    }

    /**
     * 回收拍照、裁剪、压缩后的图片文件（相册原图片文件保留）
     */
    public void recycle() {
        recycle(true);
    }

    /**
     * 回收拍照、裁剪、压缩后的图片文件（相册原图片文件保留）
     *
     * @param force 是否强制删除
     */
    private void recycle(boolean force) {
        recycleCrop(force || this.mBuilder.recycleCrop);
        recycleCompress(force || this.mBuilder.recycleCompress);
        recycleCamera(force || this.mBuilder.recycleCamera);
        if (this.mGalleryUri != null) {
            this.mGalleryUri = null;
        }
    }

    /**
     * 刷新扫描所用到的媒体文件
     */
    private void scanFileAsync() {
        try {
            Context context = getContext();
            this.mTools.scanFileAsync(context, this.mCropUri);
            this.mTools.scanFileAsync(context, this.mCameraUri);
            //this.mTools.scanFileAsync(context, this.mGalleryUri);
            Uri uri = this.mTools.getImageUriFromFile(getContext(), this.mCompressFile);
            this.mTools.scanFileAsync(context, uri);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 截图处理
     *
     * @param srcUri  原Uri
     * @param cropUri 截图Uri
     */
    private void cropImageFromUri(Uri srcUri, Uri cropUri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(srcUri, "image/*");
        // 可裁剪
        intent.putExtra("crop", true);
        // 去黑边
        intent.putExtra("scale", true);
        // 裁剪宽高
        int outputX = this.mBuilder.cropOutputX;
        int outputY = this.mBuilder.cropOutputY;
        if (outputX > 0 && outputY > 0) {
            // 裁剪宽高
            intent.putExtra("outputX", outputX);
            intent.putExtra("outputY", outputY);
            // 宽高比例
            intent.putExtra("aspectX", outputX);
            intent.putExtra("aspectY", outputY);
        } else {
            // 宽高比例
            int aspectX = this.mBuilder.cropAspectX;
            int aspectY = this.mBuilder.cropAspectY;
            intent.putExtra("aspectX", aspectX > 0 ? aspectX : 1);
            intent.putExtra("aspectY", aspectY > 0 ? aspectY : 1);
        }
        // 不返回数据
        intent.putExtra("return-data", false);
        // 输出图片类型
        intent.putExtra("outputFormat", this.mBuilder.compressFormat.toString());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cropUri);
        intent.putExtra("noFaceDetection", true);
        //监听调起获取图片意图
        startActivityForResult(intent, this.mBuilder.result_request_code);
    }

    /**
     * 跳转相机相册
     *
     * @param intent      意图
     * @param requestCode 请求码
     */
    private void startActivityForResult(Intent intent, int requestCode) {
        if (this.mBuilder.fragment != null) {
            this.mBuilder.fragment.startActivityForResult(intent, requestCode);
        } else if (this.mBuilder.activity != null) {
            this.mBuilder.activity.startActivityForResult(intent, requestCode);
        }
    }

    /**
     * 图片回调
     *
     * @param requestCode 请求码
     * @param resultCode  结果码
     * @param data        意图
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Activity.RESULT_OK != resultCode) {
            return;
        }
        if (this.mBuilder.camera_request_code == requestCode) {
            // 拍照回调
            this.mCompressFile = null;
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                if (this.mCameraUri != null) {
                    if (isCrop()) {
                        // 截图处理
                        this.mCropUri = Uri.fromFile(new File(this.mBuilder.directory, getFileName()));
                        cropImageFromUri(this.mCameraUri, this.mCropUri);
                    } else {
                        onActivityResult(this.mBuilder.result_request_code, resultCode, data);
                    }
                    // 刷新扫描媒体文件
                    scanFileAsync();
                }
            }
        } else if (this.mBuilder.gallery_request_code == requestCode) {
            // 相册回调
            this.mCompressFile = null;
            if (data != null) {
                this.mCameraUri = null;
                this.mGalleryUri = data.getData();
                if (isCrop()) {
                    // 截图处理
                    this.mCropUri = Uri.fromFile(new File(this.mBuilder.directory, getFileName()));
                    cropImageFromUri(this.mGalleryUri, this.mCropUri);
                } else {
                    onActivityResult(this.mBuilder.result_request_code, resultCode, data);
                }
                // 刷新扫描媒体文件
                scanFileAsync();
            }
        } else if (this.mBuilder.result_request_code == requestCode) {
            // 最终回调
            File srcFile = null;
            if (isCrop() && this.mCropUri != null) {
                srcFile = this.mTools.uriToFile(getContext(), this.mCropUri);
            } else if (!isCrop()) {
                if (this.mCameraUri != null) {
                    srcFile = this.mTools.uriToFile(getContext(), this.mCameraUri);
                } else if (this.mGalleryUri != null) {
                    srcFile = this.mTools.uriToFile(getContext(), this.mGalleryUri);
                }
            }

            if (srcFile != null && srcFile.exists() && srcFile.length() > 0) {
                boolean isCompressSize = this.mBuilder.isCompressSize();
                boolean isCompressQuality = this.mBuilder.isCompressQuality();
                if (isCompressSize || (isCompressQuality && srcFile.length() > this.mBuilder.compressFileSize)) {
                    // 压缩分辨率大小 || (压缩文件大小 && 文件大小不满足要求)
                    File outFile = new File(this.mBuilder.directory, getFileName());
                    // 不压缩赋值为0
                    int pxSize = isCompressSize ? this.mBuilder.compressImageSize : 0;
                    long byteSize = isCompressQuality ? this.mBuilder.compressFileSize : 0;
                    Bitmap.CompressFormat format = this.mBuilder.compressFormat;
                    // 执行压缩操作
                    AsyncCompress.run(this, srcFile, outFile, pxSize, byteSize, format);
                } else {
                    // 不用压缩，直接返回
                    onImageResult(srcFile);
                }
            } else {
                // 异常，回收图片文件
                recycle(true);
            }
            // 刷新扫描媒体文件
            scanFileAsync();
        }
    }

    private void onCompressStart(File srcFile) {
        logger("[Compress Start] SrcFile=" + srcFile);
        mCompressing = true;
        CompressCallback compressCallback = mBuilder.compressCallback;
        if (compressCallback != null) {
            compressCallback.onCompressStart();
        }
    }

    private void onCompressComplete(File srcFile, File outFile) {
        logger("[Compress Complete] SrcFile=" + srcFile + ", OutFile=" + outFile);
        mCompressing = false;
        CompressCallback compressCallback = mBuilder.compressCallback;
        if (compressCallback != null) {
            compressCallback.onCompressComplete(outFile != null);
        }
        mCompressFile = outFile;
        if (isCrop()
                && mCompressFile != null && mCompressFile.exists()
                && mCompressFile == outFile) {
            // 有压缩图片文件，且有裁剪图片文件，则回收裁剪图片文件
            recycleCrop(this.mBuilder.recycleCrop);
        }
        // 返回结果
        onImageResult(outFile != null ? outFile : srcFile);
        // 刷新扫描媒体文件
        scanFileAsync();
    }

    private void onImageResult(File file) {
        logger("[Result] File=" + file);
        ResultCallback resultCallback = mBuilder.resultCallback;
        if (resultCallback != null) {
            byte[] bytes = this.mTools.readBytesFromFile(file);
            resultCallback.onImageResult(bytes);
            // 回收图片文件
            recycle(false);
        }
    }

    public static class Builder {

        private boolean crop = false;
        private int cropAspectX = 1;
        private int cropAspectY = 1;
        private int cropOutputX = 0;
        private int cropOutputY = 0;

        private boolean compress = false;
        private int compressImageSize = 1080;
        private long compressFileSize = 200 << 10;

        private boolean recycleCamera = true;
        private boolean recycleCrop = true;
        private boolean recycleCompress = true;

        private File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

        private String fileNameFormat = "yyyymmddhhmmssSSS";
        private Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.JPEG;

        private Context context;
        private Activity activity;
        private Fragment fragment;

        private CompressCallback compressCallback;
        private ResultCallback resultCallback;

        private int camera_request_code = 81;
        private int gallery_request_code = 82;
        private int result_request_code = 80;

        public Builder(Activity activity, ResultCallback callback) {
            this.activity = activity;
            this.context = activity;
            this.resultCallback = callback;
        }

        public Builder(Fragment fragment, ResultCallback callback) {
            this.fragment = fragment;
            this.activity = fragment.getActivity();
            this.context = fragment.getContext();
            this.resultCallback = callback;
        }

        public PhotoSelector build() {
            return new PhotoSelector(this);
        }

        /**
         * 更改请求码（互不相等才有效）
         *
         * @param camera  去相机
         * @param gallery 去相册
         * @param result  结果回调
         * @return this
         */
        public Builder setRequestCode(int camera, int gallery, int result) {
            if (camera != result && camera != gallery && gallery != result) {
                this.camera_request_code = camera;
                this.gallery_request_code = gallery;
                this.result_request_code = result;
            }
            return this;
        }

        /**
         * 设置是否回收图片文件
         *
         * @param recycleCamera   回收拍照图片文件
         * @param recycleCrop     回收裁剪图片文件
         * @param recycleCompress 回收压缩图片文件
         * @return
         */
        public Builder setRecycle(boolean recycleCamera, boolean recycleCrop, boolean recycleCompress) {
            this.recycleCamera = recycleCamera;
            this.recycleCrop = recycleCrop;
            this.recycleCompress = recycleCompress;
            return this;
        }

        /**
         * @return 输出图片文件后缀
         */
        private String getFileSuffix() {
            if (Bitmap.CompressFormat.PNG.equals(compressFormat)) {
                return ".png";
            } else {
                return ".jpg";
            }
        }

        /**
         * 输出图片文件目录路径
         *
         * @param directory 目录路径
         * @return this
         */
        public Builder setDirectory(File directory) {
            this.directory = directory;
            return this;
        }

        /**
         * 是否开启裁剪功能
         *
         * @param crop 是否裁剪
         * @return this
         */
        public Builder setCrop(boolean crop) {
            this.crop = crop;
            return this;
        }

        /**
         * 图片宽高比例
         *
         * @param x x方向比例
         * @param y y方向比例
         * @return this
         */
        public Builder setCropAspect(int x, int y) {
            this.cropAspectX = x;
            this.cropAspectY = y;
            return this;
        }

        /**
         * 强制图片输出分辨率大小（<=0,则不强制）
         * 导致setCropAspect(x,y)、setCompressImageSize(size)与isCompressSize()方法失效
         *
         * @param x x方向大小
         * @param y y方向大小
         * @return this
         */
        public Builder setCropOutput(int x, int y) {
            this.cropOutputX = x;
            this.cropOutputY = y;
            return this;
        }

        /**
         * 是否开启压缩功能
         *
         * @param compress 是否压缩
         * @return this
         */
        public Builder setCompress(boolean compress) {
            this.compress = compress;
            return this;
        }

        /**
         * 压缩回调
         *
         * @param callback 结果回调
         * @return this
         */
        public Builder setCompressCallback(CompressCallback callback) {
            this.compressCallback = callback;
            return this;
        }

        /**
         * 图片压缩分辨率大小限制（单位:px;<=0,则不压缩尺寸）
         *
         * @param size 大小
         * @return this
         */
        public Builder setCompressImageSize(int size) {
            this.compressImageSize = size;
            return this;
        }

        /**
         * 文件压缩大小限制（单位:B;<=0,则不压缩质量）
         *
         * @param size 大小
         * @return this
         */
        public Builder setCompressFileSize(long size) {
            this.compressFileSize = size;
            return this;
        }

        /**
         * 文件输出名称格式
         *
         * @param format 文件名称格式
         * @return this
         */
        public Builder setFileNameFormat(String format) {
            this.fileNameFormat = format;
            return this;
        }

        /**
         * 图片压缩格式限制（PNG不支持质量压缩）
         *
         * @param format 图片格式
         * @return this
         */
        public Builder setCompressFormat(Bitmap.CompressFormat format) {
            this.compressFormat = format;
            return this;
        }

        /**
         * @return 是否压缩尺寸（非裁剪||裁剪非强制图片输出分辨率大小）
         */
        private boolean isCompressSize() {
            return this.compress && this.compressImageSize > 0
                    && (!this.crop || this.cropOutputX <= 0 || this.cropOutputY <= 0);
        }

        /**
         * @return 是否压缩质量（文件压缩大小限制大于0）
         */
        private boolean isCompressQuality() {
            return this.compress && this.compressFileSize > 0;
        }

        @NonNull
        @Override
        public String toString() {
            return "Builder{" +
                    "recycleCamera=" + recycleCamera +
                    "recycleCrop=" + recycleCrop +
                    "recycleCompress=" + recycleCompress +
                    "crop=" + crop +
                    ", cropAspectX=" + cropAspectX +
                    ", cropAspectY=" + cropAspectY +
                    ", cropOutputX=" + cropOutputX +
                    ", cropOutputY=" + cropOutputY +
                    ", compress=" + compress +
                    ", compressImageSize=" + compressImageSize +
                    ", compressFileSize=" + compressFileSize +
                    ", directory=" + directory +
                    ", compressFormat=" + compressFormat +
                    ", context=" + context +
                    ", activity=" + activity +
                    ", fragment=" + fragment +
                    ", resultCallback=" + resultCallback +
                    ", compressCallback=" + compressCallback +
                    '}';
        }
    }

    public static class AsyncCompress extends AsyncTask<Object, Void, File> {
        private PhotoSelector callback;
        private File srcFile;
        private File outFile;
        private int pxSize;
        private long byteSize;
        private Bitmap.CompressFormat format;

        private AsyncCompress(PhotoSelector callback,
                              File srcFile, File outFile,
                              Integer pxSize, Long byteSize,
                              Bitmap.CompressFormat format) {
            this.callback = callback;
            this.srcFile = srcFile;
            this.outFile = outFile;
            this.pxSize = pxSize;
            this.byteSize = byteSize;
            this.format = format;
        }

        public static void run(PhotoSelector callback,
                               File srcFile, File outFile,
                               Integer pxSize, Long byteSize,
                               Bitmap.CompressFormat format) {
            new AsyncCompress(callback, srcFile, outFile, pxSize, byteSize, format).execute();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (callback != null) {
                callback.onCompressStart(this.srcFile);
            }
        }

        @Override
        protected File doInBackground(Object... objects) {
            return compressBitmapFile(this.srcFile, this.outFile, this.pxSize, this.byteSize, this.format);
        }

        @Override
        protected void onPostExecute(File file) {
            super.onPostExecute(file);
            if (callback != null) {
                callback.onCompressComplete(this.srcFile, file);
            }
        }

        /**
         * 压缩图片文件
         *
         * @param srcFile  源文件
         * @param outFile  输出文件
         * @param pxSize   压缩分辨率大小，小于等于0不压缩，单位px
         * @param byteSize 压缩文件大小，小于等于0不压缩（受分辨率大小压缩影响），单位byte
         * @param format   压缩格式
         * @return 返回null，则表示压缩失败，或无需进行压缩
         */
        private File compressBitmapFile(File srcFile, File outFile, Integer pxSize, Long byteSize,
                                        Bitmap.CompressFormat format) {
            if (pxSize <= 0 && byteSize <= 0) {
                // 分辨率大小 与 文件大小 需压缩一项
                return null;
            }
            if (outFile != null && srcFile != null && srcFile.exists() && srcFile.length() > 0) {
                ByteArrayOutputStream baos = null;
                FileOutputStream fos = null;
                try {
                    // 获取图片旋转角度
                    int degree = getBitmapDegree(srcFile.getAbsolutePath());

                    /* 图片分辨率压缩 */
                    // 获取原图片分辨率大小
                    BitmapFactory.Options options = null;
                    if (pxSize > 0) {
                        options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        options.inSampleSize = 1;
                        BitmapFactory.decodeFile(srcFile.getAbsolutePath(), options);
                        options.inJustDecodeBounds = false;
                        int outWidth = options.outWidth;
                        int outHeight = options.outHeight;
                        // 设置缩放比例
                        options.inSampleSize = computeSampleSize(outWidth, outHeight, pxSize);
                        logger("-How much compress sample size? -" + options.inSampleSize);
                        // 缩放比例为1，则无需压缩分辨率
                        if (options.inSampleSize <= 1) {
                            options = null;
                        }

                        // 判断是否是以Bitmap进行压缩大小输出新文件
                        if (options != null || degree != 0) {
                            // 分辨率需要压缩 || 图片被旋转 = 需要进行输出新文件
                            if (byteSize <= 0) {
                                // 原不要求压缩文件大小，则取原文件大小为最大限制
                                byteSize = srcFile.length();
                            }
                        } else {
                            // 分辨率满足要求
                            if (byteSize <= 0 || srcFile.length() <= byteSize) {
                                // 文件大小不压缩 || 文件大小符合要求 = 不需要进行输出
                                return null;
                            }
                        }
                    }

                    // 将options适配完成，重新载入图片。
                    Bitmap bitmap = BitmapFactory.decodeFile(srcFile.getAbsolutePath(), options);

                    /* 旋转图片，先旋转后压缩 */
                    if (degree != 0) {
                        bitmap = rotateBitmap(bitmap, degree);
                    }

                    /* 图片质量压缩 */
                    if (!(bitmap == null || bitmap.getWidth() == 0 || bitmap.getHeight() == 0)) {
                        int quality = 100;
                        baos = new ByteArrayOutputStream();
                        // 质量100存在输出比原图大
                        bitmap.compress(format, quality, baos);
                        // PNG不支持质量压缩
                        if (byteSize > 0 && Bitmap.CompressFormat.PNG != format) {
                            while (baos.toByteArray().length > byteSize && quality > 0) {
                                baos.reset();
                                bitmap.compress(format, quality -= 5, baos);
                            }
                        }
                        if (!bitmap.isRecycled()) {
                            bitmap.recycle();
                        }
                        logger("-How much compress quality? -" + quality);
                        if (quality >= 0) {
                            fos = new FileOutputStream(outFile);
                            fos.write(baos.toByteArray());
                            fos.flush();
                        }
                    }
                    return outFile;
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (baos != null) {
                            baos.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        /**
         * 计算图片缩放比例
         *
         * @param width      原宽度
         * @param height     原高度
         * @param targetSize 目标尺寸
         * @return 缩放比例
         */
        private int computeSampleSize(int width, int height, int targetSize) {
            int maxSize = Math.max(width, height);
            if (targetSize > 0 && maxSize > targetSize) {
                return (int) Math.ceil((double) maxSize / (double) targetSize);
            } else {
                return 1;
            }
        }

        /**
         * 获取图片旋转角度
         *
         * @param path 图片文件路径
         * @return 角度
         */
        private int getBitmapDegree(String path) {
            int degree = 0;
            try {
                ExifInterface exifInterface = new ExifInterface(path);
                int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL);
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        degree = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        degree = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        degree = 270;
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return degree;
        }

        /**
         * 旋转图片
         *
         * @param bitmap  图片
         * @param degrees 旋转角度
         * @return 旋转图片
         */
        private Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
            if (degrees != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(degrees);
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
            }
            return bitmap;
        }
    }

    public static class Tools {

        /**
         * Android N 获取拍照Uri
         *
         * @param context context
         * @param file    输出文件
         * @return 获取拍照Uri
         */
        private Uri getCameraUri(Context context, File file) {
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ContentValues contentValues = new ContentValues(1);
                contentValues.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
                uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            } else {
                uri = Uri.fromFile(file);
            }
            return uri;
        }

        /**
         * Uri转File
         *
         * @param context context
         * @param uri     uri
         * @return Uri转File
         */
        private File uriToFile(Context context, Uri uri) {
            if (context == null || uri == null) {
                return null;
            }
            final String scheme = uri.getScheme();
            String path = null;
            if (scheme == null) {
                path = uri.getPath();
            } else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
                path = uri.getPath();
            } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
                Cursor cursor = context.getContentResolver().query(uri,
                        new String[]{MediaStore.Images.ImageColumns.DATA},
                        null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                        if (index > -1) {
                            path = cursor.getString(index);
                        }
                    }
                    cursor.close();
                }
            }
            return path != null ? new File(path) : null;
        }

        /**
         * 文件转字节数组
         *
         * @param file 文件
         * @return 字节数组
         */
        private byte[] readBytesFromFile(File file) {
            byte[] bytes = null;
            if (file != null && file.exists()) {
                FileInputStream fis = null;
                try {
                    bytes = new byte[(int) file.length()];
                    //read file into bytes[]
                    fis = new FileInputStream(file);
                    fis.read(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return bytes;
        }

        /**
         * 刷新扫描媒体文件
         *
         * @param context context
         * @param uri     文件uri
         */
        private void scanFileAsync(Context context, Uri uri) {
            if (context != null && uri != null) {
                try {
                    context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
                    File file = uriToFile(context, uri);
                    if (file != null) {
                        new SingleMediaScanner(context, file);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * 刷新扫描媒体文件
         *
         * @param context context
         * @param file    媒体文件
         */
        private void scanFileAsync(Context context, File file) {
            if (context != null && file != null) {
                try {
                    context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                    new SingleMediaScanner(context, file);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * 图片文件转媒体库信息Uri
         *
         * @param context context
         * @param file    媒体文件
         * @return 媒体库信息Uri
         */
        private Uri getImageUriFromFile(Context context, File file) {
            if (context == null || file == null) {
                return null;
            }
            ContentValues contentValues = new ContentValues(1);
            contentValues.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/*");
            return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        }

        private class SingleMediaScanner implements MediaScannerConnection.MediaScannerConnectionClient {

            private MediaScannerConnection connection;
            private File file;

            private SingleMediaScanner(Context context, File file) {
                this.file = file;
                this.connection = new MediaScannerConnection(context, this);
                this.connection.connect();
            }

            @Override
            public void onMediaScannerConnected() {
                this.connection.scanFile(this.file.getAbsolutePath(), null);
            }

            @Override
            public void onScanCompleted(String path, Uri uri) {
                this.connection.disconnect();
            }
        }
    }

    public interface CompressCallback {

        /**
         * 压缩开始
         */
        void onCompressStart();

        /**
         * 压缩完成
         *
         * @param compress 是否进行了压缩（存在直出，没有进行压缩）
         */
        void onCompressComplete(boolean compress);
    }

    public interface ResultCallback {

        /**
         * 图片回调
         *
         * @param bytes 图片文件字节
         */
        void onImageResult(byte[] bytes);
    }
}
