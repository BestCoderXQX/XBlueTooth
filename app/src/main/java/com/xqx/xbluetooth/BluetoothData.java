/**
 * Created by Admin on 2016/6/28.
 */
package com.xqx.xbluetooth;

public class BluetoothData {

    public static  String SENSOR_UP_ADRESS="";        //  第一个硬件MAC地址
    public static  String SENSOR_DOWN_ADRESS="";       // 第二个硬件MAC地址
    public static  String SENSOR_CENTER_ADRESS="";     //  第三个硬件MAC地址

    static String[]  list_adr={SENSOR_UP_ADRESS,SENSOR_DOWN_ADRESS,
            SENSOR_CENTER_ADRESS};

    public static String  GetNextAddress(int current_pos)
    {
        if(current_pos<=list_adr.length)
        {
            return list_adr[current_pos-1];
        }
        return  null;
    }

    public static void setAddress(int current_pos,String mac){
        list_adr[current_pos-1] = mac;
        if (current_pos==1) {
            SENSOR_UP_ADRESS = mac;
        }else if (current_pos==2){
            SENSOR_DOWN_ADRESS = mac;
        }else if (current_pos==3){
            SENSOR_CENTER_ADRESS = mac;
        }
    }

}
