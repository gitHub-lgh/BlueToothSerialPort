package com.example.bluetoothdemo.bean;
/**
 * 蓝牙设备信息实体类（设备名称及地址）
 * author:CSDN 在下木子李
 * create at 2021/1/24
 */
public class DeviceInformation {
    private String deviceName;
    private String deviceAddress;

    public DeviceInformation(String deviceName, String deviceAddress) {
        this.deviceName = deviceName;
        this.deviceAddress = deviceAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }
}
