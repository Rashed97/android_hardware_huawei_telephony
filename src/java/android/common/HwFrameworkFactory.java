package android.common;

import android.telephony.HwInnerTelephonyManager;
import android.util.Log;

public class HwFrameworkFactory {
    private static final String TAG = "HwFrameworkFactory";
    private static final Object mLock = new Object();
    private static Factory obj = null;

    public interface Factory {

        HwInnerTelephonyManager getHwInnerTelephonyManager();

    }

    public static HwInnerTelephonyManager getHwInnerTelephonyManager() {
        return getImplObject().getHwInnerTelephonyManager();
    }

    private static Factory getImplObject() {
        if (obj != null) {
            return obj;
        }
        synchronized (mLock) {
            try {
                obj = (Factory) Class.forName("huawei.android.common.HwFrameworkFactoryImpl").newInstance();
            } catch (Exception e) {
                Log.e(TAG, ": reflection exception is " + e);
            }
        }
        if (obj != null) {
            Log.v(TAG, ": successes to get AllImpl object and return....");
            return obj;
        }
        Log.e(TAG, ": failes to get AllImpl object");
        return null;
    }
}
