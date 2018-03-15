package huawei.android.common;

import android.common.HwFrameworkFactory.Factory;
import android.telephony.HwInnerTelephonyManager;
import android.telephony.HwInnerTelephonyManagerImpl;

public class HwFrameworkFactoryImpl implements Factory {
    public HwInnerTelephonyManager getHwInnerTelephonyManager() {
        return HwInnerTelephonyManagerImpl.getDefault();
    }
}
