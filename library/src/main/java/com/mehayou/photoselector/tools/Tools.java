package com.mehayou.photoselector.tools;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class Tools {

    /**
     * 图片媒体库中，通过File获取Uri
     *
     * @param context context
     * @param file    输出文件
     * @return 获取图片媒体Uri
     */
    public static Uri getImageMediaUri(Context context, File file) {
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ContentValues contentValues = new ContentValues(1);
            contentValues.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/*");
            uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        } else {
            uri = Uri.fromFile(file);
        }
        return uri;
    }

    /**
     * 图片媒体库中，通过Uri获取File
     *
     * @param context context
     * @param uri     输出Uri
     * @return 获取图片媒体File
     */
    public static File getImageMediaFile(Context context, Uri uri) {
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
     * 扫描图片媒体文件并刷新
     *
     * @param context context
     * @param uri     文件uri
     */
    public static void scanImageMediaAsync(Context context, Uri uri) {
        if (context != null && uri != null) {
            try {
                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
                File file = getImageMediaFile(context, uri);
                if (file != null) {
                    new SingleMediaScanner(context, file);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 扫描图片媒体文件并刷新
     *
     * @param context context
     * @param file    媒体文件
     */
    public static void scanImageMediaAsync(Context context, File file) {
        if (context != null && file != null) {
            try {
                Uri uri = getImageMediaUri(context, file);
                if (uri != null) {
                    context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
                }
                new SingleMediaScanner(context, file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 文件转字节数组
     *
     * @param file File
     * @return byte[]
     */
    public static byte[] readBytesFromFile(File file) {
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
     * 文件转Base4
     *
     * @param file File
     * @return Base4
     */
    public static String readBase64FromFile(File file) {
        String base64 = null;
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            byte[] bytes = new byte[in.available()];
            int length = in.read(bytes);
            base64 = Base64.encodeToString(bytes, 0, length, Base64.DEFAULT);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return base64;
    }

    /**
     * 媒体库扫描服务
     */
    private static class SingleMediaScanner implements MediaScannerConnection.MediaScannerConnectionClient {

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