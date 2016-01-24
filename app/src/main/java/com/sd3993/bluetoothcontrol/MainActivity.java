package com.sd3993.bluetoothcontrol;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_ENABLE_BT = 0;

    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket btSocket;
    ArrayAdapter mArrayAdapter;
    ArrayList<String> deviceList;
    ListView listDevices;
    TextView textStatus;
    Switch ledSwitch, dcmSwitch;
    Button btnBluetooth;
    boolean btConnected;

    IntentFilter mIntentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent & extract the name and address
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                deviceList.add(device.getName() + "\n" + device.getAddress());
            }
            mArrayAdapter.notifyDataSetChanged();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            //Show a message that the device has no bluetooth adapter
            Toast.makeText(getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();
            finish(); //finish app
        } else {
            registerReceiver(mReceiver, mIntentFilter);
            if (!mBluetoothAdapter.isEnabled()) {
                //Ask to the user turn the bluetooth on
                Intent turnBTOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnBTOn, REQUEST_ENABLE_BT);
            }
        }

        deviceList = new ArrayList<>();
        listDevices = (ListView) findViewById(R.id.list_Device);

        mArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);
        listDevices.setAdapter(mArrayAdapter);
        listDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView mAdapterView, View view, int arg2, long arg3) {
                String pairDevice = ((TextView) view).getText().toString();
                new ConnectBT().execute(pairDevice);
            }
        });

        textStatus = (TextView) findViewById(R.id.text_Status);
        ledSwitch = (Switch) findViewById(R.id.switch_LED);
        dcmSwitch = (Switch) findViewById(R.id.switch_DCM);
        btnBluetooth = (Button) findViewById(R.id.btn_Bluetooth);

        ledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                controlLED(isChecked);
            }
        });
        dcmSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                controlDCM(isChecked);
            }
        });

        btConnected = false;
        updateBTStatus("<Error>");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                scanDevices();
            } else
                finish(); //finish app
        }
    }

    private class ConnectBT extends AsyncTask<String, Void, Void>  // UI thread
    {
        private ProgressDialog mProgressDialog;
        private boolean isBtConnected = false;
        final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        String pairName, pairMAC;

        @Override
        protected void onPreExecute()
        {
            mProgressDialog = ProgressDialog.show(MainActivity.this, "Connecting...", "Please wait");  //show a progress dialog
            btConnected = true;
        }

        @Override
        protected Void doInBackground(String... params) //while the progress dialog is shown, the connection is done in background
        {
            String pairDevice = params[0];
            pairName = pairDevice.substring(0, pairDevice.length() - 18);
            pairMAC = pairDevice.substring(pairDevice.length() - 17);
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                    BluetoothDevice pairingDevice = mBluetoothAdapter.getRemoteDevice(pairMAC);//connects to the device's address and checks if it's available
                    btSocket = pairingDevice.createInsecureRfcommSocketToServiceRecord(mUUID);//create a RFCOMM (SPP) connection
                    mBluetoothAdapter.cancelDiscovery();
                    btSocket.connect();//start connection
                }
            }
            catch (IOException e)
            {
                btConnected = false; //if the try failed, you can check the exception here
                updateBTStatus("<Error>");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after doInBackground()
        {
            super.onPostExecute(result);
            updateBTStatus(pairName);

            if (!btConnected)
            {
                Toast.makeText(getApplicationContext(),"Connection failed! Please try again",Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(getApplicationContext(),"Connected",Toast.LENGTH_SHORT).show();
                isBtConnected = true;
            }
            mProgressDialog.dismiss();
        }
    }

    public void scanDevices() {
        deviceList.clear();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size()>0)
            for (BluetoothDevice bt : pairedDevices)
                deviceList.add(bt.getName() + "\n" + bt.getAddress()); //Get the device's name and the address

        mBluetoothAdapter.startDiscovery();
        mArrayAdapter.notifyDataSetChanged();
    }

    public void clickAll(View view) {
        ledSwitch.setChecked(true);
        dcmSwitch.setChecked(true);
    }

    public void clickNone(View view) {
        ledSwitch.setChecked(false);
        dcmSwitch.setChecked(false);
    }

    @SuppressLint("SetTextI18n")
    public void updateBTStatus(String pairName) {
        textStatus.setText("Status : " + (btConnected ? "Connected to " + pairName : "Not Connected"));
        btnBluetooth.setText(btConnected ? "Disconnect" : "Scan");
        findViewById(R.id.card_DeviceList).setVisibility(btConnected ? View.INVISIBLE : View.VISIBLE);
    }

    public void setBtnBluetoothFunc(View view) {
        if(!btConnected)
            scanDevices();
        else
            disconnectDevice();
    }

    public void disconnectDevice()
    {
        if (btSocket!=null)
        {
            try
            {
                btSocket.close(); //close connection
                btConnected = false;
                updateBTStatus("<Error>");
            }
            catch (IOException e)
            {
                Toast.makeText(getApplicationContext(),"Error",Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void controlLED(boolean status)
    {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write(status ? "a".getBytes() : "b".getBytes());
            }
            catch (IOException e)
            {
                Toast.makeText(getApplicationContext(),"Error",Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void controlDCM(boolean status)
    {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write(status ? "c".getBytes() : "d".getBytes());
            }
            catch (IOException e)
            {
                Toast.makeText(getApplicationContext(),"Error",Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
}
