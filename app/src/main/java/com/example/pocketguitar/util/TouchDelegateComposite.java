package com.example.pocketguitar.util;

import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes4.dex */
public class TouchDelegateComposite extends TouchDelegate {
    private final ArrayList<TouchDelegate> delegates;

    public TouchDelegateComposite(Rect bounds, View delegateView) {
        super(bounds, delegateView);
        this.delegates = new ArrayList<>();
    }

    public void addDelegate(TouchDelegate delegate) {
        this.delegates.add(delegate);
    }

    @Override // android.view.TouchDelegate
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = false;
        float x = event.getX();
        float y = event.getY();
        for (TouchDelegate delegate : this.delegates) {
            event.setLocation(x, y);
            handled = delegate.onTouchEvent(event) || handled;
        }
        return handled;
    }
}
