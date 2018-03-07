package android.telephony;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.android.internal.telephony.IHwTelephony;
import com.android.internal.telephony.IHwTelephony.Stub;

public class HwTelephonyManager {
    private static HwTelephonyManager sInstance;

    public static HwTelephonyManager getDefault() {
        return sInstance;
    }

    private IHwTelephony getIHwTelephony() throws RemoteException {
        IHwTelephony iHwTelephony = Stub.asInterface(ServiceManager.getService("phone_huawei"));
        if (iHwTelephony != null) {
            return iHwTelephony;
        }
        Log.e("HwTelephonyManager", "getIHwTelephony failed!");
        throw new RemoteException("getIHwTelephony return null");
    }

    public int getDefault4GSlotId() {
        try {
            return getIHwTelephony().getDefault4GSlotId();
        } catch (RemoteException e) {
            return 0;
        }
    }
}
