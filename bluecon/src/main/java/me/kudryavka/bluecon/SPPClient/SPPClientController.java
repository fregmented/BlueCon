package me.kudryavka.bluecon.SPPClient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;

import me.kudryavka.bluecon.Consts.BT_UUID;
import me.kudryavka.bluecon.Consts.ENUMS;
import me.kudryavka.bluecon.Listeners.SPPListener;

/**
 * Created by seyriz on 2016. 10. 3..
 */

public class SPPClientController {
    private static final String TAG = "SPPClientController";

    private Context context;

    private ENUMS.BLUETOOTH_STATES bluetooth_states;

    private ArrayList<BluetoothDevice> pairedDevices = new ArrayList<>();
    private ArrayList<SPPListener> sppListeners = new ArrayList<>();
    private BluetoothAdapter bluetoothAdapter;
    private ConnectThread connectThread;
    private ComThread comThread;

    private BluetoothDevice conDevice;

    private Integer bufferSize;

    public SPPClientController(Context context) {
        this.context = context;
        getBlueToothAdapter();
        setBluetooth_states(ENUMS.BLUETOOTH_STATES.DISCONNECTED, null);
    }

    private BluetoothAdapter getBlueToothAdapter(){
        if(bluetoothAdapter == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
                if (bluetoothManager != null)
                    this.bluetoothAdapter = bluetoothManager.getAdapter();
            } else {
                this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            }
        }
        return this.bluetoothAdapter;
    }

    private boolean isBlueToothEnabled(){
        if(getBlueToothAdapter() == null){
            setBluetooth_states(ENUMS.BLUETOOTH_STATES.NOT_SUPPORT, null);
            return false;
        }
        else {
            if(!getBlueToothAdapter().isEnabled()){
                setBluetooth_states(ENUMS.BLUETOOTH_STATES.DISABLED, null);
                return false;
            }
            else {
                return true;
            }
        }
    }

    ArrayList<BluetoothDevice> getPairedDevices(){
        if(isBlueToothEnabled()){
            if(pairedDevices == null || pairedDevices.size() == 0) {
                Set<BluetoothDevice> pDevices = getBlueToothAdapter().getBondedDevices();
                pairedDevices = new ArrayList<BluetoothDevice>();
                for (BluetoothDevice device : pDevices) {
                    pairedDevices.add(device);
                }
            }
            return pairedDevices;
        }
        return null;
    }

    ENUMS.BLUETOOTH_STATES getBluetooth_states() {
        return bluetooth_states;
    }

    private void setBluetooth_states(ENUMS.BLUETOOTH_STATES bluetooth_states, String address) {
        this.bluetooth_states = bluetooth_states;
        for(SPPListener sppListener : sppListeners){
            switch (bluetooth_states){
                case CONNECTED:
                    sppListener.onBluetoothDeviceConnected(conDevice.getName(), conDevice.getAddress());
                case DISCONNECTED:
                    sppListener.onBluetoothDeviceDisconnected(address);
                    break;
                case CONNECTING:
                    sppListener.onBluetoothDeviceConnecting(address);
                    break;
                case NOT_SUPPORT:
                    sppListener.onBluetoothNotSupported();
                    break;
                case DISABLED:
                    sppListener.onBluetoothDisabled();
                    break;
            }
            sppListener.onBluetoothDisabled();
        }
    }

    void addSPPListener(SPPListener sppListener){
        Log.d(TAG, "LISTENER " + sppListener.getClass().getCanonicalName() + " ADDED");
        if(sppListeners==null){
            sppListeners = new ArrayList<>();
        }
        sppListeners.add(sppListener);
    }

    void removeSPPLiestener(SPPListener sppListener){
        Log.d(TAG, "LISTENER " + sppListener.getClass().getCanonicalName() + " REMOVED");
        if(sppListeners!=null){
            sppListeners.remove(sppListener);
        }
    }

    void init(BluetoothDevice device, int bufferSize){
        Log.d(TAG, "DEVICE : " + device + "\nBUFFER SIZE : " + bufferSize);
        this.conDevice = device;
        this.bufferSize = bufferSize;
    }

    void init(String address, int bufferSize){
        Log.d(TAG, "DEVICE by address : " + address + "\nBUFFER SIZE : " + bufferSize);
        this.conDevice = bluetoothAdapter.getRemoteDevice(address);
        this.bufferSize = bufferSize;
    }

    boolean isInited(){
        return (this.conDevice != null && bufferSize > 0);
    }

    void connect(){
        if(conDevice!=null) {
            Log.d(TAG, "CONNECT TO : " + conDevice);
            resetThreads();
            connectThread = new ConnectThread(conDevice);
            connectThread.start();
            setBluetooth_states(ENUMS.BLUETOOTH_STATES.CONNECTING, conDevice.getAddress());
        }
    }

    private void reconnect(){
        Log.d(TAG, "RECONNECT TO : " + conDevice);
        setBluetooth_states(ENUMS.BLUETOOTH_STATES.DISCONNECTED, conDevice.getAddress());
        connect();
    }

    private synchronized void connected(BluetoothSocket socket){
        Log.d(TAG, "CONNECTED : " + conDevice);
        resetThreads();
        comThread = new ComThread(socket, bufferSize);
        comThread.start();

        setBluetooth_states(ENUMS.BLUETOOTH_STATES.CONNECTED, conDevice.getAddress());
    }

    synchronized void stop() {
        if(conDevice!=null) {
            Log.d(TAG, "DISCONNECT : " + conDevice);
            resetThreads();
            setBluetooth_states(ENUMS.BLUETOOTH_STATES.DISCONNECTED, conDevice.getAddress());
        }
    }

    void send(byte[] data) {
        ComThread t;
        synchronized (this) {
            if (getBluetooth_states() == ENUMS.BLUETOOTH_STATES.CONNECTED) {
                t = comThread;
            }
            else {
                Log.e(TAG, "NOT CONNECTED");
                return;
            }
        }
        t.write(data);
    }

    private synchronized void resetThreads() {
        resetConnectThread();
        resetComThread();
    }

    private synchronized void resetConnectThread() {
        if (connectThread != null) {
            connectThread.interrupt();
            connectThread = null;
        }
    }

    private synchronized void resetComThread() {
        if (comThread != null) {
            comThread.interrupt();
            comThread = null;
        }
    }

    private class ConnectThread extends Thread{

        private final BluetoothDevice mDevice;
        private final BluetoothSocket mSocket;

        public ConnectThread(BluetoothDevice device){
            mDevice = device;
            BluetoothSocket tempSock = null;
            try{
                tempSock = device.createRfcommSocketToServiceRecord(BT_UUID.UUID_SPP);
            }
            catch (IOException e){
                try{
                    tempSock = device.createInsecureRfcommSocketToServiceRecord(BT_UUID.UUID_SPP);
                }
                catch (IOException e2){
                    Log.e(TAG, "Failed to create SOCKET!", e2);
                }
            }
            mSocket = tempSock;
        }

        @Override
        public void run() {
            if(mSocket!=null){
                try{
                    mSocket.connect();
                }
                catch (IOException e){
                    Log.e(TAG, "Failed to connect to SOCKET. retrying...");
                    reconnect();
                    interrupt();
                }
                synchronized (SPPClientController.this){
                    connectThread = null;
                }
                connected(mSocket);
            }
        }

        @Override
        public void interrupt() {
            try{
                mSocket.close();
            }
            catch (IOException e){
                Log.e(TAG, "SOCKET closing FAILED");
            }
            super.interrupt();
        }
    }

    private class ComThread extends Thread{
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private final int buffSize;

        public ComThread(BluetoothSocket socket, int buffSize){
            this.socket = socket;
            InputStream tempIS = null;
            OutputStream tempOS = null;

            try{
                tempIS = socket.getInputStream();
                tempOS = socket.getOutputStream();
            }
            catch (IOException e){
                Log.e(TAG, "FAILED TO OPEN I/O STREAM");
            }
            inputStream = tempIS;
            outputStream = tempOS;
            this.buffSize = buffSize;
        }

        @Override
        public void run() {
            byte[] data = new byte[buffSize];
            int len;

            while (true) {
                try {
                    len = inputStream.read(data);
                    byte[] read = new byte[len];
                    System.arraycopy(data, 0, read, 0, len);
                    for (SPPListener sppListener : sppListeners) {
                        sppListener.onPacketReceived(conDevice.getAddress(), read);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "FAILED TO READ PACKET", e);
                    reconnect();
                    break;
                }
            }
            interrupt();
        }

        public void write(byte[] data){
            try{
                outputStream.write(data);
                for(SPPListener sppListener : sppListeners){
                    sppListener.onPacketSended(conDevice.getAddress(), data);
                }
            }
            catch (IOException e){
                Log.e(TAG, "FAILED TO WRITE PACKET", e);
            }
        }

        @Override
        public void interrupt() {
            try{
                socket.close();
            }
            catch (IOException e){
                Log.e(TAG, "FAILED TO CLOSE SOCKET");
            }
            super.interrupt();
        }
    }
}
