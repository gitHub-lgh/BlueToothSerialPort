package com.example.bluetoothdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.example.bluetoothdemo.adapter.MyArrayAdapter;
import com.example.bluetoothdemo.bean.DeviceInformation;
import com.example.bluetoothdemo.utils.ToastUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 蓝牙串口通信app
 * author:CSDN 在下木子李
 * create at 2021/1/24
 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int MY_PERMISSION_REQUEST_CONSTANT = 1;
    private Button mOpenBluetoothBtn;
    private Button mFoundDeviceBtn;
    private ListView mDeviceList;
    private MyArrayAdapter mAdapter;
    private List<DeviceInformation> mDatas = new ArrayList<>();
    private BluetoothAdapter mBluetoothAdapter;
    private ToastUtil mToast;
    private BroadcastReceiver mBluetoothReceiver;//用于接收蓝牙状态改变广播的广播接收者
    private String TAG = "MainActivity";
    private BroadcastReceiver mBLuetoothStateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initReceiver();
        initView();
        initListener();
    }
    /*
    注册广播接收者
     */
    private void initReceiver() {
        //创建用于接收蓝牙状态改变广播的广播接收者
        mBLuetoothStateReceiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                switch (state){
                    case BluetoothAdapter.STATE_ON:
                        mToast.showToast("蓝牙已打开");
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        mToast.showToast("蓝牙已关闭");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        mToast.showToast("蓝牙正在打开");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        mToast.showToast("蓝牙正在关闭");
                        break;
                }
            }
        };
        //创建设备扫描广播接收者
        mBluetoothReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG,"onReceive");

                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    boolean isAdded = false;//标记扫描到的设备是否已经在数据列表里了
                    //获取扫描到的设备
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    //保存设备的信息
                    DeviceInformation deviceInformation = new DeviceInformation(device.getName(),device.getAddress());
                    for (DeviceInformation data : mDatas) {
                        //判断已保存的设备信息里是否有一样的
                        if (data.getDeviceAddress().equals(deviceInformation.getDeviceAddress())) {
                            isAdded = true;
                            break;
                        }
                    }
                    if (!isAdded) {
                        //通知UI更新
                        mDatas.add(deviceInformation);
                        mAdapter.notifyDataSetChanged();
                    }
                }
            }
        };
        //注册广播接收者
        IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_FOUND);

        registerReceiver(mBLuetoothStateReceiver,filter1);
        registerReceiver(mBluetoothReceiver,filter2);
    }
    //权限是否授予，给出提示
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST_CONSTANT: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mToast.showToast("权限授权成功");
                }else{
                    mToast.showToast("权限授权失败");
                }
                return;
            }
        }
    }

    private void initView() {
        //安卓6.0开始需要动态申请权限
        if (Build.VERSION.SDK_INT >= 6.0) {
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSION_REQUEST_CONSTANT);
        }
        mOpenBluetoothBtn = findViewById(R.id.open_bluetooth_btn);
        mFoundDeviceBtn = findViewById(R.id.fount_device_btn);
        mDeviceList = findViewById(R.id.bluetooth_device_list);
        mToast = new ToastUtil(this);
        mAdapter = new MyArrayAdapter(mDatas,this);
        mDeviceList.setAdapter(mAdapter);
    }
    //初始化监听
    private void initListener() {
        mOpenBluetoothBtn.setOnClickListener(this);
        mFoundDeviceBtn.setOnClickListener(this);
        //设备列表item的点击事件
        mDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (mBluetoothAdapter.isDiscovering()) {
                    //停止搜索设备
                    mBluetoothAdapter.cancelDiscovery();
                }
                //获取点击的item的设备信息
                DeviceInformation deviceInformation = mDatas.get(position);
                //跳转到设备通信页面
                Intent intent = new Intent(MainActivity.this,CommunicationActivity.class);
                //将设备地址传递过去
                intent.putExtra("name",deviceInformation.getDeviceName());
                intent.putExtra("address",deviceInformation.getDeviceAddress());
                startActivity(intent);
            }
        });
    }

    /**
     * 检测和开启蓝牙
     */
    private void openBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            //判断蓝牙是否打开并可见
            if (!mBluetoothAdapter.isEnabled()) {
                //请求打开并可见
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent,1);
            }
        }else{
            mToast.showToast("设备不支持蓝牙功能");
        }
    }

    /**
     * 搜索蓝牙设备
     */
    private void discoverBluetooth(){
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        //搜索设备
        mBluetoothAdapter.startDiscovery();
        mToast.showToast("正在搜索设备");
    }

    /**
    点击事件
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.open_bluetooth_btn:
                //开启蓝牙
                openBluetooth();
                break;
            case R.id.fount_device_btn:
                //搜索设备
                discoverBluetooth();
                break;
            default:
                break;
        }
    }
}