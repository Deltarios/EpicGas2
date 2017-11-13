package com.devunitec.epicgas;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.devunitec.epicgas.services.BluetoothConnectionService;

import java.util.ArrayList;
import java.util.UUID;

public class GasStateActivity extends AppCompatActivity {

    private static final String LOG_TAG = GasStateActivity.class.getSimpleName();
    private static final UUID MODULE_BT_UUID = UUID.fromString("2e051fd6-c749-11e7-abc4-cec278b6b50a");

    private BluetoothConnectionService mConnectionService;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;


    private RelativeLayout viewFoundDevice;
    private TextView foundDeviceTextView;
    private ProgressBar progressBar;

    private RelativeLayout viewMain;
    private GradientDrawable levelGasColor;
    private TextView averageTankTextView;

    private FloatingActionButton fab;

    private Boolean findDevice = false;


    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getAddress().equals("20:16:10:25:08:15") && device.getName().equals("HC-05") ) {
                    findDevice = true;
                    mBluetoothDevice = device;
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if(findDevice) {
                    mBluetoothAdapter.cancelDiscovery();
                    viewFoundDevice.setVisibility(View.GONE);
                    mConnectionService = new BluetoothConnectionService(GasStateActivity.this);
                    mConnectionService.startClient(mBluetoothDevice, MODULE_BT_UUID);
                    viewMain.setVisibility(View.VISIBLE);
                } else {
                    if(viewMain.getVisibility() == View.VISIBLE) {
                        viewMain.setVisibility(View.GONE);
                    }
                    progressBar.setVisibility(View.GONE);
                    foundDeviceTextView.setText("Device not found");
                    mBluetoothAdapter.startDiscovery();
                    fab.setImageResource(R.drawable.ic_refresh_button);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gas_state);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothConnectionService.checkStateBt(this, mBluetoothAdapter);

        viewFoundDevice = (RelativeLayout) findViewById(R.id.view_found_device);
        foundDeviceTextView = (TextView) findViewById(R.id.tv_found_device);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        viewMain = (RelativeLayout) findViewById(R.id.view_main);
//        int magnitudeColor = R.color.colorAccent;
//        levelGasColor.setColor(ContextCompat.getColor(this, magnitudeColor));
        averageTankTextView = (TextView) findViewById(R.id.tv_average_tank);

        fab = (FloatingActionButton) findViewById(R.id.fab);
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, intentFilter);

        if(mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        } else {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                permissionCheck();
            }

            if(mBluetoothAdapter.startDiscovery()) {
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
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_language:
                return true;
            // Respond to a click on the "Delete all entries" menu option
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
}
