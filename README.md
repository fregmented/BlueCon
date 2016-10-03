# BlueCon

[ ![Download](https://api.bintray.com/packages/seyriz/BlueCon/bluecon/images/download.svg) ](https://bintray.com/seyriz/BlueCon/bluecon/_latestVersion)

BlueCon is SPP Service for Android.

this Service is support Android 4.4 and grater environment.

## Installation
if you use jcenter repository, just add dependency your app.
```gradle
dependencies {
    compile 'me.kudryavka:bluecon:0.1'
}
```

## Usage

just simple.

Add permission in your manifest file for using BLUETOOTH like this.
    <uses-permission android:name="android.permission.BLUETOOTH" />
and connect service for using library

first, make service connection
```java
private SPPService.SPPBinder svBinder;
private boolean isSVConned;

public ServiceConnection serviceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.e("SERVICE", "CONNNECTED");
        svBinder = (SPPService.SPPBinder)service;
        isSVConned = true;
        svBinder.addSPPListener(SPPActivity.this);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.e("SERVICE", "DISCONNNECTED");
        isSVConned = false;
    }
 };
```
next bind service.
```java
protected void onCreate(Bundle savedInstanceState) {
    ...
    bindService(new Intent(this, SPPService.class), serviceConnection, BIND_AUTO_CREATE);
    ...
}

@Override
protected void onDestroy() {
    ...
    unbindService(serviceConnection);
    ...
}
```

after the Service Connected, you can get ArrayList<PairedDevice>
```java
svBinder.getPairedDevices();
```
and you can connect device.

if you want connect to device later, you can just init device and buffur size and connect later.
```java
svBinder.init(BluetoothDevice device, int bufferSize);
```

or you can connect to device by address.
```java
svBinder.init(String address, int bufferSize);
```

or you can use default buffer size(1024bytes).
```java
svBinder.init(BluetoothDevice device);
svBinder.init(String address);
```

and connect to device when you want.
```java
svBinder.connect();
```

either you can connect to device immediately.
```java
svBinder.connect(BluetoothDevice device, int buffSize);
svBinder.connect(String address, int buffSize);
```

Of course you can use default buffer size.
```java
svBinder.connect(BluetoothDevice device);
svBinder.connect(String address);
```

after connect, you must add Listener to the Service.

You can use multiple listener at the same time.
```java
svBinder.addSPPListener(new SPPListener() {
    @Override
    public void onPacketReceived(byte[] packet) {

    }

    @Override
    public void onPacketSended(byte[] packet) {

    }

    @Override
    public void onBluetoothDeviceConnected(String name, String address) {

    }

    @Override
    public void onBluetoothDeviceDisconnected() {

    }

    @Override
    public void onBluetoothDisabled() {

    }

    @Override
    public void onBluetoothNotSupported() {

    }

    @Override
    public void onBluetoothDeviceConnecting() {

    }
});
```
you can remove listener from the Service
```java
removeSPPLiestener(SPPListener sppListener);
```

sending message to device is very simple. just call
```java
svBinder.sendPacket(byte[] data);
```
receiving packet incoming to SPPListener.onPacketReceived.