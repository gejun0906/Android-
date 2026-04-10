package com.example.marketassistant;

import java.io.Serializable;

/**
 * 用户评价数据模型
 */
public class Review implements Serializable {
    private String userName;
    private float rating;
    private String comment;
    private String date;
    private String userAvatar; // 用户头像URL（可选）

    public Review() {
    }

    public Review(String userName, float rating, String comment, String date) {
        this.userName = userName;
        this.rating = rating;
        this.comment = comment;
        this.date = date;
    }

    public Review(String userName, float rating, String comment, String date, String userAvatar) {
        this.userName = userName;
        this.rating = rating;
        this.comment = comment;
        this.date = date;
        this.userAvatar = userAvatar;
    }

    // Getters and Setters
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }
}
