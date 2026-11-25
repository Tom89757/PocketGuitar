package com.example.pocketguitar;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pocketguitar.music.MelodyNote;
import com.example.pocketguitar.music.NoteMapper;
import com.example.pocketguitar.music.Song;
import com.example.pocketguitar.music.SongPlaybackManager;
import com.example.pocketguitar.util.PausableScheduler;
import com.example.pocketguitar.util.ScreenAdapt;
import com.example.pocketguitar.util.StringPlayer;
import com.example.pocketguitar.util.TabNavigator;
import com.example.pocketguitar.view.FretSliderView;
import com.example.pocketguitar.view.FretboardView;

public class FingerstyleActivity extends AppCompatActivity {

    private FretboardView fretboardView;
    private FretSliderView fretSlider;
    private StringPlayer stringPlayer;
    private TextView btnPausePlayback;
    private PausableScheduler melodyScheduler;
    // 自动弹奏前的指板窗口，弹奏结束后恢复
    private int savedBaseFret = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenAdapt.adapt(this);
        setContentView(R.layout.activity_fingerstyle);

        fretboardView = findViewById(R.id.fretboard_view);
        fretSlider = findViewById(R.id.fret_slider);

        stringPlayer = new StringPlayer(this);

        btnPausePlayback = findViewById(R.id.btn_pause_playback);
        melodyScheduler = new PausableScheduler();
        melodyScheduler.setOnFinishListener(() -> {
            btnPausePlayback.setText("暂停");
            btnPausePlayback.setVisibility(View.GONE);
            restoreFretWindow();
        });
        btnPausePlayback.setOnClickListener(v -> togglePlaybackPause());

        fretboardView.setOnNotePressedListener(new FretboardView.OnNotePressedListener() {
            @Override
            public void onNotePressed(int stringIndex, int fret, String noteName, String noteWithOctave) {
                stringPlayer.playString(stringIndex, fret);
            }
        });

        // 拖动底部品位条切换显示区间：滑到最右为最低品位
        fretSlider.setOnFractionChangeListener(new FretSliderView.OnFractionChangeListener() {
            @Override
            public void onFractionChanged(float fraction) {
                int range = FretboardView.MAX_BASE_FRET - FretboardView.MIN_BASE_FRET;
                fretboardView.setBaseFret(FretboardView.MAX_BASE_FRET - Math.round(fraction * range));
            }
        });

        setupTopNav();
    }

    private void setupTopNav() {
        findViewById(R.id.nav_fingerstyle).setSelected(true);
        findViewById(R.id.nav_chord_panel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TabNavigator.switchTo(FingerstyleActivity.this, ChordPanelActivity.class,
                        TabNavigator.TAB_FINGERSTYLE, TabNavigator.TAB_CHORD_PANEL);
            }
        });
        findViewById(R.id.nav_library).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TabNavigator.switchTo(FingerstyleActivity.this, ChordShowActivity.class,
                        TabNavigator.TAB_FINGERSTYLE, TabNavigator.TAB_LIBRARY);
            }
        });
        findViewById(R.id.nav_compose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TabNavigator.switchTo(FingerstyleActivity.this, ComposeActivity.class,
                        TabNavigator.TAB_FINGERSTYLE, TabNavigator.TAB_COMPOSE);
            }
        });
        findViewById(R.id.nav_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishAffinity();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        SongPlaybackManager manager = SongPlaybackManager.getInstance();
        if (manager.takeMode() == SongPlaybackManager.MODE_MELODY) {
            Song song = manager.song();
            if (song != null) {
                playComposedMelody(song);
            }
        }
    }

    private void playComposedMelody(Song song) {
        if (song.melody == null || song.melody.isEmpty()) {
            return;
        }
        melodyScheduler.clear();
        btnPausePlayback.setText("暂停");
        savedBaseFret = fretboardView.getBaseFret();
        int beatMs = song.bpm > 0 ? 60000 / song.bpm : 500;
        for (MelodyNote note : song.melody) {
            if (note == null) {
                continue;
            }
            int midi = NoteMapper.parsePitch(note.pitch);
            if (midi < 0) {
                continue;
            }
            long delay = (long) ((note.bar - 1) * 4 * beatMs + (note.beat - 1) * beatMs);
            if (delay < 0) {
                delay = 0;
            }
            // 播放时再定位：优先使用当前可见品格的按弦位置，超出范围则自动滚动指板
            melodyScheduler.schedule(delay, () -> {
                int[] pos = fretboardView.showNoteForMidi(midi);
                if (pos == null) {
                    return;
                }
                stringPlayer.playString(pos[0], pos[1]);
                syncFretSlider();
            });
        }
        btnPausePlayback.setVisibility(melodyScheduler.isActive() ? View.VISIBLE : View.GONE);
    }

    /** 指板窗口变化后同步底部拖动条位置。 */
    private void syncFretSlider() {
        int range = FretboardView.MAX_BASE_FRET - FretboardView.MIN_BASE_FRET;
        float fraction = (FretboardView.MAX_BASE_FRET - fretboardView.getBaseFret()) / (float) range;
        fretSlider.setFraction(fraction);
    }

    /** 自动弹奏结束后恢复用户手动选择的指板窗口。 */
    private void restoreFretWindow() {
        if (savedBaseFret < 0) {
            return;
        }
        fretboardView.setBaseFret(savedBaseFret);
        syncFretSlider();
        savedBaseFret = -1;
    }

    private void togglePlaybackPause() {
        if (!melodyScheduler.isActive()) {
            return;
        }
        if (melodyScheduler.isPaused()) {
            melodyScheduler.resume();
            btnPausePlayback.setText("暂停");
        } else {
            melodyScheduler.pause();
            btnPausePlayback.setText("继续");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 离开页面时若自动弹奏尚未结束，直接中止并恢复指板窗口，
        // 避免切回后窗口停留在自动滚动后的高品位（表现为升 key）。
        if (melodyScheduler != null && melodyScheduler.isActive()) {
            melodyScheduler.clear();
            if (btnPausePlayback != null) {
                btnPausePlayback.setText("暂停");
                btnPausePlayback.setVisibility(View.GONE);
            }
            restoreFretWindow();
        }
    }

    @Override
    protected void onDestroy() {
        if (melodyScheduler != null) {
            melodyScheduler.clear();
        }
        if (stringPlayer != null) {
            stringPlayer.release();
        }
        super.onDestroy();
    }
}
