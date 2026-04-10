package com.example.marketassistant;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 水果新鲜度检测页面
 * 使用系统相机拍照或相册选择图片
 * 使用 OpenCV HSV 颜色空间分析水果新鲜度
 */
public class FreshnessDetectionActivity extends AppCompatActivity {

    private static final String TAG = "FreshnessDetection";
    private static final int REQUEST_CAMERA = 200;
    private static final int REQUEST_GALLERY = 201;
    private static final int PERMISSION_REQUEST = 300;

    // UI - 相机模式
    private LinearLayout layoutCameraMode;

    // UI - 预览模式
    private LinearLayout layoutPreviewMode;
    private ImageView imgPreview;

    // UI - 结果模式
    private ScrollView layoutResultMode;
    private ImageView imgResult;
    private LinearLayout layoutProgress;
    private TextView tvProgressStep;
    private CardView cardResult;
    private View btnRetry;

    // 结果文本
    private TextView tvFruitType;
    private TextView tvFreshnessScore;
    private TextView tvFreshnessLevel;
    private TextView tvColorInfo;
    private TextView tvSuggestion;

    // 状态
    private boolean opencvLoaded = false;
    private Handler handler;
    private Uri photoUri;
    private Bitmap currentBitmap; // 当前待检测的图片

    // 数据库
    private DetectionDbHelper dbHelper;

    // 水果保存建议数据
    private static final String[][] FRUIT_SUGGESTIONS = {
            {"苹果", "果蒂鲜绿、饱满、有韧性，紧贴果身，表皮 紧实、光滑、有光泽，建议冷藏保存，苹果可保鲜7-14天。"},
            {"橙子/芒果", "橙子常温约一周，冷藏2-3周。芒果成熟后冷藏，3-5天内食用。"},
            {"香蕉", "颜色亮黄、均匀\n" +
                    "表皮光滑、紧绷、没有黑斑看果柄（最关键）\n" +
                    "新鲜：果柄绿色、结实、连着不掉\n" +
                    "不新鲜：果柄发黑、干枯、一捏就断、甚至脱落\n" +"香蕉室温保存，避免阳光直射。"},
            {"西瓜", "甜：瓜脐越小越好，皮薄肉甜，拍打“嘭嘭嘭”，低沉厚实，有回弹感\n" +
                    "生瓜：“当当当”，太脆\n" +
                    "熟过 / 坏瓜：“噗噗噗”，发空发闷，西瓜未切可室温一周，切开后24小时内食用。"},
            {"苹果", "建议冷藏保存，苹果可保鲜7-14天。"},
            {"葡萄/李子", "冷藏保存，食用前再清洗。李子可保鲜3-5天。"},
            {"水果", "建议清洗后尽快食用，冷藏可延长保鲜期。避免阳光直射和高温环境。"},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_freshness_detection);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("水果新鲜度检测");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        dbHelper = new DetectionDbHelper(this);
        handler = new Handler(Looper.getMainLooper());
        initOpenCV();
        initViews();
        setupListeners();
        checkPermissions();
    }

    private void initOpenCV() {
        try {
            opencvLoaded = OpenCVLoader.initDebug();
        } catch (Exception e) {
            Log.e(TAG, "OpenCV init exception", e);
            opencvLoaded = false;
        }
        Log.d(TAG, "OpenCV loaded: " + opencvLoaded);
    }

    private void initViews() {
        // 相机模式视图
        layoutCameraMode = findViewById(R.id.layout_camera_mode);

        // 预览模式视图
        layoutPreviewMode = findViewById(R.id.layout_preview_mode);
        imgPreview = findViewById(R.id.img_preview);

        // 结果模式视图
        layoutResultMode = findViewById(R.id.layout_result_mode);
        imgResult = findViewById(R.id.img_result);
        layoutProgress = findViewById(R.id.layout_progress);
        tvProgressStep = findViewById(R.id.tv_progress_step);
        cardResult = findViewById(R.id.card_result);
        btnRetry = findViewById(R.id.btn_retry);

        // 结果文本
        tvFruitType = findViewById(R.id.tv_fruit_type);
        tvFreshnessScore = findViewById(R.id.tv_freshness_score);
        tvFreshnessLevel = findViewById(R.id.tv_freshness_level);
        tvColorInfo = findViewById(R.id.tv_color_info);
        tvSuggestion = findViewById(R.id.tv_suggestion);
    }

    private void setupListeners() {
        // 相机模式按钮
        findViewById(R.id.btn_capture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCamera();
            }
        });

        findViewById(R.id.btn_gallery).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickFromGallery();
            }
        });

        // 预览模式按钮
        findViewById(R.id.btn_reselect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recycleBitmap();
                switchToCameraMode();
            }
        });

        findViewById(R.id.btn_start_detect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentBitmap != null) {
                    switchToResultMode(currentBitmap);
                    analyzeBitmap(currentBitmap);
                }
            }
        });

        // 结果模式按钮
        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recycleBitmap();
                switchToCameraMode();
            }
        });
    }

    private void checkPermissions() {
        List<String> perms = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.CAMERA);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (!perms.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    perms.toArray(new String[0]), PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // ========== 相机/相册操作 ==========

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST);
            return;
        }

        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File photoFile = createImageFile();
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider", photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(intent, REQUEST_CAMERA);
            } else {
                Toast.makeText(this, "无法创建图片文件", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to open camera", e);
            Toast.makeText(this, "无法打开相机", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            return File.createTempFile("FRUIT_" + timeStamp + "_", ".jpg", storageDir);
        } catch (IOException e) {
            Log.e(TAG, "Failed to create image file", e);
            return null;
        }
    }

    private void pickFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;

        Bitmap bitmap = null;

        if (requestCode == REQUEST_CAMERA && photoUri != null) {
            bitmap = loadScaledBitmap(photoUri);
        } else if (requestCode == REQUEST_GALLERY && data != null && data.getData() != null) {
            bitmap = loadScaledBitmap(data.getData());
        }

        if (bitmap != null) {
            // 进入预览确认模式，而不是直接分析
            switchToPreviewMode(bitmap);
        } else {
            Toast.makeText(this, "无法加载图片", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap loadScaledBitmap(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            if (bitmap != null && (bitmap.getWidth() > 1024 || bitmap.getHeight() > 1024)) {
                float scale = Math.min(1024f / bitmap.getWidth(), 1024f / bitmap.getHeight());
                bitmap = Bitmap.createScaledBitmap(bitmap,
                        (int) (bitmap.getWidth() * scale),
                        (int) (bitmap.getHeight() * scale), true);
            }
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load bitmap", e);
            return null;
        }
    }

    private void analyzeBitmap(Bitmap bitmap) {
        if (opencvLoaded) {
            try {
                Mat mat = new Mat();
                Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                Utils.bitmapToMat(bmp32, mat);
                analyzeWithOpenCV(mat);
            } catch (Exception e) {
                Log.e(TAG, "Failed to convert bitmap for OpenCV", e);
                analyzeBasic(bitmap);
            }
        } else {
            analyzeBasic(bitmap);
        }
    }

    // ========== 界面切换 ==========

    /**
     * 切换到预览确认模式（拍照/选图后先预览）
     */
    private void switchToPreviewMode(Bitmap bitmap) {
        recycleBitmap();
        currentBitmap = bitmap;
        layoutCameraMode.setVisibility(View.GONE);
        layoutResultMode.setVisibility(View.GONE);
        layoutPreviewMode.setVisibility(View.VISIBLE);
        imgPreview.setImageBitmap(bitmap);
    }

    private void switchToResultMode(Bitmap bitmap) {
        layoutCameraMode.setVisibility(View.GONE);
        layoutPreviewMode.setVisibility(View.GONE);
        layoutResultMode.setVisibility(View.VISIBLE);
        imgResult.setImageBitmap(bitmap);
        layoutProgress.setVisibility(View.VISIBLE);
        tvProgressStep.setText(R.string.progress_step_1);
        cardResult.setVisibility(View.GONE);
        btnRetry.setVisibility(View.GONE);
    }

    private void switchToCameraMode() {
        layoutResultMode.setVisibility(View.GONE);
        layoutPreviewMode.setVisibility(View.GONE);
        layoutCameraMode.setVisibility(View.VISIBLE);
        cardResult.setVisibility(View.GONE);
        btnRetry.setVisibility(View.GONE);
        layoutProgress.setVisibility(View.GONE);
    }

    // ========== OpenCV 图像分析 ==========

    /**
     * 使用 OpenCV 进行 HSV 颜色空间分析（含多步进度提示）
     * 传入的 image Mat 将在后台线程中释放
     */
    private void analyzeWithOpenCV(final Mat image) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            tvProgressStep.setText(R.string.progress_step_1);
                        }
                    });
                    // RGBA → RGB → HSV
                    Mat rgb = new Mat();
                    Mat hsv = new Mat();
                    Imgproc.cvtColor(image, rgb, Imgproc.COLOR_RGBA2RGB);
                    Imgproc.cvtColor(rgb, hsv, Imgproc.COLOR_RGB2HSV);
                    // 计算 HSV 通道均值和标准差
                    MatOfDouble mean = new MatOfDouble();
                    MatOfDouble stddev = new MatOfDouble();
                    Core.meanStdDev(hsv, mean, stddev);

                    double hMean = mean.get(0, 0)[0];
                    double sMean = mean.get(1, 0)[0];
                    double vMean = mean.get(2, 0)[0];
                    double sStd = stddev.get(1, 0)[0];

                    final String fruitType = determineFruitType(hMean, sMean, vMean);

                    Thread.sleep(500);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            tvProgressStep.setText(R.string.progress_step_2);
                        }
                    });

                    // 统计鲜艳像素 vs 褐变像素
                    Mat freshMask = new Mat();
                    Mat decayMask = new Mat();
                    Core.inRange(hsv, new Scalar(0, 50, 50), new Scalar(180, 255, 255), freshMask);
                    Core.inRange(hsv, new Scalar(10, 0, 30), new Scalar(30, 40, 150), decayMask);

                    double totalPixels = image.rows() * image.cols();
                    double freshRatio = Core.countNonZero(freshMask) / totalPixels;
                    double decayRatio = Core.countNonZero(decayMask) / totalPixels;

                    Thread.sleep(500);

                    // 步骤3：生成新鲜度评分
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            tvProgressStep.setText(R.string.progress_step_3);
                        }
                    });

                    // 计算新鲜度综合评分 (0-100)
                    double vibrancyScore = Math.min(sMean / 180.0, 1.0) * 35;
                    double brightnessScore = (1.0 - Math.abs(vMean - 140) / 140.0) * 25;
                    double freshDecayScore = Math.max(0, Math.min(25, freshRatio * 25 - decayRatio * 15));
                    double uniformScore = Math.max(0, (1.0 - sStd / 80.0)) * 15;

                    int score = (int) (vibrancyScore + brightnessScore + freshDecayScore + uniformScore);
                    score = Math.max(15, Math.min(98, score));

                    final String colorInfo = String.format(
                            "色相均值: %.1f\n饱和度均值: %.1f\n亮度均值: %.1f\n鲜艳像素: %.0f%%",
                            hMean, sMean, vMean, freshRatio * 100);

                    final String suggestion = getSuggestion(fruitType);

                    // 释放所有 OpenCV 资源
                    rgb.release();
                    hsv.release();
                    mean.release();
                    stddev.release();
                    freshMask.release();
                    decayMask.release();
                    image.release();

                    Thread.sleep(500);

                    final int finalScore = score;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            showResult(fruitType, finalScore, colorInfo, suggestion);
                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, "OpenCV analysis failed", e);
                    try {
                        image.release();
                    } catch (Exception ignored) {
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            layoutProgress.setVisibility(View.GONE);
                            Toast.makeText(FreshnessDetectionActivity.this,
                                    "分析失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            btnRetry.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * 基础 Bitmap 分析（OpenCV 不可用时的备用方案）
     */
    private void analyzeBasic(final Bitmap bitmap) {
        // 模拟多步进度
        tvProgressStep.setText(R.string.progress_step_1);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                tvProgressStep.setText(R.string.progress_step_2);
            }
        }, 500);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                tvProgressStep.setText(R.string.progress_step_3);
            }
        }, 1000);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int width = Math.min(bitmap.getWidth(), 100);
                int height = Math.min(bitmap.getHeight(), 100);
                Bitmap small = Bitmap.createScaledBitmap(bitmap, width, height, true);

                long rSum = 0, gSum = 0, bSum = 0;
                int count = width * height;
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        int pixel = small.getPixel(x, y);
                        rSum += (pixel >> 16) & 0xFF;
                        gSum += (pixel >> 8) & 0xFF;
                        bSum += pixel & 0xFF;
                    }
                }
                float rAvg = rSum / (float) count;
                float gAvg = gSum / (float) count;
                float bAvg = bSum / (float) count;

                float maxC = Math.max(rAvg, Math.max(gAvg, bAvg));
                float minC = Math.min(rAvg, Math.min(gAvg, bAvg));
                float saturation = maxC > 0 ? (1 - minC / maxC) : 0;
                int score = (int) (saturation * 60 + 30);
                score = Math.max(20, Math.min(95, score));

                String fruitType = "水果";
                if (rAvg > gAvg && rAvg > bAvg) fruitType = "苹果";
                else if (gAvg > rAvg && gAvg > bAvg) fruitType = "西瓜";
                else if (rAvg > 150 && gAvg > 100) fruitType = "橙子/芒果";

                String colorInfo = String.format("R:%.0f G:%.0f B:%.0f\n(基础分析模式)", rAvg, gAvg, bAvg);
                String suggestion = getSuggestion(fruitType);

                showResult(fruitType, score, colorInfo, suggestion);
            }
        }, 1500);
    }

    // ========== 显示结果 ==========

    private void showResult(String fruitType, int score, String colorInfo, String suggestion) {
        layoutProgress.setVisibility(View.GONE);
        cardResult.setVisibility(View.VISIBLE);
        btnRetry.setVisibility(View.VISIBLE);

        tvFruitType.setText(fruitType);
        tvFreshnessScore.setText(String.valueOf(score));
        tvColorInfo.setText(colorInfo);
        tvSuggestion.setText(suggestion);

        // 分数颜色
        if (score >= 80) {
            tvFreshnessScore.setTextColor(0xFF4CAF50);
        } else if (score >= 60) {
            tvFreshnessScore.setTextColor(0xFF2196F3);
        } else {
            tvFreshnessScore.setTextColor(0xFFFF9800);
        }

        // 新鲜度等级
        String level;
        int levelColor, levelBgColor;
        if (score >= 80) {
            level = "非常新鲜";
            levelColor = 0xFF1B5E20;
            levelBgColor = 0xFFC8E6C9;
        } else if (score >= 60) {
            level = "较为新鲜";
            levelColor = 0xFF0D47A1;
            levelBgColor = 0xFFBBDEFB;
        } else {
            level = "新鲜度一般";
            levelColor = 0xFFE65100;
            levelBgColor = 0xFFFFE0B2;
        }
        tvFreshnessLevel.setText(level);
        tvFreshnessLevel.setTextColor(levelColor);
        tvFreshnessLevel.setBackgroundColor(levelBgColor);

        // 保存到历史记录
        saveToHistory(fruitType, score, level, colorInfo, suggestion);
    }

    /**
     * 保存检测记录到数据库
     */
    private void saveToHistory(final String fruitType, final int score, final String level,
                               final String colorInfo, final String suggestion) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String thumbnailPath = saveThumbnail(currentBitmap);
                DetectionRecord record = new DetectionRecord(
                        System.currentTimeMillis(), thumbnailPath,
                        fruitType, score, level, colorInfo, suggestion);
                dbHelper.insertRecord(record);
                Log.d(TAG, "Detection record saved to history");
            }
        }).start();
    }

    /**
     * 保存缩略图到本地文件
     */
    private String saveThumbnail(Bitmap bitmap) {
        if (bitmap == null) return null;

        // 检查用户是否允许保存图片
        SharedPreferences prefs = getSharedPreferences("market_assistant_prefs", MODE_PRIVATE);
        if (!prefs.getBoolean("save_images", true)) {
            return null;
        }

        try {
            // 缩放为缩略图
            int thumbWidth = 200;
            int thumbHeight = (int) (200.0 * bitmap.getHeight() / bitmap.getWidth());
            Bitmap thumbnail = Bitmap.createScaledBitmap(bitmap, thumbWidth, thumbHeight, true);

            // 保存到文件
            File dir = getExternalFilesDir("detection_thumbnails");
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            File file = new File(dir, "thumb_" + timeStamp + ".jpg");
            FileOutputStream fos = new FileOutputStream(file);
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.flush();
            fos.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save thumbnail", e);
            return null;
        }
    }

    /**
     * 根据 HSV 色相判断水果类型
     */
    private String determineFruitType(double hue, double saturation, double value) {
        if (saturation < 30) return "水果";
        if ((hue < 10 || hue > 170) && saturation > 50) return "苹果";
        if (hue >= 10 && hue < 25 && saturation > 50) return "橙子/芒果";
        if (hue >= 25 && hue < 35 && saturation > 40) return "香蕉";
        if (hue >= 35 && hue < 85 && saturation > 30) return "西瓜";
        if (hue >= 85 && hue < 130) return "苹果";
        if (hue >= 130 && hue < 170 && saturation > 30) return "葡萄/李子";
        return "水果";
    }

    private String getSuggestion(String fruitType) {
        for (String[] data : FRUIT_SUGGESTIONS) {
            if (data[0].equals(fruitType)) return data[1];
        }
        return FRUIT_SUGGESTIONS[FRUIT_SUGGESTIONS.length - 1][1];
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        recycleBitmap();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    private void recycleBitmap() {
        if (currentBitmap != null && !currentBitmap.isRecycled()) {
            currentBitmap.recycle();
            currentBitmap = null;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
