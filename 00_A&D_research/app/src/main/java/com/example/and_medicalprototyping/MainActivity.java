package com.example.and_medicalprototyping;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ;
import static com.example.and_medicalprototyping.ADGattUUID.AndCustomCharacteristic;
import static com.example.and_medicalprototyping.ADGattUUID.AndCustomWeightScaleMeasurement;
import static com.example.and_medicalprototyping.ADGattUUID.WeightScaleMeasurement;


public class MainActivity extends AppCompatActivity  {


    private static final int SELECT_DEVICE_REQUEST_CODE = 0;

    public static final String ACTION_BOND_STATE_CHANGED = "";

    //Variables
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private BluetoothGatt mBluetoothGatt;
    private int REQUEST_ENABLE_BLUETOOTH = 1;

    private String connectedDeviceMac ="";
    private BluetoothDevice mDevice;

    CompanionDeviceManager deviceManager;

    ////
    ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
    ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();
    ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();


    ///

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //onCreate() I am only setting up buttons
        Button scanButton = (Button) findViewById(R.id.scan);
        Button pairButton = (Button) findViewById(R.id.pair);
        Button clearButton = (Button) findViewById(R.id.clear);
        Button comandButton = (Button) findViewById(R.id.comandButton);


        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("monosaicol", "Seting up Scanning");
                //1) is BLE Supported?
                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                    Toast.makeText(MainActivity.this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
                    finish();
                }
                else {
                    Toast.makeText(MainActivity.this, R.string.ble_supported, Toast.LENGTH_SHORT).show();
                    //Check if it is disabled, if so enable it
                    //2) BLE Adapter and Manager Set Up
                    mBluetoothManager =  (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                    mBluetoothAdapter = mBluetoothManager.getAdapter();
                    // Enable Bluetooth
                    // Ensures Bluetooth is available on the device and it is enabled. If not,
                    // displays a dialog requesting user permission to enable Bluetooth.
                    if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new
                                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
                    }
                    //3) Finding BLE devices
                    //"Companion Device"
                    //For Android 8.0 (API level 26) and higher
                    //*Note disassociate(). An app is responsible for clearing its own associations if the user no longer needs them, such as when they log out or remove bonded devices.
                    //public void disassociate (String deviceMacAddress)

                    BluetoothDeviceFilter deviceFilter = new BluetoothDeviceFilter.Builder()
                            // Match only Bluetooth devices whose name matches the pattern.
                            // .setNamePattern(Pattern.compile("A&D_UA-651BLE_75E8D1"))
                            // Match only Bluetooth devices whose service UUID matches this pattern.
                            //.addServiceUuid(new ParcelUuid(new UUID(0x123abcL, -1L)), null)
                            .build();


                    AssociationRequest pairingRequest = new AssociationRequest.Builder()
                            // Find only devices that match this request filter.
                            .addDeviceFilter(deviceFilter)
                            // Stop scanning as soon as one device matching the filter is found.
                            .setSingleDevice(false)
                            .build();

                    deviceManager = (CompanionDeviceManager) getSystemService(Context.COMPANION_DEVICE_SERVICE);


                    deviceManager.associate(pairingRequest, new CompanionDeviceManager.Callback() {
                        // Called when a device is found. Launch the IntentSender so the user can
                        // select the device they want to pair with.
                        @Override
                        public void onDeviceFound(IntentSender chooserLauncher) {
                            Log.d("monosaicol","ON Device found:   " + chooserLauncher.getCreatorPackage() + "  "  );
                            try {
                                Log.d("monosaicol","Scanning Devices....");
                                startIntentSenderForResult(
                                        chooserLauncher, SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0
                                );
                            } catch (IntentSender.SendIntentException e) {
                                Log.e("MainActivity", "Failed to send intent");
                            }
                        }
                        @Override
                        public void onFailure(CharSequence error) {
                            // Handle the failure.
                        }
                    }, null);
                    Log.d("monosaicol","Done enabling Scan for Ble Devices");
                }

            }
        });



        pairButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                if(!connectedDeviceMac.equals("")) {
                    Log.d("monosaicol","Paring with:  " + connectedDeviceMac);
                    mDevice = mBluetoothAdapter.getRemoteDevice(connectedDeviceMac);
                    if (mDevice != null) {

                        mDevice.connectGatt(MainActivity.this, true, bluetoothGattCallback);
                        mBluetoothAdapter.getBluetoothLeAdvertiser();
                    }
                }
            }
        });



        clearButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                Log.d("monosaicol","User has clicked on the CLEAR button  ");

                close();
            }
        });


        comandButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                Log.d("monosaicol","User has clicked on the comandButton button  ");

                //// I need to create a command queue,
                // Send 1 command wait for response, send next command (this is how BLE works)

            }
        });



    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == SELECT_DEVICE_REQUEST_CODE && data != null) {

            BluetoothDevice deviceToPair =  data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);


            if (deviceToPair != null) {
                deviceToPair.createBond();

                //connectedDeviceMac =  deviceToPair.getAddress();
                Log.d("monosaicol", "Connecting to:   " + deviceToPair.getName());
                Log.d("monosaicol", deviceToPair.getAddress());
                connectedDeviceMac = deviceToPair.getAddress();

                // Log.d("monosaicol", String.valueOf(mDevice.getBondState()));


                ///////////////////////////////////////////////
                /////////This is working for me, this is a step further.... --> helps to identify BLE devices

            }
        } else {
            Log.d("monosaicol","Entramos por Aqui porq no teniamos bluetooth disponible");
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            BluetoothDevice device = gatt.getDevice();
            Log.d("monosaicol", "onConnectionStateChange()" + device.getAddress() + ", " + device.getName() + ", status=" + status + " newState=" + newState);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.d("monosaicol", "GATT connection not successful");

            } else {
                Log.d("monosaicol", "GATT connection  successful!!!");
                gatt.discoverServices();
            }
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            mBluetoothGatt = gatt;
            Log.d("monosaicol", "onServicesDiscovered()  " + mBluetoothGatt);
            //We have found Services, lets connect
            //requestReadFirmRevision();
            //readServices();
        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d("monosaicol", "onCharacteristicRead!!!!!!!!!!!!!  " + String.valueOf(status));
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("monosaicol","okay...................  " + characteristic.getUuid());
                //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d("monosaicol", "onCharacteristicWrite()");
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d("monosaicol", "onCharacteristicChanged()");
        }
        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d("monsaicol", "onDescriptorRead()" );
        }
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d("monosaicol", "onDescriptorWrite()" );
        }
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {     BluetoothDevice device = gatt.getDevice();
            Log.d("monosaicol", "onReadRemoteRssi()" );
        }
        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            Log.d("monosaicol", "onReliableWriteCompleted()");
        }

    };

    public void readServices() {




        Log.d("monosaicol", "requestDeviceInfo");
        if (mBluetoothGatt != null) {
            for (BluetoothGattService service :  mBluetoothGatt.getServices()) {
                Log.d("monosaicol", "Service:   " + service.getUuid());
                if (service != null) {
                    mGattCharacteristics.add((ArrayList<BluetoothGattCharacteristic>) service.getCharacteristics());
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        Log.d("monosaicol", "BluetoothGattCharacteristic:   " + characteristic.getUuid().toString() + "  " + AndCustomCharacteristic + "   " + characteristic.getDescriptors() + "   " + characteristic.getValue());

                        if (characteristic.getUuid().equals(AndCustomCharacteristic)) {
                            final byte[] dataInput = characteristic.getValue();

                            Log.d("monosaicol", String.valueOf(dataInput));
                            Log.d("monosaicol", "------------------------------------------Okay we have a characteristic =  to " + AndCustomCharacteristic);
                            mBluetoothGatt.setCharacteristicNotification(characteristic, true);
                        }
                        if (characteristic.getUuid().equals(AndCustomWeightScaleMeasurement)) {

                            final byte[] dataInput = characteristic.getValue();

                            Log.d("monosaicol", String.valueOf(dataInput));
                            Log.d("monosaicol", "------------------------------------------Okay we have a characteristic =  to " + AndCustomWeightScaleMeasurement);
                            mBluetoothGatt.setCharacteristicNotification(characteristic, true);
                        }
                        Log.d("monosaicol", String.valueOf(characteristic.getProperties()));
                        if (characteristic.getProperties() == 0) {
                            Log.d("monosaicol", "we have properties that can be read:);");
                        }
                        if (PROPERTY_READ == 0) {
                            Log.d("monosaicol", "we have 000000can be read:);");
                        }
                        if (mBluetoothGatt.readCharacteristic(characteristic)) {
                            Log.e("monosaicol", "readCharacteristic  for : " + characteristic.getUuid());
                        }
                    //    if(mBluetoothGatt.getConnectionState(mBluetoothAdapter.getRemoteDevice(connectedDeviceMac)));
                    //    Log.d("monosaicol", String.valueOf(mBluetoothGatt.getConnectionState(mBluetoothAdapter.getRemoteDevice(connectedDeviceMac))));
                    }
                }
            }
        }
    }
    public void close() {




        if(!connectedDeviceMac.equals("")) {

            //Find what characteristics hold the measurements
            //mBluetoothGatt.setCharacteristicNotification(characteristic, true);
         /*   Log.d("monosaicol" , String.valueOf(mBluetoothAdapter.getBluetoothLeAdvertiser().toString()));

            Log.d("monosaicol" , String.valueOf(mBluetoothGatt.getDevice().getName()));

            deviceManager.disassociate(connectedDeviceMac);

            if (mBluetoothGatt == null) {
                return;
            }
            mBluetoothGatt.close();
            mBluetoothGatt = null;*/

        }
        if(mGattCharacteristics == null)
        {
            Log.d("monosaicol", "mGattCharacteristics is == null");
        }
        else
            Log.d("monosaicol", "mGattCharacteristics is != null");

    }
}