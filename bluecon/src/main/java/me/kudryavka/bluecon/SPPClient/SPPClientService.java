package me.kudryavka.bluecon.SPPClient;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.IBinder;

import java.util.ArrayList;

import me.kudryavka.bluecon.Consts.ENUMS;
import me.kudryavka.bluecon.Listeners.SPPListener;

public class SPPClientService extends Service {

    private static final String TAG = "SPPClientService";

    private SPPClientController sppClientController;

    public SPPClientService() {
    }

    @Override
    public void onCreate() {
        sppClientController = new SPPClientController(getApplicationContext());
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return new SPPBinder();
    }

    private void addSPPListener(SPPListener sppListener){
        sppClientController.addSPPListener(sppListener);
    }

    private void removeSPPLiestener(SPPListener sppListener){
        sppClientController.removeSPPLiestener(sppListener);
    }


    public class SPPBinder extends android.os.Binder{
        public void init(BluetoothDevice device, int bufferSize){
            sppClientController.init(device, bufferSize);
        }

        public void init(BluetoothDevice device){
            sppClientController.init(device, 1024);
        }

        public void init(String address, int bufferSize){
            sppClientController.init(address, bufferSize);
        }

        public void init(String address){
            sppClientController.init(address, 1024);
        }

        public boolean isInited(){
            return sppClientController.isInited();
        }

        public void connect(BluetoothDevice device, int buffSize){
            sppClientController.init(device, buffSize);
            sppClientController.connect();
        }

        public void connect(BluetoothDevice device){
            sppClientController.init(device, 1024);
            sppClientController.connect();
        }

        public void connect(String address, int buffSize){
            sppClientController.init(address, buffSize);
            sppClientController.connect();
        }

        public void connect(String address){
            sppClientController.init(address, 1024);
            sppClientController.connect();
        }

        public void connect(){
            sppClientController.connect();
        }

        public void disconnedt(){
            sppClientController.stop();
        }

        public void sendPacket(byte[] data){
            sppClientController.send(data);
        }

        public ENUMS.BLUETOOTH_STATES getState(){
            return sppClientController.getBluetooth_states();
        }

        public void addSPPListener(SPPListener sppListener){
            sppClientController.addSPPListener(sppListener);
        }

        public void removeSPPLiestener(SPPListener sppListener){
            sppClientController.removeSPPLiestener(sppListener);
        }

        public ArrayList<BluetoothDevice> getPairedDevices(){
            return sppClientController.getPairedDevices();
        }
    }
}
