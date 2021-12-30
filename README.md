# CameraX

## 1 简述
CameraX 是一个 Jetpack 支持库，简化了相机应用的开发工作。提供一致且易用的 API 接口，适用于大多数 Android 设备，并可向后兼容至 Android 5.0（API 级别 21）。<br />
另外，还解决了设备兼容性问题，节省了开发者的大量兼容工作。
## 2 流程
使用CameraX开发相机比起Camera2方便很多，大概步骤可以分为四步，配置 -> 预览 -> 分析图片 -> 拍摄图片。<br />
具体可参考 [官方文档](https://developer.android.google.cn/training/camerax)
## 3 示例
> 1、获取相机 cameraProvider

```java
ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(mContext);
cameraProviderFuture.addListener(() -> {
    try {
        cameraProvider = cameraProviderFuture.get();
        // 绑定相机用例
        bindCameraUseCases();
    } catch (CameraInfoUnavailableException | InterruptedException | ExecutionException e) {
        Log.e(TAG, e.getMessage());
    }
}, ContextCompat.getMainExecutor(mContext));
```
> 2、配置预览 <br />
> 3、配置拍摄参数 <br />
> 4、配置图片分析器 <br />
> 5、将这些配置绑定到相机的生命周期 <br />

```java
private void bindCameraUseCases() {

    DisplayMetrics metrics = new DisplayMetrics();
    mCameraViewBind.getDisplay().getRealMetrics(metrics);
    int screenAspectRatio = CameraHelper.aspectRatio(metrics.widthPixels, metrics.heightPixels);

    int rotation = mCameraViewBind.getDisplay().getRotation();

    CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

    preview = new Preview.Builder()
        .setTargetAspectRatio(screenAspectRatio)
        .setTargetRotation(rotation)
        .build();

    imageCapture = new ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY) // 快速拍 or 高质量图片
        .setTargetAspectRatio(screenAspectRatio)
        .setTargetRotation(rotation)
        .setFlashMode(flashMode)
        .build();

    imageAnalyzer = new ImageAnalysis.Builder()
        .setTargetAspectRatio(screenAspectRatio)
        .setTargetRotation(rotation)
        .build();

    UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
        .addUseCase(preview)
        .addUseCase(imageAnalyzer)
        .addUseCase(imageCapture)
        .build();
    // 可设置相机分析器imageAnalyzer.setAnalyzer();
    // Make sure that there are no other use cases bound to CameraX
    cameraProvider.unbindAll();

    try {
        mCamera = cameraProvider.bindToLifecycle(this, cameraSelector, useCaseGroup);
        preview.setSurfaceProvider(mCameraViewBind.getSurfaceProvider());
    } catch (Exception e) {
        Log.e(TAG, "Use case binding failed", e);
    }
}
```
> 6、拍照，得到图片结果

```java
public void onClick() {
    ImageCapture.OutputFileOptions outputFileOptions =
        new ImageCapture.OutputFileOptions.Builder(new File(...)).build();
    imageCapture.takePicture(outputFileOptions, cameraExecutor,
                             new ImageCapture.OnImageSavedCallback() {
                                 @Override
                                 public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                                     // insert your code here.
                                 }
                                 @Override
                                 public void onError(ImageCaptureException error) {
                                     // insert your code here.
                                 }
                             }
                            );
}
```
​

[官方示例](https://github.com/android/camera-samples/tree/main/CameraXBasic) （kotlin版本）<br />
[demo](https://github.com/halloayu/CameraX.git) （Java版本）<br />​


