package com.example.pocketguitar;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.example.pocketguitar.model.ChordItem;
import com.example.pocketguitar.util.ChordImageManager;
import com.example.pocketguitar.util.ScreenAdapt;
import com.example.pocketguitar.util.SharedChordManager;
import com.example.pocketguitar.util.TabNavigator;
import com.example.pocketguitar.util.ToastUtil;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes5.dex */
public class ChordShowActivity extends AppCompatActivity implements SharedChordManager.ChordListChangeListener, SharedChordManager.CurrentSelectChordChangeListener {
    private Button addChordButton;
    private ChordImageManager chordImageManager;
    private RecyclerView chordSuffixList;
    private ViewPager2 chordViewPager;
    private ChordPagerAdapter pagerAdapter;
    private CardView rootSelectorCard;
    private SelectedChordAdapter selectedChordAdapter;
    private TextView selectedRootText;
    private SharedChordManager sharedChordManager;
    private ChordSuffixAdapter suffixAdapter;
    private final String TAG = "ChordShowActivity";
    private String currentSelectRoot = "C";
    private String currentSelectChordType = "Major";
    private final List<String> chordSuffixes = new ArrayList();
    private List<String> rootNotes = new ArrayList();

    interface OnItemClickListener {
        void onItemClick(int i);
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenAdapt.adapt(this);
        setContentView(R.layout.activity_chord_show);
        this.sharedChordManager = SharedChordManager.getInstance();
        this.sharedChordManager.initialize(getApplicationContext());
        this.sharedChordManager.addChangeListener(this);
        this.sharedChordManager.addCurrentSelectChordListener(this);
        this.currentSelectRoot = this.sharedChordManager.getCurrentSelectRoot();
        this.currentSelectChordType = this.sharedChordManager.getCurrentSelectChordType();
        this.chordImageManager = ChordImageManager.getInstance(this);
        loadRootNotes();
        loadChordSuffixes();
        initializeViews();
        setupListeners();
    }

    private void loadRootNotes() {
        this.rootNotes = this.chordImageManager.getAvailableRoots();
    }

    private void loadChordSuffixes() {
        this.chordSuffixes.clear();
        boolean loadedFromJson = false;
        try {
            JSONObject chordsData = this.chordImageManager.getChordsData();
            if (chordsData != null && chordsData.has("chords")) {
                JSONObject chords = chordsData.getJSONObject("chords");
                Iterator<String> keys = chords.keys();
                while (keys.hasNext()) {
                    String suffix = keys.next();
                    JSONObject suffixObj = chords.getJSONObject(suffix);
                    if (suffixObj.has(this.currentSelectRoot)) {
                        this.chordSuffixes.add(suffix);
                    }
                }
                if (!this.chordSuffixes.isEmpty()) {
                    loadedFromJson = true;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (!loadedFromJson) {
            try {
                List<String> availableTypes = this.chordImageManager.getAvailableChordTypes(this.currentSelectRoot);
                if (availableTypes != null && !availableTypes.isEmpty()) {
                    this.chordSuffixes.addAll(availableTypes);
                    loadedFromJson = true;
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        if (!loadedFromJson) {
            this.chordSuffixes.add("Major");
            this.chordSuffixes.add("Minor");
            this.chordSuffixes.add("Power");
            this.chordSuffixes.add("7");
            this.chordSuffixes.add("maj7");
            this.chordSuffixes.add("m7");
            this.chordSuffixes.add("sus4");
            this.chordSuffixes.add("sus2");
            this.chordSuffixes.add("aug");
            this.chordSuffixes.add("dim");
            this.chordSuffixes.add("dim7");
            this.chordSuffixes.add("11");
            this.chordSuffixes.add("9");
            this.chordSuffixes.add("7b5");
            this.chordSuffixes.add("7b9");
            this.chordSuffixes.add("7#5");
            this.chordSuffixes.add("7#9");
        }
        if (!this.chordSuffixes.contains(this.currentSelectChordType)) {
            this.currentSelectChordType = this.chordSuffixes.get(0);
            if (this.sharedChordManager != null) {
                this.sharedChordManager.setCurrentChord(this.currentSelectRoot, this.currentSelectChordType);
            }
        }
    }

    private void initializeViews() {
        this.selectedRootText = (TextView) findViewById(R.id.selected_root_text);
        this.chordSuffixList = (RecyclerView) findViewById(R.id.chord_suffix_list);
        this.chordViewPager = (ViewPager2) findViewById(R.id.chord_view_pager);
        RecyclerView selectedChordsRecycler = (RecyclerView) findViewById(R.id.selected_chords_recycler);
        this.rootSelectorCard = (CardView) findViewById(R.id.root_selector_card);
        this.addChordButton = (Button) findViewById(R.id.add_chord_button);
        this.selectedRootText.setText(this.currentSelectRoot.replace("b", "♭").replace("#", "♯"));
        this.chordSuffixList.setLayoutManager(new LinearLayoutManager(this));
        this.suffixAdapter = new ChordSuffixAdapter(this.chordSuffixes, new OnItemClickListener() { // from class: com.example.pocketguitar.ChordShowActivity$$ExternalSyntheticLambda2
            @Override // com.example.pocketguitar.ChordShowActivity.OnItemClickListener
            public final void onItemClick(int i) {
                ChordShowActivity.this.m73x2d77660e(i);
            }
        });
        int initialPosition = this.chordSuffixes.indexOf(this.currentSelectChordType);
        if (initialPosition < 0) {
            initialPosition = 0;
        }
        this.suffixAdapter.setSelectedPosition(initialPosition);
        this.chordSuffixList.setAdapter(this.suffixAdapter);
        this.pagerAdapter = new ChordPagerAdapter();
        this.chordViewPager.setAdapter(this.pagerAdapter);
        this.chordViewPager.setCurrentItem(initialPosition, false);
        LinearLayoutManager horizontalLayoutManger = new LinearLayoutManager(this, 0, false);
        selectedChordsRecycler.setLayoutManager(horizontalLayoutManger);
        this.selectedChordAdapter = new SelectedChordAdapter();
        selectedChordsRecycler.setAdapter(this.selectedChordAdapter);
        this.chordImageManager.preloadRootChords(this.currentSelectRoot);
    }

    /* JADX INFO: renamed from: lambda$initializeViews$0$com-example-guitardemo-ChordShowActivity, reason: not valid java name */
    /* synthetic */ void m73x2d77660e(int position) {
        this.currentSelectChordType = this.chordSuffixes.get(position);
        updateChordViewPager();
        this.suffixAdapter.setSelectedPosition(position);
        this.sharedChordManager.setCurrentChord(this.currentSelectRoot, this.currentSelectChordType);
    }

    private void setupListeners() {
        this.rootSelectorCard.setOnClickListener(new View.OnClickListener() { // from class: com.example.pocketguitar.ChordShowActivity$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ChordShowActivity.this.m76lambda$setupListeners$1$comexampleguitardemoChordShowActivity(view);
            }
        });
        setupTopNav();
        this.addChordButton.setOnClickListener(new View.OnClickListener() { // from class: com.example.pocketguitar.ChordShowActivity$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ChordShowActivity.this.m78lambda$setupListeners$3$comexampleguitardemoChordShowActivity(view);
            }
        });
        this.chordViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() { // from class: com.example.pocketguitar.ChordShowActivity.1
            @Override // androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
            public void onPageSelected(int position) {
                ChordShowActivity.this.currentSelectChordType = (String) ChordShowActivity.this.chordSuffixes.get(position);
                ChordShowActivity.this.suffixAdapter.setSelectedPosition(position);
                ChordShowActivity.this.chordSuffixList.scrollToPosition(position);
                ChordShowActivity.this.sharedChordManager.setCurrentChord(ChordShowActivity.this.currentSelectRoot, ChordShowActivity.this.currentSelectChordType);
            }
        });
    }

    private void setupTopNav() {
        findViewById(R.id.nav_library).setSelected(true);
        findViewById(R.id.nav_chord_panel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TabNavigator.switchTo(ChordShowActivity.this, ChordPanelActivity.class,
                        TabNavigator.TAB_LIBRARY, TabNavigator.TAB_CHORD_PANEL);
            }
        });
        findViewById(R.id.nav_fingerstyle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TabNavigator.switchTo(ChordShowActivity.this, FingerstyleActivity.class,
                        TabNavigator.TAB_LIBRARY, TabNavigator.TAB_FINGERSTYLE);
            }
        });
        findViewById(R.id.nav_compose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TabNavigator.switchTo(ChordShowActivity.this, ComposeActivity.class,
                        TabNavigator.TAB_LIBRARY, TabNavigator.TAB_COMPOSE);
            }
        });
        findViewById(R.id.nav_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishAffinity();
            }
        });
    }

    /* JADX INFO: renamed from: lambda$setupListeners$1$com-example-guitardemo-ChordShowActivity, reason: not valid java name */
    /* synthetic */ void m76lambda$setupListeners$1$comexampleguitardemoChordShowActivity(View v) {
        showRootSelectorPopup();
    }

    /* JADX INFO: renamed from: lambda$setupListeners$3$com-example-guitardemo-ChordShowActivity, reason: not valid java name */
    /* synthetic */ void m78lambda$setupListeners$3$comexampleguitardemoChordShowActivity(View v) {
        ChordItem newChord = new ChordItem(this.currentSelectRoot, this.currentSelectChordType);
        if (this.sharedChordManager.getChordList().size() >= 8) {
            ToastUtil.showRightAreaToast(this, "最多只能添加8个和弦");
            return;
        }
        try {
            if (this.sharedChordManager.addChord(newChord)) {
                ToastUtil.showRightAreaToast(this, "已添加: " + newChord.getRoot().replace("b", "♭").replace("#", "♯") + " " + newChord.getType());
            } else {
                ToastUtil.showRightAreaToast(this, "该和弦已在列表中");
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void showRootSelectorPopup() {
        View popupView = LayoutInflater.from(this).inflate(R.layout.dialog_root_selector, (ViewGroup) null);
        RecyclerView rootRecyclerView = (RecyclerView) popupView.findViewById(R.id.root_notes_recycler);
        rootRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        rootRecyclerView.setVerticalScrollBarEnabled(true);
        rootRecyclerView.setScrollbarFadingEnabled(false);
        rootRecyclerView.setScrollBarStyle(33554432);
        LinearLayout leftPanel = (LinearLayout) findViewById(R.id.left_panel);
        int width = leftPanel.getWidth() - 40;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, -2, true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.dark_surface)));
        popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);
        popupWindow.setOutsideTouchable(true);
        RootSelectorAdapter adapter = new RootSelectorAdapter(this.rootNotes, new OnItemClickListener() { // from class: com.example.pocketguitar.ChordShowActivity$$ExternalSyntheticLambda0
            @Override // com.example.pocketguitar.ChordShowActivity.OnItemClickListener
            public final void onItemClick(int i) {
                ChordShowActivity.this.m79x7df88aba(popupWindow, i);
            }
        });
        rootRecyclerView.setAdapter(adapter);
        popupWindow.showAsDropDown(this.rootSelectorCard, 0, 8);
        int[] rootCardLocation = new int[2];
        this.rootSelectorCard.getLocationOnScreen(rootCardLocation);
        Log.d("ChordShowActivity", "PopupWindow位置 (showAsDropDown): x:" + rootCardLocation[0] + " y:" + (rootCardLocation[1] + this.rootSelectorCard.getHeight() + 8) + " width:" + width);
    }

    /* JADX INFO: renamed from: lambda$showRootSelectorPopup$4$com-example-guitardemo-ChordShowActivity, reason: not valid java name */
    /* synthetic */ void m79x7df88aba(PopupWindow popupWindow, int position) {
        String newRoot = this.rootNotes.get(position);
        if (!newRoot.equals(this.currentSelectRoot)) {
            this.currentSelectRoot = newRoot;
            this.selectedRootText.setText(this.currentSelectRoot.replace("b", "♭").replace("#", "♯"));
            loadChordSuffixes();
            this.suffixAdapter.updateData(this.chordSuffixes);
            this.suffixAdapter.setSelectedPosition(0);
            this.currentSelectChordType = this.chordSuffixes.get(0);
            updateChordViewPager();
            this.sharedChordManager.setCurrentChord(this.currentSelectRoot, this.currentSelectChordType);
            this.chordImageManager.preloadRootChords(this.currentSelectRoot);
        }
        popupWindow.dismiss();
    }

    private void updateChordViewPager() {
        this.pagerAdapter.notifyDataSetChanged();
        int position = this.chordSuffixes.indexOf(this.currentSelectChordType);
        if (position >= 0) {
            this.chordViewPager.setCurrentItem(position, false);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    class ChordSuffixAdapter extends RecyclerView.Adapter<ChordSuffixAdapter.SuffixViewHolder> {
        private final OnItemClickListener listener;
        private int selectedPosition = 0;
        private List<String> suffixes;

        public ChordSuffixAdapter(List<String> suffixes, OnItemClickListener listener) {
            this.suffixes = suffixes;
            this.listener = listener;
        }

        public void updateData(List<String> newSuffixes) {
            this.suffixes = newSuffixes;
            notifyDataSetChanged();
        }

        public void setSelectedPosition(int position) {
            int oldPosition = this.selectedPosition;
            this.selectedPosition = position;
            notifyItemChanged(oldPosition);
            notifyItemChanged(this.selectedPosition);
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public SuffixViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chord_suffix, parent, false);
            return new SuffixViewHolder(view);
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public void onBindViewHolder(final SuffixViewHolder holder, int position) {
            holder.suffixText.setText(this.suffixes.get(position));
            if (position == this.selectedPosition) {
                holder.itemView.setBackgroundColor(ChordShowActivity.this.getResources().getColor(R.color.dark_card_selected));
                holder.suffixText.setTextColor(ChordShowActivity.this.getResources().getColor(R.color.dark_accent));
            } else {
                holder.itemView.setBackgroundColor(ChordShowActivity.this.getResources().getColor(R.color.dark_card));
                holder.suffixText.setTextColor(ChordShowActivity.this.getResources().getColor(R.color.dark_text_primary));
            }
            holder.itemView.setOnClickListener(new View.OnClickListener() { // from class: com.example.pocketguitar.ChordShowActivity$ChordSuffixAdapter$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    ChordSuffixAdapter.this.m80xf2061a61(holder, view);
                }
            });
        }

        /* JADX INFO: renamed from: lambda$onBindViewHolder$0$com-example-guitardemo-ChordShowActivity$ChordSuffixAdapter, reason: not valid java name */
        /* synthetic */ void m80xf2061a61(SuffixViewHolder holder, View v) {
            this.listener.onItemClick(holder.getAdapterPosition());
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public int getItemCount() {
            return this.suffixes.size();
        }

        class SuffixViewHolder extends RecyclerView.ViewHolder {
            TextView suffixText;

            SuffixViewHolder(View itemView) {
                super(itemView);
                this.suffixText = (TextView) itemView.findViewById(R.id.suffix_text);
            }
        }
    }

    private class ChordPagerAdapter extends RecyclerView.Adapter<ChordPagerAdapter.ChordPageViewHolder> {
        private ChordPagerAdapter() {
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public ChordPageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chord_image, parent, false);
            return new ChordPageViewHolder(view);
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public void onBindViewHolder(ChordPageViewHolder holder, int position) {
            String chordType = (String) ChordShowActivity.this.chordSuffixes.get(position);
            try {
                Drawable chordImage = ChordShowActivity.this.chordImageManager.getChordImage(ChordShowActivity.this.currentSelectRoot, chordType);
                if (chordImage == null) {
                    holder.chordImageView.setImageResource(android.R.drawable.ic_menu_report_image);
                } else {
                    holder.chordImageView.setImageDrawable(chordImage);
                }
            } catch (Exception e) {
                e.printStackTrace();
                holder.chordImageView.setImageResource(android.R.drawable.ic_menu_report_image);
            }
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public int getItemCount() {
            return ChordShowActivity.this.chordSuffixes.size();
        }

        class ChordPageViewHolder extends RecyclerView.ViewHolder {
            ImageView chordImageView;
            TextView chordNameText;
            TextView chordPatternText;

            ChordPageViewHolder(View itemView) {
                super(itemView);
                this.chordNameText = (TextView) itemView.findViewById(R.id.chord_name_text);
                this.chordImageView = (ImageView) itemView.findViewById(R.id.chord_image_view);
                this.chordPatternText = (TextView) itemView.findViewById(R.id.chord_pattern_text);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static class RootSelectorAdapter extends RecyclerView.Adapter<RootSelectorAdapter.RootViewHolder> {
        private final OnItemClickListener listener;
        private final List<String> roots;

        public RootSelectorAdapter(List<String> roots, OnItemClickListener listener) {
            this.roots = roots;
            this.listener = listener;
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public RootViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_root_note, parent, false);
            return new RootViewHolder(view);
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public void onBindViewHolder(final RootViewHolder holder, int position) {
            holder.textView.setText(this.roots.get(position).replace("b", "♭").replace("#", "♯"));
            holder.textView.setOnClickListener(new View.OnClickListener() { // from class: com.example.pocketguitar.ChordShowActivity$RootSelectorAdapter$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    RootSelectorAdapter.this.m81x338dfe6f(holder, view);
                }
            });
        }

        /* JADX INFO: renamed from: lambda$onBindViewHolder$0$com-example-guitardemo-ChordShowActivity$RootSelectorAdapter, reason: not valid java name */
        /* synthetic */ void m81x338dfe6f(RootViewHolder holder, View v) {
            this.listener.onItemClick(holder.getAdapterPosition());
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public int getItemCount() {
            return this.roots.size();
        }

        static class RootViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            RootViewHolder(View itemView) {
                super(itemView);
                this.textView = (TextView) itemView.findViewById(R.id.root_text);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    class SelectedChordAdapter extends RecyclerView.Adapter<SelectedChordAdapter.SelectedChordViewHolder> {
        private SelectedChordAdapter() {
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public SelectedChordViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selected_chord, parent, false);
            return new SelectedChordViewHolder(view);
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public void onBindViewHolder(final SelectedChordViewHolder holder, int position) {
            List<ChordItem> chords = ChordShowActivity.this.sharedChordManager.getChordList();
            if (position >= chords.size()) {
                return;
            }
            ChordItem chord = chords.get(position);
            try {
                Drawable chordImage = ChordShowActivity.this.chordImageManager.getChordImage(chord.getRoot(), chord.getType());
                if (chordImage == null) {
                    holder.selectedChordImage.setImageResource(android.R.drawable.ic_menu_report_image);
                } else {
                    holder.selectedChordImage.setImageDrawable(chordImage);
                }
            } catch (Exception e) {
                e.printStackTrace();
                holder.selectedChordImage.setImageResource(android.R.drawable.ic_menu_report_image);
            }
            holder.removeButton.setOnClickListener(new View.OnClickListener() { // from class: com.example.pocketguitar.ChordShowActivity$SelectedChordAdapter$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    SelectedChordAdapter.this.m82x6293446d(holder, view);
                }
            });
        }

        /* JADX INFO: renamed from: lambda$onBindViewHolder$0$com-example-guitardemo-ChordShowActivity$SelectedChordAdapter, reason: not valid java name */
        /* synthetic */ void m82x6293446d(SelectedChordViewHolder holder, View v) {
            int pos = holder.getAdapterPosition();
            if (pos != -1) {
                ChordShowActivity.this.sharedChordManager.removeChord(pos);
            }
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public int getItemCount() {
            return ChordShowActivity.this.sharedChordManager.getChordList().size();
        }

        class SelectedChordViewHolder extends RecyclerView.ViewHolder {
            ImageButton removeButton;
            ImageView selectedChordImage;
            TextView selectedChordName;

            SelectedChordViewHolder(View itemView) {
                super(itemView);
                this.selectedChordImage = (ImageView) itemView.findViewById(R.id.selected_chord_image);
                this.selectedChordName = (TextView) itemView.findViewById(R.id.selected_chord_name);
                this.removeButton = (ImageButton) itemView.findViewById(R.id.remove_chord_button);
            }
        }
    }

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onDestroy() {
        this.sharedChordManager.removeChangeListener(this);
        this.sharedChordManager.removeCurrentChordListener(this);
        super.onDestroy();
    }

    @Override // com.example.pocketguitar.util.SharedChordManager.ChordListChangeListener
    public void onChordListChanged(List<ChordItem> chordList) {
        runOnUiThread(new Runnable() { // from class: com.example.pocketguitar.ChordShowActivity$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                ChordShowActivity.this.m74xef693434();
            }
        });
    }

    /* JADX INFO: renamed from: lambda$onChordListChanged$5$com-example-guitardemo-ChordShowActivity, reason: not valid java name */
    /* synthetic */ void m74xef693434() {
        this.selectedChordAdapter.notifyDataSetChanged();
    }

    @Override // com.example.pocketguitar.util.SharedChordManager.CurrentSelectChordChangeListener
    public void onCurrentSelectChordChanged(final String root, final String type) {
        if (!root.equals(this.currentSelectRoot) || !type.equals(this.currentSelectChordType)) {
            runOnUiThread(new Runnable() { // from class: com.example.pocketguitar.ChordShowActivity$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    ChordShowActivity.this.m75x3be9b1f8(root, type);
                }
            });
        }
    }

    /* JADX INFO: renamed from: lambda$onCurrentSelectChordChanged$6$com-example-guitardemo-ChordShowActivity, reason: not valid java name */
    /* synthetic */ void m75x3be9b1f8(String root, String type) {
        this.currentSelectRoot = root;
        this.currentSelectChordType = type;
        this.selectedRootText.setText(this.currentSelectRoot.replace("b", "♭").replace("#", "♯"));
        loadChordSuffixes();
        this.suffixAdapter.updateData(this.chordSuffixes);
        int position = this.chordSuffixes.indexOf(this.currentSelectChordType);
        if (position >= 0) {
            this.suffixAdapter.setSelectedPosition(position);
            this.chordSuffixList.scrollToPosition(position);
            this.chordViewPager.setCurrentItem(position, false);
        }
        this.chordImageManager.preloadRootChords(this.currentSelectRoot);
    }
}
