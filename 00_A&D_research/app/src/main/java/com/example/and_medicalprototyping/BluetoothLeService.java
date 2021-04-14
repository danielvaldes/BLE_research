package com.example.and_medicalprototyping;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class BluetoothLeService extends Service {

    public static final String TAG = "BluetoothLeService";

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;

    private Binder binder = new LocalBinder();

    public boolean initialize() {
        // If bluetoothManager is null, try to set it
        if (bluetoothManager == null) {
            bluetoothManager = getSystemService(BluetoothManager.class);
            if (bluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        // For API level 18 and higher, get a reference to BluetoothAdapter through
        // BluetoothManager.
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("monosaicol","mmm.....");
        return binder;
    }

    class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            Log.d("monosaicol","mmm.....2");
            return BluetoothLeService.this;
        }
    }

}
