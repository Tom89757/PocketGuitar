package com.example.pocketguitar.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.example.pocketguitar.R;

/**
 * 底部品位拖动条：slider_bg 作为轨道，slider_knob 作为当前位置的半透明蒙版。
 */
public class FretSliderView extends View {

    private static final float SRC_BG_W = 577f;
    private static final float SRC_BG_H = 58f;
    private static final float SRC_KNOB_W = 269f;

    private final Bitmap track;
    private final Bitmap knob;
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

    // 默认停在最右侧（显示最低的 7 个品位）
    private float fraction = 1f;
    private OnFractionChangeListener listener;

    public interface OnFractionChangeListener {
        void onFractionChanged(float fraction);
    }

    public FretSliderView(Context context) {
        this(context, null);
    }

    public FretSliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        track = BitmapFactory.decodeResource(getResources(), R.drawable.solo_slider_bg);
        knob = BitmapFactory.decodeResource(getResources(), R.drawable.solo_slider_knob);
    }

    public void setOnFractionChangeListener(OnFractionChangeListener listener) {
        this.listener = listener;
    }

    public float getFraction() {
        return fraction;
    }

    public void setFraction(float fraction) {
        float value = Math.max(0f, Math.min(1f, fraction));
        if (Math.abs(value - this.fraction) < 0.0001f) {
            return;
        }
        this.fraction = value;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = Math.round(width * SRC_BG_H / SRC_BG_W);
        setMeasuredDimension(width, height);
    }

    private float knobWidth() {
        return getWidth() * SRC_KNOB_W / SRC_BG_W;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Rect trackRect = new Rect(0, 0, getWidth(), getHeight());
        canvas.drawBitmap(track, null, trackRect, bitmapPaint);

        float knobW = knobWidth();
        float left = fraction * (getWidth() - knobW);
        Rect knobRect = new Rect(Math.round(left), 0, Math.round(left + knobW), getHeight());
        canvas.drawBitmap(knob, null, knobRect, bitmapPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                updateFraction(event.getX());
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void updateFraction(float x) {
        float travel = getWidth() - knobWidth();
        if (travel <= 0f) {
            return;
        }
        float value = Math.max(0f, Math.min(1f, (x - knobWidth() / 2f) / travel));
        if (Math.abs(value - fraction) < 0.0001f) {
            return;
        }
        fraction = value;
        if (listener != null) {
            listener.onFractionChanged(fraction);
        }
        invalidate();
    }
}
