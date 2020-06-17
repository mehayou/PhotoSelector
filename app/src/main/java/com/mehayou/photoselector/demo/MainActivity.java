package com.mehayou.photoselector.demo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
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
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener, CompoundButton.OnCheckedChangeListener,
        PhotoSelector.CompressCallback,
        PhotoSelector.PermissionCallback,
        PhotoSelector.ResultCallback<File> {

    private ImageView mImageView;
    private ProgressBar mProgressBar;

    private CheckBox cbRecycleCamera;
    private CheckBox cbRecycleCrop;
    private CheckBox cbRecycleCompress;

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
    private int maxSize = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        cbRecycleCamera = findViewById(R.id.cb_recycle_camera);
        cbRecycleCrop = findViewById(R.id.cb_recycle_crop);
        cbRecycleCompress = findViewById(R.id.cb_recycle_compress);

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
        mBuilder.setPermissionCallback(this);
        mPhotoSelector = mBuilder.build();

        cbRecycleCamera.setOnCheckedChangeListener(this);
        cbRecycleCrop.setOnCheckedChangeListener(this);
        cbRecycleCompress.setOnCheckedChangeListener(this);
        cbFormat.setOnCheckedChangeListener(this);
        cbCompress.setOnCheckedChangeListener(this);
        cbCrop.setOnCheckedChangeListener(this);
        cbRecycleCamera.setChecked(true);
        cbRecycleCrop.setChecked(true);
        cbRecycleCompress.setChecked(true);
        cbFormat.setChecked(true);
        cbCompress.setChecked(false);
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
        mBuilder.setCompressCallback(isChecked ? this : null);
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
            case R.id.cb_recycle_camera:
            case R.id.cb_recycle_crop:
            case R.id.cb_recycle_compress:
                mBuilder.setRecycle(cbRecycleCamera.isChecked(), cbRecycleCrop.isChecked(), cbRecycleCompress.isChecked());
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

    public void showDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setTitle("授权失败");
        dialog.setMessage("相机存储权限无法正常使用，是否前往应用设置开启权限？");
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity activity = MainActivity.this;
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
                        activity.startActivity(intent);
                    }
                });
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mPhotoSelector.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mPhotoSelector.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onPermissionRationale(List<String> permissions) {
        showDialog();
        show("[Permission] request denied - " + permissions.toString()
                .replace("[", "")
                .replace("]", "")
                .replace(" ", ""));
    }

    @Override
    public void onCompressStart() {
        mProgressBar.setVisibility(View.VISIBLE);
        show("[compress] Start");
    }

    @Override
    public void onCompressComplete(boolean compress) {
        mProgressBar.setVisibility(View.GONE);
        show("[compress] Complete=" + compress);
    }

    @Override
    public void onImageResult(File file) {
        if (file != null) {
            show("[result]");
            show("file=" + file.getAbsolutePath());
//            show("bytes=" + bytes.length);
//            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//            mImageView.setImageBitmap(bitmap);
        }
    }
}