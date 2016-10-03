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

import me.kudryavka.bluecon.Consts.BT_UUID;
import me.kudryavka.bluecon.Consts.ENUMS;
import me.kudryavka.bluecon.Listeners.SPPListener;

import static me.kudryavka.bluecon.Consts.ENUMS.SERVER_MODE.SINGLE_CONNECTION;

/**
 * Created by seyriz on 2016. 10. 4..
 */

public class SPPServerController {
    private static final String TAG = "SPPServerController";

    private Context context;

    private static SPPServerController instance;

    private ArrayList<SPPListener> sppListeners = new ArrayList<>();
    private BluetoothAdapter bluetoothAdapter;
    private ENUMS.BLUETOOTH_STATES bluetooth_states;

    private ConnectionThread connectionThread;
    private CommunicationThread communicationThread;

    private Integer bufferSize;
    private String serverName;
    private ENUMS.SERVER_MODE serverMode;

    public static SPPServerController getInstance(Context context){
        if(instance == null){
            instance = new SPPServerController(context);
        }
        return instance;
    }

    private SPPServerController(Context context) {
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

    public ENUMS.BLUETOOTH_STATES getBluetooth_states() {
        return bluetooth_states;
    }

    private void setBluetooth_states(ENUMS.BLUETOOTH_STATES bluetooth_states, @Nullable String address) {
        this.bluetooth_states = bluetooth_states;
        for(SPPListener sppListener : sppListeners){
            switch (bluetooth_states){
                case CONNECTED:
                    sppListener.onBluetoothDeviceConnected(null, null);
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

    public void startServer(String serverName, int bufferSize, ENUMS.SERVER_MODE serverMode){
        this.serverName = serverName;
        this.bufferSize = bufferSize;
        this.serverMode = serverMode;
        start();
    }

    private void start(){
        if(serverName!=null) {
            Log.d(TAG, "SERVER NAME : " + serverName);
            killThreads();
            startConnectionThread();
            setBluetooth_states(ENUMS.BLUETOOTH_STATES.CONNECTING, null);
        }
    }

    private void startConnectionThread(){
        if(connectionThread==null) {
            connectionThread = new ConnectionThread(serverName, getBlueToothAdapter());
            connectionThread.start();
        }
    }

    private void startCommunicationThread(){
        if(communicationThread==null){
            communicationThread = new CommunicationThread(bufferSize);
            communicationThread.start();
        }
    }

    public synchronized void stop(){
        killThreads();
    }

    private synchronized void restartConnectionThread(){
        killConnectionThread();
        startConnectionThread();
    }

    private synchronized void restartCommunicationThread(){
        killCommunicationThread();
        startCommunicationThread();
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

    public void sendPacket(String address, byte[] data){
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
        t.sendPacket(address, data);
    }

    public void broadcastPacket(byte[] data){
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
        t.broadcastPacket(data);
    }

    public synchronized ArrayList<BluetoothDevice> getConnectedDevices(){
        if(getBluetooth_states() == ENUMS.BLUETOOTH_STATES.CONNECTED){
            return communicationThread.getConnectedDevice();
        }
        else {
            Log.e(TAG, "NOT CONNECTED");
            return new ArrayList<>();
        }
    }

    private synchronized void connected(BluetoothSocket socket){
        Log.d(TAG, "CONNECTED : " + socket.getRemoteDevice().getAddress());
        if(communicationThread==null){
            startCommunicationThread();
        }
        communicationThread.addSocket(socket);
        setBluetooth_states(ENUMS.BLUETOOTH_STATES.CONNECTED, socket.getRemoteDevice().getAddress());
    }

    private class ConnectionThread extends Thread{
        private final String serverName;
        private final BluetoothAdapter bluetoothAdapter;
        private BluetoothServerSocket serverSocket;

        public ConnectionThread(String serverName, BluetoothAdapter bluetoothAdapter) {
            Log.d(TAG, "ConnectionThread new Instace");
            this.serverName = serverName;
            this.bluetoothAdapter = bluetoothAdapter;
            try{
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(serverName, BT_UUID.UUID_SPP);
            }
            catch (IOException e){
                try{
                    serverSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(serverName, BT_UUID.UUID_SPP);
                }
                catch (IOException e2){
                    Log.e(TAG, "Failed to create SOCKET!", e2);
                    restartConnectionThread();
                }
            }
        }

        @Override
        public void run() {
            Log.d(TAG, "ConnectionThread run");
            BluetoothSocket socket = null;
            if(serverSocket!=null){
                while (true) {
                    try {
                        socket = serverSocket.accept();
                        if (socket != null) {
                            connected(socket);
                            if(serverMode == SINGLE_CONNECTION) {
                                break;
                            }
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "FAILED TO ACCEPT SOCKET CONNECTION", e);
                        restartConnectionThread();
                        break;
                    }
                }
            }
        }

        @Override
        public void interrupt() {
            try{
                serverSocket.close();
            }
            catch (IOException e){
                Log.e(TAG, "SOCKET closing FAILED");
            }
            super.interrupt();
        }
    }

    private class CommunicationThread extends Thread{
        private final ArrayList<BluetoothSocket> sockets;
        private final HashMap<String, OutputStream> outputStreams;
        private final ArrayList<ReadThread> readThreads;
        private final int buffSize;

        public CommunicationThread(int buffSize){
            Log.d(TAG, "CommunicationThread new Instace");
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
            Log.d(TAG, "CommunicationThread run");
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

        public void broadcastPacket(byte[] packet){
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
            try {
                sockets.remove(socket);
                readThreads.remove(readThreads);
                outputStreams.remove(socket.getRemoteDevice().getAddress());
                readThread.interrupt();
                readThread = null;
                socket.close();
            }
            catch (IOException e){
                Log.e(TAG, "FAILED TO CLOSE DISCONNECTED SOCKET");
            }
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
                        Log.e(TAG, "CONNECTION CLOSED BY PEER", e);
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
