# BlueCon

[ ![Download](https://api.bintray.com/packages/seyriz/BlueCon/bluecon/images/download.svg) ](https://bintray.com/seyriz/BlueCon/bluecon/_latestVersion)

BlueCon is SPP Server & Client Library for Android.

this Service is support Android 4.4 and grater environment.

## Installation
if you use jcenter repository, just add dependency your app.
```gradle
dependencies {
    ...
    compile 'me.kudryavka:bluecon:0.2'
    ...
}
```

## Usage

just simple.

Add permission in your manifest file for using BLUETOOTH like this.
```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
```

### Client

#### as a Service
for using client service, Using SPPClientService.
Define Binder and Service connect condition.
```java
private SPPClientService.SPPBinder svBinder;
private boolean isSVConned;
```
Bind service to Application's context.
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    ...
    bindService(new Intent(this, SPPClientService.class), serviceConnection, BIND_AUTO_CREATE);
    ...
}
```
Unbind service when app is dead
```java
@Override
protected void onDestroy() {
    ... 
    unbindService(serviceConnection);
    ...
}
```
Make ServiceConnection and add SPPLietner to the Service.
```java
public ServiceConnection serviceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.e("SERVICE", "CONNNECTED");
        svBinder = (SPPClientService.SPPBinder)service;
        isSVConned = true;
        svBinder.addSPPListener(new SPPListener() {
            @Override
            public void onPacketReceived(String address, byte[] packet) {
                // Received packets
            }

            @Override
            public void onPacketSended(String address, byte[] packet) {
                // Sended packets
            }

            @Override
            public void onBluetoothDeviceConnected(String name, String address) {
                // on bluetooth device connected
            }

            @Override
            public void onBluetoothDeviceConnecting(@Nullable String name) {
                // on connecting to device
            }

            @Override
            public void onBluetoothDeviceDisconnected(@Nullable String name) {
                // on disconnected from device
            }

            @Override
            public void onBluetoothDisabled() {
                // if bluetooth is off
            }

            @Override
            public void onBluetoothNotSupported() {
                // Android device has not bluetooth module
            }
        });
    }
    
    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.e("SERVICE", "DISCONNNECTED");
        isSVConned = false;
    }
};
```
You can get Paird device list.
```java
ArrayList<BluetoothDevice> devices = svBinder.getPairedDevices();
```
You can connect to device using BluetoothDevice or Device's MAC address
```java
svBinder.connect(BluetoothDevice device);
svBinder.connect(String address);
```
Sending a packet
```java
svBinder.sendPacket(byte[] data)
```

#### As a instance
Get Controller's Instance
```java
SPPClientController sppClientController = SPPClientController.getInstance(getApplicationContext());
```
Add SPPListener to Instance
```java
sppClientController.addSPPListener(new SPPListener() {
    @Override
    public void onPacketReceived(String address, byte[] packet) {
        // Received packets
    }

    @Override
    public void onPacketSended(String address, byte[] packet) {
        // Sended packets
    }

    @Override
    public void onBluetoothDeviceConnected(String name, String address) {
        // on bluetooth device connected
    }

    @Override
    public void onBluetoothDeviceConnecting(@Nullable String name) {
        // on connecting to device
    }

    @Override
    public void onBluetoothDeviceDisconnected(@Nullable String name) {
        // on disconnected from device
    }

    @Override
    public void onBluetoothDisabled() {
        // if bluetooth is off
    }

    @Override
    public void onBluetoothNotSupported() {
        // Android device has not bluetooth module
    }
});
```
You can get Paird device list.
```java
ArrayList<BluetoothDevice> devices = sppClientController.getPairedDevices();
```
You can connect to device using BluetoothDevice or Device's MAC address
```java
sppClientController.connect(BluetoothDevice device);
sppClientController.connect(String address);
```
Sending a packet
```java
sppClientController.sendPacket(byte[] data)
```

### Server

#### as a Service
for using server service, Using SPPServerService.
Define Binder and Service connect condition.
```java
private SPPServerService.SPPBinder svBinder;
private boolean isSVConned;
```
Bind service to Application's context.
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    ...
    bindService(new Intent(this, SPPServerService.class), serviceConnection, BIND_AUTO_CREATE);
    ...
}
```
Unbind service when app is dead
```java
@Override
protected void onDestroy() {
    ... 
    unbindService(serviceConnection);
    ...
}
```
Make ServiceConnection and add SPPLietner to the Service.
```java
public ServiceConnection serviceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.e("SERVICE", "CONNNECTED");
        svBinder = (SPPServerController.SPPBinder)service;
        isSVConned = true;
        svBinder.addSPPListener(new SPPListener() {
            @Override
            public void onPacketReceived(String address, byte[] packet) {
                // Received packets
            }

            @Override
            public void onPacketSended(String address, byte[] packet) {
                // Sended packets
            }

            @Override
            public void onBluetoothDeviceConnected(String name, String address) {
                // on bluetooth device connected
            }

            @Override
            public void onBluetoothDeviceConnecting(@Nullable String name) {
                // on connecting to device
            }

            @Override
            public void onBluetoothDeviceDisconnected(@Nullable String name) {
                // on disconnected from device
            }

            @Override
            public void onBluetoothDisabled() {
                // if bluetooth is off
            }

            @Override
            public void onBluetoothNotSupported() {
                // Android device has not bluetooth module
            }
        });
    }
    
    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.e("SERVICE", "DISCONNNECTED");
        isSVConned = false;
    }
};
```
You can get Connecte device list.
```java
ArrayList<BluetoothDevice> devices = sppServerController.getConnectedDevice()
```
You can start server with server's name, inputStream buffersize, server mode.
```java
SERVER_MODE{SINGLE_CONNECTION, MULTIPLE_CONNECTION}
sppServerController.startServer(String serverName, int bufferSize, ENUMS.SERVER_MODE serverMode);
```
Sending a packet
```java
sppServerController.sendPacket(String addressbyte[] data);
```
or broadcast
```java
sppServerController.broadcastPacket(byte[] data);
```

#### As a instance
Get Controller's Instance
```java
SPPServerController sppServerController = SPPServerController.getInstance(getApplicationContext());
```
Add SPPListener to Instance
```java
sppServerController.addSPPListener(new SPPListener() {
    @Override
    public void onPacketReceived(String address, byte[] packet) {
        // Received packets
    }

    @Override
    public void onPacketSended(String address, byte[] packet) {
        // Sended packets
    }

    @Override
    public void onBluetoothDeviceConnected(String name, String address) {
        // on bluetooth device connected
    }

    @Override
    public void onBluetoothDeviceConnecting(@Nullable String name) {
        // on connecting to device
    }

    @Override
    public void onBluetoothDeviceDisconnected(@Nullable String name) {
        // on disconnected from device
    }

    @Override
    public void onBluetoothDisabled() {
        // if bluetooth is off
    }

    @Override
    public void onBluetoothNotSupported() {
        // Android device has not bluetooth module
    }
});
```
You can get Connecte device list.
```java
ArrayList<BluetoothDevice> devices = sppServerController.getConnectedDevice()
```
You can start server with server's name, inputStream buffersize, server mode.
```java
SERVER_MODE{SINGLE_CONNECTION, MULTIPLE_CONNECTION}
sppServerController.startServer(String serverName, int bufferSize, ENUMS.SERVER_MODE serverMode);
```
Sending a packet
```java
sppServerController.sendPacket(String addressbyte[] data);
```
or broadcast
```java
sppServerController.broadcastPacket(byte[] data);
```
