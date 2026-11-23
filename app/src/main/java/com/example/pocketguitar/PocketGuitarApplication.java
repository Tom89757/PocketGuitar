package com.example.pocketguitar;

import android.app.Application;
import com.example.pocketguitar.util.ChordImageManager;
import com.example.pocketguitar.util.SharedChordManager;

/* JADX INFO: loaded from: classes5.dex */
public class PocketGuitarApplication extends Application {
    @Override // android.app.Application
    public void onCreate() {
        super.onCreate();
        SharedChordManager.getInstance().initialize(getApplicationContext());
        ChordImageManager.initialize(this);
    }
}
