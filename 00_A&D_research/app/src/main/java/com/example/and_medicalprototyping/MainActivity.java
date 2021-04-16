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
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;


import static com.example.and_medicalprototyping.ADGattUUID.myWeightScaleCharacteristic;
import static com.example.and_medicalprototyping.ADGattUUID.myWeightScaleDescriptor;
import static com.example.and_medicalprototyping.ADGattUUID.myWeightScaleMetricCharacteristic;
import static com.example.and_medicalprototyping.ADGattUUID.myWeightScaleMetricService;
import static com.example.and_medicalprototyping.ADGattUUID.myWeightScaleService;


public class MainActivity extends AppCompatActivity {


    private static final int SELECT_DEVICE_REQUEST_CODE = 0;

    //Variables
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private BluetoothGatt mBluetoothGatt;
    private int REQUEST_ENABLE_BLUETOOTH = 1;
    private BluetoothDevice mDevice;
    private String currentConnectedDeviceMac ="";

    //Helps with finding BLE devices
    private CompanionDeviceManager deviceManager;


    // UI
    private TextView textViewWeight, textDeviceInfo;
    private int myRssi;
    private Boolean metric; //Lb/Kg
    private ArrayList<Double> WeightValuesList = new ArrayList<Double>();
    private ArrayList<WeightEntry> WeightEntryList = new ArrayList<WeightEntry>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //init UI
        textViewWeight = (TextView) findViewById(R.id.weightStats);
        textDeviceInfo = (TextView) findViewById(R.id.bleDeviceInfo);
        Button clearButton = (Button) findViewById(R.id.clear);
        Button scanButton = (Button) findViewById(R.id.ScanButton);




        clearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("monosaicol","User has clicked on the CLEAR button  ");
                clear();
            }
        });
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("monosaicol","User has clicked on the scanButton   ");
                scan();
            }
        });
    }

    //
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == SELECT_DEVICE_REQUEST_CODE && data != null) {

            BluetoothDevice deviceToPair =  data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);

            if (deviceToPair != null) {
                deviceToPair.createBond();
                Log.d("monosaicol", "Attempting Connection with:   " + deviceToPair.getName() + "  " + deviceToPair.getAddress());

                currentConnectedDeviceMac = deviceToPair.getAddress();
                if(!currentConnectedDeviceMac.equals("")) {
                    mDevice = mBluetoothAdapter.getRemoteDevice(currentConnectedDeviceMac);
                    if (mDevice != null) {
                        mDevice.connectGatt(MainActivity.this, true, bluetoothGattCallback);
                        mBluetoothAdapter.getBluetoothLeAdvertiser();
                    }
                }
            }
        } else {
            Log.d("monosaicol","We failed at connecting with the BLE try again (Restart APP??)"); //TODO: needs a delayed loop to search for BLEs again
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            BluetoothDevice device = gatt.getDevice();
            Log.d("monosaicol", "onConnectionStateChange()   " + device.getAddress() + ", " + device.getName() + ", status=" + status + " newState=" + newState);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.d("monosaicol", "GATT connection not successful");

            } else {
                Log.d("monosaicol", "GATT connection  successful!!!");
                gatt.discoverServices();
                textDeviceInfo.setText("We are PAIRED to: " + currentConnectedDeviceMac + "\n" + device.getName());
            }
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d("monosaicol", "onServicesDiscovered()  " + status);

            if(mBluetoothGatt == null && status == 0) {
                mBluetoothGatt = gatt;
                mBluetoothGatt.readRemoteRssi();
                //read Metric
                Log.d("monosaicol","!!!!!!!!!!!!!!!!!!!!!!!!!  " + mBluetoothGatt.readCharacteristic(mBluetoothGatt.getService(myWeightScaleMetricService).getCharacteristic(myWeightScaleCharacteristic)) + " " + mBluetoothGatt.getService(myWeightScaleMetricService).getCharacteristic(myWeightScaleMetricCharacteristic).getValue());
                debuggingCharacteristics(mBluetoothGatt);
                // mBluetoothGatt.readCharacteristic()
            }
        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            final byte[] dataInput = characteristic.getValue();

            if(dataInput != null && dataInput.length >0)
            {
                StringBuilder stringBuilder = null;
                stringBuilder = new StringBuilder(dataInput.length);
                for(byte byteChar : dataInput)
                    stringBuilder.append(String.format("%02X ", byteChar));
                Log.d("monosaicol", "DataInput:: properties: " + characteristic.getProperties() + " characteistic length  " + characteristic.getValue().length + "       : " + String.valueOf(stringBuilder));
            }
        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d("monosaicol", "onCharacteristicWrite()");
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d("monosaicol", "onCharacteristicChanged()  " + characteristic.getUuid());

            textViewWeight.setText("");
            final byte[] dataInput = characteristic.getValue();

                 Log.d("monosaicol","reading for WS received");
            int flag = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            String flagString = Integer.toBinaryString(flag);
            int offset=0;
            for(int index = flagString.length(); 0 < index ; index--) {
                String key = flagString.substring(index-1 , index);
                if(index == flagString.length()) {
                    double convertValue = 0;
                    if(key.equals("0"))
                        convertValue = 0.1f;
                    else
                        convertValue = 0.1f;

                    // Unit
                    offset+=1;

                    // Value
                    double value = (double)(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset)) * convertValue;
                    Log.d("monosaicol", "Weight Value :" + value + "  ");
                    offset+=2;
                    //Add value to a list, and display on Screen View List

                    WeightEntry entry = new WeightEntry();
                    entry.setWeight(Math.floor(value * 100) / 100);
                    entry.setMetric(metric);
                    entry.setRssi(myRssi);
                   // entry.setTimeStamp();

                    WeightEntryList.add(entry);
                }

            }

            //Update UI
            displayWeights();
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d("monsaicol", "onDescriptorRead() "  + status);
        }
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d("monosaicol", "onDescriptorWrite() "  + status);
        }
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {     BluetoothDevice device = gatt.getDevice();

            myRssi = rssi;
            textDeviceInfo.setText((textDeviceInfo.getText() != null ? textDeviceInfo.getText() : "") + "  " + String.valueOf(rssi));

            Log.d("monosaicol", "onReadRemoteRssi()  "  + myRssi);
            enableNotify();
        }
        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            Log.d("monosaicol", "onReliableWriteCompleted()");
        }
    };

    private void scan()
    {
        Log.d("monosaicol", "Setting up Scanning");
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
            BluetoothDeviceFilter deviceFilter = new BluetoothDeviceFilter.Builder()
                    // Match only Bluetooth devices whose name matches the pattern.                            // ****Right now I am not using any filters
                    //.setNamePattern(Pattern.compile("A&D"))
                    // Match only Bluetooth devices whose service UUID matches this pattern.
                    //  .addServiceUuid(new ParcelUuid(new UUID(0x123abcL, -1L)), null)
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
        }
    }

    //dettach from BLE device
    public void clear() {
        if(!currentConnectedDeviceMac.equals("")) {
            Log.d("monosaicol" , "closing: " + String.valueOf(mBluetoothGatt.getDevice().getName()));
            deviceManager.disassociate(currentConnectedDeviceMac);
            currentConnectedDeviceMac = "";
            textDeviceInfo.setText("");
            textViewWeight.setText("");
            WeightValuesList.clear();


            if (mBluetoothGatt == null) {
                return;
            }
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    private void enableNotify()
    {
        if(mBluetoothGatt.getService(myWeightScaleService).getCharacteristic(myWeightScaleCharacteristic).getDescriptor(myWeightScaleDescriptor).setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE )) {
            mBluetoothGatt.writeDescriptor(mBluetoothGatt.getService(myWeightScaleService).getCharacteristic(myWeightScaleCharacteristic).getDescriptor(myWeightScaleDescriptor));
            mBluetoothGatt.setCharacteristicNotification(mBluetoothGatt.getService(myWeightScaleService).getCharacteristic(myWeightScaleCharacteristic), true);
            Log.d("monosaicol","we have gone through the process of enabling Weight scale Notifications");
        }
    }

    private void displayWeights()
    {
        for (int i = WeightEntryList.size()-1; i >= 0; i--) {
            Log.d("monosaicol", ((String.valueOf((WeightEntryList.get(i).getWeight()))) + " " + (String.valueOf((WeightEntryList.get(i).getMetric()))) + " " + (String.valueOf((WeightEntryList.get(i).getTimeStamp()))+"\n")));
            textViewWeight.setText((textViewWeight.getText() != null ? textViewWeight.getText() : "") +  ((String.valueOf((WeightEntryList.get(i).getWeight()))) + " " + (String.valueOf((WeightEntryList.get(i).getMetric()))) + " " + (String.valueOf((WeightEntryList.get(i).getTimeStamp()))+"\n")));
        }
    }

    private void debuggingCharacteristics(BluetoothGatt gatt){

        Log.d("monosaicol", " readServices.... " + gatt.getServices().size());
        if (mBluetoothGatt != null) {
            for (BluetoothGattService service : gatt.getServices()) {
                if(service != null)
                {
                    Log.d("monosaicol", "Service: " + String.valueOf(service.getUuid()));
                    for(BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        if (characteristic != null) {
                            Log.d("monosaicol", "Characteristic: " + String.valueOf(characteristic.getUuid()));

                           /* if (characteristic.getUuid().equals(myWeightScaleMetricCharacteristic)) {
                                Log.d("monosaicol", "as;;e;;e;e;e;;e");
                            }*/
                        }
                    }
                }

               /* */
            }
        }
    }
}