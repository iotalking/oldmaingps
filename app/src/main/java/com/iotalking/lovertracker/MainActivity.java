package com.iotalking.lovertracker;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import com.baidu.location.BDLocation;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.TextureMapView;
import com.baidu.mapapi.map.UiSettings;
import com.baidu.mapapi.model.LatLng;
import com.iotalking.lovertracker.service.WakeupService;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private TextureMapView mMapView;
    private BaiduMap mBaiduMap;
    private MyReceiver mReceiver;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        mMapView = (TextureMapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMyLocationEnabled(true);
        requestGPSPerssion();
        registerReceiver();
        startService();
        requestGPSPerssion();
    }

    final int GPS_REQUEST_CODE = 6666;
    @RequiresApi(api = Build.VERSION_CODES.M)
    void requestGPSPerssion(){
        if(checkSelfPermission(Manifest.permission_group.LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(shouldShowRequestPermissionRationale(Manifest.permission_group.LOCATION)){

            }else{
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},GPS_REQUEST_CODE);
            }
        }else{
            setMyLocation(WakeupService.getLastLocation());
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        if(requestCode == GPS_REQUEST_CODE){
            boolean gpsGranted = false;
            for(int i = 0;i<permissions.length;i++){
                String permission = permissions[i];
                if(permission.equals(Manifest.permission.ACCESS_FINE_LOCATION) ||
                        permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION)){
                    if(grantResults[i] == PackageManager.PERMISSION_GRANTED){
                        gpsGranted = true;
                        break;
                    }
                }
            }
            if(gpsGranted){
                setMyLocation(WakeupService.getLastLocation());
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    void registerReceiver(){
        mReceiver = new MyReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WakeupService.LOCATION_ACTION);
        filter.addAction(WakeupService.MOVE_TO_FRONT_ACTION);
        registerReceiver(mReceiver,filter);
    }
    void unregisterReceiver(){
        if(mReceiver != null){
            this.unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }
    void startService(){
        Intent i = new Intent();
        i.setClass(this,WakeupService.class);
        i.putExtra("taskId",getTaskId());
        startService(i);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mBaiduMap = null;
        mMapView.onDestroy();
    }
    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

    void setMyLocation(BDLocation location){
        if(location == null){
            Intent i = new Intent();
            i.setClass(this,WakeupService.class);
            i.setAction(WakeupService.RESTART_GPS_ACTION);
            startService(i);
            return;
        }
        // 构造定位数据
        MyLocationData locData = new MyLocationData.Builder()
                .accuracy(location.getRadius())
                // 此处设置开发者获取到的方向信息，顺时针0-360
                .latitude(location.getLatitude())
                .longitude(location.getLongitude()).build();
        // 设置定位数据
        mBaiduMap.setMyLocationData(locData);

        LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
        MapStatusUpdate u = MapStatusUpdateFactory.newLatLngZoom(ll, 20.0f);
        mBaiduMap.animateMapStatus(u);

        Log.i(TAG,"addr:"+location.getAddrStr());
    }
    class MyReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(WakeupService.LOCATION_ACTION)){
                BDLocation location = intent.getParcelableExtra("location");
                setMyLocation(location);
            }
        }
    }
}
