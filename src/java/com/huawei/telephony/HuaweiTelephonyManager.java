package com.huawei.telephony;

import android.telephony.HwTelephonyManager;

public class HuaweiTelephonyManager {
    private static HuaweiTelephonyManager mInstance = new HuaweiTelephonyManager();

    public static HuaweiTelephonyManager getDefault() {
        return mInstance;
    }

    public int getDefault4GSlotId() {
        return HwTelephonyManager.getDefault().getDefault4GSlotId();
    }
}
