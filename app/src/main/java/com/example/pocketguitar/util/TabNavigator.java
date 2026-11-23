package com.example.pocketguitar.util;

import android.app.Activity;
import android.content.Intent;
import com.example.pocketguitar.R;

/**
 * 三个功能页签（和弦面板 / 指弹面板 / 和弦库）之间的切换。
 * 按页签顺序做左右滑动过渡：切到右侧页签从右滑入，切到左侧页签从左滑入。
 */
public final class TabNavigator {

    public static final int TAB_CHORD_PANEL = 0;
    public static final int TAB_FINGERSTYLE = 1;
    public static final int TAB_LIBRARY = 2;
    public static final int TAB_COMPOSE = 3;

    private TabNavigator() {
    }

    public static void switchTo(Activity activity, Class<?> target, int fromTab, int toTab) {
        if (fromTab == toTab) {
            return;
        }
        Intent intent = new Intent(activity, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        activity.startActivity(intent);
        if (toTab > fromTab) {
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } else {
            activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }
}
