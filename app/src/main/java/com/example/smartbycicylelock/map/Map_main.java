package com.example.smartbycicylelock.map;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.nfc.Tag;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.smartbycicylelock.BlueTooth.BluetoothLeService;
import com.example.smartbycicylelock.R;

import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class Map_main extends AppCompatActivity {
    private boolean mConnected = false;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private String lon;
    private String lat;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_main);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);

        MapPOIItem customMarker = new MapPOIItem();
        MapPoint mappoint = MapPoint.mapPointWithGeoCoord(convert(lat), convert(lon)); // GPS값을 받아오면 여길로 넣어주면됨
//        MapPoint mappoint = MapPoint.mapPointWithGeoCoord(37.5514579595, 126.951949155); // GPS값을 받아오면 여길로 넣어주면됨
        MapView mapView = new MapView(this);
        mapView.setZoomLevel(1, true);
        ViewGroup mapViewContainer = findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);

//        try{
//            PackageInfo info =  getPackageManager().getPackageInfo("com.example.smartbycicylelock", PackageManager.GET_SIGNATURES);
//            for(Signature signature : info.signatures){
//                MessageDigest md = MessageDigest.getInstance("SHA");
//                md.update(signature.toByteArray());
//                Log.d("KeyHash", Base64.encodeToString(md.digest(), Base64.DEFAULT));
//            }
//
//        }catch (PackageManager.NameNotFoundException e)
//        {
//            e.printStackTrace();
//        }catch (NoSuchAlgorithmException e)
//        {
//            e.printStackTrace();
//        }

        customMarker.setItemName("자전거 위치");
        customMarker.setTag(1);
        customMarker.setMapPoint(mappoint);
        customMarker.setMarkerType(MapPOIItem.MarkerType.BluePin); // 마커타입을 커스텀 마커로 지정.
        customMarker.setCustomImageAutoscale(false); // hdpi, xhdpi 등 안드로이드 플랫폼의 스케일을 사용할 경우 지도 라이브러리의 스케일 기능을 꺼줌.
        customMarker.setCustomImageAnchor(0.5f, 1.0f); // 마커 이미지중 기준이 되는 위치(앵커포인트) 지정 - 마커 이미지 좌측 상단 기준 x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) 값.
        mapView.addPOIItem(customMarker);
        mapView.setMapCenterPoint(mappoint, true);



    }
    double convert(String jwa){
        int a = Integer.parseInt(jwa, 16);
        return a*0.000001;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d("BLE", "Connect request result=" + result);
        }
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                lon = intent.getStringExtra("LAT");
                lat = intent.getStringExtra("LON");
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


}
