package com.ayu.camerax;

import androidx.camera.core.AspectRatio;

public class CameraHelper {

    public static final long ANIMATION_FAST_MILLIS = 50;
    public static final long ANIMATION_SLOW_MILLIS = 100;
    public static double RATIO_4_3_VALUE = 4.0 / 3.0;
    public static double RATIO_16_9_VALUE = 16.0 / 9.0;

    /**
     *  [androidx.camera.core.ImageAnalysis.Builder] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    public static int aspectRatio(int width, int height) {
        double maxValue = Math.max(width, height) + 0.0;
        double previewRatio = maxValue /  Math.min(width, height);
        if (Math.abs(previewRatio - RATIO_4_3_VALUE) <= Math.abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

}
