package com.devunitec.epicgas.services;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.devunitec.epicgas.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * Created by Deltarios on 11/11/17.
 */

public class BluetoothConnectionService {
    private static final String LOG_TAG = BluetoothConnectionService.class.getSimpleName();
    private static final String NAME_APP = String.valueOf(R.string.app_name);
    private static final UUID MODULE_BT_UUID = UUID.fromString("2e051fd6-c749-11e7-abc4-cec278b6b50a");
    private static final int REQUEST_ENABLE_BT = 1;

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

    public String sendInput() {
        return mStreamThread.passDataInput();
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

        private String dataSendInput;

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
            byte[] dataByte = new byte[1024];
            int bytes;

            while(true) {
                try {
                    bytes = inputStream.read(dataByte);
                    String incomingMessage = new String(dataByte, 0, bytes);
                    Log.d(LOG_TAG, "InputStream: " + incomingMessage);
                } catch (IOException e) {
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
            dataSendInput = text;
        }

        public String passDataInput() {
            return dataSendInput;
        }

        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
            }
        }
    }

    public static void checkStateBt(AppCompatActivity activity, BluetoothAdapter bluetoothAdapter) {
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
