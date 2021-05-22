package com.example.bluetoothdemo.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * 用于展示Toast的工具类
 */
public class ToastUtil {
    private final Context mContext;

    public ToastUtil(Context context){
        this.mContext = context;
    }
    public void showToast(CharSequence text){
        Toast.makeText(mContext,text,Toast.LENGTH_SHORT).show();
    }
}
