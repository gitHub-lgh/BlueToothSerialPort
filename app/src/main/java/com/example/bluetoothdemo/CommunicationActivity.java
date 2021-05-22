package com.example.bluetoothdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bluetoothdemo.utils.ToastUtil;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
/**
 * 串口通信页面
 * author:CSDN 在下木子李
 * create at 2021/1/24
 */

public class CommunicationActivity extends AppCompatActivity {
    private EditText mEditText;
    private Button mSendBtn;
    private String mAddress;
    private BluetoothAdapter mBlueToothAdapter;
    private BluetoothDevice mDevice;
    private BluetoothSocket mBluetoothSocket;
    private final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");//蓝牙串口服务的UUID
    private ToastUtil mToast;
    private TextView mReceiveContent;
    private TextView mSendContent;
    private TextView mCancelConn;
    private String mSendContentStr;
    private static OutputStream mOS;
    private String TAG = "CommunicationActivity";
    private String mName;
    private TextView mBtName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.communication_layout);
        Intent intent = getIntent();
        //得到传输过来的设备地址
        mAddress = intent.getStringExtra("address");
        mName = intent.getStringExtra("name");
        initView();
        initListener();
        //开始连接
        connectDevice();
    }

    private void initView() {
        mEditText = findViewById(R.id.send_edit_text);
        mSendBtn = findViewById(R.id.send_text_btn);
        mToast = new ToastUtil(this);
        mReceiveContent = findViewById(R.id.received_text_content);
        mSendContent = findViewById(R.id.send_text_content);
        mCancelConn = findViewById(R.id.cancel_conn_btn);
        mBtName = findViewById(R.id.bluetooth_name);
        mBtName.setText(mName);
    }

    private void initListener() {
        mSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSendContentStr = mEditText.getText().toString();
                //发送信息
                sendMessage(mSendContentStr);
            }
        });
        mCancelConn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    /**
     * 发送数据的方法
     * @param contentStr
     */
    private void sendMessage(String contentStr) {
        if (mBluetoothSocket.isConnected()) {
            try {
                //获取输出流
                mOS = mBluetoothSocket.getOutputStream();
                if (mOS != null) {
                    //写数据（参数为byte数组）
                    mOS.write(contentStr.getBytes("GBK"));
                    mEditText.getText().clear();
                    mSendContent.append(contentStr);
                    mToast.showToast("发送成功");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            mToast.showToast("没有设备已连接");
        }
    }

    /**
     * 与目标设备建立连接
     */
    private void connectDevice() {

        //获取默认蓝牙设配器
        mBlueToothAdapter = BluetoothAdapter.getDefaultAdapter();
        //通过地址拿到该蓝牙设备device
        mDevice = mBlueToothAdapter.getRemoteDevice(mAddress);
        try {
            //建立socket通信
            mBluetoothSocket = mDevice.createRfcommSocketToServiceRecord(mUUID);
            mBluetoothSocket.connect();
            if (mBluetoothSocket.isConnected()) {
                mToast.showToast("连接成功");
                //开启接收数据的线程
                ReceiveDataThread thread = new ReceiveDataThread();
                thread.start();
            }else{
                mToast.showToast("连接失败，结束重进");
            }
        } catch (IOException e) {
            e.printStackTrace();
            mToast.showToast("连接出错！ ");
            finish();
            try {
                mBluetoothSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mBluetoothSocket.isConnected()) {
                //关闭socket
                mBluetoothSocket.close();
                mBlueToothAdapter = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 负责接收数据的线程
     */
    public class ReceiveDataThread extends Thread{

        private InputStream inputStream;

        public ReceiveDataThread() {
            super();
            try {
                //获取连接socket的输入流
                inputStream = mBluetoothSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void run() {
            super.run();
            int len = 0;
            byte[] buffer = new byte[256];
            while (true){
                try {
                    inputStream.read(buffer);
                    for (byte b : buffer) {
                        Log.d(TAG,"b:" + b);
                    }
                    //设置GBK格式可以获取到中文信息，不会乱码
                    //这里的长度减3是因为使用蓝牙助手调试的原因，正常项目不需要减3
                    String a = new String(buffer,0,buffer.length - 3,"GBK");
                    Log.d(TAG,"a:" + a);
//                    byte[] gbks = "你好".getBytes("GBK");
//                    for (byte gbk : gbks) {
//                        Log.d(TAG,"gbk:" + gbk);
//                    }
//                    String[] chars = a.split(" ");
//                    String str = "";
//                    for(int i = 0; i<chars.length;i++){
//                        str += (char)Integer.parseInt(chars[i]);
//                    }
                    //Log.d(TAG,"str:" + str);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //将收到的数据显示在TextView上
                            mReceiveContent.append(a);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
