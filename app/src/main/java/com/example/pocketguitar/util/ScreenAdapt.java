package com.example.pocketguitar.util;

import android.app.Activity;
import android.util.DisplayMetrics;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/* JADX INFO: loaded from: classes4.dex */
public final class ScreenAdapt {
    private static final float DESIGN_WIDTH = 960.0f;
    /**
     * 首次适配时确定的密度。
     * 改写 DisplayMetrics 的 densityDpi 后，系统会按比例重算 widthPixels，
     * 若每次都拿 widthPixels 重新算密度会不断漂移（页面间 dp 尺寸不一致），
     * 因此只计算一次并复用。
     */
    private static float targetDensity = 0f;

    private ScreenAdapt() {
    }

    public static void adapt(Activity activity) {
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        if (targetDensity <= 0f) {
            float width = Math.max(dm.widthPixels, dm.heightPixels);
            float candidate = width / DESIGN_WIDTH;
            if (candidate <= 0.0f) {
                return;
            }
            targetDensity = candidate;
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
