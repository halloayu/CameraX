package com.ayu.camerax;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CameraActivity extends AppCompatActivity {

    public static final String TAG = "CameraActivity";

    private Context mContext;
    private FrameLayout mPreviewFrame;
    private PreviewView mCameraViewBind; // 预览界面
    private ImageView mCameraExit; // 退出拍照
    private TakePhotoButton mCameraCapture; // 拍照
    private ImageView mCameraFlip; // 翻转摄像头

    private Camera mCamera;

    private DisplayManager displayManager;
    private ExecutorService cameraExecutor;

    private int displayId = -1;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private Preview preview;
    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalyzer;
    private ProcessCameraProvider cameraProvider;

    private final DisplayManager.DisplayListener displayListener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {
        }

        @Override
        public void onDisplayRemoved(int displayId) {
        }

        @SuppressLint("UnsafeOptInUsageError")
        @Override
        public void onDisplayChanged(int displayId) {
            if (displayId == CameraActivity.this.displayId) {
                if (preview != null) {
                    preview.setTargetRotation(getWindow().getDecorView().getDisplay().getRotation());
                }
                if (imageCapture != null) {
                    imageCapture.setTargetRotation(getWindow().getDecorView().getDisplay().getRotation());
                }

                if (imageAnalyzer != null) {
                    imageAnalyzer.setTargetRotation(getWindow().getDecorView().getDisplay().getRotation());
                }
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mContext = getApplicationContext();
        initView();
        initListener();

        cameraExecutor = Executors.newSingleThreadExecutor();
        displayManager = (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
        displayManager.registerDisplayListener(displayListener, null);

        mCameraViewBind.post(() -> {
            displayId = mCameraViewBind.getDisplay().getDisplayId();
            setUpCamera();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        displayManager.unregisterDisplayListener(displayListener);
    }

    private void initView() {
        mPreviewFrame = findViewById(R.id.camerax_preview_frame);
        mCameraViewBind = findViewById(R.id.camerax_view_bind);
        mCameraExit = findViewById(R.id.camerax_iv_close);
        mCameraCapture = findViewById(R.id.camera_capture);
        mCameraFlip = findViewById(R.id.camerax_iv_flip);
    }

    private void initListener() {

        mCameraCapture.setOnClickListener(view -> {
            if (imageCapture != null) {
                // 创建文件放图片数据
                File temp = ImageUtils.createImageFile(mContext.getExternalFilesDir("images"));
                ImageCapture.OutputFileOptions outputFileOptions =
                        new ImageCapture.OutputFileOptions.Builder(temp).build();
                imageCapture.takePicture(outputFileOptions, cameraExecutor,
                        new ImageCapture.OnImageSavedCallback() {
                            @Override
                            public void onImageSaved(@Nullable ImageCapture.OutputFileResults outputFileResults) {
                                // insert your code here.
                                Log.e(TAG, "" + outputFileResults.getSavedUri());
                                Intent i = new Intent();
                                i.putExtra("path", temp.getAbsolutePath());
                                setResult(Activity.RESULT_OK, i);
                                finish();
                            }

                            @Override
                            public void onError(@NonNull ImageCaptureException error) {
                                // insert your code here.
                            }
                        }
                );

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Display flash animation to indicate that photo was captured
                    mPreviewFrame.postDelayed(() -> {
                        mPreviewFrame.setForeground(new ColorDrawable(Color.BLACK));
                        mPreviewFrame.postDelayed(() ->
                                mPreviewFrame.setForeground(null), CameraHelper.ANIMATION_SLOW_MILLIS);
                    }, CameraHelper.ANIMATION_FAST_MILLIS);
                }
            }
        });

        mCameraFlip.setOnClickListener(view -> {
            if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                lensFacing = CameraSelector.LENS_FACING_BACK;
            } else {
                lensFacing = CameraSelector.LENS_FACING_FRONT;
            }
            try {
                bindCameraUseCases();
            } catch (Exception e) {
                // Do nothing
            }
        });

        // 退出拍照
        mCameraExit.setOnClickListener(v -> finish());

        // 聚焦
        mCameraViewBind.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // 如果是正面，不用对焦
                if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    return false;
                }

                CameraControl cameraControl = mCamera.getCameraControl();
                DisplayMetrics mDisplayMetrics = mContext.getResources().getDisplayMetrics();
                int screenWidth = mDisplayMetrics.widthPixels;
                int screenHeight = mDisplayMetrics.heightPixels;

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    float sRawX = event.getRawX();
                    float sRawY = event.getRawY();

                    MeteringPointFactory factory = new SurfaceOrientedMeteringPointFactory(screenWidth, screenHeight);
                    MeteringPoint point = factory.createPoint(sRawX, sRawY);
                    MeteringPoint point2 = factory.createPoint(sRawX + 10, sRawY + 10);
                    FocusMeteringAction action = new FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                            .addPoint(point2, FocusMeteringAction.FLAG_AE) // could have many
                            // auto calling cancelFocusAndMetering in 5 seconds
                            .setAutoCancelDuration(5, TimeUnit.SECONDS)
                            .build();

                    ListenableFuture future = cameraControl.startFocusAndMetering(action);
                    future.addListener(() -> {
                        try {
                            FocusMeteringResult result = (FocusMeteringResult) future.get();
                            // 聚焦结果
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage());
                        }
                    }, cameraExecutor);
                }
                return true;
            }
        });
    }

    private void setUpCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(mContext);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                // 默认打开后置摄像头
                if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    lensFacing = CameraSelector.LENS_FACING_BACK;
                } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    lensFacing = CameraSelector.LENS_FACING_FRONT;
                }

                bindCameraUseCases();
            } catch (CameraInfoUnavailableException | InterruptedException | ExecutionException e) {
                Log.e(TAG, e.getMessage());
            }
        }, ContextCompat.getMainExecutor(mContext));
    }

    /**
     * 更新相机参数
     */
    @SuppressLint({"UnsafeOptInUsageError", "ResourceType"})
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

        int flashMode = ImageCapture.FLASH_MODE_OFF;
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
}
