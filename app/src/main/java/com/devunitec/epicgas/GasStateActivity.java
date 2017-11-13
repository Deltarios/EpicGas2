package com.devunitec.epicgas;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class GasStateActivity extends AppCompatActivity {

    private static final String LOG_TAG = GasStateActivity.class.getSimpleName();
    private static final UUID MODULE_BT_UUID = UUID.fromString("2e051fd6-c749-11e7-abc4-cec278b6b50a");
    private static final int MESSAGE_READ = 0;
    private final int REQUEST_ENABLE_BT = 1;

    private BluetoothConnectionService mConnectionService;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;

    private RelativeLayout viewFoundDevice;
    private TextView foundDeviceTextView;
    private ProgressBar progressBar;

    private RelativeLayout viewMain;
    private FloatingActionButton fab;
    private Boolean findDevice = false;

    private Handler mHandler;
    private String mDataInput;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getAddress().equals("20:16:10:25:08:15") && device.getName().equals("HC-05")) {
                    findDevice = true;
                    mBluetoothDevice = device;
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (findDevice) {
                    viewFoundDevice.setVisibility(View.GONE);
                    mConnectionService = new BluetoothConnectionService(GasStateActivity.this);
                    mConnectionService.startClient(mBluetoothDevice, MODULE_BT_UUID);
                    viewMain.setVisibility(View.VISIBLE);
                    fab.setVisibility(View.VISIBLE);
                    fab.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String numberCall = "999203922";
                            Intent intent = new Intent(Intent.ACTION_DIAL);
                            intent.setData(Uri.parse("tel:" + numberCall));
                        }
                    });
                } else {
                    if (viewMain.getVisibility() == View.VISIBLE) {
                        viewMain.setVisibility(View.GONE);
                    }
                    progressBar.setVisibility(View.GONE);
                    foundDeviceTextView.setText("Device not found");
                    fab.setVisibility(View.VISIBLE);
                    fab.setImageResource(R.drawable.ic_refresh_button);
                    fab.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mBluetoothAdapter.startDiscovery();
                            fab.setVisibility(View.GONE);
                        }
                    });
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gas_state);

        viewFoundDevice = (RelativeLayout) findViewById(R.id.view_found_device);
        foundDeviceTextView = (TextView) findViewById(R.id.tv_found_device);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        viewMain = (RelativeLayout) findViewById(R.id.view_main);

        fab = (FloatingActionButton) findViewById(R.id.fab);

        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                if(msg.what == MESSAGE_READ) {
                    String readMessage = (String) msg.obj;
                    Log.i(LOG_TAG, readMessage);
                    mDataInput = readMessage;
                }
            }
        };

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        checkStateBt(this, mBluetoothAdapter);

    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, intentFilter);

        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        } else {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                permissionCheck();
            }

            if (mBluetoothAdapter.startDiscovery()) {
                viewFoundDevice.setVisibility(View.VISIBLE);
                foundDeviceTextView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);
            } else {
                foundDeviceTextView.setText("Error to start discovery device...");
            }
        }

        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_language:
                return true;
            case R.id.action_select_tank:
                return true;
            case R.id.action_about:
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void permissionCheck() {
        int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
        permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
        if (permissionCheck != 0) {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
        }
    }

    public class BluetoothConnectionService {
        private final String LOG_TAG = BluetoothConnectionService.class.getSimpleName();
        private final String NAME_APP = String.valueOf(R.string.app_name);
        private final UUID MODULE_BT_UUID = UUID.fromString("2e051fd6-c749-11e7-abc4-cec278b6b50a");
        private final int MESSAGE_READ = 0;
        public boolean changeAvail = false;


        private final BluetoothAdapter mBluetoothAdapter;
        private BluetoothDevice mBluetoothDevice;
        private Context mContext;

        private UUID mDeviceUUID;
        private AcceptThread mAcceptThread;
        private ConnectThread mConnectThread;
        private StreamThread mStreamThread;
        private ProgressDialog mProgressDialog;


        public BluetoothConnectionService(Context context) {
            this.mContext = context;
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            start();
        }

        public void startClient(BluetoothDevice device, UUID uuid) {
            mProgressDialog = ProgressDialog.show(mContext, "Connecting Bluetooth"
                    , "Please Wait...", true);

            mConnectThread = new ConnectThread(device, uuid);
            mConnectThread.start();
        }

        private void connected(BluetoothSocket socket, BluetoothDevice device) {
            Log.d(LOG_TAG, "connected: Starting.");
            mStreamThread = new StreamThread(socket);
            mStreamThread.start();
        }

        private synchronized void start() {
            Log.d(LOG_TAG, "start");

            if(mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }

            if(mAcceptThread == null) {
                mAcceptThread = new AcceptThread();
                mAcceptThread.start();
            }
        }

        public void write(byte[] out) {
            // Create temporary object
            StreamThread r;

            // Synchronize a copy of the ConnectedThread
            Log.d(LOG_TAG, "write: Write Called.");
            //perform the write
            mStreamThread.write(out);
        }

        private class AcceptThread extends Thread{
            private final BluetoothServerSocket bluetoothServerSocket;

            public AcceptThread() {
                BluetoothServerSocket tmpSocket = null;

                try {
                    tmpSocket = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME_APP, MODULE_BT_UUID);
                    Log.d(LOG_TAG, "AcceptThread: Setting up Server using: " + MODULE_BT_UUID);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Accept Thread - IOException: " + e.getMessage());
                    e.printStackTrace();
                }
                bluetoothServerSocket = tmpSocket;
            }

            @Override
            public void run() {
                Log.d(LOG_TAG, "run: AcceptThread Running.");
                BluetoothSocket bluetoothSocket = null;

                try {
                    Log.d(LOG_TAG, "run: RFCOM server socket start.....");
                    bluetoothSocket = bluetoothServerSocket.accept();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "AcceptThread - IOException: " + e.getMessage());
                    e.printStackTrace();
                }

                if(bluetoothSocket != null) {
                    connected(bluetoothSocket, mBluetoothDevice);
                }
                Log.i(LOG_TAG, "end AcceptThread ");
            }

            public void cancel() {
                Log.d(LOG_TAG, "cancel: Canceling AcceptThread.");
                try {
                    bluetoothServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(LOG_TAG, "cancel: Close of AcceptThread ServerSocket failed. " + e.getMessage());
                }
            }
        }

        private class ConnectThread extends Thread {
            private BluetoothSocket bluetoothSocket;

            public ConnectThread(BluetoothDevice device, UUID uuid) {
                Log.d(LOG_TAG, "ConnectThread: started.");
                mBluetoothDevice = device;
                mDeviceUUID = uuid;
            }

            @Override
            public void run() {
                BluetoothSocket socket = null;
                Log.i(LOG_TAG, "RUN ConnectThread ");

                try {
                    Log.d(LOG_TAG, "ConnectThread: Trying to create Socket using UUID: " + mBluetoothDevice.getUuids()[0].getUuid());
                    socket = mBluetoothDevice.createRfcommSocketToServiceRecord(mBluetoothDevice.getUuids()[0].getUuid());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                bluetoothSocket = socket;
                mBluetoothAdapter.cancelDiscovery();

                try {
                    bluetoothSocket.connect();
                    Log.d(LOG_TAG, "run: ConnectThread connected.");
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        bluetoothSocket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        Log.e(LOG_TAG, "mConnectThread: run: Unable to close connection in socket " + e1.getMessage());
                    }
                    Log.d(LOG_TAG, "run: ConnectThread: Could not connect to UUID: " + MODULE_BT_UUID);
                }
                connected(bluetoothSocket, mBluetoothDevice);
            }

            public void cancel() {
                try {
                    Log.d(LOG_TAG, "cancel: Closing Client Socket.");
                    bluetoothSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(LOG_TAG, "cancel: close() of Socket in ConnectThread failed. " + e.getMessage());
                }
            }
        }

        private class StreamThread extends Thread {
            private final BluetoothSocket bluetoothSocket;
            private final InputStream inputStream;
            private final OutputStream outputStream;

            public StreamThread(BluetoothSocket bluetoothSocket) {
                Log.d(LOG_TAG, "StreamThread: Starting.");
                this.bluetoothSocket = bluetoothSocket;
                InputStream tmpInputStream = null;
                OutputStream tmpOutputStream = null;


                try {
                    mProgressDialog.dismiss();
                } catch (NullPointerException exception) {
                    exception.printStackTrace();
                }

                try {
                    tmpInputStream = bluetoothSocket.getInputStream();
                    tmpOutputStream = bluetoothSocket.getOutputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                inputStream = tmpInputStream;
                outputStream = tmpOutputStream;
            }

            @Override
            public void run() {
                byte[] dataByte = new byte[1];
                int bytes;

                while(true) {
                    try {
                        changeAvail = true;
                        bytes = inputStream.read(dataByte);
                        String incomingMessage = new String(dataByte, 0, bytes);
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, incomingMessage).sendToTarget();

                        // Log.d(LOG_TAG, "InputStream: " + incomingMessage);
                    } catch (IOException e) {
                        changeAvail = false;
                        Log.e(LOG_TAG, "write: Error reading Input Stream. " + e.getMessage());
                        break;
                    }
                }
            }

            public void write(byte[] bytes) {
                String text = new String(bytes, Charset.defaultCharset());
                Log.d(LOG_TAG, "write: Writing to outputstream: " + text);
                try {
                    outputStream.write(bytes);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "write: Error writing to output stream. " + e.getMessage());
                }
            }

            public void cancel() {
                try {
                    bluetoothSocket.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public void checkStateBt(AppCompatActivity activity, BluetoothAdapter bluetoothAdapter) {
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableIntentBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activity.startActivityForResult(enableIntentBt, REQUEST_ENABLE_BT);
            }
        } else {
            Toast.makeText(activity.getBaseContext(), "Device does not support bluetooth", Toast.LENGTH_SHORT).show();
        }
    }
}
