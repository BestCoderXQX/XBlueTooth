package com.xqx.xbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothService mBluetoothService = null;
    private String mConnectedDeviceName = null;



    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;


    //传感器坐标
    public static  final int MAGIKARE_SENSOR_UP=1;
    public static final int MAGIKARE_SENSOR_DOWN=2;
    public static  final  int MAGIKARE_SENSOR_CENTER=3;



    public static  float [] m_receive_data_up;          //第一个传感器的数据
    public static  float [] m_receive_data_down;        //第二个传感器的数据
    public static  float [] m_receive_data_center;      //第三个传感器的数据

    private boolean sensor_ready=false;

    // 已经连接上的传感器个数
    private int hasConnectedPostion = 1;

    private final Handler mHandler = new Handler() {
        // 匿名内部类写法，实现接口Handler的一些方法
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:   //状态改变的消息
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            break;
                    }
                    break;
                case MESSAGE_READ:      // 获取数据
                    try {
                        String str=msg.getData().getString("index");  //坐标  代表 哪一个传感器的  从1开始
                        int index=Integer.valueOf(str);
                        switch (index)
                        {
                            //第一个传感器
                            case MAGIKARE_SENSOR_UP:
                                m_receive_data_up=msg.getData().getFloatArray("Data");
                                Log.i("数据一","数据"+m_receive_data_up.toString());
                                break;
                            case MAGIKARE_SENSOR_DOWN:
                                m_receive_data_down=msg.getData().getFloatArray("Data");
                                Log.i("数据二","数据"+m_receive_data_up.toString());
                                break;
                            case MAGIKARE_SENSOR_CENTER:
                                m_receive_data_center=msg.getData().getFloatArray("Data");
                                Log.i("数据三","数据"+m_receive_data_up.toString());
                                break;
                        }
                    } catch (Exception e) {

                    }
                    break;
                case MESSAGE_DEVICE_NAME: //成功连接到了一个传感器
                    mConnectedDeviceName = msg.getData().getString("device_name");  //获取到连接到的设备  设备名+index
                    Toast.makeText(MainActivity.this,"成功连接"+mConnectedDeviceName,Toast.LENGTH_SHORT).show();
                    Log.i("xqxinfo","成功连接"+mConnectedDeviceName);

                    if (hasConnectedPostion==BluetoothService.SENSEOR_NUM){  //如果已经连接上的==总个数
                        sensor_ready = true;  //传感器准备好了
                    }
                    if(mBluetoothService!=null&&HasNext())// 满足条件  hasConnectedPosition + 1
                    {
                        // 有下一个传感器需要连接 ， 获取MAC地址
                        Log.i("xqxinfo","开始下一个连接,位置"+hasConnectedPostion);

                        String address=BluetoothData.GetNextAddress(hasConnectedPostion);
                        if(address!=null)
                        {
                            BluetoothDevice nextSensor=mBluetoothAdapter.getRemoteDevice(address);
                            if(nextSensor!=null)
                            {
                                mBluetoothService.connect(nextSensor, hasConnectedPostion);
                            }
                        }
                    }


                    break;
                case MESSAGE_TOAST:   //设备丢失等失败的情况下 ，重新连接

                    int index=msg.getData().getInt("device_id");
                    Toast.makeText(getApplicationContext(),msg.getData().getString("toast"), Toast.LENGTH_SHORT).show();
                    Log.i("xqxinfo","设备"+index+"丢失");

                    if(mBluetoothService!=null)
                    {
                        switch (index) {
                            case MAGIKARE_SENSOR_UP:
                                BluetoothDevice sensor_up = mBluetoothAdapter.getRemoteDevice(BluetoothData.SENSOR_UP_ADRESS);
                                if (sensor_up != null)
                                    mBluetoothService.connect(sensor_up, MAGIKARE_SENSOR_UP);
                                break;

                            case MAGIKARE_SENSOR_DOWN:
                                BluetoothDevice sensor_down = mBluetoothAdapter.getRemoteDevice(BluetoothData.SENSOR_DOWN_ADRESS);
                                if (sensor_down != null)
                                    mBluetoothService.connect(sensor_down, MAGIKARE_SENSOR_DOWN);
                                break;

                            case MAGIKARE_SENSOR_CENTER:
                                BluetoothDevice center = mBluetoothAdapter.getRemoteDevice(BluetoothData.SENSOR_CENTER_ADRESS);
                                if (center != null)
                                    mBluetoothService.connect(center, MAGIKARE_SENSOR_CENTER);
                                break;
                        }
                    }

                    break;
            }
        }
    };


    private Button btnMac1;
    private Button btnMac2;
    private Button btnMac3;
    private Button btnMac4;
    private Button btnMac5;
    private Button btnStart;
    private Button btnStop;
    private TextView txtMac;
    private int position = 0 ; //位置

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initEvent();

    }

    private void initEvent() {
        btnMac1.setOnClickListener(this);
        btnMac2.setOnClickListener(this);
        btnMac3.setOnClickListener(this);
        btnMac4.setOnClickListener(this);
        btnMac5.setOnClickListener(this);
        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);
    }

    private void initView() {
        btnMac1 = (Button) findViewById(R.id.btnMac1);
        btnMac2 = (Button) findViewById(R.id.btnMac2);
        btnMac3 = (Button) findViewById(R.id.btnMac3);
        btnMac4 = (Button) findViewById(R.id.btnMac4);
        btnMac5 = (Button) findViewById(R.id.btnMac5);
        btnStart = (Button) findViewById(R.id.btnStart);
        btnStop = (Button) findViewById(R.id.btnStop);
        txtMac = (TextView) findViewById(R.id.txtMac);
    }


    @Override
    public void onStart() {
        super.onStart();
    }

    public synchronized void onResume() {
        super.onResume();

        if (mBluetoothService != null) {
            if (mBluetoothService.getState() == BluetoothService.STATE_NONE) {
                mBluetoothService.start();
                sensor_ready=false;
            }
        }
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()){
            case R.id.btnMac1:
                position ++ ;
                BluetoothData.setAddress(position,"20:16:08:24:90:99");
                txtMac.append("\n"+btnMac1.getText().toString());break;
            case R.id.btnMac2:
                position ++ ;
                BluetoothData.setAddress(position,"20:16:08:24:91:14");
                txtMac.append("\n"+btnMac2.getText().toString());break;
            case R.id.btnMac3:
                position ++ ;
                BluetoothData.setAddress(position,"20:16:10:09:62:41");
                txtMac.append("\n"+btnMac3.getText().toString());break;
            case R.id.btnMac4:
                position ++ ;
                BluetoothData.setAddress(position,"20:16:06:15:38:09");
                txtMac.append("\n"+btnMac4.getText().toString());break;
            case R.id.btnMac5:
                position ++ ;
                BluetoothData.setAddress(position,"20:16:10:09:62:40");
                txtMac.append("\n"+btnMac5.getText().toString());break;
            case R.id.btnStart:
                openBlueTooth();
                break;
            case R.id.btnStop:
                closeBlueTooth();
                break;
        }
        BluetoothService.SENSEOR_NUM = position;
    }

    private void closeBlueTooth() {
        if (mBluetoothService!=null) {
            mBluetoothService.stop();
            mBluetoothService = null;
            BluetoothData.SENSOR_UP_ADRESS="";
            BluetoothData.SENSOR_DOWN_ADRESS="";
            BluetoothData.SENSOR_CENTER_ADRESS="";
        }
        hasConnectedPostion = 1;  //初始化
        position = 0 ;
        txtMac.setText("");
        Toast.makeText(this,"已停止当前所有蓝牙连接",Toast.LENGTH_SHORT);
    }

    /**
     * 打开蓝牙连接操作
     */
    private void openBlueTooth() {
        // 获取蓝牙适配器
        txtMac.append("\n"+BluetoothData.GetNextAddress(1));
        txtMac.append("\n"+BluetoothData.GetNextAddress(2));
        txtMac.append("\n"+BluetoothData.GetNextAddress(3));

        Log.i("xqxinfo","开启蓝牙："+BluetoothData.GetNextAddress(1)+"///"+BluetoothData.GetNextAddress(2)+"///"+BluetoothData.GetNextAddress(3));
        try {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                Toast.makeText(this, "蓝牙不可用", Toast.LENGTH_LONG).show();
                Log.i("xqxinfo","蓝牙不可用");
                return;
            }
            // 蓝牙未打开，则 打开适配器
            if (!mBluetoothAdapter.isEnabled()) mBluetoothAdapter.enable();

            hasConnectedPostion = 1;  //初始化

            // 打开蓝牙服务
            if (mBluetoothService == null)
                mBluetoothService = new BluetoothService(this, mHandler); // 用来管理蓝牙的连接

            if(mBluetoothService!=null)
            {
                Log.i("xqxinfo","mBluetoothService!=null"+BluetoothData.SENSOR_UP_ADRESS);

                //初始时连接一个蓝牙
                //获取第一个MAC对应的蓝牙硬件设备
                BluetoothDevice sensor_up=mBluetoothAdapter.getRemoteDevice(BluetoothData.SENSOR_UP_ADRESS);
                Log.i("xqxinfo","传感器地址"+sensor_up.getAddress());

                if(sensor_up!=null)
                {   //连接
                    Log.i("xqxinfo","开始连接第一个"+BluetoothData.SENSOR_UP_ADRESS);
                    mBluetoothService.connect(sensor_up,MAGIKARE_SENSOR_UP);
                }
                else
                {
                    Log.i("xqxinfo","sensor_up==null");
                }
            }

        }
        catch (Exception err){}
    }

    @Override
    public synchronized void onPause() {
        super.onPause();

    }

    @Override
    public void onStop() {
        super.onStop();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBluetoothService != null) mBluetoothService.stop();
    }



    //检测传感器状态
    public boolean SensorIsReady()
    {
        Log.d("传感器是否准备好",String.valueOf(sensor_ready));
        return sensor_ready;
    }

    public boolean HasNext()
    {
        boolean has=false;
        if(hasConnectedPostion <BluetoothService.SENSEOR_NUM)
        {
            has=true;
            hasConnectedPostion++;
        }
        Log.d("Current Pos",""+ hasConnectedPostion);
        return has;
    }


}
