package me.kudryavka.bluecon.SPPServer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;

import me.kudryavka.bluecon.Consts.BT_UUID;
import me.kudryavka.bluecon.Consts.ENUMS;
import me.kudryavka.bluecon.Listeners.SPPListener;
import me.kudryavka.bluecon.SPPClient.SPPClientController;

/**
 * Created by seyriz on 2016. 10. 3..
 */

public class SPPServerController {

    private static final String TAG = "SPPServerController";

    private enum SERVER_MODE{SINGLE_CONNECTION, MULTIPLE_CONNECTION};
    private Context context;

    private ENUMS.BLUETOOTH_STATES bluetooth_states;

    private ArrayList<BluetoothDevice> pairedDevices = new ArrayList<>();
    private ArrayList<SPPListener> sppListeners = new ArrayList<>();
    private BluetoothAdapter bluetoothAdapter;
    private ConnectThread connectThread;
    private ComThread comThread;

    private Integer bufferSize;
    private String serverName;
    private SERVER_MODE serverMode;

    public SPPServerController(Context context) {
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

    private void setBluetooth_states(ENUMS.BLUETOOTH_STATES bluetooth_states, @Nullable String address) {
        this.bluetooth_states = bluetooth_states;
        for(SPPListener sppListener : sppListeners){
            switch (bluetooth_states){
                case CONNECTED:
                    sppListener.onBluetoothDeviceConnected(null, null);
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

    void startServerSingle(String serverName, int bufferSize){
        Log.d(TAG, "SINGLE_CONNECTION\nserverName : " + serverName + "\nBUFFER SIZE : " + bufferSize);
        this.serverName = serverName;
        this.bufferSize = bufferSize;
        this.serverMode = SERVER_MODE.SINGLE_CONNECTION;
        start();
    }

    void startServerMultiple(String serverName, int bufferSize){
        Log.d(TAG, "MULTIPLE_CONNECTION\nserverName : " + serverName + "\nBUFFER SIZE : " + bufferSize);
        this.serverName = serverName;
        this.bufferSize = bufferSize;
        this.serverMode = SERVER_MODE.MULTIPLE_CONNECTION;
        start();
    }

    private void start(){
        if(serverName!=null) {
            Log.d(TAG, "SERVER NAME : " + serverName);
            resetThreads();
            connectThread = new ConnectThread(serverName, getBlueToothAdapter());
            connectThread.start();
            setBluetooth_states(ENUMS.BLUETOOTH_STATES.CONNECTING, null);
        }
    }

    private void restart(){
        Log.d(TAG, "RESTART SERVER : " + serverName);
        setBluetooth_states(ENUMS.BLUETOOTH_STATES.DISCONNECTED, null);
        start();
    }

    private synchronized void connected(BluetoothSocket socket){
        Log.d(TAG, "CONNECTED");
        resetThreads();
        if(comThread==null) {
            comThread = new ComThread(bufferSize);
            comThread.start();
            comThread.addSocket(socket);
        }
        else {
            comThread.addSocket(socket);
        }

        setBluetooth_states(ENUMS.BLUETOOTH_STATES.CONNECTED, socket.getRemoteDevice().getAddress());
    }

    synchronized void stop() {
        if(serverName!=null) {
            Log.d(TAG, "STOP : " + serverName);
            resetThreads();
            setBluetooth_states(ENUMS.BLUETOOTH_STATES.DISCONNECTED, null);
        }
    }

    void sendPacket(String address, byte[] data){
        ComThread t;
        synchronized (this){
            if(getBluetooth_states() == ENUMS.BLUETOOTH_STATES.CONNECTED){
                t = comThread;
            }
            else {
                Log.e(TAG, "NOT CONNECTED");
                return;
            }
        }
        t.sendPacket(address, data);
    }

    void broadcast(byte[] data){
        ComThread t;
        synchronized (this){
            if(getBluetooth_states() == ENUMS.BLUETOOTH_STATES.CONNECTED){
                t = comThread;
            }
            else {
                Log.e(TAG, "NOT CONNECTED");
                return;
            }
        }
        t.broadcast(data);
    }

    synchronized ArrayList<BluetoothDevice> getConnectedDevices(){
        if(getBluetooth_states() == ENUMS.BLUETOOTH_STATES.CONNECTED){
            return comThread.getConnectedDevice();
        }
        else {
            Log.e(TAG, "NOT CONNECTED");
            return new ArrayList<>();
        }
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

        private final BluetoothAdapter mDevice;
        private final BluetoothServerSocket mSocket;

        public ConnectThread(String name, BluetoothAdapter adapter){
            mDevice = adapter;
            BluetoothServerSocket tempSock = null;
            try{
                tempSock = adapter.listenUsingRfcommWithServiceRecord(name, BT_UUID.UUID_SPP);
            }
            catch (IOException e){
                try{
                    tempSock = adapter.listenUsingInsecureRfcommWithServiceRecord(name, BT_UUID.UUID_SPP);
                }
                catch (IOException e2){
                    Log.e(TAG, "Failed to create SOCKET!", e2);
                    restart();
                }
            }
            mSocket = tempSock;
        }

        @Override
        public void run() {
            BluetoothSocket socket = null;
            if(mSocket!=null){
                while (true) {
                    try {
                        socket = mSocket.accept();
                        if (socket != null) {
                            connected(socket);
                            mSocket.close();
                            if(serverMode == SERVER_MODE.SINGLE_CONNECTION) {
                                break;
                            }
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "FAILED TO ACCEPT SOCKET CONNECTION", e);

                        restart();
                        break;
                    }
                }
            }
            interrupt();
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
        private final ArrayList<BluetoothSocket> sockets;
        private final HashMap<String, OutputStream> outputStreams;
        private final ArrayList<ReadThread> readThreads;
        private final int buffSize;

        public ComThread(int buffSize){
            sockets = new ArrayList<>();
            outputStreams = new HashMap<>();
            readThreads = new ArrayList<>();
            this.buffSize = buffSize;
        }

        public void addSocket(BluetoothSocket socket){
            sockets.add(socket);
            InputStream tempIS = null;
            OutputStream tempOS = null;

            try{
                tempIS = socket.getInputStream();
                tempOS = socket.getOutputStream();
            }
            catch (IOException e){
                Log.e(TAG, "FAILED TO OPEN I/O STREAM");
            }
            outputStreams.put(socket.getRemoteDevice().getAddress(), tempOS);

            ReadThread r = new ReadThread(socket, tempIS);
            r.start();
            readThreads.add(r);
        }

        @Override
        public void run() {
        }

        public void sendPacket(String address, byte[] packet){
            try{
                outputStreams.get(address).write(packet);
                for(SPPListener sppListener : sppListeners){
                    sppListener.onPacketSended(address, packet);
                }
            }
            catch (NullPointerException e){
                Log.e(TAG, address + " is not connected");
            }
            catch (IOException e){
                Log.e(TAG, "FAILED TO WRITE PACKET TO " + address, e);
            }
        }

        public void broadcast(byte[] packet){
            for(String address : outputStreams.keySet()) {
                try {
                    outputStreams.get(address).write(packet);
                    for (SPPListener sppListener : sppListeners) {
                        sppListener.onPacketSended(address, packet);
                    }
                }
                catch (NullPointerException e){
                    Log.e(TAG, address + " is not connected");
                }
                catch (IOException e){
                    Log.e(TAG, "FAILED TO WRITE PACKET TO " + address, e);
                }
            }
        }

        public void disconnected(BluetoothSocket socket, ReadThread readThread){
            sockets.remove(socket);
            readThreads.remove(readThreads);
            outputStreams.remove(socket.getRemoteDevice().getAddress());
            readThread.interrupt();
            readThread = null;
        }

        ArrayList<BluetoothDevice> getConnectedDevice(){
            ArrayList<BluetoothDevice> devices = new ArrayList<>();
            for(BluetoothSocket so : sockets){
                devices.add(so.getRemoteDevice());
            }
            return devices;
        }

        @Override
        public void interrupt() {
            for(BluetoothSocket socket : sockets) {
                try {
                    socket.close();
                } catch (IOException e) {
                    Log.e(TAG, "FAILED TO CLOSE SOCKET");
                }
            }
            for(ReadThread r : readThreads){
                r.interrupt();
            }
            super.interrupt();
        }

        private class ReadThread extends Thread{
            private BluetoothSocket socket;
            private InputStream inputStream;

            public ReadThread(BluetoothSocket socket, InputStream inputStream) {
                this.socket = socket;
                this.inputStream = inputStream;
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
                            sppListener.onPacketReceived(socket.getRemoteDevice().getAddress(), read);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "FAILED TO READ PACKET", e);
                        disconnected(socket, this);
                        break;
                    }
                }
            }

            @Override
            public void interrupt() {
                try {
                    inputStream.close();
                }
                catch (IOException e){
                    Log.e(TAG, "FAILED TO CLOSE SOCKET");
                }
                super.interrupt();
            }
        }
    }
}
