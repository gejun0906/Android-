package com.example.marketassistant;

import android.content.Context;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * API服务类 - 用于连接后端获取真实数据
 * 这是一个示例类，展示如何从服务器获取超市数据
 */
public class ApiService {

    private static final String BASE_URL = "https://your-api-server.com/api/";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * 获取附近超市列表
     * @param latitude 纬度
     * @param longitude 经度
     * @param radius 搜索半径（米）
     * @param callback 回调接口
     */
    public static void getNearbyMarkets(double latitude, double longitude, int radius, MarketCallback callback) {
        executor.execute(() -> {
            try {
                String urlString = BASE_URL + "markets/nearby?" +
                        "lat=" + latitude +
                        "&lng=" + longitude +
                        "&radius=" + radius;

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // 解析JSON数据
                    List<Market> markets = parseMarketsJson(response.toString());
                    callback.onSuccess(markets);
                } else {
                    callback.onFailure(new Exception("HTTP Error: " + responseCode));
                }
                connection.disconnect();
            } catch (Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * 获取超市详细信息
     * @param marketId 超市ID
     * @param callback 回调接口
     */
    public static void getMarketDetail(String marketId, MarketDetailCallback callback) {
        executor.execute(() -> {
            try {
                String urlString = BASE_URL + "markets/" + marketId;
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    Market market = parseMarketJson(response.toString());
                    callback.onSuccess(market);
                } else {
                    callback.onFailure(new Exception("HTTP Error: " + responseCode));
                }
                connection.disconnect();
            } catch (Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * 提交用户评价
     * @param marketId 超市ID
     * @param review 评价对象
     * @param callback 回调接口
     */
    public static void submitReview(String marketId, Review review, SubmitCallback callback) {
        executor.execute(() -> {
            try {
                String urlString = BASE_URL + "markets/" + marketId + "/reviews";
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                // 构建JSON数据
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("userName", review.getUserName());
                jsonBody.put("rating", review.getRating());
                jsonBody.put("comment", review.getComment());

                connection.getOutputStream().write(jsonBody.toString().getBytes());

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    callback.onSuccess("评价提交成功");
                } else {
                    callback.onFailure(new Exception("HTTP Error: " + responseCode));
                }
                connection.disconnect();
            } catch (Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * 解析超市列表JSON
     */
    private static List<Market> parseMarketsJson(String jsonString) throws Exception {
        List<Market> markets = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(jsonString);
        JSONArray dataArray = jsonObject.getJSONArray("data");

        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject item = dataArray.getJSONObject(i);
            Market market = new Market();
            market.setId(item.getString("id"));
            market.setName(item.getString("name"));
            market.setLatitude(item.getDouble("latitude"));
            market.setLongitude(item.getDouble("longitude"));
            market.setAddress(item.getString("address"));
            market.setRating((float) item.getDouble("rating"));
            market.setFreshnessRating((float) item.getDouble("freshnessRating"));
            market.setStatus(item.getString("status"));
            market.setBusinessHours(item.getString("businessHours"));
            market.setDescription(item.getString("description"));
            market.setReviewCount(item.getInt("reviewCount"));
            markets.add(market);
        }
        return markets;
    }

    /**
     * 解析单个超市JSON
     */
    private static Market parseMarketJson(String jsonString) throws Exception {
        JSONObject jsonObject = new JSONObject(jsonString);
        JSONObject data = jsonObject.getJSONObject("data");

        Market market = new Market();
        market.setId(data.getString("id"));
        market.setName(data.getString("name"));
        market.setLatitude(data.getDouble("latitude"));
        market.setLongitude(data.getDouble("longitude"));
        market.setAddress(data.getString("address"));
        market.setRating((float) data.getDouble("rating"));
        market.setFreshnessRating((float) data.getDouble("freshnessRating"));
        market.setStatus(data.getString("status"));
        market.setBusinessHours(data.getString("businessHours"));
        market.setDescription(data.getString("description"));
        market.setReviewCount(data.getInt("reviewCount"));

        // 解析评价列表
        if (data.has("reviews")) {
            JSONArray reviewsArray = data.getJSONArray("reviews");
            for (int i = 0; i < reviewsArray.length(); i++) {
                JSONObject reviewObj = reviewsArray.getJSONObject(i);
                Review review = new Review();
                review.setUserName(reviewObj.getString("userName"));
                review.setRating((float) reviewObj.getDouble("rating"));
                review.setComment(reviewObj.getString("comment"));
                review.setDate(reviewObj.getString("date"));
                market.addReview(review);
            }
        }
        return market;
    }

    // 回调接口
    public interface MarketCallback {
        void onSuccess(List<Market> markets);
        void onFailure(Exception e);
    }

    public interface MarketDetailCallback {
        void onSuccess(Market market);
        void onFailure(Exception e);
    }

    public interface SubmitCallback {
        void onSuccess(String message);
        void onFailure(Exception e);
    }
}
