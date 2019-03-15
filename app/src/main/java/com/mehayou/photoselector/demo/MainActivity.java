package com.mehayou.photoselector.demo;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.mehayou.photoselector.PhotoSelector;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements PhotoSelector.Callback,
        View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private ImageView mImageView;
    private ProgressBar mProgressBar;

    private CheckBox cbFormat;

    private CheckBox cbCompress;
    private EditText etMaxDpi;
    private EditText etMaxSize;

    private CheckBox cbCrop;
    private EditText etOutputX;
    private EditText etOutputY;
    private EditText etAspectX;
    private EditText etAspectY;

    private ScrollView mScrollView;
    private TextView mTextView;
    private StringBuilder mStringBuilder;
    private Runnable mRunnable;

    private PhotoSelector mPhotoSelector;
    private PhotoSelector.Builder mBuilder;

    private int aspectX = 1;
    private int aspectY = 1;
    private int outputX = 0;
    private int outputY = 0;

    private int maxDpi = 1080;
    private int maxSize = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);

        mImageView = findViewById(R.id.image_view);
        mProgressBar = findViewById(R.id.progress_bar);
        mScrollView = findViewById(R.id.scroll_view);
        mTextView = findViewById(R.id.text_view);
        mProgressBar.setVisibility(View.GONE);
        mProgressBar.getIndeterminateDrawable()
                .setColorFilter(getResources().getColor(R.color.colorAccent), PorterDuff.Mode.SRC_IN);
        findViewById(R.id.image_camera).setOnClickListener(this);
        findViewById(R.id.image_gallery).setOnClickListener(this);
        findViewById(R.id.image_recycle).setOnClickListener(this);

        cbFormat = findViewById(R.id.cb_format);
        cbCompress = findViewById(R.id.cb_compress);
        etMaxDpi = findViewById(R.id.et_compress_max_dpi);
        etMaxSize = findViewById(R.id.et_compress_max_size);
        cbCrop = findViewById(R.id.cb_crop);
        etOutputX = findViewById(R.id.et_crop_output_x);
        etOutputY = findViewById(R.id.et_crop_output_y);
        etAspectX = findViewById(R.id.et_crop_aspect_x);
        etAspectY = findViewById(R.id.et_crop_aspect_y);

        mBuilder = new PhotoSelector.Builder(this, this);
        mPhotoSelector = mBuilder.build();

        cbFormat.setOnCheckedChangeListener(this);
        cbCompress.setOnCheckedChangeListener(this);
        cbCrop.setOnCheckedChangeListener(this);
        cbFormat.setChecked(true);
        cbCompress.setChecked(true);
        onCheckedChanged(cbCrop, false);
    }

    private void loadValue() {
        loadCropValue();
        loadCompressValue();
    }

    private void loadCropValue() {
        boolean isChecked = cbCrop.isChecked();
        etOutputX.setEnabled(isChecked);
        etOutputY.setEnabled(isChecked);
        etAspectX.setEnabled(isChecked);
        etAspectY.setEnabled(isChecked);
        outputX = getValue(etOutputX, outputX, 0, 6000, isChecked);
        outputY = getValue(etOutputY, outputY, 0, 6000, isChecked);
        aspectX = getValue(etAspectX, aspectX, 1, 10000, isChecked);
        aspectY = getValue(etAspectY, aspectY, 1, 10000, isChecked);
        mBuilder.setCrop(isChecked);
        mBuilder.setCropOutput(outputX, outputY);
        mBuilder.setCropAspect(aspectX, aspectY);
    }

    private void loadCompressValue() {
        boolean isChecked = cbCompress.isChecked();
        cbFormat.setEnabled(isChecked);
        etMaxDpi.setEnabled(isChecked);
        etMaxSize.setEnabled(isChecked);
        maxDpi = getValue(etMaxDpi, maxDpi, 200, 6000, isChecked);
        maxSize = getValue(etMaxSize, maxSize, 20, 2048, isChecked);
        if (isChecked) {
            cbFormat.setText(cbFormat.getHint());
        } else {
            cbFormat.setText("");
        }
        mBuilder.setCompress(isChecked);
        mBuilder.setCompressImageSize(maxDpi);
        mBuilder.setCompressFileSize(maxSize << 10);
    }

    private int getValue(TextView view, int last, int min, int max, boolean isChecked) {
        int value = 0;
        try {
            String string = view.getText().toString().trim();
            if (!TextUtils.isEmpty(string)) {
                value = Integer.valueOf(string);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (value >= min && value <= max) {
            last = value;
        }
        view.setText(isChecked ? String.valueOf(last) : "");
        return last;
    }

    private void show(String msg) {
        if (mStringBuilder == null) {
            mStringBuilder = new StringBuilder();
        }
        mStringBuilder.append(msg);
        mStringBuilder.append("\n");
        mTextView.setText(mStringBuilder.toString());

        if (mRunnable == null) {
            mRunnable = new Runnable() {
                @Override
                public void run() {
                    int offset = mTextView.getMeasuredHeight() - mScrollView.getMeasuredHeight();
                    if (offset > 0) {
                        mScrollView.scrollTo(0, offset);
                    }
                }
            };
        }
        mTextView.post(mRunnable);
    }

    @Override
    public void onCheckedChanged(CompoundButton view, boolean isChecked) {
        switch (view.getId()) {
            case R.id.cb_format:
                mBuilder.setCompressFormat(isChecked ? Bitmap.CompressFormat.JPEG : Bitmap.CompressFormat.PNG);
                view.setText(isChecked ? "JPG" : "PNG");
                view.setHint(isChecked ? "JPG" : "PNG");
                break;
            case R.id.cb_compress:
                loadCompressValue();
                break;
            case R.id.cb_crop:
                loadCropValue();
                break;
            default:
                break;
        }
    }

    @Override
    public void onClick(View view) {
        if (mPhotoSelector.isCompressing()) {
            return;
        }
        switch (view.getId()) {
            case R.id.image_camera:
                loadValue();
                show("--------------------");
                show("-> toCamera");
                mPhotoSelector.toCamera();
                break;
            case R.id.image_gallery:
                loadValue();
                show("--------------------");
                show("-> toGallery");
                mPhotoSelector.toGallery();
                break;
            case R.id.image_recycle:
                mImageView.setImageResource(0);
                mPhotoSelector.recycle();
                show("--------------------");
                show("-> The last image file was recycle!");
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mPhotoSelector.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCompressStart(File srcFile) {
        mProgressBar.setVisibility(View.VISIBLE);
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(srcFile.getPath());
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
        show("[start]");
        show("srcFile=" + srcFile);
        show("degree=" + degree);
    }

    @Override
    public void onCompressComplete(File srcFile, File outFile) {
        mProgressBar.setVisibility(View.GONE);
        show("[complete]");
        show("srcFile=" + srcFile);
        show("outFile=" + outFile);
    }

    @Override
    public boolean onImageResult(File file) {
        if (file != null) {
            show("[result]");
            show("file=" + file);
            mImageView.setImageURI(Uri.fromFile(file));
        }
        return false;
    }
}