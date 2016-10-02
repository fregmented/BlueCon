package me.kudryavka.bluecon.SPP;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.IBinder;

import java.util.ArrayList;

import me.kudryavka.bluecon.Consts.ENUMS;

public class SPPService extends Service {

    private static final String TAG = "SPPService";

    private SPPController sppController;

    public SPPService() {
    }

    @Override
    public void onCreate() {
        sppController = new SPPController(getApplicationContext());
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return new SPPBinder();
    }

    private void addSPPListener(SPPListener sppListener){
        sppController.addSPPListener(sppListener);
    }

    private void removeSPPLiestener(SPPListener sppListener){
        sppController.removeSPPLiestener(sppListener);
    }


    public class SPPBinder extends android.os.Binder{
        public void init(BluetoothDevice device, int bufferSize){
            sppController.init(device, bufferSize);
        }

        public void init(BluetoothDevice device){
            sppController.init(device, 1024);
        }

        public void init(String address, int bufferSize){
            sppController.init(address, bufferSize);
        }

        public void init(String address){
            sppController.init(address, 1024);
        }

        public boolean isInited(){
            return sppController.isInited();
        }

        public void connect(BluetoothDevice device, int buffSize){
            sppController.init(device, buffSize);
            sppController.connect();
        }

        public void connect(BluetoothDevice device){
            sppController.init(device, 1024);
            sppController.connect();
        }

        public void connect(String address, int buffSize){
            sppController.init(address, buffSize);
            sppController.connect();
        }

        public void connect(String address){
            sppController.init(address, 1024);
            sppController.connect();
        }

        public void connect(){
            sppController.connect();
        }

        public void disconnedt(){
            sppController.stop();
        }

        public void sendPacket(byte[] data){
            sppController.send(data);
        }

        public ENUMS.BLUETOOTH_STATES getState(){
            return sppController.getBluetooth_states();
        }

        public void addSPPListener(SPPListener sppListener){
            sppController.addSPPListener(sppListener);
        }

        public void removeSPPLiestener(SPPListener sppListener){
            sppController.removeSPPLiestener(sppListener);
        }

        public ArrayList<BluetoothDevice> getPairedDevices(){
            return sppController.getPairedDevices();
        }
    }


}
