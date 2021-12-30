package com.ayu.camerax;

import android.annotation.SuppressLint;

import java.io.File;
import java.text.SimpleDateFormat;

public class ImageUtils {

    @SuppressLint("SimpleDateFormat")
    public static File createImageFile(File baseFolder) {
        return new File(baseFolder, new SimpleDateFormat("yyyyMMddHHmmss").format(System.currentTimeMillis()) + ".jpg");
    }
}
