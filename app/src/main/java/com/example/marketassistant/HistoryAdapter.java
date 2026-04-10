package com.example.marketassistant;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

/**
 * 历史记录列表适配器
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final Context context;
    private final List<DetectionRecord> recordList;
    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;

    public interface OnItemClickListener {
        void onItemClick(DetectionRecord record);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(DetectionRecord record, int position);
    }

    public HistoryAdapter(Context context, List<DetectionRecord> recordList) {
        this.context = context;
        this.recordList = recordList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        DetectionRecord record = recordList.get(position);

        holder.imgThumbnail.setImageDrawable(null);
        String thumbnailPath = record.getThumbnailPath();
        if (thumbnailPath != null && new File(thumbnailPath).exists()) {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                Bitmap thumbnail = BitmapFactory.decodeFile(thumbnailPath, options);
                if (thumbnail != null) {
                    holder.imgThumbnail.setImageBitmap(thumbnail);
                } else {
                    holder.imgThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            } catch (OutOfMemoryError e) {
                holder.imgThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            holder.imgThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.tvFruitType.setText(record.getFruitType());
        holder.tvLevel.setText(record.getLevel());
        holder.tvLevel.setTextColor(record.getLevelColor());
        holder.tvLevel.setBackgroundColor(record.getLevelBgColor());
        holder.tvScore.setText(String.valueOf(record.getScore()));
        holder.tvScore.setTextColor(record.getScoreColor());
        holder.tvTime.setText(record.getFormattedTime());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clickListener != null) {
                    clickListener.onItemClick(record);
                }
            }
        });

        // 长按事件
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (longClickListener != null) {
                    longClickListener.onItemLongClick(record, holder.getAdapterPosition());
                }
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return recordList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumbnail;
        TextView tvFruitType;
        TextView tvLevel;
        TextView tvScore;
        TextView tvTime;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumbnail = itemView.findViewById(R.id.img_thumbnail);
            tvFruitType = itemView.findViewById(R.id.tv_fruit_type);
            tvLevel = itemView.findViewById(R.id.tv_level);
            tvScore = itemView.findViewById(R.id.tv_score);
            tvTime = itemView.findViewById(R.id.tv_time);
        }
    }
}
