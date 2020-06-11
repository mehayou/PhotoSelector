# 相机相册照片选择器
[![API](https://img.shields.io/badge/API-15%2B-brightgreen.svg)](https://android-arsenal.com/api?level=15)
[![License](https://img.shields.io/badge/license-Apache%202-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![](https://img.shields.io/github/release/mehayou/photoselector.svg?color=red)](https://github.com/mehayou/PhotoSelector/releases)

#### 介绍
调用系统相机相册，拍摄照片、选取图片，进行裁剪、压缩处理图片。
* 简单、实用、方便
* 适配已知系统版本问题（API15~30）
* 系统相册媒体库显示刷新（拍照、裁剪、压缩后的图片）
* 异步处理压缩图片
* 校正拍照后图片角度被旋转

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
* Android6.0（即API23）及以上时，需要手动申请相机、读写权限，示例如下，请自行完善：
```java
ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
```
* Android10（即API29）及以上时，需要申明访问公共外部存储权限，在AndroidManifest.xml中，application节点下添加：
```xml
android:requestLegacyExternalStorage="true"
```

## 使用方法
#### 更改默认参数进行实例化（以下配置都是默认参数）：
```java
PhotoSelector selector = new PhotoSelector.Builder(this, this)
        .setRequestCode(81, 82, 80) // Activity回调请求码，互不相等才有效
        .setRecycle(true, true, true) // 是否回收拍照、裁剪、压缩图片
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
        .build();
```
注：建议分辨率大一些，文件小一些，图片更清晰

#### 重写Activity/Fragment回调方法
```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    selector.onActivityResult(requestCode, resultCode, data);
}
```

#### 调用相机相册
调用前，需获取相机以及读写权限
```java
// 去拍照
selector.toCamera();
// 去相册
selector.toGallery();
```

## 方法说明
#### 方法
```java
// 去拍照
selector.toCamera();
// 去相册
selector.toGallery();
// 回收拍照、裁剪、压缩后的图片文件（相册原图片文件保留）
selector.recycle();
// 是否正在压缩图片
selector.isCompressing();
```

#### 回调
结果回调
```java
new PhotoSelector.ResultCallback() {
    /**
     * 结果回调
     * @param bytes 图片文件字节
     */
    @Override
    public void onImageResult(byte[] bytes) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        imageView.setImageBitmap(bitmap);
    }
};
```
压缩回调
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

## 版本说明
#### v1.0.1
防止拍照、裁剪、压缩图片生成占用存储空间，新增回收机制，“无感”获取图片。
* 新增配置是否回收拍照、裁剪、压缩图片
* 修改默认不开启压缩
* 修改压缩回调单独配置
* 修改结果回调返回byte字节数组
