package android.telephony;

import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.HwModemCapability;
import com.android.internal.telephony.IHwTelephony;
import com.android.internal.telephony.IHwTelephony.Stub;

public class HwTelephonyManager {
    public static final String CARD_TYPE_SIM1 = "gsm.sim1.type";
    public static final String CARD_TYPE_SIM2 = "gsm.sim2.type";
    private static final String[] CDMA_CPLMNS = new String[]{"46003", "45502", "46012"};
    public static final int DUAL_MODE_TELECOM_LTE_CARD = 43;
    private static final String GC_ICCID = "8985231";
    private static final String PROP_VALUE_C_CARD_PLMN = "gsm.sim.c_card.plmn";
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

    public boolean isCTSimCard(int slotId) {
        boolean isCTCardType;
        boolean result;
        int cardType = getCardType(slotId);
        Rlog.d(TAG, "[isCTSimCard]: cardType = " + cardType);
        switch (cardType) {
            case 30:
            case 41:
            case DUAL_MODE_TELECOM_LTE_CARD /*43*/:
                isCTCardType = true;
                break;
            default:
                isCTCardType = false;
                break;
        }
        if (!isCTCardType || (HwModemCapability.isCapabilitySupport(9) ^ true) == false) {
            result = isCTCardType;
        } else {
            boolean isCdmaCplmn = false;
            String cplmn = getCplmn(slotId);
            String[] strArr = CDMA_CPLMNS;
            int i = 0;
            int length = strArr.length;
            while (i < length) {
                if (strArr[i].equals(cplmn)) {
                    isCdmaCplmn = true;
                    Rlog.d(TAG, "[isCTSimCard]: hisi cdma  isCdmaCplmn = " + isCdmaCplmn);
                    result = isCdmaCplmn;
                    if (TextUtils.isEmpty(cplmn)) {
                        try {
                            result = getIHwTelephony().isCtSimCard(slotId);
                        } catch (RemoteException ex) {
                            ex.printStackTrace();
                        }
                    }
                    Rlog.d(TAG, "[isCTSimCard]: hisi cdma  isCdmaCplmn according iccid = " + result);
                } else {
                    i++;
                }
            }
            Rlog.d(TAG, "[isCTSimCard]: hisi cdma  isCdmaCplmn = " + isCdmaCplmn);
            result = isCdmaCplmn;
            if (TextUtils.isEmpty(cplmn)) {
                try {
                    result = getIHwTelephony().isCtSimCard(slotId);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
            Rlog.d(TAG, "[isCTSimCard]: hisi cdma  isCdmaCplmn according iccid = " + result);
        }
        if (result) {
            String preIccid = SystemProperties.get("gsm.sim.preiccid_" + slotId, "");
            if (GC_ICCID.equals(preIccid)) {
                result = false;
                Rlog.d(TAG, "Hongkong GC card is not CT card:" + preIccid);
            }
        }
        Rlog.d(TAG, "[isCTSimCard]: result = " + result);
        return result;
    }

    private String getCplmn(int slotId) {
        String result = "";
        String value = SystemProperties.get(PROP_VALUE_C_CARD_PLMN, "");
        if (!(value == null || ("".equals(value) ^ true) == false)) {
            String[] substr = value.split(",");
            if (substr.length == 2 && Integer.parseInt(substr[1]) == slotId) {
                result = substr[0];
            }
        }
        Rlog.d(TAG, "getCplmn for Slot : " + slotId + " result is : " + result);
        return result;
    }

    public boolean isCDMASimCard(int slotId) {
        int cardType = getCardType(slotId);
        Rlog.d(TAG, "[isCDMASimCard]: cardType = " + cardType);
        switch (cardType) {
            case 30:
            case 40:
            case 41:
            case DUAL_MODE_TELECOM_LTE_CARD /*43*/:
                return true;
            default:
                return false;
        }
    }

    public int getCardType(int slotId) {
        if (slotId == 0) {
            return SystemProperties.getInt(CARD_TYPE_SIM1, -1);
        }
        if (slotId == 1) {
            return SystemProperties.getInt(CARD_TYPE_SIM2, -1);
        }
        return -1;
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

    public boolean isDataConnectivityDisabled(int slotId, String tag) {
/*
        Bundle bundle = this.mDpm.getPolicy(null, tag);
        boolean allow = false;
        if (bundle != null) {
            allow = bundle.getBoolean("value");
        }
        if (allow && 1 == slotId) {
            return true;
        }
*/
        return false;
    }
}
