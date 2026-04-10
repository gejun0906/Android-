package com.example.marketassistant;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 超市/市场数据模型
 */
public class Market implements Serializable {
    private String id;
    private String name;
    private double latitude;
    private double longitude;
    private String address;
    private float rating; // 综合评分
    private float freshnessRating; // 新鲜度评分
    private String status; // 营业状态
    private String businessHours; // 营业时间
    private String description; // 描述
    private int reviewCount; // 评价数量
    private List<Review> reviews; // 用户评价列表

    public Market() {
        this.reviews = new ArrayList<>();
    }

    public Market(String id, String name, double latitude, double longitude,
                  String address, float rating, float freshnessRating,
                  String status, String businessHours, String description, int reviewCount) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.rating = rating;
        this.freshnessRating = freshnessRating;
        this.status = status;
        this.businessHours = businessHours;
        this.description = description;
        this.reviewCount = reviewCount;
        this.reviews = new ArrayList<>();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public float getFreshnessRating() {
        return freshnessRating;
    }

    public void setFreshnessRating(float freshnessRating) {
        this.freshnessRating = freshnessRating;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBusinessHours() {
        return businessHours;
    }

    public void setBusinessHours(String businessHours) {
        this.businessHours = businessHours;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }

    public void addReview(Review review) {
        if (this.reviews == null) {
            this.reviews = new ArrayList<>();
        }
        this.reviews.add(review);
    }

    /**
     * 判断是否为新鲜度高评分商户
     * @return true 如果新鲜度评分 >= 4.8
     */
    public boolean isHighFreshnessRating() {
        return freshnessRating >= 4.8f;
    }

    /**
     * 计算距离（单位：米）
     * @param latitude 用户纬度
     * @param longitude 用户经度
     * @return 距离（米）
     */
    public double calculateDistance(double latitude, double longitude) {
        double earthRadius = 6371000; // 地球半径（米）
        double dLat = Math.toRadians(this.latitude - latitude);
        double dLng = Math.toRadians(this.longitude - longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(this.latitude))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    /**
     * 获取格式化的距离字符串
     * @param latitude 用户纬度
     * @param longitude 用户经度
     * @return 格式化的距离（如："500m" 或 "1.2km"）
     */
    public String getFormattedDistance(double latitude, double longitude) {
        double distance = calculateDistance(latitude, longitude);
        if (distance < 1000) {
            return String.format("%.0fm", distance);
        } else {
            return String.format("%.1fkm", distance / 1000);
        }
    }
}
