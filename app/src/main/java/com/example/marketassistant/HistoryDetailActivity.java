package com.example.marketassistant;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

/**
 * 检测历史详情页面
 */
public class HistoryDetailActivity extends AppCompatActivity {

    private DetectionRecord record;
    private DetectionDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.history_detail_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        dbHelper = new DetectionDbHelper(this);
        record = (DetectionRecord) getIntent().getSerializableExtra("record");

        if (record == null) {
            finish();
            return;
        }

        displayRecord();
        setupDeleteButton();
    }

    private void displayRecord() {
        // 加载图片
        ImageView imgDetail = findViewById(R.id.img_detail);
        String path = record.getThumbnailPath();
        if (path != null && new File(path).exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            if (bitmap != null) {
                imgDetail.setImageBitmap(bitmap);
            }
        }

        // 水果类型
        TextView tvFruitType = findViewById(R.id.tv_fruit_type);
        tvFruitType.setText(record.getFruitType());

        // 分数
        TextView tvScore = findViewById(R.id.tv_freshness_score);
        tvScore.setText(String.valueOf(record.getScore()));
        tvScore.setTextColor(record.getScoreColor());

        // 等级
        TextView tvLevel = findViewById(R.id.tv_freshness_level);
        tvLevel.setText(record.getLevel());
        tvLevel.setTextColor(record.getLevelColor());
        tvLevel.setBackgroundColor(record.getLevelBgColor());

        // 颜色分析
        TextView tvColorInfo = findViewById(R.id.tv_color_info);
        tvColorInfo.setText(record.getColorInfo() != null ? record.getColorInfo() : "");

        // 保存建议
        TextView tvSuggestion = findViewById(R.id.tv_suggestion);
        tvSuggestion.setText(record.getSuggestion() != null ? record.getSuggestion() : "");

        // 检测时间
        TextView tvDetectTime = findViewById(R.id.tv_detect_time);
        tvDetectTime.setText(record.getFormattedTime());
    }

    private void setupDeleteButton() {
        findViewById(R.id.btn_delete_record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(HistoryDetailActivity.this)
                        .setTitle(R.string.delete)
                        .setMessage(R.string.history_confirm_delete)
                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        dbHelper.deleteRecord(record.getId());
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(HistoryDetailActivity.this,
                                                        R.string.history_deleted, Toast.LENGTH_SHORT).show();
                                                setResult(RESULT_OK);
                                                finish();
                                            }
                                        });
                                    }
                                }).start();
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
