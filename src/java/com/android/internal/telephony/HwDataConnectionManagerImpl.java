package com.android.internal.telephony;

import android.net.TrafficStats;
import android.os.SystemProperties;
import android.provider.Settings.System;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArraySet;
import com.android.internal.telephony.dataconnection.AbstractDcTrackerBase;
import com.android.internal.telephony.dataconnection.AbstractDcTrackerBase.DcTrackerBaseReference;
import com.android.internal.telephony.dataconnection.ApnSetting;
import com.android.internal.telephony.dataconnection.DcTracker;
import com.android.internal.telephony.dataconnection.HwDcTrackerBaseReference;
import java.util.HashMap;

public class HwDataConnectionManagerImpl implements HwDataConnectionManager {
    private static int CONNECTIVITY_SERVICE_NEED_SET_USER_DATA = 1100;
    private static HwDataConnectionManager mInstance = new HwDataConnectionManagerImpl();

    public static native String sbmcgmGenId(String str, String str2, String str3);

    public static native String sbmcgmGenPasswd(String str);

    static {
        if (SystemProperties.getBoolean("ro.config.sbmcgm", false)) {
            try {
                System.load(SystemProperties.get("ro.config.sbmjni_uri"));
            } catch (UnsatisfiedLinkError e) {
                Rlog.e("SBM", "sbnam load sbm jni fail:", e);
            }
        }
    }

    public DcTrackerBaseReference createHwDcTrackerBaseReference(AbstractDcTrackerBase dcTrackerBase) {
        return new HwDcTrackerBaseReference((DcTracker) dcTrackerBase);
    }

    public static HwDataConnectionManager getDefault() {
        return mInstance;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean needSetUserDataEnabled(boolean r12) {
        /*
        r11 = this;
        r7 = 0;
        r6 = 1;
        r8 = "connectivity";
        r8 = android.os.ServiceManager.getService(r8);
        r0 = android.net.IConnectivityManager.Stub.asInterface(r8);
        r2 = android.os.Parcel.obtain();
        r4 = android.os.Parcel.obtain();
        r1 = r0.asBinder();	 Catch:{ RemoteException -> 0x005d }
        if (r1 == 0) goto L_0x002f;
    L_0x001b:
        r8 = "android.net.IConnectivityManager";
        r2.writeInterfaceToken(r8);	 Catch:{ RemoteException -> 0x005d }
        if (r12 == 0) goto L_0x0059;
    L_0x0023:
        r8 = r6;
    L_0x0024:
        r2.writeInt(r8);	 Catch:{ RemoteException -> 0x005d }
        r8 = CONNECTIVITY_SERVICE_NEED_SET_USER_DATA;	 Catch:{ RemoteException -> 0x005d }
        r8 = r8 + 1;
        r9 = 0;
        r1.transact(r8, r2, r4, r9);	 Catch:{ RemoteException -> 0x005d }
    L_0x002f:
        android.database.DatabaseUtils.readExceptionFromParcel(r4);	 Catch:{ RemoteException -> 0x005d }
        r5 = r4.readInt();	 Catch:{ RemoteException -> 0x005d }
        r8 = "HwDataConnectionManager";
        r9 = new java.lang.StringBuilder;	 Catch:{ RemoteException -> 0x005d }
        r9.<init>();	 Catch:{ RemoteException -> 0x005d }
        r10 = "needSetUserDataEnabled result = ";
        r9 = r9.append(r10);	 Catch:{ RemoteException -> 0x005d }
        r9 = r9.append(r5);	 Catch:{ RemoteException -> 0x005d }
        r9 = r9.toString();	 Catch:{ RemoteException -> 0x005d }
        android.telephony.Rlog.d(r8, r9);	 Catch:{ RemoteException -> 0x005d }
        if (r5 != r6) goto L_0x005b;
    L_0x0052:
        r4.recycle();
        r2.recycle();
        return r6;
    L_0x0059:
        r8 = r7;
        goto L_0x0024;
    L_0x005b:
        r6 = r7;
        goto L_0x0052;
    L_0x005d:
        r3 = move-exception;
        r3.printStackTrace();	 Catch:{ all -> 0x0068 }
        r4.recycle();
        r2.recycle();
        return r6;
    L_0x0068:
        r6 = move-exception;
        r4.recycle();
        r2.recycle();
        throw r6;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.HwDataConnectionManagerImpl.needSetUserDataEnabled(boolean):boolean");
    }

    public long getThisModemMobileTxPackets(HashMap<String, Integer> mIfacePhoneHashMap, int phoneId) {
        long total = 0;
        for (String iface : getThisModemMobileIfaces(mIfacePhoneHashMap, phoneId)) {
            long temp = TrafficStats.getTxPackets(iface);
            if (temp == 0) {
                Rlog.d("HwDataConnectionManager", "getThisModemMobileTxPackets is 0");
            }
            total += temp;
        }
        return total;
    }

    public long getThisModemMobileRxPackets(HashMap<String, Integer> mIfacePhoneHashMap, int phoneId) {
        long total = 0;
        for (String iface : getThisModemMobileIfaces(mIfacePhoneHashMap, phoneId)) {
            long temp = TrafficStats.getRxPackets(iface);
            if (temp == 0) {
                Rlog.d("HwDataConnectionManager", "getThisModemMobileRxPackets is 0");
            }
            total += temp;
        }
        return total;
    }

    private String[] getThisModemMobileIfaces(HashMap<String, Integer> mIfacePhoneHashMap, int phoneId) {
        ArraySet<String> mobileIfaces = new ArraySet();
        for (String iface : TrafficStats.getMobileIfaces()) {
            if (mIfacePhoneHashMap.get(iface) == null || ((Integer) mIfacePhoneHashMap.get(iface)).equals(Integer.valueOf(phoneId))) {
                mobileIfaces.add(iface);
            }
        }
        return (String[]) mobileIfaces.toArray(new String[mobileIfaces.size()]);
    }

    public boolean getNamSwitcherForSoftbank() {
        return SystemProperties.getBoolean("ro.config.sbmcgm", false);
    }

    public boolean isSoftBankCard(Phone mPhone) {
        String softbankPlmns = System.getString(mPhone.getContext().getContentResolver(), "hw_softbank_plmn");
        TelephonyManager tm = (TelephonyManager) mPhone.getContext().getSystemService("phone");
        String operator = tm != null ? tm.getSimOperator() : "";
        Rlog.e("HwDataConnectionManager", "sbnam:isSoftBankCard sbnam hw_softbank_plmn:" + softbankPlmns + " operator:" + operator);
        if (softbankPlmns != null) {
            String[] plmns = softbankPlmns.split(",");
            int length = plmns.length;
            int i = 0;
            while (i < length) {
                String plmn = plmns[i];
                if (plmn == null || !plmn.equals(operator)) {
                    i++;
                } else {
                    Rlog.e("HwDataConnectionManager", "sbnam:isSoftBankCard sbnam find softbank card " + operator);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isValidMsisdn(Phone mPhone) {
        String line1Number = mPhone.getLine1Number();
        if (line1Number != null) {
            return line1Number.isEmpty() ^ 1;
        }
        return false;
    }

    public HashMap<String, String> encryptApnInfoForSoftBank(Phone phone, ApnSetting apnSetting) {
        String sUsername = apnSetting.user;
        String sPassword = apnSetting.password;
        Rlog.e("HwDataConnectionManager", "softbanknam: before encryption");
        if (!TextUtils.isEmpty(sUsername)) {
            return null;
        }
        if (!TextUtils.isEmpty(sPassword)) {
            return null;
        }
        TelephonyManager tm = (TelephonyManager) phone.getContext().getSystemService("phone");
        if (tm == null) {
            return null;
        }
        if (!isSoftBankCard(phone)) {
            Rlog.e("HwDataConnectionManager", "softbanknam: not softbank card");
            return null;
        } else if (isValidMsisdn(phone)) {
            String msn = tm.getLine1Number();
            if (msn != null && msn.length() > 11) {
                msn = msn.substring(0, 11);
            }
            String imei = tm.getDeviceId();
            if (imei != null && imei.length() > 14) {
                imei = imei.substring(0, 14);
            }
            String imsi = tm.getSubscriberId();
            if (imsi != null && imsi.length() > 15) {
                imsi = imsi.substring(0, 15);
            }
            String iccid = tm.getSimSerialNumber();
            if (iccid != null && iccid.length() > 19) {
                iccid = iccid.substring(0, 19);
            }
            String user = sbmcgmGenId(msn, imei, imsi);
            if (TextUtils.isEmpty(user)) {
                return null;
            }
            HashMap<String, String> userInfo = new HashMap();
            userInfo.put("username", user);
            userInfo.put("password", sbmcgmGenPasswd(iccid));
            Rlog.e("HwDataConnectionManager", "softbanknam: after encryption finish");
            return userInfo;
        } else {
            Rlog.e("HwDataConnectionManager", "softbanknam: no msisdn softbank card");
            return null;
        }
    }

    public boolean isDeactivatingSlaveData() {
        HwPhoneService service = HwPhoneService.getInstance();
        if (service == null) {
            return false;
        }
        return service.isDeactivatingSlaveData();
    }

    public boolean isSlaveActive() {
        HwPhoneService service = HwPhoneService.getInstance();
        if (service == null) {
            return false;
        }
        return service.isSlaveActive();
    }

    public boolean isSwitchingToSlave() {
        HwPhoneService service = HwPhoneService.getInstance();
        if (service == null) {
            return false;
        }
        return service.isSwitchingToSlave();
    }

    public void registerImsCallStates(boolean enable, int phoneId) {
        HwPhoneService service = HwPhoneService.getInstance();
        if (service != null) {
            service.registerImsCallStates(enable, phoneId);
        }
    }
}
