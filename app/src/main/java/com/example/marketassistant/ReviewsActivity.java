package com.example.marketassistant;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 评价详情页面
 */
public class ReviewsActivity extends AppCompatActivity {

    private TextView tvMarketName;
    private TextView tvOverallRating;
    private RecyclerView recyclerView;
    private ReviewAdapter reviewAdapter;
    private Market market;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews);

        // 获取传递的超市数据
        market = (Market) getIntent().getSerializableExtra("market");

        if (market == null) {
            finish();
            return;
        }

        // 初始化视图
        tvMarketName = findViewById(R.id.tv_market_name);
        tvOverallRating = findViewById(R.id.tv_overall_rating);
        recyclerView = findViewById(R.id.recycler_view_reviews);

        // 设置数据
        tvMarketName.setText(market.getName());
        tvOverallRating.setText(String.format("综合评分: %.1f | 新鲜度: %.1f",
                market.getRating(), market.getFreshnessRating()));

        // 设置评价列表
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewAdapter = new ReviewAdapter(this, market.getReviews());
        recyclerView.setAdapter(reviewAdapter);

        // 设置返回按钮
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("用户评价");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
