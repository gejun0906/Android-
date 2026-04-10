package com.example.marketassistant;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * 超市列表适配器
 */
public class MarketAdapter extends RecyclerView.Adapter<MarketAdapter.MarketViewHolder> {

    private Context context;
    private List<Market> marketList;
    private OnMarketClickListener listener;

    public interface OnMarketClickListener {
        void onMarketClick(Market market);
    }

    public MarketAdapter(Context context, List<Market> marketList, OnMarketClickListener listener) {
        this.context = context;
        this.marketList = marketList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MarketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_market, parent, false);
        return new MarketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MarketViewHolder holder, int position) {
        Market market = marketList.get(position);

        holder.tvName.setText(market.getName());
        holder.tvAddress.setText(market.getAddress());
        holder.tvStatus.setText(market.getStatus());
        holder.tvBusinessHours.setText(market.getBusinessHours());
        holder.tvDescription.setText(market.getDescription());
        holder.tvReviewCount.setText(market.getReviewCount() + "条评价");

        // 设置评分
        holder.ratingBar.setRating(market.getRating());
        holder.tvRating.setText(String.format("%.1f", market.getRating()));

        // 设置新鲜度评分
        holder.tvFreshnessRating.setText(String.format("新鲜度: %.1f", market.getFreshnessRating()));

        // 高评分商户标记
        if (market.isHighFreshnessRating()) {
            holder.imgHighRating.setVisibility(View.VISIBLE);
            holder.tvHighRatingBadge.setVisibility(View.VISIBLE);
            holder.cardView.setCardBackgroundColor(context.getResources().getColor(android.R.color.holo_green_light, null));
        } else {
            holder.imgHighRating.setVisibility(View.GONE);
            holder.tvHighRatingBadge.setVisibility(View.GONE);
            holder.cardView.setCardBackgroundColor(context.getResources().getColor(android.R.color.white, null));
        }

        // 点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMarketClick(market);
            }
        });

        // 查看评价按钮
        holder.tvViewReviews.setOnClickListener(v -> {
            Intent intent = new Intent(context, ReviewsActivity.class);
            intent.putExtra("market", market);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return marketList.size();
    }

    static class MarketViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvName;
        TextView tvAddress;
        TextView tvStatus;
        TextView tvBusinessHours;
        TextView tvDescription;
        TextView tvRating;
        TextView tvFreshnessRating;
        TextView tvReviewCount;
        TextView tvViewReviews;
        TextView tvHighRatingBadge;
        RatingBar ratingBar;
        ImageView imgHighRating;

        public MarketViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_view);
            tvName = itemView.findViewById(R.id.tv_market_name);
            tvAddress = itemView.findViewById(R.id.tv_address);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvBusinessHours = itemView.findViewById(R.id.tv_business_hours);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvRating = itemView.findViewById(R.id.tv_rating);
            tvFreshnessRating = itemView.findViewById(R.id.tv_freshness_rating);
            tvReviewCount = itemView.findViewById(R.id.tv_review_count);
            tvViewReviews = itemView.findViewById(R.id.tv_view_reviews);
            tvHighRatingBadge = itemView.findViewById(R.id.tv_high_rating_badge);
            ratingBar = itemView.findViewById(R.id.rating_bar);
            imgHighRating = itemView.findViewById(R.id.img_high_rating);
        }
    }
}
