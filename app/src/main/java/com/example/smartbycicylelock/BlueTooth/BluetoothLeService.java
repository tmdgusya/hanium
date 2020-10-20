package com.example.smartbycicylelock.BlueTooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.List;
import java.util.UUID;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BluetoothLeService extends Service {
    private final static String TAG = "BluetoothLeService";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    private final IBinder mBinder = new LocalBinder();

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATC_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    public final static String LON = "com.example.bluetooth.le.LON";
    public final static String LAT = "com.example.bluetooth.le.LAT";

    public final static UUID UUID_DISPLAY_RAITING_BATTERY_PERCENT =
            UUID.fromString(SampleGattAttributes.DISPLAY_RAITING_BATTERY_PERCENT);
    public final static UUID UUID_BATTERY_CHARACTERISTIC_SERVICE =
            UUID.fromString(SampleGattAttributes.BATTERY_CHARACTERISTIC_SERVICE);
    public final static UUID LAT_DATA =
            UUID.fromString(SampleGattAttributes.LAT);
    public final static UUID LON_DATA =
            UUID.fromString(SampleGattAttributes.LON);

    // GATT Callback 함수
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            // 연결 상태에 따라서
            if(newState == BluetoothProfile.STATE_CONNECTED){
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATC_CONNECTED;
                broadcastUpdate(intentAction);
                Log.d("yoojs", "GATT 서버 연결 상태");
                Log.d("yoojs", "서비스 검색을 시작 중 :" + mBluetoothGatt.discoverServices());
            } else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.d("yoojs", "GATT 서버 연결 끊긴 상태");
                broadcastUpdate(intentAction);
            }
        }

        // GATT 연결 시도시
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else{
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS){
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                Log.d(TAG, "onCharacteristicRead: " + characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }
    };

    private void broadcastUpdate(final String action){
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }


    private void broadcastUpdate(final  String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        // UUID가 배터리 사용량이면
        if (UUID_DISPLAY_RAITING_BATTERY_PERCENT.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            if(data != null && data.length>0){
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data){
                    stringBuilder.append(String.format("%d ", byteChar));
                }
                intent.putExtra("BATTERY", new String(data) + "\n" + stringBuilder.toString());
            }
        }

        // 위도 characteristic 읽기
        else if (LAT_DATA.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(int i=data.length-1; i>=0; i--){
                    stringBuilder.append(String.format("%X", data[i]));
                }
                // 위도 String 값
                String finaldata = stringBuilder.toString();
                // 위도 int 값
                int intData = Integer.parseInt(finaldata, 16);
                // 위도 Double 값 --------
                double doubleData = (double)intData * 0.000001;
                Log.d("ggg", String.valueOf(doubleData));
                intent.putExtra(LAT, intData);
            }
        }
        // 경도 characteristic 읽기
        else if (LON_DATA.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(int i=data.length-1; i>=0; i--){
                    stringBuilder.append(String.format("%X", data[i]));
                }
                // 경도 String 값
                String finaldata = stringBuilder.toString();
                // 경도 int 값
                int intData = Integer.parseInt(finaldata, 16);
                // 경도 Double 값 --------
                double doubleData = (double)intData * 0.000001;
                Log.d("ggg", String.valueOf(doubleData));
                intent.putExtra(LON,intData);
            }
        }
        // 지정 UUID가 아닐시
        else {
            final byte[] data = characteristic.getValue();
            if(data != null && data.length>0){
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data){
                    stringBuilder.append(String.format("%X ", byteChar));
                }
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder{
        public BluetoothLeService getService(){
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    // GATT자원 close
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    // Local Bluetooth adapter 초기화
    // true를 반환하면 초기화 성공
    public boolean initialize(){
        if(mBluetoothManager == null){
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if(mBluetoothManager == null){
                Log.d("yoojs", "BluetoothManager 초기화 불가능");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if(mBluetoothAdapter == null){
            Log.d("yoojs", "BluetoothAdapter 가져올 수 없음");
            return false;
        }

        return true;
    }

    //
    public boolean connect(final  String address){
        if(mBluetoothAdapter == null || address == null){
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // 이전에 연결된 장치. 다시 연결 시도
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        // 자동 연결 // 파라미터는 false
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    // DisConnect
    // onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int) 콜백
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    // 지정된 BluetoothGattCharacteristic에 대한 읽기 요청 후 읽기 결과 보고
    // Characteristic 읽기
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    // Characteristic 쓰기
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] data){
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        if(characteristic==null)
        {
            Log.d("ddd","bb");
        }
        characteristic.setValue(data);
        mBluetoothGatt.writeCharacteristic(characteristic);
    }

    // 부여된 Characteristic에 대한 알림발생 또는 중지
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, true);
        Log.d("yoojs", "setCharcteristicNotification uuid");
        Log.d("yoojs", String.valueOf(characteristic.getUuid()));

        if (BATTERY_CHARACTERISTIC_SERVICE.equals(characteristic.getUuid()))
        {
            Log.d("yoojs", "setCharacteristicNotification 호출");
            List<BluetoothGattDescriptor> descriptor = characteristic.getDescriptors();
            for(BluetoothGattDescriptor x : descriptor) {
                Log.d("yoojs", "descriptor : " + String.valueOf(descriptor));
            }
            descriptor.get(0).setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor.get(0));
        }
        // lat = 위도일 때
        if ("697c7a96-11e5-4a70-98e3-d5273296e47f".equals(characteristic.getUuid())) {
            List<BluetoothGattDescriptor> descriptor = characteristic.getDescriptors();
            descriptor.get(0).setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor.get(0));
        }
        // 경도일 때 = lon
        if ("f099cb58-4ad3-4239-bd2d-6b5724cc9097".equals(characteristic.getUuid())) {
            List<BluetoothGattDescriptor> descriptor = characteristic.getDescriptors();
            descriptor.get(0).setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor.get(0));
        }
    }

    // 연결된 기기에서 지원되는 GATT 서비스 목록 검색
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
}

















