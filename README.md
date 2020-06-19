# 相机相册照片选择器
[![API](https://img.shields.io/badge/API-16%2B-brightgreen.svg)](https://android-arsenal.com/api?level=16)
[![License](https://img.shields.io/badge/license-Apache%202-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![](https://img.shields.io/github/release/mehayou/photoselector.svg?color=red)](https://github.com/mehayou/PhotoSelector/releases)

#### 介绍
调用系统相机相册，拍摄照片、选取图片，进行裁剪、压缩处理图片。
* 简单、实用、方便
* 适配已知系统版本问题（API16~30）
* 无需手动申请动态权限
* 校正拍照后图片角度被旋转
* 异步处理图片压缩、结果回调
* 系统相册媒体库显示刷新（拍照、裁剪、压缩后的图片）
* 多种回调结果类型可选（Flie、byte[]、Bitmap、Drawable、String）

## 依赖
参照最终版本名称替换下面的“releases”
#### Gradle
```java
implementation 'com.mehayou:photoselector:releases'
```
#### Maven
```
<dependency>
  <groupId>com.mehayou</groupId>
  <artifactId>photoselector</artifactId>
  <version>releases</version>
  <type>pom</type>
</dependency>
```

## 权限申请
* 在AndroidManifest.xml中添加申明相机、读写权限，如下：
```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.CAMERA" />
```
* Android6.0（即API23）及以上时，需要手动申请相机、读写权限，从1.0.2版本开始无需申请，以下版本请自行完善，示例如下：
```java
ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
```
* Android10（即API29）及以上时，需要申明访问公共外部存储权限，在AndroidManifest.xml中，application节点下添加：
```xml
android:requestLegacyExternalStorage="true"
```

## 使用说明
#### 更改默认参数进行实例化（以下配置都是默认参数）：
```java
PhotoSelector selector = new PhotoSelector.Builder(this)
        .setRequestCode(81, 82, 80) // 结果回调、权限申请回调共用的请求码，互不相等才有效
        .setPermissionCallback(null) // 权限回调，用户选择不再询问被拒，弹窗提示
        .setRecycle(true, true, true) // 是否回收拍照、裁剪、压缩图片（返回结果类型为File时，最终生成的图片不会自动被回收）
        .setCompress(false) // 是否开启压缩功能
        .setCompressCallback(null) // 压缩回调监听
        .setCompressImageSize(1080) // 压缩图片分辨率大小，为0则不压缩，单位px
        .setCompressFileSize(200 << 10) // 压缩文件大小，为0则不压缩，单位byte
        .setCompressFormat(Bitmap.CompressFormat.JPEG) // 压缩输出格式，PNG不支持压缩文件大小
        .setCrop(false) // 是否开启裁剪功能
        .setCropAspect(1, 1) // 裁剪比例，小于1则默认为1
        .setCropOutput(0, 0) // 强制裁剪图片分辨率大小，为0则不裁剪，注：使用后setCropAspect、setCompressImageSize方法失效
        .setFileNameFormat("yyyymmddhhmmssSSS") // 输出文件名称格式，若格式不正确或为空，则默认取时间戳
        .setDirectory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)) // 输出目录
        .create(this); // 结果回调，默认回调结果类型为File，可用ResultFactory选择其他类型
```
注：建议分辨率大一些，文件小一些，图片更清晰

#### 重写Activity/Fragment回调方法
结果回调
```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    selector.onActivityResult(requestCode, resultCode, data);
}
```
权限申请回调
```java
@Override
public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    selector.onRequestPermissionsResult(requestCode, permissions, grantResults);
}
```

#### 调用相机相册
调用前，需获取相机以及读写权限
```java
// 去拍照，会自行执行权限申请
selector.toCamera();
// 去相册，会自行执行权限申请
selector.toGallery();
```

## 方法以及回调
#### 方法
```java
// 去拍照，会自行执行权限申请
selector.toCamera();
// 去相册，会自行执行权限申请
selector.toGallery();
// 回收拍照、裁剪、压缩后的图片文件（不涉及相册原文件）
selector.recycle();
// 是否正在压缩图片
selector.isCompressing();
// 处理结果回调
selector.onActivityResult(requestCode, resultCode, data);
// 处理权限回调
selector.onRequestPermissionsResult(requestCode, permissions, grantResults);
```

#### 结果回调
```java
new PhotoSelector.ResultCallback<File>() {
    /**
     * 结果回调
     * @param file 图片文件
     */
    @Override
    public void onImageResult(File file) {
        imageView.setImageURI(Uri.fromFile(file));
    }
};
```
结果回调可选类型
```java
// 返回文件
ResultFactory.callbackFile(this);
// 返回字节数组
ResultFactory.callbackBytes(this);
// 返回Base64
ResultFactory.callbackBase64(this);
// 返回Bitmap
ResultFactory.callbackBitmap(this);
// 返回Drawable
ResultFactory.callbackDrawable(this);
```

#### 压缩回调
```java
new PhotoSelector.CompressCallback() {
    /**
     * 压缩开始
     */
    @Override
    public void onCompressStart() {
        //loading start
    }

    /**
     * 压缩完成
     * @param compress 是否进行了压缩（存在直出，没有进行压缩）
     */
    @Override
    public void onCompressComplete(boolean compress) {
        //loading complete
    }
};
```

#### 权限回调
```java
new PhotoSelector.PermissionCallback() {
    /**
     * 授权失败
     * @param permissions 授权被拒且不再询问的权限
     */
    @Override
    public void onPermissionRationale(List<String> permissions) {
        // TODO 弹窗提示
    }
};
```
## 版本说明
#### v1.0.2
无需手动申请权限，多种类型随意选择。
* 提升最小API至16
* 新增申请权限，无需手动申请
* 新增回调结果多种类型可选（Flie、byte[]、Bitmap、Drawable、String）
#### v1.0.1
防止拍照、裁剪、压缩图片生成占用存储空间，新增回收机制，“无感”获取图片。
* 新增配置是否回收拍照、裁剪、压缩图片
* 修改默认不开启压缩
* 修改压缩回调单独配置
* 修改结果回调返回byte字节数组
