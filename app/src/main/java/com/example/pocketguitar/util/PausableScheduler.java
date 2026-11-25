package com.example.pocketguitar.util;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.util.ArrayList;
import java.util.List;

/** 可暂停/继续的定时任务队列，用于 AI 曲谱的自动弹奏。 */
public class PausableScheduler {

    public interface OnFinishListener {
        void onFinished();
    }

    private static final class Task {
        long remainingMs;
        long postedAt = -1;
        final Runnable action;

        Task(long delayMs, Runnable action) {
            this.remainingMs = delayMs;
            this.action = action;
        }
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Task> tasks = new ArrayList<>();
    private OnFinishListener finishListener;
    private boolean paused;

    public void setOnFinishListener(OnFinishListener listener) {
        this.finishListener = listener;
    }

    public void schedule(long delayMs, Runnable action) {
        Task task = new Task(Math.max(0, delayMs), action);
        tasks.add(task);
        if (!paused) {
            post(task);
        }
    }

    private void post(final Task task) {
        task.postedAt = SystemClock.uptimeMillis();
        handler.postDelayed(() -> {
            tasks.remove(task);
            task.postedAt = -1;
            task.action.run();
            if (tasks.isEmpty() && finishListener != null) {
                finishListener.onFinished();
            }
        }, task.remainingMs);
    }

    public void pause() {
        if (paused) {
            return;
        }
        paused = true;
        handler.removeCallbacksAndMessages(null);
        long now = SystemClock.uptimeMillis();
        for (Task task : tasks) {
            if (task.postedAt >= 0) {
                task.remainingMs = Math.max(0, task.remainingMs - (now - task.postedAt));
                task.postedAt = -1;
            }
        }
    }

    public void resume() {
        if (!paused) {
            return;
        }
        paused = false;
        for (Task task : tasks) {
            post(task);
        }
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isActive() {
        return !tasks.isEmpty();
    }

    public void clear() {
        paused = false;
        handler.removeCallbacksAndMessages(null);
        tasks.clear();
    }
}
