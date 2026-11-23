package com.example.pocketguitar.util;

import android.app.Activity;
import android.util.DisplayMetrics;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/* JADX INFO: loaded from: classes4.dex */
public final class ScreenAdapt {
    private static final float DESIGN_WIDTH = 960.0f;

    private ScreenAdapt() {
    }

    public static void adapt(Activity activity) {
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        float targetDensity = dm.widthPixels / DESIGN_WIDTH;
        if (targetDensity <= 0.0f) {
            return;
        }
        float fontScaleRatio = dm.scaledDensity / dm.density;
        dm.density = targetDensity;
        dm.scaledDensity = targetDensity * fontScaleRatio;
        dm.densityDpi = (int) (160.0f * targetDensity);
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(2);
    }
}
