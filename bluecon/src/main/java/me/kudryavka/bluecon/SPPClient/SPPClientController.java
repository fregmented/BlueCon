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
import me.kudryavka.bluecon.SPPServer.SPPServerController;

/**
 * Created by seyriz on 2016. 10. 3..
 */

public class SPPClientController {
    private static final String TAG = "SPPClientController";

    private Context context;

    private static SPPClientController instance;

    private ENUMS.BLUETOOTH_STATES bluetooth_states;

    private ArrayList<BluetoothDevice> pairedDevices = new ArrayList<>();
    private ArrayList<SPPListener> sppListeners = new ArrayList<>();
    private BluetoothAdapter bluetoothAdapter;
    private ConnectionThread connectionThread;
    private CommunicationThread communicationThread;

    private BluetoothDevice conDevice;

    private Integer bufferSize;

    public static SPPClientController getInstance(Context context) {
        if(instance==null){
            instance = new SPPClientController(context);
        }
        return instance;
    }

    private SPPClientController(Context context) {
        this.context = context;
        isBlueToothEnabled();
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

    public ArrayList<BluetoothDevice> getPairedDevices(){
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

    public ENUMS.BLUETOOTH_STATES getBluetooth_states() {
        return bluetooth_states;
    }

    private void setBluetooth_states(ENUMS.BLUETOOTH_STATES bluetooth_states, String address) {
        this.bluetooth_states = bluetooth_states;
        for(SPPListener sppListener : sppListeners){
            switch (bluetooth_states){
                case CONNECTED:
                    sppListener.onBluetoothDeviceConnected(conDevice.getName(), conDevice.getAddress());
                    break;
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
        }
    }

    public void addSPPListener(SPPListener sppListener){
        Log.d(TAG, "LISTENER " + sppListener.getClass().getCanonicalName() + " ADDED");
        if(sppListeners==null){
            sppListeners = new ArrayList<>();
        }
        sppListeners.add(sppListener);
    }

    public void removeSPPLiestener(SPPListener sppListener){
        Log.d(TAG, "LISTENER " + sppListener.getClass().getCanonicalName() + " REMOVED");
        if(sppListeners!=null){
            sppListeners.remove(sppListener);
        }
    }

    void connect(){
        Log.d(TAG, "CONNECT TO : " + conDevice);
        stop();
        startConnectionThread(conDevice);
        setBluetooth_states(ENUMS.BLUETOOTH_STATES.CONNECTING, conDevice.getAddress());
    }

    public void connect(BluetoothDevice device){
        Log.d(TAG, "DEVICE : " + device + "\nBUFFER SIZE : " + bufferSize);
        this.conDevice = device;
        this.bufferSize = 1024;
        connect();
    }

    public void connect(String address){
        Log.d(TAG, "DEVICE by address : " + address + "\nBUFFER SIZE : " + bufferSize);
        this.conDevice = bluetoothAdapter.getRemoteDevice(address);
        this.bufferSize = 1024;
        connect();
    }

    public void connect(BluetoothDevice device, int bufferSize){
        Log.d(TAG, "DEVICE : " + device + "\nBUFFER SIZE : " + bufferSize);
        this.conDevice = device;
        this.bufferSize = bufferSize;
        connect();
    }

    public void connect(String address, int bufferSize){
        Log.d(TAG, "DEVICE by address : " + address + "\nBUFFER SIZE : " + bufferSize);
        this.conDevice = bluetoothAdapter.getRemoteDevice(address);
        this.bufferSize = bufferSize;
        connect();
    }

    private void reconnect(){
        Log.d(TAG, "RECONNECT TO : " + conDevice);
        setBluetooth_states(ENUMS.BLUETOOTH_STATES.DISCONNECTED, conDevice.getAddress());
        connect();
    }

    private synchronized void connected(BluetoothSocket socket){
        Log.d(TAG, "CONNECTED : " + conDevice);
        killThreads();
        startCommunicationThread(socket);
        setBluetooth_states(ENUMS.BLUETOOTH_STATES.CONNECTED, conDevice.getAddress());
    }

    private void startConnectionThread(BluetoothDevice device){
        if(connectionThread==null) {
            connectionThread = new ConnectionThread(device);
            connectionThread.start();
        }
    }

    private void startCommunicationThread(BluetoothSocket socket){
        if(communicationThread==null){
            communicationThread = new CommunicationThread(socket, bufferSize);
            communicationThread.start();
        }
    }

    public synchronized void stop(){
        killThreads();
    }

    private synchronized void restartConnectionThread(BluetoothDevice device){
        killConnectionThread();
        startConnectionThread(device);
    }

    private synchronized void restartCommunicationThread(BluetoothSocket socket){
        killCommunicationThread();
        startCommunicationThread(socket);
    }

    private synchronized void killThreads(){
        killCommunicationThread();
        killConnectionThread();
    }

    private synchronized void killConnectionThread(){
        if(connectionThread!=null) {
            connectionThread.interrupt();
            connectionThread = null;
        }

    }

    private synchronized void killCommunicationThread(){
        if(communicationThread!=null){
            communicationThread.interrupt();
            communicationThread = null;
        }
    }

    public void sendPacket(byte[] data){
        CommunicationThread t;
        synchronized (this){
            if(getBluetooth_states() == ENUMS.BLUETOOTH_STATES.CONNECTED){
                t = communicationThread;
            }
            else {
                Log.e(TAG, "NOT CONNECTED");
                return;
            }
        }
        t.sendPacket(data);
    }

    private class ConnectionThread extends Thread{

        private final BluetoothDevice mDevice;
        private final BluetoothSocket mSocket;

        public ConnectionThread(BluetoothDevice device){
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
                    restartConnectionThread(mDevice);
                    interrupt();
                }
                synchronized (SPPClientController.this){
                    connectionThread = null;
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

    private class CommunicationThread extends Thread{
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private final int buffSize;

        public CommunicationThread(BluetoothSocket socket, int buffSize){
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

        public void sendPacket(byte[] data){
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
