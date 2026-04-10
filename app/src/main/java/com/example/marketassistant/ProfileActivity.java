package com.example.marketassistant;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import java.io.File;

/**
 * 个人中心 / 设置页面
 */
public class ProfileActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "market_assistant_prefs";
    private static final String KEY_SAVE_IMAGES = "save_images";
    private static final String KEY_DARK_MODE = "dark_mode";

    private SharedPreferences prefs;
    private DetectionDbHelper dbHelper;
    private TextView tvRecordCount;
    private TextView tvCacheSize;
    private TextView tvVersion;
    private SwitchCompat switchSaveImages;
    private SwitchCompat switchDarkMode;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.profile_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        dbHelper = new DetectionDbHelper(this);

        initViews();
        setupSwitches();
        setupClickListeners();
    }

    private void initViews() {
        tvRecordCount = findViewById(R.id.tv_record_count);
        tvCacheSize = findViewById(R.id.tv_cache_size);
        tvVersion = findViewById(R.id.tv_version);
        switchSaveImages = findViewById(R.id.switch_save_images);
        switchDarkMode = findViewById(R.id.switch_dark_mode);

        tvVersion.setText(String.format(getString(R.string.profile_version), BuildConfig.VERSION_NAME));
    }

    private void setupSwitches() {
        // 加载当前设置
        switchSaveImages.setChecked(prefs.getBoolean(KEY_SAVE_IMAGES, true));
        switchDarkMode.setChecked(prefs.getBoolean(KEY_DARK_MODE, false));

        switchSaveImages.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_SAVE_IMAGES, isChecked).apply();
            }
        });

        switchDarkMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();
                AppCompatDelegate.setDefaultNightMode(
                        isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
    }

    private void setupClickListeners() {
        // 我的检测记录
        findViewById(R.id.card_my_records).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ProfileActivity.this, HistoryActivity.class));
            }
        });

        // 收藏商户
        findViewById(R.id.card_favorite_markets).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ProfileActivity.this,
                        R.string.profile_feature_developing, Toast.LENGTH_SHORT).show();
            }
        });

        // 清除缓存
        findViewById(R.id.card_clear_cache).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(ProfileActivity.this)
                        .setTitle(R.string.profile_clear_cache)
                        .setMessage("确定要清除所有缓存文件吗？")
                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                clearCache();
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        });

        // 关于系统
        findViewById(R.id.card_about).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAboutDialog();
            }
        });

        // 意见反馈
        findViewById(R.id.card_feedback).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFeedbackDialog();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateRecordCount();
        updateCacheSize();
    }

    private void updateRecordCount() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final int count = dbHelper.getRecordCount();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        tvRecordCount.setText(String.format(getString(R.string.profile_record_count), count));
                    }
                });
            }
        }).start();
    }

    private void updateCacheSize() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                long totalSize = 0;
                // 检测缩略图
                File thumbnailDir = getExternalFilesDir("detection_thumbnails");
                if (thumbnailDir != null && thumbnailDir.exists()) {
                    totalSize += getDirSize(thumbnailDir);
                }
                // 拍照临时文件
                File picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                if (picturesDir != null && picturesDir.exists()) {
                    totalSize += getDirSize(picturesDir);
                }
                final String sizeText = formatFileSize(totalSize);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        tvCacheSize.setText(sizeText);
                    }
                });
            }
        }).start();
    }

    private void clearCache() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                File thumbnailDir = getExternalFilesDir("detection_thumbnails");
                if (thumbnailDir != null && thumbnailDir.exists()) {
                    deleteDir(thumbnailDir);
                }
                File picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                if (picturesDir != null && picturesDir.exists()) {
                    deleteDir(picturesDir);
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        tvCacheSize.setText("0MB");
                        Toast.makeText(ProfileActivity.this,
                                R.string.profile_cache_cleared, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    private void showAboutDialog() {
        String content = String.format(getString(R.string.profile_about_content), BuildConfig.VERSION_NAME);
        new AlertDialog.Builder(this)
                .setTitle(R.string.profile_about)
                .setMessage(content)
                .setPositiveButton(R.string.confirm, null)
                .show();
    }

    private void showFeedbackDialog() {
        final EditText editText = new EditText(this);
        editText.setHint(R.string.profile_feedback_hint);
        editText.setMinLines(3);
        editText.setPadding(48, 32, 48, 16);

        new AlertDialog.Builder(this)
                .setTitle(R.string.profile_feedback)
                .setView(editText)
                .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String feedback = editText.getText().toString().trim();
                        if (!feedback.isEmpty()) {
                            Toast.makeText(ProfileActivity.this,
                                    R.string.profile_feedback_thanks, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ========== 工具方法 ==========

    private long getDirSize(File dir) {
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else if (file.isDirectory()) {
                    size += getDirSize(file);
                }
            }
        }
        return size;
    }

    private void deleteDir(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    file.delete();
                } else if (file.isDirectory()) {
                    deleteDir(file);
                }
            }
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return String.format("%.1fKB", bytes / 1024.0);
        return String.format("%.1fMB", bytes / (1024.0 * 1024));
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
