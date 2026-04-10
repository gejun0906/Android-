package com.example.marketassistant;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import java.util.ArrayList;
import java.util.List;

/**
 * 市场辅助模块 - 主界面
 * 功能：定位附近超市、显示用户评价、标记新鲜度高评分商户
 */
public class MarketAssistantActivity extends AppCompatActivity implements AMapLocationListener {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private MapView mapView;
    private AMap aMap;
    private AMapLocationClient locationClient;
    private RecyclerView recyclerView;
    private MarketAdapter marketAdapter;
    private List<Market> marketList;
    private LatLng currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_market_assistant);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("附近新鲜商户");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 高德地图隐私合规设置（SDK 8.1.0+ 必须）
        try {
            AMapLocationClient.updatePrivacyShow(getApplicationContext(), true, true);
            AMapLocationClient.updatePrivacyAgree(getApplicationContext(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 初始化地图
        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        aMap = mapView.getMap();

        // 初始化RecyclerView
        recyclerView = findViewById(R.id.recycler_view_markets);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        marketList = new ArrayList<>();
        marketAdapter = new MarketAdapter(this, marketList, new MarketAdapter.OnMarketClickListener() {
            @Override
            public void onMarketClick(Market market) {
                // 点击列表项时，地图移动到对应位置
                LatLng position = new LatLng(market.getLatitude(), market.getLongitude());
                aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 16));
            }
        });
        recyclerView.setAdapter(marketAdapter);

        // 检查并请求权限
        checkAndRequestPermissions();
    }

    /**
     * 检查并请求定位权限
     */
    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    PERMISSION_REQUEST_CODE);
        } else {
            initLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initLocation();
            } else {
                Toast.makeText(this, "未授权定位权限，使用默认位置", Toast.LENGTH_SHORT).show();
                useDefaultLocation();
            }
        }
    }

    /**
     * 初始化定位
     */
    private void initLocation() {
        try {
            locationClient = new AMapLocationClient(getApplicationContext());
            locationClient.setLocationListener(this);

            AMapLocationClientOption option = new AMapLocationClientOption();
            option.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            option.setOnceLocation(true);
            option.setNeedAddress(true);
            option.setHttpTimeOut(8000);

            locationClient.setLocationOption(option);
            locationClient.startLocation();

            // 设置地图UI设置
            aMap.getUiSettings().setZoomControlsEnabled(true);
            aMap.getUiSettings().setCompassEnabled(true);
            aMap.getUiSettings().setMyLocationButtonEnabled(true);
            aMap.setMyLocationEnabled(true);

            // 定位超时兜底：8秒未定位成功则使用默认位置
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (currentLocation == null) {
                        Toast.makeText(MarketAssistantActivity.this,
                                "定位超时，使用默认位置", Toast.LENGTH_SHORT).show();
                        useDefaultLocation();
                    }
                }
            }, 8000);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "定位服务不可用，使用默认位置", Toast.LENGTH_SHORT).show();
            useDefaultLocation();
        }
    }

    @Override
    public void onLocationChanged(AMapLocation location) {
        if (location != null && location.getErrorCode() == 0) {
            currentLocation = new LatLng(location.getLatitude(), location.getLongitude());

            // 移动地图到当前位置
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));

            // 加载附近超市数据（这里使用模拟数据，实际项目中应从服务器获取）
            loadNearbyMarkets(location.getLatitude(), location.getLongitude());
        } else {
            // 定位失败，使用默认位置
            Toast.makeText(this, "定位失败，使用默认位置", Toast.LENGTH_SHORT).show();
            useDefaultLocation();
        }
    }

    /**
     * 使用默认位置（定位失败时的兜底方案）
     */
    private void useDefaultLocation() {
        if (currentLocation != null) return; // 已有位置，不重复加载
        double defaultLat = 39.9042;
        double defaultLng = 116.4074;
        currentLocation = new LatLng(defaultLat, defaultLng);
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
        loadNearbyMarkets(defaultLat, defaultLng);
    }

    /**
     * 加载附近超市数据
     */
    private void loadNearbyMarkets(double latitude, double longitude) {
        // 清空现有数据
        marketList.clear();
        aMap.clear();

        // 模拟附近超市数据（实际项目中应该从API获取）
        marketList.add(new Market(
                "1",
                "永辉超市(文化路店)",
                latitude + 0.005,
                longitude + 0.005,
                "文化路123号",
                4.8f,
                4.9f,
                "营业中",
                "08:00-22:00",
                "大型连锁超市，商品种类齐全",
                150
        ));

        marketList.add(new Market(
                "2",
                "盒马鲜生(万达广场店)",
                latitude + 0.008,
                longitude - 0.003,
                "万达广场B1层",
                4.9f,
                5.0f,
                "营业中",
                "07:00-23:00",
                "新鲜水果蔬菜，30分钟配送",
                220
        ));

        marketList.add(new Market(
                "3",
                "华润万家(中心店)",
                latitude - 0.004,
                longitude + 0.006,
                "建设路88号",
                4.6f,
                4.7f,
                "营业中",
                "08:30-21:30",
                "品质保证，价格实惠",
                98
        ));

        marketList.add(new Market(
                "4",
                "家乐福(CBD店)",
                latitude - 0.006,
                longitude - 0.008,
                "CBD商圈1号",
                4.5f,
                4.4f,
                "营业中",
                "09:00-22:00",
                "国际连锁，进口商品丰富",
                180
        ));

        marketList.add(new Market(
                "5",
                "鲜丰水果(社区店)",
                latitude + 0.002,
                longitude + 0.002,
                "幸福小区门口",
                4.7f,
                4.8f,
                "营业中",
                "07:30-21:00",
                "专注新鲜水果，品质优良",
                75
        ));

        // 添加用户评价
        addReviewsToMarkets();

        // 在地图上标记超市
        for (Market market : marketList) {
            addMarkerToMap(market);
        }

        // 更新列表
        marketAdapter.notifyDataSetChanged();
    }

    /**
     * 添加用户评价数据
     */
    private void addReviewsToMarkets() {
        // 为每个超市添加评价
        if (marketList.size() > 0) {
            marketList.get(0).addReview(new Review("张三", 5.0f, "蔬菜很新鲜，品质很好！", "2024-03-08"));
            marketList.get(0).addReview(new Review("李四", 4.8f, "价格合理，环境干净", "2024-03-07"));
            marketList.get(0).addReview(new Review("王五", 4.5f, "服务态度不错", "2024-03-06"));
        }

        if (marketList.size() > 1) {
            marketList.get(1).addReview(new Review("赵六", 5.0f, "配送超快，水果超新鲜！", "2024-03-08"));
            marketList.get(1).addReview(new Review("孙七", 5.0f, "盒马真的很棒，质量一流", "2024-03-07"));
            marketList.get(1).addReview(new Review("周八", 4.8f, "海鲜很新鲜，推荐", "2024-03-06"));
        }

        if (marketList.size() > 2) {
            marketList.get(2).addReview(new Review("吴九", 4.5f, "老牌超市，值得信赖", "2024-03-08"));
            marketList.get(2).addReview(new Review("郑十", 4.8f, "商品齐全，停车方便", "2024-03-07"));
        }

        if (marketList.size() > 3) {
            marketList.get(3).addReview(new Review("钱一", 4.3f, "进口食品很多", "2024-03-08"));
            marketList.get(3).addReview(new Review("陈二", 4.6f, "品类丰富，质量可靠", "2024-03-07"));
        }

        if (marketList.size() > 4) {
            marketList.get(4).addReview(new Review("刘三", 4.9f, "水果新鲜度非常高！", "2024-03-08"));
            marketList.get(4).addReview(new Review("杨四", 4.7f, "专业水果店，品质保证", "2024-03-07"));
        }
    }

    /**
     * 在地图上添加标记
     */
    private void addMarkerToMap(Market market) {
        LatLng position = new LatLng(market.getLatitude(), market.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions()
                .position(position)
                .title(market.getName())
                .snippet("评分: " + market.getRating() + " | 新鲜度: " + market.getFreshnessRating());

        // 根据新鲜度评分设置不同颜色的标记
        if (market.getFreshnessRating() >= 4.8f) {
            // 新鲜度高评分商户 - 使用绿色标记
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        } else if (market.getFreshnessRating() >= 4.5f) {
            // 中等评分 - 使用蓝色标记
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        } else {
            // 一般评分 - 使用橙色标记
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
        }

        Marker marker = aMap.addMarker(markerOptions);
        marker.setObject(market);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (locationClient != null) {
            locationClient.onDestroy();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
