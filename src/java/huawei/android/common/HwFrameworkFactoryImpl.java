package huawei.android.common;

import android.net.wifi.HwInnerNetworkManager;
import android.net.wifi.HwInnerNetworkManagerImpl;
import android.net.wifi.HwInnerTelephonyManager;
import android.net.wifi.HwInnerTelephonyManagerImpl;

public class HwFrameworkFactoryImpl implements Factory {
    public static final String ACTION_HW_CHOOSER = "com.huawei.intent.action.hwCHOOSER";
    private static final String TAG = "HwFrameworkFactoryImpl";

    public HwInnerNetworkManager getHwInnerNetworkManager() {
        return HwInnerNetworkManagerImpl.getDefault();
    }

    public HwInnerTelephonyManager getHwInnerTelephonyManager() {
        return HwInnerTelephonyManagerImpl.getDefault();
    }
}
