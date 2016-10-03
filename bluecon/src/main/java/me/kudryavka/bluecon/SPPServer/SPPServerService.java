package me.kudryavka.bluecon.SPPServer;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.IBinder;

import java.util.ArrayList;

import me.kudryavka.bluecon.Consts.ENUMS;
import me.kudryavka.bluecon.Listeners.SPPListener;
import me.kudryavka.bluecon.SPPClient.SPPClientController;
import me.kudryavka.bluecon.SPPClient.SPPClientService;

/**
 * Created by seyriz on 2016. 10. 3..
 */

public class SPPServerService extends Service {

    private static final String TAG = "SPPServerService";

    private SPPServerController sppServerController;

    public SPPServerService() {
    }

    @Override
    public void onCreate() {
        sppServerController = new SPPServerController(getApplicationContext());
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return new SPPBinder();
    }

    private void addSPPListener(SPPListener sppListener){
        sppServerController.addSPPListener(sppListener);
    }

    private void removeSPPLiestener(SPPListener sppListener){
        sppServerController.removeSPPLiestener(sppListener);
    }


    public class SPPBinder extends android.os.Binder{

        public void startServerSingle(String serverName){
            sppServerController.startServerSingle(serverName, 1024);
        }

        public void startServerMultiple(String serverName){
            sppServerController.startServerMultiple(serverName, 1024);
        }

        public void startServerSingle(String serverName, int bufferSize){
            sppServerController.startServerSingle(serverName, bufferSize);
        }

        public void startServerMultiple(String serverName, int bufferSize){
            sppServerController.startServerMultiple(serverName, bufferSize);
        }

        public void stop(){
            sppServerController.stop();
        }

        public void sendPacket(String address, byte[] data){
            sppServerController.sendPacket(address, data);
        }

        public void broadcast(byte[] data){
            sppServerController.broadcast(data);
        }

        public ArrayList<BluetoothDevice> getConnectedDevices(){
            return sppServerController.getConnectedDevices();
        }

        public ENUMS.BLUETOOTH_STATES getState(){
            return sppServerController.getBluetooth_states();
        }

        public void addSPPListener(SPPListener sppListener){
            sppServerController.addSPPListener(sppListener);
        }

        public void removeSPPLiestener(SPPListener sppListener){
            sppServerController.removeSPPLiestener(sppListener);
        }

        public ArrayList<BluetoothDevice> getPairedDevices(){
            return sppServerController.getPairedDevices();
        }
    }
}
