package com.example.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app.database.SessionEntity;
import com.example.app.R;

public class SessionAdapter extends ListAdapter<SessionEntity, SessionAdapter.SessionViewHolder> {

    public SessionAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<SessionEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<SessionEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull SessionEntity oldItem, @NonNull SessionEntity newItem) {
                    return oldItem.getId() == newItem.getId();
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
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateStr = sdf.format(new java.util.Date(session.getTimestamp()));
            tvTimestamp.setText(dateStr);
            tvDuration.setText("Duration: " + session.getDurationMs() + " ms");
            tvDominantActivity.setText("Activity: " + session.getDominantActivity());
            tvAvgConfidence.setText("Avg Confidence: " + String.format("%.2f", session.getAvgConfidence()));
            tvPredictionCount.setText("Predictions: " + session.getPredictionCount());
            tvAvgLatency.setText("Avg Latency: " + session.getAvgLatency() + " ns");
        }
    }
}
