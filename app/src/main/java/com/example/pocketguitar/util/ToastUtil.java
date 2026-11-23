package com.example.pocketguitar.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import java.util.Objects;

/* JADX INFO: loaded from: classes4.dex */
public class ToastUtil {
    public static void showCustomToast(final Context context, String message, double xPositionRatio) {
        TextView messageView = new TextView(context);
        messageView.setText(message);
        messageView.setPadding(30, 20, 30, 20);
        messageView.setTextColor(ViewCompat.MEASURED_STATE_MASK);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(15.0f);
        shape.setColor(Color.parseColor("#CCFFFFFF"));
        messageView.setBackground(shape);
        final PopupWindow popupWindow = new PopupWindow(messageView, -2, -2);
        messageView.measure(View.MeasureSpec.makeMeasureSpec(0, 0), View.MeasureSpec.makeMeasureSpec(0, 0));
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int targetX = (int) (((double) screenWidth) * xPositionRatio);
        final int targetX2 = targetX - (messageView.getMeasuredWidth() / 2);
        new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: com.example.pocketguitar.util.ToastUtil$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                ToastUtil.lambda$showCustomToast$0(context, popupWindow, targetX2);
            }
        });
    }

    static /* synthetic */ void lambda$showCustomToast$0(Context context, final PopupWindow popupWindow, int finalTargetX) {
        try {
            View decorView = ((Activity) context).getWindow().getDecorView();
            popupWindow.showAtLocation(decorView, 0, finalTargetX, 1250);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Handler handler = new Handler(Looper.getMainLooper());
        Objects.requireNonNull(popupWindow);
        handler.postDelayed(new Runnable() { // from class: com.example.pocketguitar.util.ToastUtil$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                popupWindow.dismiss();
            }
        }, 1000L);
    }

    public static void showCenterToast(Context context, String message) {
        showCustomToast(context, message, 0.5d);
    }

    public static void showRightAreaToast(Context context, String message) {
        showCustomToast(context, message, 0.624d);
    }
}
