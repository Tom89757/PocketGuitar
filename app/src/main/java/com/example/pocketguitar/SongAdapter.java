package com.example.pocketguitar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pocketguitar.music.SongMeta;
import java.util.ArrayList;
import java.util.List;

/** 曲谱列表的 RecyclerView 适配器。 */
public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {

    public interface Listener {
        void onOpen(SongMeta meta, int position);
        void onPlayChords(SongMeta meta, int position);
        void onPlayMelody(SongMeta meta, int position);
        void onExport(SongMeta meta, int position);
        void onDelete(SongMeta meta, int position);
    }

    private final List<SongMeta> items = new ArrayList<>();
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<SongMeta> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    public List<SongMeta> getItems() {
        return items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SongMeta meta = items.get(position);
        holder.tvTitle.setText(meta.title != null ? meta.title : "未命名");

        StringBuilder info = new StringBuilder();
        if (meta.key != null && !meta.key.isEmpty()) {
            info.append(meta.key).append("调");
        }
        if (meta.bpm > 0) {
            if (info.length() > 0) info.append("  ·  ");
            info.append("BPM ").append(meta.bpm);
        }
        if (meta.savedAt != null && !meta.savedAt.isEmpty()) {
            if (info.length() > 0) info.append("  ·  ");
            info.append(meta.savedAt);
        }
        holder.tvInfo.setText(info);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOpen(meta, holder.getAdapterPosition());
        });
        holder.btnPlayChords.setOnClickListener(v -> {
            if (listener != null) listener.onPlayChords(meta, holder.getAdapterPosition());
        });
        holder.btnPlayMelody.setOnClickListener(v -> {
            if (listener != null) listener.onPlayMelody(meta, holder.getAdapterPosition());
        });
        holder.btnExport.setOnClickListener(v -> {
            if (listener != null) listener.onExport(meta, holder.getAdapterPosition());
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(meta, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvInfo;
        final TextView btnPlayChords;
        final TextView btnPlayMelody;
        final TextView btnExport;
        final TextView btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvInfo = itemView.findViewById(R.id.tv_info);
            btnPlayChords = itemView.findViewById(R.id.btn_play_chords);
            btnPlayMelody = itemView.findViewById(R.id.btn_play_melody);
            btnExport = itemView.findViewById(R.id.btn_export);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
