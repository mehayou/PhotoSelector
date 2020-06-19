package com.mehayou.photoselector.compress;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.util.Log;

import com.mehayou.photoselector.BuildConfig;
import com.mehayou.photoselector.PhotoSelector;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

class CompressAsyncTask extends AsyncTask<Object, Void, File> {

    private void logger(String message) {
        if (BuildConfig.DEBUG) {
            Log.i(PhotoSelector.class.getSimpleName(), message);
        }
    }

    private Compress.Callback callback;
    private File srcFile;
    private File outFile;
    private int pxSize;
    private long byteSize;
    private Bitmap.CompressFormat format;

    CompressAsyncTask(File srcFile, File outFile,
                      Integer pxSize, Long byteSize,
                      Bitmap.CompressFormat format,
                      Compress.Callback callback) {
        this.callback = callback;
        this.srcFile = srcFile;
        this.outFile = outFile;
        this.pxSize = pxSize;
        this.byteSize = byteSize;
        this.format = format;
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
                    if (BuildConfig.DEBUG) {
                        logger("-How much compress sample size? -" + options.inSampleSize);
                    }
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
                    if (BuildConfig.DEBUG) {
                        logger("-How much compress quality? -" + quality);
                    }
                    fos = new FileOutputStream(outFile);
                    fos.write(baos.toByteArray());
                    fos.flush();
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