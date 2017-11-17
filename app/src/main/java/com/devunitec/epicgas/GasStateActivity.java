package com.devunitec.epicgas;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.os.ParcelUuid;
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
import java.util.UUID;

public class GasStateActivity extends AppCompatActivity {

    private static final String LOG_TAG = GasStateActivity.class.getSimpleName();
    private static final UUID MODULE_BT_UUID = UUID.fromString("2e051fd6-c749-11e7-abc4-cec278b6b50a");
    private static final int MESSAGE_READ = 0;
    private final int REQUEST_ENABLE_BT = 1;

    private ConnectedThread mConnectedThread;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothSocket mBluetoothSocket = null;


    private RelativeLayout viewFoundDevice;
    private TextView foundDeviceTextView;
    private ProgressBar progressBar;

    private RelativeLayout viewMain;
    private FloatingActionButton fab;
    private Boolean findDevice = false;
    private ProgressDialog mProgressDialog;

    private Handler bluetoothIn;

    private String inputStreamData;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getAddress().equals("00:21:13:01:4F:10")) {
                    findDevice = true;
                    mBluetoothDevice = device;
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (findDevice) {
                    viewFoundDevice.setVisibility(View.GONE);
                    // mProgressDialog = ProgressDialog.show(getApplicationContext(), "Connecting Bluetooth", "Please Wait...", true);
                    viewMain.setVisibility(View.VISIBLE);
                    fab.setVisibility(View.VISIBLE);
                    fab.setImageResource(R.drawable.ic_action_call);
                    fab.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String numberCall = "9991532758";
                            Intent intent = new Intent(Intent.ACTION_DIAL);
                            intent.setData(Uri.parse("tel:" + numberCall));
                            startActivity(intent);
                        }
                    });
                    if (mBluetoothDevice != null) {
                        try {
                            mBluetoothSocket = createBluetoothSocket(mBluetoothDevice);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        try {
                            mBluetoothSocket.connect();
                        } catch (IOException e) {
                            try {
                                mBluetoothSocket.close();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                            e.printStackTrace();
                        }
                        mConnectedThread = new ConnectedThread(mBluetoothSocket);
                        mConnectedThread.start();
                    }
                    // mProgressDialog.cancel();

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
                            progressBar.setVisibility(View.VISIBLE);
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

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        checkStateBt(mBluetoothAdapter);

        viewFoundDevice = (RelativeLayout) findViewById(R.id.view_found_device);
        foundDeviceTextView = (TextView) findViewById(R.id.tv_found_device);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        viewMain = (RelativeLayout) findViewById(R.id.view_main);

        fab = (FloatingActionButton) findViewById(R.id.fab);

        bluetoothIn = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_READ) {
                    String inputMessage = (String) msg.obj;
                    Log.e(LOG_TAG, inputMessage);
                    //Toast.makeText(getBaseContext(), inputMessage, Toast.LENGTH_SHORT).show();
                }
            }
        };
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
                fab.setVisibility(View.GONE);
                viewMain.setVisibility(View.GONE);
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

    private BluetoothSocket createBluetoothSocket(BluetoothDevice bluetoothDevice) throws IOException {
        return bluetoothDevice.createRfcommSocketToServiceRecord(bluetoothDevice.getUuids()[0].getUuid());
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    Log.e(LOG_TAG + " 2", buffer.toString());
                    bluetoothIn.obtainMessage(MESSAGE_READ, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();

            }
        }
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

    @Override
    protected void onPause() {
        super.onPause();
        try {
            mBluetoothSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        unregisterReceiver(broadcastReceiver);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void permissionCheck() {
        int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
        permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
        if (permissionCheck != 0) {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
        }
    }

    private void checkStateBt(BluetoothAdapter bluetoothAdapter) {
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableIntentBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntentBt, REQUEST_ENABLE_BT);
            }
        } else {
            Toast.makeText(this, "Device does not support bluetooth", Toast.LENGTH_SHORT).show();
        }
    }
}
