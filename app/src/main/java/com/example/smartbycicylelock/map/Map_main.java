package com.example.smartbycicylelock.map;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.BitmapFactory;
import android.net.wifi.p2p.WifiP2pManager;
import android.nfc.Tag;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import com.example.smartbycicylelock.BlueTooth.BluetoothLeService;
import com.example.smartbycicylelock.BlueTooth.SampleGattAttributes;
import com.example.smartbycicylelock.R;

import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Map_main extends AppCompatActivity {
    private static final String NOTIFICATION_CHANNEL_ID = "10001";
    //    private String mDeviceAddress = "4C:11:AE:D5:1E:3E";
    private String mDeviceAddress = "A4:CF:12:86:F6:BA"; // default Arduino
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private double lon = 0;
    private double lat = 0;
    private String TAG = "roach";
    private MapPoint mappoing = MapPoint.mapPointWithGeoCoord(37.5514579595, 126.951949155);
    private MapPOIItem customMarker = new MapPOIItem();
    private ViewGroup mapViewContainer;
    private MapView mapView;
    private Switch syncSwitch;
    private View syncView;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.d("roach", "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
            Log.d("roach", mDeviceAddress);
            Log.d(TAG, "cool");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_main);
        syncSwitch = (Switch) findViewById(R.id.sync);
        syncView = (View)findViewById(R.id.sync_view);
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String uuid = null;
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
// TO DO : Create Button
        customMarker = new MapPOIItem();
        mapView = new MapView(this);
        mapView.setZoomLevel(1, true);
        mapViewContainer = findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);

// 앱 Hash값 인식
//        try{
////            PackageInfo info =  getPackageManager().getPackageInfo("com.example.smartbycicylelock", PackageManager.GET_SIGNATURES);
////            for(Signature signature : info.signatures){
////                MessageDigest md = MessageDigest.getInstance("SHA");
////                md.update(signature.toByteArray());
////                Log.d("KeyHash", Base64.encodeToString(md.digest(), Base64.DEFAULT));
////            }
////
////        }catch (PackageManager.NameNotFoundException e)
////        {
////            e.printStackTrace();
////        }catch (NoSuchAlgorithmException e)
////        {
////            e.printStackTrace();
////        }

        customMarker.setItemName("자전거 위치");
        customMarker.setTag(1);
        customMarker.setMapPoint(mappoing);
        customMarker.setMarkerType(MapPOIItem.MarkerType.BluePin); // 마커타입을 커스텀 마커로 지정.
        customMarker.setCustomImageAutoscale(false); // hdpi, xhdpi 등 안드로이드 플랫폼의 스케일을 사용할 경우 지도 라이브러리의 스케일 기능을 꺼줌.
        customMarker.setCustomImageAnchor(0.5f, 1.0f); // 마커 이미지중 기준이 되는 위치(앵커포인트) 지정 - 마커 이미지 좌측 상단 기준 x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) 값.
        mapView.addPOIItem(customMarker);

        syncView.bringToFront();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        mapView.setMapCenterPoint(mappoing, true);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        syncSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setlat();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                setlon();
                setmapping(lat, lon);
            }
        });


    }
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    void setlat(){
        final BluetoothGattCharacteristic characteristic =
                mGattCharacteristics.get(4).get(0);
        mNotifyCharacteristic = characteristic;
        mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, true);
        mBluetoothLeService.readCharacteristic(characteristic);
    }
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    void setlon(){
        final BluetoothGattCharacteristic characteristic =
                mGattCharacteristics.get(5).get(0);
        mNotifyCharacteristic = characteristic;
        mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, true);
        mBluetoothLeService.readCharacteristic(characteristic);
    }
    void saveLatData(double data){
        double temp_data = data;
        if(temp_data != 0){
            lat = data;
        }
    }

    void saveLonData(double data){
        double temp_data = data;
        if(temp_data != 0){
            lon = data;
        }
    }
    void setmapping(double lat, double lon){
        mappoing = MapPoint.mapPointWithGeoCoord(lat, lon); // GPS값을 받아오면 여길로 넣어주면됨
        customMarker.setMapPoint(mappoing);
        mapView.addPOIItem(customMarker);
        mapView.setMapCenterPoint(mappoing, true);

    }
    double convert(int jwa){
        return jwa*0.000001;
    }
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void get_service_Data(List<BluetoothGattService> gattServices){
        String uuid;
        for (BluetoothGattService gattService : gattServices){
            uuid = gattService.getUuid().toString();
            List<BluetoothGattCharacteristic> gattCharacteristics;
            gattCharacteristics = gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();
            for(BluetoothGattCharacteristic characteristic : gattCharacteristics) {
                charas.add(characteristic);
                mBluetoothLeService.readCharacteristic(characteristic);
                Log.d(TAG, String.valueOf(characteristic));
            }
            mGattCharacteristics.add(charas);
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
        NotificationSomethings();
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.d(TAG, "Service Connect Ted");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.d(TAG, "Service Disconnect Ted");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                get_service_Data(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                saveLatData(intent.getDoubleExtra(BluetoothLeService.LAT, 0.0));
                saveLonData(intent.getDoubleExtra(BluetoothLeService.LON, 0.0));
                Log.d("lat", String.valueOf(lat));
                Log.d("lon", String.valueOf(lon));
                if(syncSwitch.isChecked()){
                    Log.d("exec", "함수실행");
                    setlat();
                    setlon();
                    setmapping(lat, lon);
                }
            }
        }
    };

    public void NotificationSomethings() {


        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

//        Intent notificationIntent = new Intent(this, ResultActivity.class);
//        notificationIntent.putExtra("notificationId", count); //전달할 값
//        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK) ;
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,  PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_foreground)) //BitMap 이미지 요구
                .setContentTitle("자전거 도난 경고!!!!!")
                .setContentText("현재 자전거의 상태를 확인해주세요!!!!")
                // 더 많은 내용이라서 일부만 보여줘야 하는 경우 아래 주석을 제거하면 setContentText에 있는 문자열 대신 아래 문자열을 보여줌
                //.setStyle(new NotificationCompat.BigTextStyle().bigText("더 많은 내용을 보여줘야 하는 경우..."))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                .setContentIntent(pendingIntent) // 사용자가 노티피케이션을 탭시 ResultActivity로 이동하도록 설정
                .setAutoCancel(true);

        //OREO API 26 이상에서는 채널 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            builder.setSmallIcon(R.drawable.ic_launcher_foreground); //mipmap 사용시 Oreo 이상에서 시스템 UI 에러남
            CharSequence channelName  = "노티페케이션 채널";
            String descriptio= "오레오 이상을 위한 것임";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName , importance);
            channel.setDescription("xxxxxxx");

            // 노티피케이션 채널을 시스템에 등록
            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);

        }else builder.setSmallIcon(R.mipmap.ic_launcher); // Oreo 이하에서 mipmap 사용하지 않으면 Couldn't create icon: StatusBarIcon 에러남

        assert notificationManager != null;
        notificationManager.notify(1234, builder.build()); // 고유숫자로 노티피케이션 동작시킴

    }
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

}
