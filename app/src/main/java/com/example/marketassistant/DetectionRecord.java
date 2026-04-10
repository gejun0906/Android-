package com.example.marketassistant;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 水果新鲜度检测记录数据模型
 */
public class DetectionRecord implements Serializable {

    private long id;
    private long detectTime;
    private String thumbnailPath;
    private String fruitType;
    private int score;
    private String level;
    private String colorInfo;
    private String suggestion;

    public DetectionRecord() {
    }

    public DetectionRecord(long detectTime, String thumbnailPath, String fruitType,
                           int score, String level, String colorInfo, String suggestion) {
        this.detectTime = detectTime;
        this.thumbnailPath = thumbnailPath;
        this.fruitType = fruitType;
        this.score = score;
        this.level = level;
        this.colorInfo = colorInfo;
        this.suggestion = suggestion;
    }

    // ========== Getters & Setters ==========

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getDetectTime() {
        return detectTime;
    }

    public void setDetectTime(long detectTime) {
        this.detectTime = detectTime;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getFruitType() {
        return fruitType;
    }

    public void setFruitType(String fruitType) {
        this.fruitType = fruitType;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getColorInfo() {
        return colorInfo;
    }

    public void setColorInfo(String colorInfo) {
        this.colorInfo = colorInfo;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    // ========== 便捷方法 ==========

    /**
     * 获取格式化的检测时间
     */
    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(detectTime));
    }

    /**
     * 获取新鲜度等级对应的文字颜色
     */
    public int getLevelColor() {
        if (score >= 80) return 0xFF1B5E20;
        if (score >= 60) return 0xFF0D47A1;
        return 0xFFE65100;
    }

    /**
     * 获取新鲜度等级对应的背景颜色
     */
    public int getLevelBgColor() {
        if (score >= 80) return 0xFFC8E6C9;
        if (score >= 60) return 0xFFBBDEFB;
        return 0xFFFFE0B2;
    }

    /**
     * 获取分数对应的颜色
     */
    public int getScoreColor() {
        if (score >= 80) return 0xFF4CAF50;
        if (score >= 60) return 0xFF2196F3;
        return 0xFFFF9800;
    }
}
