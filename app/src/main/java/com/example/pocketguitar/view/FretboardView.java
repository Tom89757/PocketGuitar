package com.example.pocketguitar.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.example.pocketguitar.R;

/**
 * 指弹指板：用 solo 素材横向拼接品位图。
 * left.png 为木板左端（琴颈末端，不响应按压），right.png 为最右一格（含琴枕），
 * 中间品位在 lab.png（带记号点）与 pattern.png 之间切换。
 */
public class FretboardView extends View {

    private static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    // 6弦..1弦 空弦音 MIDI 值
    private static final int[] OPEN_MIDI = {40, 45, 50, 55, 59, 64};

    // 素材原始尺寸与关键位置
    private static final float SRC_TILE_W = 130f;
    private static final float SRC_TILE_H = 551f;
    private static final float SRC_LEFT_W = 225f;
    private static final float SRC_LEFT_TILE_H = 640f;
    private static final float SRC_WOOD_LEFT = 180f;
    // left.png 中首弦位置比中间品位图低 43.5px
    private static final float SRC_LEFT_STRING_OFFSET = 43.5f;
    // 中间品位图中 6 根弦的中心 y（6弦在上、1弦在下）
    private static final float[] SRC_STRING_Y = {43f, 135.5f, 230f, 325f, 418.5f, 513f};

    // 一屏显示 7 个可按压的品格
    private static final int SPACE_COUNT = 7;
    public static final int MIN_BASE_FRET = 1;
    public static final int MAX_BASE_FRET = 14;
    // 带品位记号点的品位
    private static final int[] INLAY_FRETS = {3, 5, 7, 9, 12, 15, 17, 19};
    // 音符显示后自动消失的时间
    private static final long NOTE_HIDE_DELAY_MS = 700L;
    // 音名提示色：C D E F G A B
    private static final int[] NOTE_COLORS = {
            0xFFE53935, 0xFFFB8C00, 0xFF43A047, 0xFF00ACC1, 0xFF1E88E5, 0xFF8E24AA, 0xFFD81B60
    };
    // 晕开的同心圆动画时长
    private static final long RIPPLE_DURATION_MS = 400L;
    // 晕开的最大倍数
    private static final float RIPPLE_MAX_SCALE = 0.7f;

    private Bitmap tileLeft;
    private Bitmap tileRight;
    private Bitmap tileLab;
    private Bitmap tilePattern;

    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgeStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float scaleY = 1f;
    private float boardTop;
    private float boardBottom;
    private float fretSpaceW = 1f;
    // 木板左端（left.png 的品丝）位置，也是第一个可按压品格格的左边界
    private float firstFretLeft;

    // 窗口最低品位（最右侧那一格）；最左侧可按压格为 baseFret + SPACE_COUNT - 1
    private int baseFret = MIN_BASE_FRET;
    private int selectedString = -1;
    private int selectedFret = -1;

    // 同心圆晕开进度 0~1（>=1 表示没有动画）
    private float rippleProgress = 2f;
    private ValueAnimator rippleAnimator;

    private final Runnable hideNoteRunnable = new Runnable() {
        @Override
        public void run() {
            selectedString = -1;
            selectedFret = -1;
            stopRipple();
            invalidate();
        }
    };

    private OnNotePressedListener listener;

    public interface OnNotePressedListener {
        void onNotePressed(int stringIndex, int fret, String noteName, String noteWithOctave);
    }

    public FretboardView(Context context) {
        this(context, null);
    }

    public FretboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        tileLeft = BitmapFactory.decodeResource(getResources(), R.drawable.solo_left);
        tileRight = BitmapFactory.decodeResource(getResources(), R.drawable.solo_right);
        tileLab = BitmapFactory.decodeResource(getResources(), R.drawable.solo_lab);
        tilePattern = BitmapFactory.decodeResource(getResources(), R.drawable.solo_pattern);
        initPaints();
    }

    private void initPaints() {
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setFakeBoldText(true);
        labelPaint.setShadowLayer(dp(3), 0, dp(1), 0xB3000000);

        badgePaint.setColor(NOTE_COLORS[0]);

        badgeStrokePaint.setStyle(Paint.Style.STROKE);
        badgeStrokePaint.setColor(0x66000000);
        badgeStrokePaint.setStrokeWidth(dp(2));

        badgeTextPaint.setColor(Color.WHITE);
        badgeTextPaint.setTextAlign(Paint.Align.CENTER);
        badgeTextPaint.setFakeBoldText(true);

        ripplePaint.setStyle(Paint.Style.STROKE);
        ripplePaint.setStrokeWidth(dp(2));
    }

    public void setBaseFret(int baseFret) {
        int value = Math.max(MIN_BASE_FRET, Math.min(MAX_BASE_FRET, baseFret));
        if (value == this.baseFret) {
            return;
        }
        this.baseFret = value;
        selectedString = -1;
        selectedFret = -1;
        invalidate();
    }

    public int getBaseFret() {
        return baseFret;
    }

    public void setOnNotePressedListener(OnNotePressedListener listener) {
        this.listener = listener;
    }

    /** 外部触发：高亮某个弦/品，随后自动消失。 */
    public void showNote(int stringIndex, int fret) {
        selectedString = stringIndex;
        selectedFret = fret;
        removeCallbacks(hideNoteRunnable);
        postDelayed(hideNoteRunnable, NOTE_HIDE_DELAY_MS);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 木板左端留出背景图音孔的位置；剩余宽度 = 木板左端 + 7 个等宽品格
        float woodLeft = w * 0.15f;
        float boardWidth = w - woodLeft;
        float capRatio = (SRC_LEFT_W - SRC_WOOD_LEFT) / SRC_TILE_W;
        fretSpaceW = boardWidth / (SPACE_COUNT + capRatio);
        firstFretLeft = woodLeft + (SRC_LEFT_W - SRC_WOOD_LEFT) * (fretSpaceW / SRC_TILE_W);

        // 顶部给功能图标让位，底部给拖动条让位
        float boardHeight = h * 0.74f;
        scaleY = boardHeight / SRC_TILE_H;
        boardTop = h * 0.10f;
        boardBottom = boardTop + boardHeight;
    }

    private float stringY(int stringIndex) {
        return boardTop + SRC_STRING_Y[stringIndex] * scaleY;
    }

    /** 某格对应的品位：最左为最高品位 */
    private int fretOfSpace(int space) {
        return baseFret + (SPACE_COUNT - 1 - space);
    }

    private float spaceLeft(int space) {
        return firstFretLeft + space * fretSpaceW;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawTiles(canvas);
        drawFretNumbers(canvas);
        drawSelectedNote(canvas);
    }

    private void drawTiles(Canvas canvas) {
        int top = Math.round(boardTop);
        int bottom = Math.round(boardTop + SRC_TILE_H * scaleY);

        // 木板左端：按素材原始比例绘制（左侧透明区只保留琴弦）
        float scaleX = fretSpaceW / SRC_TILE_W;
        float capTop = boardTop - SRC_LEFT_STRING_OFFSET * scaleY;
        Rect capDst = new Rect(
                Math.round(firstFretLeft - SRC_LEFT_W * scaleX),
                Math.round(capTop),
                Math.round(firstFretLeft),
                Math.round(capTop + SRC_LEFT_TILE_H * scaleY));
        canvas.drawBitmap(tileLeft, null, capDst, bitmapPaint);

        // 7 个可按压品格
        for (int s = 0; s < SPACE_COUNT; s++) {
            Bitmap tile;
            if (s == SPACE_COUNT - 1) {
                tile = tileRight;
            } else {
                tile = hasInlay(fretOfSpace(s)) ? tileLab : tilePattern;
            }
            Rect dst = new Rect(Math.round(spaceLeft(s)), top,
                    Math.round(spaceLeft(s + 1)), bottom);
            canvas.drawBitmap(tile, null, dst, bitmapPaint);
        }
    }

    private boolean hasInlay(int fret) {
        for (int value : INLAY_FRETS) {
            if (value == fret) {
                return true;
            }
        }
        return false;
    }

    /** 顶部品位序号：小字号，居中于两根品柱之间 */
    private void drawFretNumbers(Canvas canvas) {
        float gap = SRC_STRING_Y[0] * scaleY;
        labelPaint.setTextSize(gap * 0.46f);
        float baseline = boardTop + gap / 2f - (labelPaint.ascent() + labelPaint.descent()) / 2f;
        for (int s = 0; s < SPACE_COUNT; s++) {
            float cx = spaceLeft(s) + fretSpaceW / 2f;
            canvas.drawText(String.valueOf(fretOfSpace(s)), cx, baseline, labelPaint);
        }
    }

    /** 按下的音符：居中于两品之间，落在所按琴弦上，并带一圈晕开的同心圆 */
    private void drawSelectedNote(Canvas canvas) {
        if (selectedString < 0 || selectedFret < 0) {
            return;
        }
        int space = SPACE_COUNT - 1 - (selectedFret - baseFret);
        if (space < 0 || space >= SPACE_COUNT) {
            return;
        }
        float cx = spaceLeft(space) + fretSpaceW / 2f;
        float cy = stringY(selectedString);
        float rowH = (SRC_STRING_Y[1] - SRC_STRING_Y[0]) * scaleY;
        float radius = Math.min(fretSpaceW * 0.3f, rowH * 0.34f);

        String note = noteName(selectedString, selectedFret);
        int color = colorOfNote(note);

        // 晕开的同心圆（两圈，第二圈延后出现，扩到最大即消失）
        for (int i = 0; i < 2; i++) {
            float progress = rippleProgress - i * 0.3f;
            if (progress <= 0f || progress >= 1f) {
                continue;
            }
            int alpha = (int) (160 * (1f - progress * progress));
            ripplePaint.setColor((alpha << 24) | (color & 0xFFFFFF));
            ripplePaint.setStrokeWidth(dp(2) + dp(1.5f) * (1f - progress));
            canvas.drawCircle(cx, cy, radius * (1f + progress * RIPPLE_MAX_SCALE), ripplePaint);
        }

        badgePaint.setColor(color);
        canvas.drawCircle(cx, cy, radius, badgePaint);
        canvas.drawCircle(cx, cy, radius, badgeStrokePaint);

        badgeTextPaint.setTextSize(radius * 0.95f);
        float baseline = cy - (badgeTextPaint.ascent() + badgeTextPaint.descent()) / 2f;
        canvas.drawText(displayNote(note), cx, baseline, badgeTextPaint);
    }

    /** 音名对应提示色，升号沿用其基础音名的颜色 */
    private int colorOfNote(String note) {
        switch (note.charAt(0)) {
            case 'D':
                return NOTE_COLORS[1];
            case 'E':
                return NOTE_COLORS[2];
            case 'F':
                return NOTE_COLORS[3];
            case 'G':
                return NOTE_COLORS[4];
            case 'A':
                return NOTE_COLORS[5];
            case 'B':
                return NOTE_COLORS[6];
            default:
                return NOTE_COLORS[0];
        }
    }

    private void startRipple() {
        stopRipple();
        rippleAnimator = ValueAnimator.ofFloat(0f, 1f);
        rippleAnimator.setDuration(RIPPLE_DURATION_MS);
        rippleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                rippleProgress = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        rippleAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                // 扩到最大后立即全部消失，避免残留一圈浅色圆
                rippleProgress = 2f;
                invalidate();
            }
        });
        rippleAnimator.start();
    }

    private void stopRipple() {
        if (rippleAnimator != null) {
            rippleAnimator.cancel();
            rippleAnimator = null;
        }
        rippleProgress = 2f;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                handleTouch(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void handleTouch(float x, float y) {
        // 只响应指板区域、且只响应品柱之间的 7 个格子
        if (y < boardTop || y > boardBottom || x < firstFretLeft) {
            return;
        }
        int space = (int) ((x - firstFretLeft) / fretSpaceW);
        space = Math.max(0, Math.min(SPACE_COUNT - 1, space));

        int string = nearestString(y);
        int fret = fretOfSpace(space);
        if (string == selectedString && fret == selectedFret) {
            return;
        }
        selectedString = string;
        selectedFret = fret;
        if (listener != null) {
            listener.onNotePressed(string, fret, noteName(string, fret), noteWithOctave(string, fret));
        }
        removeCallbacks(hideNoteRunnable);
        postDelayed(hideNoteRunnable, NOTE_HIDE_DELAY_MS);
        startRipple();
        invalidate();
    }

    private int nearestString(float y) {
        int best = 0;
        float bestDistance = Float.MAX_VALUE;
        for (int i = 0; i < 6; i++) {
            float distance = Math.abs(y - stringY(i));
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(hideNoteRunnable);
        stopRipple();
        super.onDetachedFromWindow();
    }

    private int midiOf(int stringIndex, int fret) {
        return OPEN_MIDI[stringIndex] + fret;
    }

    private String noteName(int stringIndex, int fret) {
        return NOTE_NAMES[midiOf(stringIndex, fret) % 12];
    }

    private String noteWithOctave(int stringIndex, int fret) {
        int midi = midiOf(stringIndex, fret);
        return NOTE_NAMES[midi % 12] + (midi / 12 - 1);
    }

    private String displayNote(String note) {
        return note.replace("#", "♯");
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
