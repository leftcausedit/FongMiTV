package com.fongmi.android.tv.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

public class BitmapUtil {

    /*
pixelSpacing tells how many pixels to skip each pixel.
If pixelSpacing > 1: the average color is an estimate, but higher values mean better performance
If pixelSpacing == 1: the average color will be the real average
If pixelSpacing < 1: the method will most likely crash (don't use values below 1)
*/
    public static int getBitmapAverageColor(Bitmap bitmap, int pixelSpacing) {
        int R = 0; int G = 0; int B = 0;
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        int n = 0;
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int i = 0; i < pixels.length; i += pixelSpacing) {
            int color = pixels[i];
            R += Color.red(color);
            G += Color.green(color);
            B += Color.blue(color);
            n++;
        }
        return Color.rgb(R / n, G / n, B / n);
    }
}
