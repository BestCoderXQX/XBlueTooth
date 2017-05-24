package com.xqx.xbluetooth;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.Queue;

/*
* @author xqx
* @emil djlxqx@163.com
* create at 2017/5/24
* description:  蓝牙服务
*/


public class BluetoothService {

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String NAME = "BluetoothData";
    public static  int  SENSEOR_NUM = 0 ;    //需要连接的传感器硬件个数
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mAcceptThread;// 请求连接的监听进程
    private ConnectThread mConnectThread;// 连接一个设备的进程
    public ConnectedThread[] mConnectedThread = new ConnectedThread[SENSEOR_NUM];// 已经连接之后的管理进程
    private int mState;// 当前状态

    // 指明连接状态的常量
    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    public BluetoothService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    private synchronized void setState(int state) {
        mState = state;
        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public synchronized int getState() {
        return mState;
    }

    public synchronized void start() {

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Start the thread to listen on a BluetoothServerSocket
//        if (mAcceptThread == null)
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
        setState(STATE_LISTEN);
    }

    public synchronized void connect(BluetoothDevice device,int index) {

        // Cancel any thread currently running a connection
        //连接一个蓝牙时
//        index=1;
        if (mConnectedThread[index-1] != null) {
            mConnectedThread[index-1].cancel();
            mConnectedThread[index-1]=null;
        }
        mConnectThread=new ConnectThread(device,index);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothSocket socket,BluetoothDevice device,int index) {
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        // Cancel the accept thread because we only want to connect to one device
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread[index-1] = new ConnectedThread(socket,index);

        mConnectedThread[index-1].start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);  //发送消息  已连接
        Bundle bundle = new Bundle();
        bundle.putString("device_name", device.getName()+" "+index);

        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    public synchronized void stop() {

        if (mConnectedThread != null) {
            for(int i=0;i<mConnectedThread.length;i++)
            {
                mConnectedThread[i].cancel();
            }
            mConnectedThread = null;
        }
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        setState(STATE_NONE);
    }

    private void connectionFailed(int index) {
        setState(STATE_LISTEN);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString("toast", "未能连接设备"+index);
        bundle.putInt("device_id",index);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    private void connectionLost(int index) {
        setState(STATE_LISTEN);
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString("toast", "设备丢失"+index);
        bundle.putInt("device_id",index);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        //private int index;
        public AcceptThread() {
            BluetoothServerSocket tmp = null;
           // this.index=index;
            // Create a new listening server socket
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            }
            catch (IOException e) {}
            mmServerSocket = tmp;
        }

        public void run() {
            setName("AcceptThread");
            BluetoothSocket socket = null;
            Log.d("MAGIKARE","接收socket线程");
            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:// Situation normal. Start the connected thread.
                               // connected(socket, socket.getRemoteDevice(),index);
                                break;
                            case STATE_NONE:break;
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                }
                                catch (IOException e) {}
                                break;
                        }
                    }
                }
            }

        }

        public void cancel() {

            try {
                mmServerSocket.close();
            }
            catch (IOException e) {}
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection with a
     * device. It runs straight through; the connection either succeeds or
     * fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private int index;
        public ConnectThread(BluetoothDevice device,int index) {
            mmDevice = device;
            this.index=index;
            Log.d("脉吉 Connecting",device.getAddress());
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);// Get a BluetoothSocket for a connection with the given BluetoothDevice
            }
            catch (IOException e) {}
            mmSocket = tmp;
        }

        public void run() {

            setName("ConnectThread");
            mAdapter.cancelDiscovery();// Always cancel discovery because it will slow down a connection

            // Make a connection to the BluetoothSocket
            try {
                mmSocket.connect();// This is a blocking call and will only return on a successful connection or an exception
            }
            catch (IOException e) {
                connectionFailed(this.index);
                try {
                    mmSocket.close();
                } catch (IOException e2) {}

                BluetoothService.this.start();// 引用来说明要调用的是外部类的方法 run
                return;
            }

            synchronized (BluetoothService.this) {// Reset the ConnectThread because we're done
                mConnectThread = null;
            }
            connected(mmSocket, mmDevice,index);// Start the connected thread
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {

            }
        }
    }

    /**
     * This thread runs during a connection with a remote device. It handles all
     * incoming and outgoing transmissions.
     */
    class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private int index;
        private Queue<Byte> queueBuffer = new LinkedList<Byte>();
        private byte[] packBuffer = new byte[11];
        public ConnectedThread(BluetoothSocket socket,int index) {

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            this.index=index;
            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {}

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        private float [] fData=new float[31];
        private String strDate,strTime;
        public void run() {
            byte[] tempInputBuffer = new byte[1024];
            int acceptedLen = 0;
            byte sHead;
            // Keep listening to the InputStream while connected
            long lLastTime = System.currentTimeMillis(); // 获取开始时间
            while (true) {

                try {
                    // 每次对inputBuffer做覆盖处理

                    acceptedLen = mmInStream.read(tempInputBuffer);
                    Log.d("BTL",""+acceptedLen);
                    for (int i = 0; i < acceptedLen; i++) queueBuffer.add(tempInputBuffer[i]);// 从缓冲区读取到的数据，都存到队列里


                    while (queueBuffer.size() >= 11) {
//                        Log.e("MAGIKARE",queueBuffer.size()+"");
                        if ((queueBuffer.poll()) != 0x55) continue;// peek()返回对首但不删除 poll 移除并返回
                        sHead = queueBuffer.poll();
                        for (int j = 0; j < 9; j++) packBuffer[j] = queueBuffer.poll();
                        switch (sHead) {//

                            case 0x52://角速度
                                fData[3] = ((((short) packBuffer[1]) << 8) | ((short) packBuffer[0] & 0xff)) / 32768.0f * 2000;
                                fData[4] = ((((short) packBuffer[3]) << 8) | ((short) packBuffer[2] & 0xff)) / 32768.0f * 2000;
                                fData[5] = ((((short) packBuffer[5]) << 8) | ((short) packBuffer[4] & 0xff)) / 32768.0f * 2000;
                                fData[17] = ((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff)) / 100.0f;
//                                RecordData(sHead,String.format("% 10.2f", fData[3])+String.format("% 10.2f", fData[4])+String.format("% 10.2f", fData[5])+" ");
                                break;
                            case 0x53://角度
                                fData[6] = ((((short) packBuffer[1]) << 8) | ((short) packBuffer[0] & 0xff)) / 32768.0f * 180;
                                fData[7] = ((((short) packBuffer[3]) << 8) | ((short) packBuffer[2] & 0xff)) / 32768.0f * 180;
                                fData[8] = ((((short) packBuffer[5]) << 8) | ((short) packBuffer[4] & 0xff)) / 32768.0f * 180;
                                fData[17] = ((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff)) / 100.0f;
//                                RecordData(sHead,String.format("% 10.2f", fData[6])+String.format("% 10.2f", fData[7])+String.format("% 10.2f", fData[8]));
                                break;
                            case 0x59://四元数
                                fData[23] = ((((short) packBuffer[1]) << 8) | ((short) packBuffer[0] & 0xff)) / 32768.0f;
                                fData[24] = ((((short) packBuffer[3]) << 8) | ((short) packBuffer[2] & 0xff))/32768.0f;
                                fData[25] = ((((short) packBuffer[5]) << 8) | ((short) packBuffer[4] & 0xff))/32768.0f;
                                fData[26] = ((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff))/32768.0f;
//                                RecordData(sHead,String.format("% 7.3f", fData[23])+String.format("% 7.3f", fData[24])+String.format("% 7.3f", fData[25])+String.format("% 7.3f", fData[26]));
                                break;
                        }//switch
                    }//while (queueBuffer.size() >= 11)

                    long lTimeNow = System.currentTimeMillis(); // 获取开始时间
                    if (lTimeNow - lLastTime > 80) {
                        lLastTime = lTimeNow;
                        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_READ);
                        Bundle bundle = new Bundle();
                        bundle.putString("index",String.valueOf(this.index));
                        bundle.putFloatArray("Data", fData);
                        bundle.putString("Date", strDate);
                        bundle.putString("Time", strTime);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                    }

                } catch (IOException e) {
                    connectionLost(this.index);
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                mHandler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1,buffer).sendToTarget();// Share the sent message back to the UI Activity
            } catch (IOException e) {}
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {}
        }
    }

}
