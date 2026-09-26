package com.example.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.app.database.SessionEntity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SessionAdapter extends ListAdapter<SessionEntity, SessionAdapter.SessionViewHolder> {

    public SessionAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<SessionEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<SessionEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull SessionEntity oldItem, @NonNull SessionEntity newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull SessionEntity oldItem, @NonNull SessionEntity newItem) {
                    return oldItem.equals(newItem);
                }
            };

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        SessionEntity session = getItem(position);
        holder.bind(session);
    }

    static class SessionViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTimestamp;
        private final TextView tvDuration;
        private final TextView tvDominantActivity;
        private final TextView tvAvgConfidence;
        private final TextView tvPredictionCount;
        private final TextView tvAvgLatency;

        SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvDominantActivity = itemView.findViewById(R.id.tvDominantActivity);
            tvAvgConfidence = itemView.findViewById(R.id.tvAvgConfidence);
            tvPredictionCount = itemView.findViewById(R.id.tvPredictionCount);
            tvAvgLatency = itemView.findViewById(R.id.tvAvgLatency);
        }

        void bind(SessionEntity session) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.getDefault());
            String dateStr = sdf.format(new Date(session.getTimestamp()));
            if (tvTimestamp != null) tvTimestamp.setText(dateStr);
            if (tvDominantActivity != null) tvDominantActivity.setText(session.getDominantActivity());

            long sec = session.getDurationMs() / 1000;
            String durationStr = (sec >= 60) ? (sec / 60) + "m " + (sec % 60) + "s" : sec + "s";
            if (tvDuration != null) tvDuration.setText("Duration: " + durationStr);

            if (tvAvgConfidence != null) tvAvgConfidence.setText(String.format(Locale.getDefault(), "Confidence: %.1f%%", session.getAvgConfidence()));
            if (tvPredictionCount != null) tvPredictionCount.setText("Predictions: " + session.getPredictionCount());
            if (tvAvgLatency != null) tvAvgLatency.setText(String.format(Locale.getDefault(), "Latency: %.1f ms", session.getAvgLatency()));

            itemView.setOnClickListener(v -> {
                try {
                    Navigation.findNavController(v).navigate(R.id.historyDetailFragment);
                } catch (Exception ignored) {
                }
            });
        }
    }
}
