package android.telephony;

import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import com.android.internal.telephony.IHwTelephony;
import com.android.internal.telephony.IHwTelephony.Stub;

public class HwTelephonyManager {
    private static final String TAG = "HwTelephonyManager";
    private static HwTelephonyManager sInstance = new HwTelephonyManager();

    public static HwTelephonyManager getDefault() {
        return sInstance;
    }

    private IHwTelephony getIHwTelephony() throws RemoteException {
        IHwTelephony iHwTelephony = Stub.asInterface(ServiceManager.getService("phone_huawei"));
        if (iHwTelephony != null) {
            return iHwTelephony;
        }
        Log.e(TAG, "getIHwTelephony failed!");
        throw new RemoteException("getIHwTelephony return null");
    }

    public void setDefaultDataSlotId(int slotId) {
        try {
            getIHwTelephony().setDefaultDataSlotId(slotId);
        } catch (RemoteException e) {
        } catch (NullPointerException e2) {
        }
    }

    public int getDefault4GSlotId() {
        try {
            return getIHwTelephony().getDefault4GSlotId();
        } catch (RemoteException e) {
            return 0;
        }
    }

    public void setDefault4GSlotId(int slotId, Message msg) {
        Rlog.d(TAG, "In setDefault4GSlotId");
        try {
            getIHwTelephony().setDefault4GSlotId(slotId, msg);
        } catch (RemoteException ex) {
            Rlog.e(TAG, "RemoteException ex = " + ex);
        }
    }

    public boolean isSetDefault4GSlotIdEnabled() {
        Rlog.d(TAG, "In isSetDefault4GSlotIdEnabled");
        try {
            return getIHwTelephony().isSetDefault4GSlotIdEnabled();
        } catch (RemoteException ex) {
            Rlog.e(TAG, "RemoteException ex = " + ex);
            return false;
        }
    }

    public void waitingSetDefault4GSlotDone(boolean waiting) {
        Rlog.d(TAG, "In waitingSetDefault4GSlotDone");
        try {
            getIHwTelephony().waitingSetDefault4GSlotDone(waiting);
        } catch (RemoteException ex) {
            Rlog.e(TAG, "RemoteException ex = " + ex);
        }
    }

    public String getIccATR() {
        String strATR = SystemProperties.get("gsm.sim.hw_atr", "null");
        strATR = strATR + "," + SystemProperties.get("gsm.sim.hw_atr1", "null");
        Rlog.d(TAG, "getIccATR: [" + strATR + "]");
        return strATR;
    }
}
