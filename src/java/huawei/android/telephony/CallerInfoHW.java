package huawei.android.telephony;

import android.database.Cursor;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import com.android.i18n.phonenumbers.CountryCodeToRegionCodeMapUtils;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.internal.telephony.HwTelephonyProperties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CallerInfoHW implements TelephonyInterfacesHW {
    private static final String CHINA_AREACODE = "0";
    private static final String CHINA_OPERATOR_MCC = "460";
    private static final int CN_FIXED_NUMBER_WITH_AREA_CODE_MIN_LEN = 9;
    private static final String CN_MPN_PATTERN = "^(1)\\d{10}$";
    private static final int CN_NUM_MATCH = 11;
    protected static final boolean DBG = false;
    private static final String FIXED_NUMBER_TOP2_TOKEN1 = "01";
    private static final String FIXED_NUMBER_TOP2_TOKEN2 = "02";
    private static final String[] INTERNATIONAL_PREFIX = new String[]{"+00", "+", "00"};
    private static final String[] IPHEAD = new String[]{"10193", "11808", "12593", "17900", "17901", "17908", "17909", "17910", "17911", "17931", "17950", "17951", "17960", "17968", "17969", "96435"};
    private static final int IPHEAD_LENTH = 5;
    private static final boolean IS_SUPPORT_DUAL_NUMBER = SystemProperties.getBoolean("ro.config.hw_dual_number", false);
    public static final int MIN_MATCH = 7;
    private static final String[] NORMAL_PREFIX_MCC = new String[]{"602", "722"};
    private static final String TAG = "CallerInfo";
    private static CallerInfoHW sCallerInfoHwInstance = null;
    private static PhoneNumberUtil sInstance = PhoneNumberUtil.getInstance();
    private boolean IS_CHINA_TELECOM;
    private boolean IS_MIIT_NUM_MATCH;
    private final int NUM_LONG_CUST;
    private final int NUM_SHORT_CUST;
    private final Map<Integer, ArrayList<String>> chineseFixNumberAreaCodeMap;
    private int configMatchNum = SystemProperties.getInt("ro.config.hwft_MatchNum", 7);
    private int configMatchNumShort;
    private final Map<Integer, List<String>> countryCallingCodeToRegionCodeMap;
    private int countryCodeforCN;
    private String mNetworkOperator;
    private int mSimNumLong;
    private int mSimNumShort;

    public CallerInfoHW() {
        boolean equals;
        int i = 7;
        if (this.configMatchNum >= 7) {
            i = this.configMatchNum;
        }
        this.NUM_LONG_CUST = i;
        this.configMatchNumShort = SystemProperties.getInt("ro.config.hwft_MatchNumShort", this.NUM_LONG_CUST);
        this.NUM_SHORT_CUST = this.configMatchNumShort >= this.NUM_LONG_CUST ? this.NUM_LONG_CUST : this.configMatchNumShort;
        this.mSimNumLong = this.NUM_LONG_CUST;
        this.mSimNumShort = this.NUM_SHORT_CUST;
        if (SystemProperties.get("ro.config.hw_opta", "0").equals("92")) {
            equals = SystemProperties.get("ro.config.hw_optb", "0").equals("156");
        } else {
            equals = false;
        }
        this.IS_CHINA_TELECOM = equals;
        this.IS_MIIT_NUM_MATCH = SystemProperties.getBoolean("ro.config.miit_number_match", false);
        this.mNetworkOperator = null;
        this.countryCodeforCN = sInstance.getCountryCodeForRegion("CN");
        this.countryCallingCodeToRegionCodeMap = CountryCodeToRegionCodeMapUtils.getCountryCodeToRegionCodeMap();
        this.chineseFixNumberAreaCodeMap = ChineseFixNumberAreaCodeMap.getChineseFixNumberAreaCodeMap();
    }

    public static synchronized CallerInfoHW getInstance() {
        CallerInfoHW callerInfoHW;
        synchronized (CallerInfoHW.class) {
            if (sCallerInfoHwInstance == null) {
                sCallerInfoHwInstance = new CallerInfoHW();
            }
            callerInfoHW = sCallerInfoHwInstance;
        }
        return callerInfoHW;
    }

    public String getCountryIsoFromDbNumber(String number) {
        logd("getCountryIsoFromDbNumber(), number: " + number);
        if (TextUtils.isEmpty(number)) {
            return null;
        }
        int len = getIntlPrefixLength(number);
        if (len > 0) {
            String tmpNumber = number.substring(len);
            for (Integer intValue : this.countryCallingCodeToRegionCodeMap.keySet()) {
                int countrycode = intValue.intValue();
                if (tmpNumber.startsWith(Integer.toString(countrycode))) {
                    String countryIso = sInstance.getRegionCodeForCountryCode(countrycode);
                    logd("getCountryIsoFromDbNumber(), find matched country code: " + countrycode + ", and country iso: " + countryIso);
                    return countryIso;
                }
            }
            logd("getCountryIsoFromDbNumber(), no matched country code, returns null");
        }
        return null;
    }

    public boolean compareNums(String num1, String netIso1, String num2, String netIso2) {
        boolean z = false;
        String str = null;
        String str2 = null;
        String str3 = null;
        String str4 = null;
        int i = this.NUM_LONG_CUST;
        int i2 = this.NUM_SHORT_CUST;
        if (num1 == null || num2 == null) {
            return false;
        }
        int numMatchShort;
        logd("compareNums, num1 = " + num1 + ", netIso1 = " + netIso1 + ", num2 = " + num2 + ", netIso2 = " + netIso2);
        if (SystemProperties.getInt("ro.config.hwft_MatchNum", 0) == 0) {
            int numMatch;
            numMatch = SystemProperties.getInt(HwTelephonyProperties.PROPERTY_GLOBAL_VERSION_NUM_MATCH, 7);
            numMatchShort = SystemProperties.getInt(HwTelephonyProperties.PROPERTY_GLOBAL_VERSION_NUM_MATCH_SHORT, numMatch);
            if (numMatch < 7) {
                i = 7;
            } else {
                i = numMatch;
            }
            this.mSimNumLong = i;
            if (numMatchShort >= i) {
                i2 = i;
            } else {
                i2 = numMatchShort;
            }
            this.mSimNumShort = i2;
            logd("compareNums, after setprop NUM_LONG = " + i + ", NUM_SHORT = " + i2);
        }
        if (num1.indexOf(64) < 0) {
            num1 = PhoneNumberUtils.stripSeparators(num1);
        }
        if (num2.indexOf(64) < 0) {
            num2 = PhoneNumberUtils.stripSeparators(num2);
        }
        num1 = formatedForDualNumber(num1);
        num2 = formatedForDualNumber(num2);
        if (this.IS_CHINA_TELECOM && num1.startsWith("**133") && num1.endsWith("#")) {
            num1 = num1.substring(0, num1.length() - 1);
            logd("compareNums, num1 startsWith **133 && endsWith #");
        }
        if (this.IS_CHINA_TELECOM && num2.startsWith("**133") && num2.endsWith("#")) {
            num2 = num2.substring(0, num2.length() - 1);
            logd("compareNums, num2 startsWith **133 && endsWith #");
        }
        if (num1.equals(num2)) {
            logd("compareNums, full compare returns true.");
            return true;
        }
        String origNum1 = num1;
        String origNum2 = num2;
        if (!TextUtils.isEmpty(netIso1)) {
            String formattedNum1 = PhoneNumberUtils.formatNumberToE164(num1, netIso1.toUpperCase(Locale.US));
            if (formattedNum1 != null) {
                logd("compareNums, formattedNum1: " + formattedNum1 + ", with netIso1: " + netIso1);
                num1 = formattedNum1;
            }
        }
        if (!TextUtils.isEmpty(netIso2)) {
            String formattedNum2 = PhoneNumberUtils.formatNumberToE164(num2, netIso2.toUpperCase(Locale.US));
            if (formattedNum2 != null) {
                logd("compareNums, formattedNum2: " + formattedNum2 + ", with netIso2: " + netIso2);
                num2 = formattedNum2;
            }
        }
        if (num1.equals(num2)) {
            logd("compareNums, full compare for formatted number returns true.");
            return true;
        }
        int countryCodeLen1 = getIntlPrefixAndCCLen(num1);
        if (countryCodeLen1 > 0) {
            str = num1.substring(0, countryCodeLen1);
            num1 = num1.substring(countryCodeLen1);
            logd("compareNums, num1 after remove prefix: " + num1 + ", num1Prefix: " + str);
        }
        int countryCodeLen2 = getIntlPrefixAndCCLen(num2);
        if (countryCodeLen2 > 0) {
            str2 = num2.substring(0, countryCodeLen2);
            num2 = num2.substring(countryCodeLen2);
            logd("compareNums, num2 after remove prefix: " + num2 + ", num2Prefix: " + str2);
        }
        if (isRoamingCountryNumberByPrefix(str, netIso1) || isRoamingCountryNumberByPrefix(str2, netIso2)) {
            logd("compareNums, num1 or num2 belong to roaming country");
            numMatch = SystemProperties.getInt(HwTelephonyProperties.PROPERTY_GLOBAL_VERSION_NUM_MATCH_ROAMING, 7);
            numMatchShort = SystemProperties.getInt(HwTelephonyProperties.PROPERTY_GLOBAL_VERSION_NUM_MATCH_SHORT_ROAMING, numMatch);
            i = numMatch < 7 ? 7 : numMatch;
            i2 = numMatchShort >= i ? i : numMatchShort;
            logd("compareNums, roaming prop NUM_LONG = " + i + ", NUM_SHORT = " + i2);
        }
        if (isEqualCountryCodePrefix(str, netIso1, str2, netIso2)) {
            int areaCodeLen;
            boolean isNum1CnNumber = isChineseNumberByPrefix(str, netIso1);
            boolean z2 = false;
            if (isNum1CnNumber) {
                i = 11;
                if (num1 != null && num1.startsWith("86")) {
                    num1 = num1.substring(2);
                }
                num1 = deleteIPHead(num1);
                z2 = isChineseMobilePhoneNumber(num1);
                if (!z2) {
                    areaCodeLen = getChineseFixNumberAreaCodeLength(num1);
                    if (areaCodeLen > 0) {
                        str3 = num1.substring(0, areaCodeLen);
                        num1 = num1.substring(areaCodeLen);
                        logd("compareNums, CN num1 after remove area code: " + num1 + ", num1AreaCode: " + str3);
                    }
                }
            } else if ("PE".equalsIgnoreCase(netIso1)) {
                logd("compareNums, PE num1 start with 0 not remove it");
            } else if (num1.length() >= 7 && "0".equals(num1.substring(0, 1)) && ("0".equals(num1.substring(1, 2)) ^ 1) != 0) {
                num1 = num1.substring(1);
                logd("compareNums, num1 remove 0 at beginning");
            }
            boolean isNum2CnNumber = isChineseNumberByPrefix(str2, netIso2);
            int i3 = 0;
            if (isNum2CnNumber) {
                i = 11;
                if (num2 != null && num2.startsWith("86")) {
                    num2 = num2.substring(2);
                }
                num2 = deleteIPHead(num2);
                i3 = isChineseMobilePhoneNumber(num2);
                if (i3 == 0) {
                    areaCodeLen = getChineseFixNumberAreaCodeLength(num2);
                    if (areaCodeLen > 0) {
                        str4 = num2.substring(0, areaCodeLen);
                        num2 = num2.substring(areaCodeLen);
                        logd("compareNums, CN num2 after remove area code: " + num2 + ", num2AreaCode: " + str4);
                    }
                }
            } else if ("PE".equalsIgnoreCase(netIso2)) {
                logd("compareNums, PE num2 start with 0 not remove it");
            } else if (num2.length() >= 7 && "0".equals(num2.substring(0, 1)) && ("0".equals(num2.substring(1, 2)) ^ 1) != 0) {
                num2 = num2.substring(1);
                logd("compareNums, num2 remove 0 at beginning");
            }
            if ((!z2 || (r14 ^ 1) == 0) && (z2 || r14 == 0)) {
                if (z2 && r14 != 0) {
                    logd("compareNums, num1 and num2 are both MPN, continue to compare");
                } else if (isNum1CnNumber && isNum2CnNumber && !isEqualChineseFixNumberAreaCode(r16, r19)) {
                    logd("compareNums, areacode prefix not same, return false");
                    return false;
                }
                return compareNumsInternal(num1, num2, i, i2);
            }
            if (shouldDoNumberMatchAgainBySimMccmnc(origNum1, netIso1) || shouldDoNumberMatchAgainBySimMccmnc(origNum2, netIso2)) {
                z = compareNumsInternal(origNum1, origNum2, this.mSimNumLong, this.mSimNumShort);
            }
            logd("compareNums, num1 and num2 not both MPN, return " + z);
            return z;
        }
        if (shouldDoNumberMatchAgainBySimMccmnc(origNum1, netIso1) || shouldDoNumberMatchAgainBySimMccmnc(origNum2, netIso2)) {
            z = compareNumsInternal(origNum1, origNum2, this.mSimNumLong, this.mSimNumShort);
        }
        logd("compareNums, countrycode prefix not same, return " + z);
        return z;
    }

    public boolean compareNums(String num1, String num2) {
        int NUM_LONG = this.NUM_LONG_CUST;
        int NUM_SHORT = this.NUM_SHORT_CUST;
        if (num1 == null || num2 == null) {
            return false;
        }
        logd("compareNums, num1 = " + num1 + ", num2 = " + num2);
        if (SystemProperties.getInt("ro.config.hwft_MatchNum", 0) == 0) {
            int numMatch = SystemProperties.getInt(HwTelephonyProperties.PROPERTY_GLOBAL_VERSION_NUM_MATCH, 7);
            int numMatchShort = SystemProperties.getInt(HwTelephonyProperties.PROPERTY_GLOBAL_VERSION_NUM_MATCH_SHORT, numMatch);
            NUM_LONG = numMatch < 7 ? 7 : numMatch;
            NUM_SHORT = numMatchShort >= NUM_LONG ? NUM_LONG : numMatchShort;
            logd("compareNums, after setprop NUM_LONG = " + NUM_LONG + ", NUM_SHORT = " + NUM_SHORT);
        }
        if (num1.indexOf(64) < 0) {
            num1 = PhoneNumberUtils.stripSeparators(num1);
        }
        if (num2.indexOf(64) < 0) {
            num2 = PhoneNumberUtils.stripSeparators(num2);
        }
        num1 = formatedForDualNumber(num1);
        num2 = formatedForDualNumber(num2);
        if (this.IS_CHINA_TELECOM && num1.startsWith("**133") && num1.endsWith("#")) {
            num1 = num1.substring(0, num1.length() - 1);
            logd("compareNums, num1 startsWith **133 && endsWith #");
        }
        if (this.IS_CHINA_TELECOM && num2.startsWith("**133") && num2.endsWith("#")) {
            num2 = num2.substring(0, num2.length() - 1);
            logd("compareNums, num2 startsWith **133 && endsWith #");
        }
        if (NUM_SHORT < NUM_LONG) {
            logd("compareNums, NUM_SHORT have been set! Only do full compare.");
            return num1.equals(num2);
        }
        int num1Len = num1.length();
        int num2Len = num2.length();
        if (num1Len > NUM_LONG) {
            num1 = num1.substring(num1Len - NUM_LONG);
        }
        if (num2Len > NUM_LONG) {
            num2 = num2.substring(num2Len - NUM_LONG);
        }
        logd("compareNums, new num1 = " + num1 + ", new num2 = " + num2);
        return num1.equals(num2);
    }

    public int getCallerIndex(Cursor cursor, String compNum) {
        return getCallerIndex(cursor, compNum, "number");
    }

    public int getCallerIndex(Cursor cursor, String compNum, String columnName) {
        return getCallerIndex(cursor, compNum, columnName, SystemProperties.get(HwTelephonyProperties.PROPERTY_NETWORK_COUNTRY_ISO, ""));
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getCallerIndex(android.database.Cursor r41, java.lang.String r42, java.lang.String r43, java.lang.String r44) {
        /*
        r40 = this;
        r18 = 0;
        r16 = 0;
        r33 = 0;
        r39 = 0;
        r37 = 0;
        r6 = 0;
        r31 = 0;
        r17 = 0;
        r38 = 0;
        r14 = 0;
        r34 = 0;
        r35 = 0;
        r30 = -1;
        r27 = -1;
        r21 = -1;
        r0 = r40;
        r10 = r0.NUM_LONG_CUST;
        r0 = r40;
        r11 = r0.NUM_SHORT_CUST;
        r4 = android.text.TextUtils.isEmpty(r42);
        if (r4 == 0) goto L_0x0051;
    L_0x002a:
        if (r41 == 0) goto L_0x0034;
    L_0x002c:
        r4 = r41.getCount();
        if (r4 <= 0) goto L_0x0034;
    L_0x0032:
        r21 = 0;
    L_0x0034:
        r4 = "CallerInfo";
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r7 = "CallerInfoHW(),null == compNum! fixedIndex = ";
        r5 = r5.append(r7);
        r0 = r21;
        r5 = r5.append(r0);
        r5 = r5.toString();
        android.util.Log.e(r4, r5);
        return r21;
    L_0x0051:
        if (r41 == 0) goto L_0x0059;
    L_0x0053:
        r4 = r41.getCount();
        if (r4 > 0) goto L_0x0076;
    L_0x0059:
        r4 = "CallerInfo";
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r7 = "CallerInfoHW(), cursor is empty! fixedIndex = ";
        r5 = r5.append(r7);
        r0 = r21;
        r5 = r5.append(r0);
        r5 = r5.toString();
        android.util.Log.e(r4, r5);
        return r21;
    L_0x0076:
        r21 = r40.getFullMatchIndex(r41, r42, r43);
        r4 = IS_SUPPORT_DUAL_NUMBER;
        if (r4 == 0) goto L_0x0097;
    L_0x007e:
        r4 = -1;
        r0 = r21;
        if (r4 != r0) goto L_0x0097;
    L_0x0083:
        r32 = android.telephony.PhoneNumberUtils.stripSeparators(r42);
        r32 = formatedForDualNumber(r32);
        r0 = r40;
        r1 = r41;
        r2 = r32;
        r3 = r43;
        r21 = r0.getFullMatchIndex(r1, r2, r3);
    L_0x0097:
        r4 = -1;
        r0 = r21;
        if (r4 == r0) goto L_0x009d;
    L_0x009c:
        return r21;
    L_0x009d:
        r4 = "getCallerIndex(), not full match proceed to check..";
        logd(r4);
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "getCallerIndex(), NUM_LONG = ";
        r4 = r4.append(r5);
        r4 = r4.append(r10);
        r5 = ",NUM_SHORT = ";
        r4 = r4.append(r5);
        r4 = r4.append(r11);
        r4 = r4.toString();
        logd(r4);
        r4 = "ro.config.hwft_MatchNum";
        r5 = 0;
        r4 = android.os.SystemProperties.getInt(r4, r5);
        if (r4 != 0) goto L_0x0115;
    L_0x00cf:
        r4 = "gsm.hw.matchnum";
        r5 = 7;
        r28 = android.os.SystemProperties.getInt(r4, r5);
        r4 = "gsm.hw.matchnum.short";
        r0 = r28;
        r29 = android.os.SystemProperties.getInt(r4, r0);
        r4 = 7;
        r0 = r28;
        if (r0 >= r4) goto L_0x04b6;
    L_0x00e5:
        r10 = 7;
    L_0x00e6:
        r0 = r40;
        r0.mSimNumLong = r10;
        r0 = r29;
        if (r0 < r10) goto L_0x04ba;
    L_0x00ee:
        r11 = r10;
    L_0x00ef:
        r0 = r40;
        r0.mSimNumShort = r11;
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "getCallerIndex(), after setprop NUM_LONG = ";
        r4 = r4.append(r5);
        r4 = r4.append(r10);
        r5 = ", NUM_SHORT = ";
        r4 = r4.append(r5);
        r4 = r4.append(r11);
        r4 = r4.toString();
        logd(r4);
    L_0x0115:
        r42 = android.telephony.PhoneNumberUtils.stripSeparators(r42);
        r42 = formatedForDualNumber(r42);
        r15 = r42.length();
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "compNum: ";
        r4 = r4.append(r5);
        r0 = r42;
        r4 = r4.append(r0);
        r5 = ", countryIso: ";
        r4 = r4.append(r5);
        r0 = r44;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
        r0 = r40;
        r4 = r0.IS_CHINA_TELECOM;
        if (r4 == 0) goto L_0x0176;
    L_0x014d:
        r4 = "**133";
        r0 = r42;
        r4 = r0.startsWith(r4);
        if (r4 == 0) goto L_0x0176;
    L_0x0158:
        r4 = "#";
        r0 = r42;
        r4 = r0.endsWith(r4);
        if (r4 == 0) goto L_0x0176;
    L_0x0163:
        r4 = r42.length();
        r4 = r4 + -1;
        r5 = 0;
        r0 = r42;
        r42 = r0.substring(r5, r4);
        r4 = "compNum startsWith **133 && endsWith #";
        logd(r4);
    L_0x0176:
        r6 = r42;
        r4 = "gsm.hw.operator.numeric";
        r5 = "";
        r4 = android.os.SystemProperties.get(r4, r5);
        r0 = r40;
        r0.mNetworkOperator = r4;
        r23 = 0;
        r4 = android.text.TextUtils.isEmpty(r44);
        if (r4 != 0) goto L_0x01c6;
    L_0x018e:
        r4 = java.util.Locale.US;
        r0 = r44;
        r4 = r0.toUpperCase(r4);
        r0 = r42;
        r23 = android.telephony.PhoneNumberUtils.formatNumberToE164(r0, r4);
        if (r23 == 0) goto L_0x01c6;
    L_0x019e:
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "formattedCompNum: ";
        r4 = r4.append(r5);
        r0 = r23;
        r4 = r4.append(r0);
        r5 = ", with countryIso: ";
        r4 = r4.append(r5);
        r0 = r44;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
        r42 = r23;
    L_0x01c6:
        r0 = r40;
        r1 = r42;
        r19 = r0.getIntlPrefixAndCCLen(r1);
        if (r19 <= 0) goto L_0x0216;
    L_0x01d0:
        r4 = 0;
        r0 = r42;
        r1 = r19;
        r17 = r0.substring(r4, r1);
        r0 = r42;
        r1 = r19;
        r42 = r0.substring(r1);
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "compNum after remove prefix: ";
        r4 = r4.append(r5);
        r0 = r42;
        r4 = r4.append(r0);
        r5 = ", compNumLen: ";
        r4 = r4.append(r5);
        r5 = r42.length();
        r4 = r4.append(r5);
        r5 = ", compNumPrefix: ";
        r4 = r4.append(r5);
        r0 = r17;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
    L_0x0216:
        r4 = android.text.TextUtils.isEmpty(r23);
        if (r4 != 0) goto L_0x04be;
    L_0x021c:
        r32 = r23;
    L_0x021e:
        r0 = r40;
        r1 = r17;
        r2 = r44;
        r4 = r0.isRoamingCountryNumberByPrefix(r1, r2);
        if (r4 == 0) goto L_0x026e;
    L_0x022a:
        r4 = "compNum belongs to roaming country";
        logd(r4);
        r4 = "gsm.hw.matchnum.roaming";
        r5 = 7;
        r28 = android.os.SystemProperties.getInt(r4, r5);
        r4 = "gsm.hw.matchnum.short.roaming";
        r0 = r28;
        r29 = android.os.SystemProperties.getInt(r4, r0);
        r4 = 7;
        r0 = r28;
        if (r0 >= r4) goto L_0x04c2;
    L_0x0246:
        r10 = 7;
    L_0x0247:
        r0 = r29;
        if (r0 < r10) goto L_0x04c6;
    L_0x024b:
        r11 = r10;
    L_0x024c:
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "getCallerIndex(), roaming prop NUM_LONG = ";
        r4 = r4.append(r5);
        r4 = r4.append(r10);
        r5 = ", NUM_SHORT = ";
        r4 = r4.append(r5);
        r4 = r4.append(r11);
        r4 = r4.toString();
        logd(r4);
    L_0x026e:
        r0 = r40;
        r1 = r17;
        r2 = r44;
        r24 = r0.isChineseNumberByPrefix(r1, r2);
        r25 = 0;
        if (r24 == 0) goto L_0x04ca;
    L_0x027c:
        r11 = 11;
        r10 = 11;
        if (r42 == 0) goto L_0x0294;
    L_0x0282:
        r4 = "86";
        r0 = r42;
        r4 = r0.startsWith(r4);
        if (r4 == 0) goto L_0x0294;
    L_0x028d:
        r4 = 2;
        r0 = r42;
        r42 = r0.substring(r4);
    L_0x0294:
        r0 = r40;
        r1 = r42;
        r42 = r0.deleteIPHead(r1);
        r0 = r40;
        r1 = r42;
        r25 = r0.isChineseMobilePhoneNumber(r1);
        if (r25 != 0) goto L_0x02f0;
    L_0x02a6:
        r0 = r40;
        r1 = r42;
        r12 = r0.getChineseFixNumberAreaCodeLength(r1);
        if (r12 <= 0) goto L_0x02f0;
    L_0x02b0:
        r4 = 0;
        r0 = r42;
        r14 = r0.substring(r4, r12);
        r0 = r42;
        r42 = r0.substring(r12);
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "CN compNum after remove area code: ";
        r4 = r4.append(r5);
        r0 = r42;
        r4 = r4.append(r0);
        r5 = ", compNumLen: ";
        r4 = r4.append(r5);
        r5 = r42.length();
        r4 = r4.append(r5);
        r5 = ", compNumAreaCode: ";
        r4 = r4.append(r5);
        r4 = r4.append(r14);
        r4 = r4.toString();
        logd(r4);
    L_0x02f0:
        r15 = r42.length();
        if (r15 < r10) goto L_0x0734;
    L_0x02f6:
        r4 = r15 - r10;
        r0 = r42;
        r16 = r0.substring(r4);
        r4 = r15 - r11;
        r0 = r42;
        r18 = r0.substring(r4);
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "11:, compNumLong = ";
        r4 = r4.append(r5);
        r0 = r16;
        r4 = r4.append(r0);
        r5 = ",compNumShort = ";
        r4 = r4.append(r5);
        r0 = r18;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
        r4 = r41.moveToFirst();
        if (r4 == 0) goto L_0x047b;
    L_0x0332:
        r0 = r41;
        r1 = r43;
        r13 = r0.getColumnIndex(r1);
        r4 = "normalized_number";
        r0 = r41;
        r22 = r0.getColumnIndex(r4);
        r4 = "data4";
        r0 = r41;
        r20 = r0.getColumnIndex(r4);
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "11: columnIndex: ";
        r4 = r4.append(r5);
        r4 = r4.append(r13);
        r5 = ", formatColumnIndex: ";
        r4 = r4.append(r5);
        r0 = r22;
        r4 = r4.append(r0);
        r5 = ", data4ColumnIndex: ";
        r4 = r4.append(r5);
        r0 = r20;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
        r4 = -1;
        if (r13 == r4) goto L_0x047b;
    L_0x0380:
        r0 = r41;
        r33 = r0.getString(r13);
        r31 = r33;
        if (r33 == 0) goto L_0x0511;
    L_0x038a:
        r4 = 64;
        r0 = r33;
        r4 = r0.indexOf(r4);
        if (r4 >= 0) goto L_0x0511;
    L_0x0394:
        r31 = android.telephony.PhoneNumberUtils.stripSeparators(r33);
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "origTmpNum: ";
        r4 = r4.append(r5);
        r0 = r31;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
        r4 = -1;
        r0 = r22;
        if (r4 == r0) goto L_0x0517;
    L_0x03b6:
        r0 = r41;
        r1 = r22;
        r35 = r0.getString(r1);
        r0 = r40;
        r1 = r31;
        r2 = r35;
        r4 = r0.isValidData4Number(r1, r2);
        if (r4 == 0) goto L_0x0513;
    L_0x03ca:
        r33 = r35;
    L_0x03cc:
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "11: tmpNumFormat: ";
        r4 = r4.append(r5);
        r0 = r35;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
        r36 = r33.length();
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "11: tmpNum = ";
        r4 = r4.append(r5);
        r0 = r33;
        r4 = r4.append(r0);
        r5 = ", tmpNum.length11: ";
        r4 = r4.append(r5);
        r5 = r33.length();
        r4 = r4.append(r5);
        r5 = ",ID = ";
        r4 = r4.append(r5);
        r5 = r41.getPosition();
        r4 = r4.append(r5);
        r4 = r4.toString();
        logd(r4);
        r0 = r33;
        r1 = r32;
        r4 = r0.equals(r1);
        if (r4 == 0) goto L_0x053c;
    L_0x042a:
        r27 = r41.getPosition();
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "11: > NUM_LONG numLongID = ";
        r4 = r4.append(r5);
        r0 = r27;
        r4 = r4.append(r0);
        r5 = ", formattedNum full match!";
        r4 = r4.append(r5);
        r4 = r4.toString();
        logd(r4);
    L_0x044e:
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "11:  numLongID = ";
        r4 = r4.append(r5);
        r0 = r27;
        r4 = r4.append(r0);
        r5 = ",numShortID = ";
        r4 = r4.append(r5);
        r0 = r30;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
        r4 = -1;
        r0 = r27;
        if (r4 == r0) goto L_0x0727;
    L_0x0479:
        r21 = r27;
    L_0x047b:
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "fixedIndex: ";
        r4 = r4.append(r5);
        r0 = r21;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
        r4 = -1;
        r0 = r21;
        if (r4 != r0) goto L_0x04b5;
    L_0x0499:
        r0 = r40;
        r1 = r44;
        r4 = r0.shouldDoNumberMatchAgainBySimMccmnc(r6, r1);
        if (r4 == 0) goto L_0x04b5;
    L_0x04a3:
        r0 = r40;
        r8 = r0.mSimNumLong;
        r0 = r40;
        r9 = r0.mSimNumShort;
        r4 = r40;
        r5 = r41;
        r7 = r43;
        r21 = r4.getCallerIndexInternal(r5, r6, r7, r8, r9);
    L_0x04b5:
        return r21;
    L_0x04b6:
        r10 = r28;
        goto L_0x00e6;
    L_0x04ba:
        r11 = r29;
        goto L_0x00ef;
    L_0x04be:
        r32 = r6;
        goto L_0x021e;
    L_0x04c2:
        r10 = r28;
        goto L_0x0247;
    L_0x04c6:
        r11 = r29;
        goto L_0x024c;
    L_0x04ca:
        r4 = "PE";
        r0 = r44;
        r4 = r4.equalsIgnoreCase(r0);
        if (r4 == 0) goto L_0x04dd;
    L_0x04d5:
        r4 = "PE compNum start with 0 not remove it";
        logd(r4);
        goto L_0x02f0;
    L_0x04dd:
        r4 = r42.length();
        r5 = 7;
        if (r4 < r5) goto L_0x02f0;
    L_0x04e4:
        r4 = "0";
        r5 = 0;
        r7 = 1;
        r0 = r42;
        r5 = r0.substring(r5, r7);
        r4 = r4.equals(r5);
        if (r4 == 0) goto L_0x02f0;
    L_0x04f5:
        r4 = "0";
        r5 = 1;
        r7 = 2;
        r0 = r42;
        r5 = r0.substring(r5, r7);
        r4 = r4.equals(r5);
        r4 = r4 ^ 1;
        if (r4 == 0) goto L_0x02f0;
    L_0x0508:
        r4 = 1;
        r0 = r42;
        r42 = r0.substring(r4);
        goto L_0x02f0;
    L_0x0511:
        r4 = -1;
        return r4;
    L_0x0513:
        r33 = r31;
        goto L_0x03cc;
    L_0x0517:
        r4 = -1;
        r0 = r20;
        if (r4 == r0) goto L_0x0538;
    L_0x051c:
        r0 = r41;
        r1 = r20;
        r35 = r0.getString(r1);
        r0 = r40;
        r1 = r31;
        r2 = r35;
        r4 = r0.isValidData4Number(r1, r2);
        if (r4 == 0) goto L_0x0534;
    L_0x0530:
        r33 = r35;
        goto L_0x03cc;
    L_0x0534:
        r33 = r31;
        goto L_0x03cc;
    L_0x0538:
        r33 = r31;
        goto L_0x03cc;
    L_0x053c:
        r38 = 0;
        r34 = 0;
        r0 = r40;
        r1 = r33;
        r19 = r0.getIntlPrefixAndCCLen(r1);
        if (r19 <= 0) goto L_0x0590;
    L_0x054a:
        r4 = 0;
        r0 = r33;
        r1 = r19;
        r38 = r0.substring(r4, r1);
        r0 = r33;
        r1 = r19;
        r33 = r0.substring(r1);
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "11: tmpNum after remove prefix: ";
        r4 = r4.append(r5);
        r0 = r33;
        r4 = r4.append(r0);
        r5 = ", tmpNum.length11: ";
        r4 = r4.append(r5);
        r5 = r33.length();
        r4 = r4.append(r5);
        r5 = ", tmpNumPrefix: ";
        r4 = r4.append(r5);
        r0 = r38;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
    L_0x0590:
        r4 = 0;
        r0 = r40;
        r1 = r17;
        r2 = r44;
        r3 = r38;
        r4 = r0.isEqualCountryCodePrefix(r1, r2, r3, r4);
        if (r4 == 0) goto L_0x06a8;
    L_0x059f:
        if (r24 == 0) goto L_0x066e;
    L_0x05a1:
        r0 = r40;
        r1 = r33;
        r33 = r0.deleteIPHead(r1);
        r0 = r40;
        r1 = r33;
        r26 = r0.isChineseMobilePhoneNumber(r1);
        if (r25 == 0) goto L_0x05b7;
    L_0x05b3:
        r4 = r26 ^ 1;
        if (r4 != 0) goto L_0x05bb;
    L_0x05b7:
        if (r25 != 0) goto L_0x05c9;
    L_0x05b9:
        if (r26 == 0) goto L_0x05c9;
    L_0x05bb:
        r4 = "11: compNum and tmpNum not both MPN, continue";
        logd(r4);
    L_0x05c1:
        r4 = r41.moveToNext();
        if (r4 == 0) goto L_0x044e;
    L_0x05c7:
        goto L_0x0380;
    L_0x05c9:
        if (r25 == 0) goto L_0x0610;
    L_0x05cb:
        if (r26 == 0) goto L_0x0610;
    L_0x05cd:
        r4 = "11: compNum and tmpNum are both MPN, continue to match by mccmnc";
        logd(r4);
    L_0x05d3:
        r36 = r33.length();
        r0 = r36;
        if (r0 < r10) goto L_0x06d8;
    L_0x05db:
        r4 = r36 - r10;
        r0 = r33;
        r37 = r0.substring(r4);
        r4 = -1;
        r0 = r27;
        if (r4 != r0) goto L_0x06b0;
    L_0x05e8:
        r0 = r16;
        r1 = r37;
        r4 = r0.compareTo(r1);
        if (r4 != 0) goto L_0x06b0;
    L_0x05f2:
        r27 = r41.getPosition();
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "11: > NUM_LONG numLongID = ";
        r4 = r4.append(r5);
        r0 = r27;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
        goto L_0x05c1;
    L_0x0610:
        r0 = r40;
        r1 = r33;
        r12 = r0.getChineseFixNumberAreaCodeLength(r1);
        if (r12 <= 0) goto L_0x065c;
    L_0x061a:
        r4 = 0;
        r0 = r33;
        r34 = r0.substring(r4, r12);
        r0 = r33;
        r33 = r0.substring(r12);
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "11: CN tmpNum after remove area code: ";
        r4 = r4.append(r5);
        r0 = r33;
        r4 = r4.append(r0);
        r5 = ", tmpNum.length11: ";
        r4 = r4.append(r5);
        r5 = r33.length();
        r4 = r4.append(r5);
        r5 = ", tmpNumAreaCode: ";
        r4 = r4.append(r5);
        r0 = r34;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
    L_0x065c:
        r0 = r40;
        r1 = r34;
        r4 = r0.isEqualChineseFixNumberAreaCode(r14, r1);
        if (r4 != 0) goto L_0x05d3;
    L_0x0666:
        r4 = "11: areacode prefix not same, continue";
        logd(r4);
        goto L_0x05c1;
    L_0x066e:
        r4 = r33.length();
        r5 = 7;
        if (r4 < r5) goto L_0x05d3;
    L_0x0675:
        r4 = "0";
        r5 = 0;
        r7 = 1;
        r0 = r33;
        r5 = r0.substring(r5, r7);
        r4 = r4.equals(r5);
        if (r4 == 0) goto L_0x05d3;
    L_0x0686:
        r4 = "0";
        r5 = 1;
        r7 = 2;
        r0 = r33;
        r5 = r0.substring(r5, r7);
        r4 = r4.equals(r5);
        r4 = r4 ^ 1;
        if (r4 == 0) goto L_0x05d3;
    L_0x0699:
        r4 = 1;
        r0 = r33;
        r33 = r0.substring(r4);
        r4 = "11: tmpNum remove 0 at beginning";
        logd(r4);
        goto L_0x05d3;
    L_0x06a8:
        r4 = "11: countrycode prefix not same, continue";
        logd(r4);
        goto L_0x05c1;
    L_0x06b0:
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "11: >=NUM_LONG, and !=,  tmpNumLong = ";
        r4 = r4.append(r5);
        r0 = r37;
        r4 = r4.append(r0);
        r5 = ", numLongID:";
        r4 = r4.append(r5);
        r0 = r27;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
        goto L_0x05c1;
    L_0x06d8:
        r0 = r36;
        if (r0 < r11) goto L_0x071f;
    L_0x06dc:
        r4 = r36 - r11;
        r0 = r33;
        r39 = r0.substring(r4);
        r4 = -1;
        r0 = r30;
        if (r4 != r0) goto L_0x06f7;
    L_0x06e9:
        r0 = r18;
        r1 = r39;
        r4 = r0.compareTo(r1);
        if (r4 != 0) goto L_0x06f7;
    L_0x06f3:
        r30 = r41.getPosition();
    L_0x06f7:
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "11: >=NUM_SHORT, tmpNumShort = ";
        r4 = r4.append(r5);
        r0 = r39;
        r4 = r4.append(r0);
        r5 = ", numShortID:";
        r4 = r4.append(r5);
        r0 = r30;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
        goto L_0x05c1;
    L_0x071f:
        r4 = "tmpNum11, continue";
        logd(r4);
        goto L_0x05c1;
    L_0x0727:
        r4 = -1;
        r0 = r30;
        if (r4 == r0) goto L_0x0730;
    L_0x072c:
        r21 = r30;
        goto L_0x047b;
    L_0x0730:
        r21 = -1;
        goto L_0x047b;
    L_0x0734:
        if (r15 < r11) goto L_0x0acb;
    L_0x0736:
        r4 = r15 - r11;
        r0 = r42;
        r18 = r0.substring(r4);
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "7:  compNumShort = ";
        r4 = r4.append(r5);
        r0 = r18;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
        r4 = r41.moveToFirst();
        if (r4 == 0) goto L_0x047b;
    L_0x075d:
        r0 = r41;
        r1 = r43;
        r13 = r0.getColumnIndex(r1);
        r4 = "normalized_number";
        r0 = r41;
        r22 = r0.getColumnIndex(r4);
        r4 = "data4";
        r0 = r41;
        r20 = r0.getColumnIndex(r4);
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "7: columnIndex: ";
        r4 = r4.append(r5);
        r4 = r4.append(r13);
        r5 = ", formatColumnIndex: ";
        r4 = r4.append(r5);
        r0 = r22;
        r4 = r4.append(r0);
        r5 = ", data4ColumnIndex: ";
        r4 = r4.append(r5);
        r0 = r20;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
        r4 = -1;
        if (r13 == r4) goto L_0x047b;
    L_0x07ab:
        r0 = r41;
        r33 = r0.getString(r13);
        r31 = r33;
        if (r33 == 0) goto L_0x08a8;
    L_0x07b5:
        r4 = 64;
        r0 = r33;
        r4 = r0.indexOf(r4);
        if (r4 >= 0) goto L_0x08a8;
    L_0x07bf:
        r31 = android.telephony.PhoneNumberUtils.stripSeparators(r33);
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "origTmpNum: ";
        r4 = r4.append(r5);
        r0 = r31;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
        r4 = -1;
        r0 = r22;
        if (r4 == r0) goto L_0x08ae;
    L_0x07e1:
        r0 = r41;
        r1 = r22;
        r35 = r0.getString(r1);
        r0 = r40;
        r1 = r31;
        r2 = r35;
        r4 = r0.isValidData4Number(r1, r2);
        if (r4 == 0) goto L_0x08aa;
    L_0x07f5:
        r33 = r35;
    L_0x07f7:
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "7: tmpNumFormat: ";
        r4 = r4.append(r5);
        r0 = r35;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
        r36 = r33.length();
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "7: tmpNum = ";
        r4 = r4.append(r5);
        r0 = r33;
        r4 = r4.append(r0);
        r5 = ", tmpNum.length7: ";
        r4 = r4.append(r5);
        r5 = r33.length();
        r4 = r4.append(r5);
        r5 = ",ID = ";
        r4 = r4.append(r5);
        r5 = r41.getPosition();
        r4 = r4.append(r5);
        r4 = r4.toString();
        logd(r4);
        r0 = r33;
        r1 = r32;
        r4 = r0.equals(r1);
        if (r4 == 0) goto L_0x08d3;
    L_0x0855:
        r30 = r41.getPosition();
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "7: >= NUM_SHORT numShortID = ";
        r4 = r4.append(r5);
        r0 = r30;
        r4 = r4.append(r0);
        r5 = ", formattedNum full match!";
        r4 = r4.append(r5);
        r4 = r4.toString();
        logd(r4);
    L_0x0879:
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "7: numShortID = ";
        r4 = r4.append(r5);
        r0 = r30;
        r4 = r4.append(r0);
        r5 = ",numLongID = ";
        r4 = r4.append(r5);
        r0 = r27;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
        r4 = -1;
        r0 = r30;
        if (r4 == r0) goto L_0x0abe;
    L_0x08a4:
        r21 = r30;
        goto L_0x047b;
    L_0x08a8:
        r4 = -1;
        return r4;
    L_0x08aa:
        r33 = r31;
        goto L_0x07f7;
    L_0x08ae:
        r4 = -1;
        r0 = r20;
        if (r4 == r0) goto L_0x08cf;
    L_0x08b3:
        r0 = r41;
        r1 = r20;
        r35 = r0.getString(r1);
        r0 = r40;
        r1 = r31;
        r2 = r35;
        r4 = r0.isValidData4Number(r1, r2);
        if (r4 == 0) goto L_0x08cb;
    L_0x08c7:
        r33 = r35;
        goto L_0x07f7;
    L_0x08cb:
        r33 = r31;
        goto L_0x07f7;
    L_0x08cf:
        r33 = r31;
        goto L_0x07f7;
    L_0x08d3:
        r38 = 0;
        r34 = 0;
        r0 = r40;
        r1 = r33;
        r19 = r0.getIntlPrefixAndCCLen(r1);
        if (r19 <= 0) goto L_0x0927;
    L_0x08e1:
        r4 = 0;
        r0 = r33;
        r1 = r19;
        r38 = r0.substring(r4, r1);
        r0 = r33;
        r1 = r19;
        r33 = r0.substring(r1);
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "7: tmpNum after remove prefix: ";
        r4 = r4.append(r5);
        r0 = r33;
        r4 = r4.append(r0);
        r5 = ", tmpNum.length7: ";
        r4 = r4.append(r5);
        r5 = r33.length();
        r4 = r4.append(r5);
        r5 = ", tmpNumPrefix: ";
        r4 = r4.append(r5);
        r0 = r38;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
    L_0x0927:
        r4 = 0;
        r0 = r40;
        r1 = r17;
        r2 = r44;
        r3 = r38;
        r4 = r0.isEqualCountryCodePrefix(r1, r2, r3, r4);
        if (r4 == 0) goto L_0x0a4c;
    L_0x0936:
        if (r24 == 0) goto L_0x0a12;
    L_0x0938:
        r0 = r40;
        r1 = r33;
        r33 = r0.deleteIPHead(r1);
        r0 = r40;
        r1 = r33;
        r26 = r0.isChineseMobilePhoneNumber(r1);
        if (r25 == 0) goto L_0x094e;
    L_0x094a:
        r4 = r26 ^ 1;
        if (r4 != 0) goto L_0x0952;
    L_0x094e:
        if (r25 != 0) goto L_0x0960;
    L_0x0950:
        if (r26 == 0) goto L_0x0960;
    L_0x0952:
        r4 = "7: compNum and tmpNum not both MPN, continue";
        logd(r4);
    L_0x0958:
        r4 = r41.moveToNext();
        if (r4 == 0) goto L_0x0879;
    L_0x095e:
        goto L_0x07ab;
    L_0x0960:
        if (r25 == 0) goto L_0x09b4;
    L_0x0962:
        if (r26 == 0) goto L_0x09b4;
    L_0x0964:
        r4 = "7: compNum and tmpNum are both MPN, continue to match by mccmnc";
        logd(r4);
    L_0x096a:
        r36 = r33.length();
        r0 = r36;
        if (r0 < r10) goto L_0x0a54;
    L_0x0972:
        r4 = r36 - r11;
        r0 = r33;
        r39 = r0.substring(r4);
        r4 = -1;
        r0 = r27;
        if (r4 != r0) goto L_0x098d;
    L_0x097f:
        r0 = r18;
        r1 = r39;
        r4 = r0.compareTo(r1);
        if (r4 != 0) goto L_0x098d;
    L_0x0989:
        r27 = r41.getPosition();
    L_0x098d:
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "7: >=NUM_LONG, tmpNumShort = ";
        r4 = r4.append(r5);
        r0 = r39;
        r4 = r4.append(r0);
        r5 = ", numLongID:";
        r4 = r4.append(r5);
        r0 = r27;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
        goto L_0x0958;
    L_0x09b4:
        r0 = r40;
        r1 = r33;
        r12 = r0.getChineseFixNumberAreaCodeLength(r1);
        if (r12 <= 0) goto L_0x0a00;
    L_0x09be:
        r4 = 0;
        r0 = r33;
        r34 = r0.substring(r4, r12);
        r0 = r33;
        r33 = r0.substring(r12);
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "7: CN tmpNum after remove area code: ";
        r4 = r4.append(r5);
        r0 = r33;
        r4 = r4.append(r0);
        r5 = ", tmpNum.length7: ";
        r4 = r4.append(r5);
        r5 = r33.length();
        r4 = r4.append(r5);
        r5 = ", tmpNumAreaCode: ";
        r4 = r4.append(r5);
        r0 = r34;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
    L_0x0a00:
        r0 = r40;
        r1 = r34;
        r4 = r0.isEqualChineseFixNumberAreaCode(r14, r1);
        if (r4 != 0) goto L_0x096a;
    L_0x0a0a:
        r4 = "7: areacode prefix not same, continue";
        logd(r4);
        goto L_0x0958;
    L_0x0a12:
        r4 = r33.length();
        r5 = 7;
        if (r4 < r5) goto L_0x096a;
    L_0x0a19:
        r4 = "0";
        r5 = 0;
        r7 = 1;
        r0 = r33;
        r5 = r0.substring(r5, r7);
        r4 = r4.equals(r5);
        if (r4 == 0) goto L_0x096a;
    L_0x0a2a:
        r4 = "0";
        r5 = 1;
        r7 = 2;
        r0 = r33;
        r5 = r0.substring(r5, r7);
        r4 = r4.equals(r5);
        r4 = r4 ^ 1;
        if (r4 == 0) goto L_0x096a;
    L_0x0a3d:
        r4 = 1;
        r0 = r33;
        r33 = r0.substring(r4);
        r4 = "7: tmpNum remove 0 at beginning";
        logd(r4);
        goto L_0x096a;
    L_0x0a4c:
        r4 = "7: countrycode prefix not same, continue";
        logd(r4);
        goto L_0x0958;
    L_0x0a54:
        r0 = r36;
        if (r0 < r11) goto L_0x0ab6;
    L_0x0a58:
        r4 = r36 - r11;
        r0 = r33;
        r39 = r0.substring(r4);
        r4 = -1;
        r0 = r30;
        if (r4 != r0) goto L_0x0a8e;
    L_0x0a65:
        r0 = r18;
        r1 = r39;
        r4 = r0.compareTo(r1);
        if (r4 != 0) goto L_0x0a8e;
    L_0x0a6f:
        r30 = r41.getPosition();
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "7: >= NUM_SHORT numShortID = ";
        r4 = r4.append(r5);
        r0 = r30;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
        goto L_0x0958;
    L_0x0a8e:
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "7: >=NUM_SHORT, and !=, tmpNumShort = ";
        r4 = r4.append(r5);
        r0 = r39;
        r4 = r4.append(r0);
        r5 = ", numShortID:";
        r4 = r4.append(r5);
        r0 = r30;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
        goto L_0x0958;
    L_0x0ab6:
        r4 = "7: continue";
        logd(r4);
        goto L_0x0958;
    L_0x0abe:
        r4 = -1;
        r0 = r27;
        if (r4 == r0) goto L_0x0ac7;
    L_0x0ac3:
        r21 = r27;
        goto L_0x047b;
    L_0x0ac7:
        r21 = -1;
        goto L_0x047b;
    L_0x0acb:
        r4 = r41.moveToFirst();
        if (r4 == 0) goto L_0x047b;
    L_0x0ad1:
        r0 = r41;
        r1 = r43;
        r13 = r0.getColumnIndex(r1);
        r4 = "normalized_number";
        r0 = r41;
        r22 = r0.getColumnIndex(r4);
        r4 = "data4";
        r0 = r41;
        r20 = r0.getColumnIndex(r4);
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "5: columnIndex: ";
        r4 = r4.append(r5);
        r4 = r4.append(r13);
        r5 = ", formatColumnIndex: ";
        r4 = r4.append(r5);
        r0 = r22;
        r4 = r4.append(r0);
        r5 = ", data4ColumnIndex: ";
        r4 = r4.append(r5);
        r0 = r20;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
        r4 = -1;
        if (r13 == r4) goto L_0x047b;
    L_0x0b1f:
        r0 = r41;
        r33 = r0.getString(r13);
        r31 = r33;
        if (r33 == 0) goto L_0x0c08;
    L_0x0b29:
        r4 = 64;
        r0 = r33;
        r4 = r0.indexOf(r4);
        if (r4 >= 0) goto L_0x0c08;
    L_0x0b33:
        r31 = android.telephony.PhoneNumberUtils.stripSeparators(r33);
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "origTmpNum: ";
        r4 = r4.append(r5);
        r0 = r31;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
        r4 = -1;
        r0 = r22;
        if (r4 == r0) goto L_0x0c0e;
    L_0x0b55:
        r0 = r41;
        r1 = r22;
        r35 = r0.getString(r1);
        r0 = r40;
        r1 = r31;
        r2 = r35;
        r4 = r0.isValidData4Number(r1, r2);
        if (r4 == 0) goto L_0x0c0a;
    L_0x0b69:
        r33 = r35;
    L_0x0b6b:
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "5: tmpNumFormat: ";
        r4 = r4.append(r5);
        r0 = r35;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
        r36 = r33.length();
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "5: tmpNum = ";
        r4 = r4.append(r5);
        r0 = r33;
        r4 = r4.append(r0);
        r5 = ", tmpNum.length: ";
        r4 = r4.append(r5);
        r5 = r33.length();
        r4 = r4.append(r5);
        r5 = ",ID = ";
        r4 = r4.append(r5);
        r5 = r41.getPosition();
        r4 = r4.append(r5);
        r4 = r4.toString();
        logd(r4);
        r0 = r33;
        r1 = r32;
        r4 = r0.equals(r1);
        if (r4 == 0) goto L_0x0c33;
    L_0x0bc9:
        r21 = r41.getPosition();
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "5: break! numLongID = ";
        r4 = r4.append(r5);
        r0 = r21;
        r4 = r4.append(r0);
        r5 = ", formattedNum full match!";
        r4 = r4.append(r5);
        r4 = r4.toString();
        logd(r4);
    L_0x0bed:
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "5: fixedIndex = ";
        r4 = r4.append(r5);
        r0 = r21;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
        goto L_0x047b;
    L_0x0c08:
        r4 = -1;
        return r4;
    L_0x0c0a:
        r33 = r31;
        goto L_0x0b6b;
    L_0x0c0e:
        r4 = -1;
        r0 = r20;
        if (r4 == r0) goto L_0x0c2f;
    L_0x0c13:
        r0 = r41;
        r1 = r20;
        r35 = r0.getString(r1);
        r0 = r40;
        r1 = r31;
        r2 = r35;
        r4 = r0.isValidData4Number(r1, r2);
        if (r4 == 0) goto L_0x0c2b;
    L_0x0c27:
        r33 = r35;
        goto L_0x0b6b;
    L_0x0c2b:
        r33 = r31;
        goto L_0x0b6b;
    L_0x0c2f:
        r33 = r31;
        goto L_0x0b6b;
    L_0x0c33:
        r38 = 0;
        r34 = 0;
        r0 = r40;
        r1 = r33;
        r19 = r0.getIntlPrefixAndCCLen(r1);
        if (r19 <= 0) goto L_0x0c87;
    L_0x0c41:
        r4 = 0;
        r0 = r33;
        r1 = r19;
        r38 = r0.substring(r4, r1);
        r0 = r33;
        r1 = r19;
        r33 = r0.substring(r1);
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "5: tmpNum after remove prefix: ";
        r4 = r4.append(r5);
        r0 = r33;
        r4 = r4.append(r0);
        r5 = ", tmpNum.length5: ";
        r4 = r4.append(r5);
        r5 = r33.length();
        r4 = r4.append(r5);
        r5 = ", tmpNumPrefix: ";
        r4 = r4.append(r5);
        r0 = r38;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
    L_0x0c87:
        r4 = 0;
        r0 = r40;
        r1 = r17;
        r2 = r44;
        r3 = r38;
        r4 = r0.isEqualCountryCodePrefix(r1, r2, r3, r4);
        if (r4 == 0) goto L_0x0d97;
    L_0x0c96:
        if (r24 == 0) goto L_0x0d5d;
    L_0x0c98:
        r0 = r40;
        r1 = r33;
        r33 = r0.deleteIPHead(r1);
        r0 = r40;
        r1 = r33;
        r26 = r0.isChineseMobilePhoneNumber(r1);
        if (r25 == 0) goto L_0x0cae;
    L_0x0caa:
        r4 = r26 ^ 1;
        if (r4 != 0) goto L_0x0cb2;
    L_0x0cae:
        if (r25 != 0) goto L_0x0cc0;
    L_0x0cb0:
        if (r26 == 0) goto L_0x0cc0;
    L_0x0cb2:
        r4 = "5: compNum and tmpNum not both MPN, continue";
        logd(r4);
    L_0x0cb8:
        r4 = r41.moveToNext();
        if (r4 == 0) goto L_0x0bed;
    L_0x0cbe:
        goto L_0x0b1f;
    L_0x0cc0:
        if (r25 == 0) goto L_0x0cff;
    L_0x0cc2:
        if (r26 == 0) goto L_0x0cff;
    L_0x0cc4:
        r4 = "5: compNum and tmpNum are both MPN, continue to match by mccmnc";
        logd(r4);
    L_0x0cca:
        r36 = r33.length();
        r0 = r36;
        if (r0 != r15) goto L_0x0d9f;
    L_0x0cd2:
        r4 = -1;
        r0 = r21;
        if (r4 != r0) goto L_0x0cb8;
    L_0x0cd7:
        r0 = r42;
        r1 = r33;
        r4 = r0.compareTo(r1);
        if (r4 != 0) goto L_0x0cb8;
    L_0x0ce1:
        r21 = r41.getPosition();
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "5: break! numLongID = ";
        r4 = r4.append(r5);
        r0 = r21;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
        goto L_0x0cb8;
    L_0x0cff:
        r0 = r40;
        r1 = r33;
        r12 = r0.getChineseFixNumberAreaCodeLength(r1);
        if (r12 <= 0) goto L_0x0d4b;
    L_0x0d09:
        r4 = 0;
        r0 = r33;
        r34 = r0.substring(r4, r12);
        r0 = r33;
        r33 = r0.substring(r12);
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "5: CN tmpNum after remove area code: ";
        r4 = r4.append(r5);
        r0 = r33;
        r4 = r4.append(r0);
        r5 = ", tmpNum.length5: ";
        r4 = r4.append(r5);
        r5 = r33.length();
        r4 = r4.append(r5);
        r5 = ", tmpNumAreaCode: ";
        r4 = r4.append(r5);
        r0 = r34;
        r4 = r4.append(r0);
        r4 = r4.toString();
        logd(r4);
    L_0x0d4b:
        r0 = r40;
        r1 = r34;
        r4 = r0.isEqualChineseFixNumberAreaCode(r14, r1);
        if (r4 != 0) goto L_0x0cca;
    L_0x0d55:
        r4 = "5: areacode prefix not same, continue";
        logd(r4);
        goto L_0x0cb8;
    L_0x0d5d:
        r4 = r33.length();
        r5 = 7;
        if (r4 < r5) goto L_0x0cca;
    L_0x0d64:
        r4 = "0";
        r5 = 0;
        r7 = 1;
        r0 = r33;
        r5 = r0.substring(r5, r7);
        r4 = r4.equals(r5);
        if (r4 == 0) goto L_0x0cca;
    L_0x0d75:
        r4 = "0";
        r5 = 1;
        r7 = 2;
        r0 = r33;
        r5 = r0.substring(r5, r7);
        r4 = r4.equals(r5);
        r4 = r4 ^ 1;
        if (r4 == 0) goto L_0x0cca;
    L_0x0d88:
        r4 = 1;
        r0 = r33;
        r33 = r0.substring(r4);
        r4 = "5: tmpNum remove 0 at beginning";
        logd(r4);
        goto L_0x0cca;
    L_0x0d97:
        r4 = "5: countrycode prefix not same, continue";
        logd(r4);
        goto L_0x0cb8;
    L_0x0d9f:
        r4 = "5: continue";
        logd(r4);
        goto L_0x0cb8;
        */
        throw new UnsupportedOperationException("Method not decompiled: huawei.android.telephony.CallerInfoHW.getCallerIndex(android.database.Cursor, java.lang.String, java.lang.String, java.lang.String):int");
    }

    public static boolean isfixedIndexValid(String cookie, Cursor cursor) {
        int fixedIndex = new CallerInfoHW().getCallerIndex(cursor, cookie, "number");
        return fixedIndex != -1 ? cursor.moveToPosition(fixedIndex) : false;
    }

    private static void logd(String msg) {
    }

    private int getIntlPrefixLength(String number) {
        if (TextUtils.isEmpty(number) || isNormalPrefix(number)) {
            return 0;
        }
        int len = INTERNATIONAL_PREFIX.length;
        for (int i = 0; i < len; i++) {
            if (number.startsWith(INTERNATIONAL_PREFIX[i])) {
                return INTERNATIONAL_PREFIX[i].length();
            }
        }
        return 0;
    }

    public int getIntlPrefixAndCCLen(String number) {
        if (TextUtils.isEmpty(number)) {
            return 0;
        }
        int len = getIntlPrefixLength(number);
        if (len > 0) {
            String tmpNumber = number.substring(len);
            for (Integer intValue : this.countryCallingCodeToRegionCodeMap.keySet()) {
                int countrycode = intValue.intValue();
                if (tmpNumber.startsWith(Integer.toString(countrycode))) {
                    logd("extractCountryCodeFromNumber(), find matched country code: " + countrycode);
                    return len + Integer.toString(countrycode).length();
                }
            }
            logd("extractCountryCodeFromNumber(), no matched country code");
            len = 0;
        } else {
            logd("extractCountryCodeFromNumber(), no valid prefix in number: " + number);
        }
        return len;
    }

    private boolean isChineseMobilePhoneNumber(String number) {
        if (TextUtils.isEmpty(number) || number.length() < 11 || !number.substring(number.length() - 11).matches(CN_MPN_PATTERN)) {
            return false;
        }
        logd("isChineseMobilePhoneNumber(), return true for number: " + number);
        return true;
    }

    private int getChineseFixNumberAreaCodeLength(String number) {
        int len = 0;
        String tmpNumber = number;
        if (TextUtils.isEmpty(number) || number.length() < 9) {
            return 0;
        }
        if (!number.startsWith("0")) {
            tmpNumber = "0" + number;
        }
        String top2String = tmpNumber.substring(0, 2);
        String areaCodeString;
        ArrayList<String> areaCodeArray;
        int areaCodeArraySize;
        int i;
        if (top2String.equals(FIXED_NUMBER_TOP2_TOKEN1) || top2String.equals(FIXED_NUMBER_TOP2_TOKEN2)) {
            areaCodeString = tmpNumber.substring(0, 3);
            areaCodeArray = (ArrayList) this.chineseFixNumberAreaCodeMap.get(Integer.valueOf(1));
            areaCodeArraySize = areaCodeArray.size();
            i = 0;
            while (i < areaCodeArraySize) {
                if (areaCodeString.equals(areaCodeArray.get(i))) {
                    len = tmpNumber.equals(number) ? 3 : 2;
                    logd("getChineseFixNumberAreaCodeLength(), matched area code len: " + len + ", number: " + number);
                } else {
                    i++;
                }
            }
        } else {
            areaCodeString = tmpNumber.substring(0, 4);
            areaCodeArray = (ArrayList) this.chineseFixNumberAreaCodeMap.get(Integer.valueOf(2));
            areaCodeArraySize = areaCodeArray.size();
            i = 0;
            while (i < areaCodeArraySize) {
                if (areaCodeString.equals(areaCodeArray.get(i))) {
                    len = tmpNumber.equals(number) ? 4 : 3;
                    logd("getChineseFixNumberAreaCodeLength(), matched area code len: " + len + ", number: " + number);
                } else {
                    i++;
                }
            }
        }
        return len;
    }

    private boolean isEqualChineseFixNumberAreaCode(String compNumAreaCode, String dbNumAreaCode) {
        if (TextUtils.isEmpty(compNumAreaCode) && TextUtils.isEmpty(dbNumAreaCode)) {
            return true;
        }
        if (TextUtils.isEmpty(compNumAreaCode) && (TextUtils.isEmpty(dbNumAreaCode) ^ 1) != 0) {
            return !this.IS_MIIT_NUM_MATCH;
        } else {
            if (!TextUtils.isEmpty(compNumAreaCode) && TextUtils.isEmpty(dbNumAreaCode)) {
                return !this.IS_MIIT_NUM_MATCH;
            } else {
                if (!compNumAreaCode.startsWith("0")) {
                    compNumAreaCode = "0" + compNumAreaCode;
                }
                if (!dbNumAreaCode.startsWith("0")) {
                    dbNumAreaCode = "0" + dbNumAreaCode;
                }
                return compNumAreaCode.equals(dbNumAreaCode);
            }
        }
    }

    private String deleteIPHead(String number) {
        if (TextUtils.isEmpty(number)) {
            return number;
        }
        int numberLen = number.length();
        if (numberLen < 5) {
            logd("deleteIPHead() numberLen is short than 5!");
            return number;
        }
        if (Arrays.binarySearch(IPHEAD, number.substring(0, 5)) >= 0) {
            number = number.substring(5, numberLen);
        }
        logd("deleteIPHead() new Number: " + number);
        return number;
    }

    private boolean isChineseNumberByPrefix(String numberPrefix, String netIso) {
        if (TextUtils.isEmpty(numberPrefix)) {
            logd("isChineseNumberByPrefix(), networkCountryIso: " + netIso);
            if (netIso == null || !"CN".equals(netIso.toUpperCase())) {
                return false;
            }
            return true;
        }
        return Integer.toString(this.countryCodeforCN).equals(numberPrefix.substring(getIntlPrefixLength(numberPrefix)));
    }

    private boolean isEqualCountryCodePrefix(String num1Prefix, String netIso1, String num2Prefix, String netIso2) {
        if (TextUtils.isEmpty(num1Prefix) ? TextUtils.isEmpty(num2Prefix) : false) {
            logd("isEqualCountryCodePrefix(), both have no country code, return true");
            return true;
        }
        boolean ret;
        String netIso;
        int countryCode;
        if (TextUtils.isEmpty(num1Prefix) && (TextUtils.isEmpty(num2Prefix) ^ 1) != 0) {
            logd("isEqualCountryCodePrefix(), netIso1: " + netIso1 + ", netIso2: " + netIso2);
            if (TextUtils.isEmpty(netIso1)) {
                ret = true;
            } else {
                netIso = netIso1.toUpperCase();
                if ("CN".equals(netIso)) {
                    countryCode = this.countryCodeforCN;
                } else {
                    countryCode = sInstance.getCountryCodeForRegion(netIso);
                }
                ret = num2Prefix.substring(getIntlPrefixLength(num2Prefix)).equals(Integer.toString(countryCode));
            }
        } else if (TextUtils.isEmpty(num1Prefix) || !TextUtils.isEmpty(num2Prefix)) {
            ret = num1Prefix.substring(getIntlPrefixLength(num1Prefix)).equals(num2Prefix.substring(getIntlPrefixLength(num2Prefix)));
        } else {
            logd("isEqualCountryCodePrefix(), netIso1: " + netIso1 + ", netIso2: " + netIso2);
            if (TextUtils.isEmpty(netIso2)) {
                ret = true;
            } else {
                netIso = netIso2.toUpperCase();
                if ("CN".equals(netIso)) {
                    countryCode = this.countryCodeforCN;
                } else {
                    countryCode = sInstance.getCountryCodeForRegion(netIso);
                }
                ret = num1Prefix.substring(getIntlPrefixLength(num1Prefix)).equals(Integer.toString(countryCode));
            }
        }
        return ret;
    }

    private int getFullMatchIndex(Cursor cursor, String compNum, String columnName) {
        compNum = PhoneNumberUtils.stripSeparators(compNum);
        if (this.IS_CHINA_TELECOM && compNum.startsWith("**133") && compNum.endsWith("#")) {
            compNum = compNum.substring(0, compNum.length() - 1);
            logd("full match check, compNum startsWith **133 && endsWith #");
        }
        logd("full match check, compNum: " + compNum);
        if (cursor == null || !cursor.moveToFirst()) {
            return -1;
        }
        int columnIndex = cursor.getColumnIndex(columnName);
        if (-1 == columnIndex) {
            return -1;
        }
        while (true) {
            String tmpNum = cursor.getString(columnIndex);
            if (tmpNum != null && tmpNum.indexOf(64) < 0) {
                tmpNum = PhoneNumberUtils.stripSeparators(tmpNum);
            }
            logd("full match check, tmpNum: " + tmpNum);
            if (compNum.equals(tmpNum)) {
                int fixedIndex = cursor.getPosition();
                logd("exact match: break! fixedIndex = " + fixedIndex);
                return fixedIndex;
            } else if (!cursor.moveToNext()) {
                return -1;
            }
        }
    }

    private boolean shouldDoNumberMatchAgainBySimMccmnc(String number, String countryIso) {
        if (SystemProperties.getBoolean(HwTelephonyProperties.PROPERTY_NETWORK_ISROAMING, false) && getFormatNumberByCountryISO(number, countryIso) == null) {
            return true;
        }
        return false;
    }

    private String getFormatNumberByCountryISO(String number, String countryIso) {
        if (TextUtils.isEmpty(number) || TextUtils.isEmpty(countryIso)) {
            return null;
        }
        return PhoneNumberUtils.formatNumberToE164(number, countryIso.toUpperCase(Locale.US));
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int getCallerIndexInternal(android.database.Cursor r19, java.lang.String r20, java.lang.String r21, int r22, int r23) {
        /*
        r18 = this;
        r7 = 0;
        r6 = 0;
        r11 = 0;
        r14 = 0;
        r13 = 0;
        r10 = -1;
        r9 = -1;
        r8 = -1;
        r15 = new java.lang.StringBuilder;
        r15.<init>();
        r16 = "getCallerIndexInternal, compNum: ";
        r15 = r15.append(r16);
        r0 = r20;
        r15 = r15.append(r0);
        r16 = ", numLong: ";
        r15 = r15.append(r16);
        r0 = r22;
        r15 = r15.append(r0);
        r16 = ", numShort: ";
        r15 = r15.append(r16);
        r0 = r23;
        r15 = r15.append(r0);
        r15 = r15.toString();
        logd(r15);
        r15 = android.text.TextUtils.isEmpty(r20);
        if (r15 == 0) goto L_0x0067;
    L_0x0041:
        if (r19 == 0) goto L_0x004a;
    L_0x0043:
        r15 = r19.getCount();
        if (r15 <= 0) goto L_0x004a;
    L_0x0049:
        r8 = 0;
    L_0x004a:
        r15 = "CallerInfo";
        r16 = new java.lang.StringBuilder;
        r16.<init>();
        r17 = "getCallerIndexInternal(),null == compNum! fixedIndex = ";
        r16 = r16.append(r17);
        r0 = r16;
        r16 = r0.append(r8);
        r16 = r16.toString();
        android.util.Log.e(r15, r16);
        return r8;
    L_0x0067:
        r5 = r20.length();
        r15 = 7;
        r0 = r22;
        if (r0 >= r15) goto L_0x018d;
    L_0x0070:
        r2 = 7;
    L_0x0071:
        r0 = r23;
        if (r0 < r2) goto L_0x0191;
    L_0x0075:
        r3 = r2;
    L_0x0076:
        r15 = new java.lang.StringBuilder;
        r15.<init>();
        r16 = "getCallerIndexInternal, after check NUM_LONG: ";
        r15 = r15.append(r16);
        r15 = r15.append(r2);
        r16 = ", NUM_SHORT: ";
        r15 = r15.append(r16);
        r15 = r15.append(r3);
        r15 = r15.toString();
        logd(r15);
        if (r19 == 0) goto L_0x0175;
    L_0x009a:
        if (r5 < r2) goto L_0x0235;
    L_0x009c:
        r15 = r5 - r2;
        r0 = r20;
        r6 = r0.substring(r15);
        r15 = r5 - r3;
        r0 = r20;
        r7 = r0.substring(r15);
        r15 = new java.lang.StringBuilder;
        r15.<init>();
        r16 = "11: compNumLong = ";
        r15 = r15.append(r16);
        r15 = r15.append(r6);
        r16 = ", compNumShort = ";
        r15 = r15.append(r16);
        r15 = r15.append(r7);
        r15 = r15.toString();
        logd(r15);
        r15 = r19.moveToFirst();
        if (r15 == 0) goto L_0x0175;
    L_0x00d4:
        r0 = r19;
        r1 = r21;
        r4 = r0.getColumnIndex(r1);
        r15 = -1;
        if (r4 == r15) goto L_0x0175;
    L_0x00df:
        r0 = r19;
        r11 = r0.getString(r4);
        if (r11 == 0) goto L_0x0195;
    L_0x00e7:
        r15 = 64;
        r15 = r11.indexOf(r15);
        if (r15 >= 0) goto L_0x0195;
    L_0x00ef:
        r11 = android.telephony.PhoneNumberUtils.stripSeparators(r11);
        r12 = r11.length();
        r15 = new java.lang.StringBuilder;
        r15.<init>();
        r16 = "11: tmpNum = ";
        r15 = r15.append(r16);
        r15 = r15.append(r11);
        r16 = ", tmpNum.length11: ";
        r15 = r15.append(r16);
        r16 = r11.length();
        r15 = r15.append(r16);
        r16 = ", ID = ";
        r15 = r15.append(r16);
        r16 = r19.getPosition();
        r15 = r15.append(r16);
        r15 = r15.toString();
        logd(r15);
        r0 = r20;
        r15 = r0.equals(r11);
        if (r15 == 0) goto L_0x0197;
    L_0x0134:
        r9 = r19.getPosition();
        r15 = new java.lang.StringBuilder;
        r15.<init>();
        r16 = "exact match: break! numLongID = ";
        r15 = r15.append(r16);
        r15 = r15.append(r9);
        r15 = r15.toString();
        logd(r15);
    L_0x014f:
        r15 = new java.lang.StringBuilder;
        r15.<init>();
        r16 = "11: numLongID = ";
        r15 = r15.append(r16);
        r15 = r15.append(r9);
        r16 = ", numShortID = ";
        r15 = r15.append(r16);
        r15 = r15.append(r10);
        r15 = r15.toString();
        logd(r15);
        r15 = -1;
        if (r15 == r9) goto L_0x022c;
    L_0x0174:
        r8 = r9;
    L_0x0175:
        r15 = new java.lang.StringBuilder;
        r15.<init>();
        r16 = "getCallerIndexInternal, fixedIndex: ";
        r15 = r15.append(r16);
        r15 = r15.append(r8);
        r15 = r15.toString();
        logd(r15);
        return r8;
    L_0x018d:
        r2 = r22;
        goto L_0x0071;
    L_0x0191:
        r3 = r23;
        goto L_0x0076;
    L_0x0195:
        r15 = -1;
        return r15;
    L_0x0197:
        if (r12 < r2) goto L_0x01ed;
    L_0x0199:
        r15 = r12 - r2;
        r13 = r11.substring(r15);
        r15 = -1;
        if (r15 != r9) goto L_0x01c3;
    L_0x01a2:
        r15 = r6.compareTo(r13);
        if (r15 != 0) goto L_0x01c3;
    L_0x01a8:
        r9 = r19.getPosition();
        r15 = new java.lang.StringBuilder;
        r15.<init>();
        r16 = "11: > NUM_LONG numLongID = ";
        r15 = r15.append(r16);
        r15 = r15.append(r9);
        r15 = r15.toString();
        logd(r15);
    L_0x01c3:
        r15 = new java.lang.StringBuilder;
        r15.<init>();
        r16 = "11: >= NUM_LONG, and !=,  tmpNumLong = ";
        r15 = r15.append(r16);
        r15 = r15.append(r13);
        r16 = ", numLongID: ";
        r15 = r15.append(r16);
        r15 = r15.append(r9);
        r15 = r15.toString();
        logd(r15);
    L_0x01e5:
        r15 = r19.moveToNext();
        if (r15 == 0) goto L_0x014f;
    L_0x01eb:
        goto L_0x00df;
    L_0x01ed:
        if (r12 < r3) goto L_0x0225;
    L_0x01ef:
        r15 = r12 - r3;
        r14 = r11.substring(r15);
        r15 = -1;
        if (r15 != r10) goto L_0x0202;
    L_0x01f8:
        r15 = r7.compareTo(r14);
        if (r15 != 0) goto L_0x0202;
    L_0x01fe:
        r10 = r19.getPosition();
    L_0x0202:
        r15 = new java.lang.StringBuilder;
        r15.<init>();
        r16 = "11: >= NUM_SHORT, tmpNumShort = ";
        r15 = r15.append(r16);
        r15 = r15.append(r14);
        r16 = ", numShortID:";
        r15 = r15.append(r16);
        r15 = r15.append(r10);
        r15 = r15.toString();
        logd(r15);
        goto L_0x01e5;
    L_0x0225:
        r15 = "tmpNum11, continue";
        logd(r15);
        goto L_0x01e5;
    L_0x022c:
        r15 = -1;
        if (r15 == r10) goto L_0x0232;
    L_0x022f:
        r8 = r10;
        goto L_0x0175;
    L_0x0232:
        r8 = -1;
        goto L_0x0175;
    L_0x0235:
        if (r5 < r3) goto L_0x039f;
    L_0x0237:
        r15 = r5 - r3;
        r0 = r20;
        r7 = r0.substring(r15);
        r15 = new java.lang.StringBuilder;
        r15.<init>();
        r16 = "7: compNumShort = ";
        r15 = r15.append(r16);
        r15 = r15.append(r7);
        r15 = r15.toString();
        logd(r15);
        r15 = r19.moveToFirst();
        if (r15 == 0) goto L_0x0175;
    L_0x025c:
        r0 = r19;
        r1 = r21;
        r4 = r0.getColumnIndex(r1);
        r15 = -1;
        if (r4 == r15) goto L_0x0175;
    L_0x0267:
        r0 = r19;
        r11 = r0.getString(r4);
        if (r11 == 0) goto L_0x02ff;
    L_0x026f:
        r15 = 64;
        r15 = r11.indexOf(r15);
        if (r15 >= 0) goto L_0x02ff;
    L_0x0277:
        r11 = android.telephony.PhoneNumberUtils.stripSeparators(r11);
        r12 = r11.length();
        r15 = new java.lang.StringBuilder;
        r15.<init>();
        r16 = "7: tmpNum = ";
        r15 = r15.append(r16);
        r15 = r15.append(r11);
        r16 = ", tmpNum.length7: ";
        r15 = r15.append(r16);
        r16 = r11.length();
        r15 = r15.append(r16);
        r16 = ", ID = ";
        r15 = r15.append(r16);
        r16 = r19.getPosition();
        r15 = r15.append(r16);
        r15 = r15.toString();
        logd(r15);
        r0 = r20;
        r15 = r0.equals(r11);
        if (r15 == 0) goto L_0x0301;
    L_0x02bc:
        r10 = r19.getPosition();
        r15 = new java.lang.StringBuilder;
        r15.<init>();
        r16 = "exact match numShortID = ";
        r15 = r15.append(r16);
        r15 = r15.append(r10);
        r15 = r15.toString();
        logd(r15);
    L_0x02d7:
        r15 = new java.lang.StringBuilder;
        r15.<init>();
        r16 = "7: numShortID = ";
        r15 = r15.append(r16);
        r15 = r15.append(r10);
        r16 = ", numLongID = ";
        r15 = r15.append(r16);
        r15 = r15.append(r9);
        r15 = r15.toString();
        logd(r15);
        r15 = -1;
        if (r15 == r10) goto L_0x0396;
    L_0x02fc:
        r8 = r10;
        goto L_0x0175;
    L_0x02ff:
        r15 = -1;
        return r15;
    L_0x0301:
        if (r12 < r2) goto L_0x0340;
    L_0x0303:
        r15 = r12 - r3;
        r14 = r11.substring(r15);
        r15 = -1;
        if (r15 != r9) goto L_0x0316;
    L_0x030c:
        r15 = r7.compareTo(r14);
        if (r15 != 0) goto L_0x0316;
    L_0x0312:
        r9 = r19.getPosition();
    L_0x0316:
        r15 = new java.lang.StringBuilder;
        r15.<init>();
        r16 = "7: >= NUM_LONG, tmpNumShort = ";
        r15 = r15.append(r16);
        r15 = r15.append(r14);
        r16 = ", numLongID:";
        r15 = r15.append(r16);
        r15 = r15.append(r9);
        r15 = r15.toString();
        logd(r15);
    L_0x0338:
        r15 = r19.moveToNext();
        if (r15 == 0) goto L_0x02d7;
    L_0x033e:
        goto L_0x0267;
    L_0x0340:
        if (r12 < r3) goto L_0x038f;
    L_0x0342:
        r15 = r12 - r3;
        r14 = r11.substring(r15);
        r15 = -1;
        if (r15 != r10) goto L_0x036c;
    L_0x034b:
        r15 = r7.compareTo(r14);
        if (r15 != 0) goto L_0x036c;
    L_0x0351:
        r10 = r19.getPosition();
        r15 = new java.lang.StringBuilder;
        r15.<init>();
        r16 = "7: >= NUM_SHORT numShortID = ";
        r15 = r15.append(r16);
        r15 = r15.append(r10);
        r15 = r15.toString();
        logd(r15);
    L_0x036c:
        r15 = new java.lang.StringBuilder;
        r15.<init>();
        r16 = "7: >= NUM_SHORT, and !=, tmpNumShort = ";
        r15 = r15.append(r16);
        r15 = r15.append(r14);
        r16 = ", numShortID:";
        r15 = r15.append(r16);
        r15 = r15.append(r10);
        r15 = r15.toString();
        logd(r15);
        goto L_0x0338;
    L_0x038f:
        r15 = "7: continue";
        logd(r15);
        goto L_0x0338;
    L_0x0396:
        r15 = -1;
        if (r15 == r9) goto L_0x039c;
    L_0x0399:
        r8 = r9;
        goto L_0x0175;
    L_0x039c:
        r8 = -1;
        goto L_0x0175;
    L_0x039f:
        r15 = r19.moveToFirst();
        if (r15 == 0) goto L_0x0175;
    L_0x03a5:
        r0 = r19;
        r1 = r21;
        r4 = r0.getColumnIndex(r1);
        r15 = -1;
        if (r4 == r15) goto L_0x0175;
    L_0x03b0:
        r0 = r19;
        r11 = r0.getString(r4);
        if (r11 == 0) goto L_0x043b;
    L_0x03b8:
        r15 = 64;
        r15 = r11.indexOf(r15);
        if (r15 >= 0) goto L_0x043b;
    L_0x03c0:
        r11 = android.telephony.PhoneNumberUtils.stripSeparators(r11);
        r12 = r11.length();
        r15 = new java.lang.StringBuilder;
        r15.<init>();
        r16 = "5: tmpNum = ";
        r15 = r15.append(r16);
        r15 = r15.append(r11);
        r16 = ", tmpNum.length: ";
        r15 = r15.append(r16);
        r16 = r11.length();
        r15 = r15.append(r16);
        r16 = ", ID = ";
        r15 = r15.append(r16);
        r16 = r19.getPosition();
        r15 = r15.append(r16);
        r15 = r15.toString();
        logd(r15);
        if (r12 != r5) goto L_0x043d;
    L_0x03ff:
        r0 = r20;
        r15 = r0.compareTo(r11);
        if (r15 != 0) goto L_0x0443;
    L_0x0407:
        r8 = r19.getPosition();
        r15 = new java.lang.StringBuilder;
        r15.<init>();
        r16 = "5: break! numLongID = ";
        r15 = r15.append(r16);
        r15 = r15.append(r8);
        r15 = r15.toString();
        logd(r15);
    L_0x0422:
        r15 = new java.lang.StringBuilder;
        r15.<init>();
        r16 = "5: fixedIndex = ";
        r15 = r15.append(r16);
        r15 = r15.append(r8);
        r15 = r15.toString();
        logd(r15);
        goto L_0x0175;
    L_0x043b:
        r15 = -1;
        return r15;
    L_0x043d:
        r15 = "5: continue";
        logd(r15);
    L_0x0443:
        r15 = r19.moveToNext();
        if (r15 == 0) goto L_0x0422;
    L_0x0449:
        goto L_0x03b0;
        */
        throw new UnsupportedOperationException("Method not decompiled: huawei.android.telephony.CallerInfoHW.getCallerIndexInternal(android.database.Cursor, java.lang.String, java.lang.String, int, int):int");
    }

    private boolean compareNumsInternal(String num1, String num2, int numLong, int numShort) {
        logd("compareNumsInternal, num1: " + num1 + ", num2: " + num2 + ", numLong: " + numLong + ", numShort: " + numShort);
        if (TextUtils.isEmpty(num1) || TextUtils.isEmpty(num2)) {
            return false;
        }
        int num1Len = num1.length();
        int num2Len = num2.length();
        int NUM_LONG = numLong < 7 ? 7 : numLong;
        int NUM_SHORT = numShort >= NUM_LONG ? NUM_LONG : numShort;
        logd("compareNumsInternal, after check NUM_LONG: " + NUM_LONG + ", NUM_SHORT: " + NUM_SHORT);
        String num1Short;
        if (num1Len >= NUM_LONG) {
            String num1Long = num1.substring(num1Len - NUM_LONG);
            num1Short = num1.substring(num1Len - NUM_SHORT);
            logd("compareNumsInternal, 11: num1Long = " + num1Long + ", num1Short = " + num1Short);
            if (num2Len >= NUM_LONG) {
                if (num1Long.compareTo(num2.substring(num2Len - NUM_LONG)) == 0) {
                    logd("compareNumsInternal, 11: >= NUM_LONG return true");
                    return true;
                }
            } else if (num2Len >= NUM_SHORT && num1Short.compareTo(num2.substring(num2Len - NUM_SHORT)) == 0) {
                logd("compareNumsInternal, 11: >= NUM_SHORT return true");
                return true;
            }
        } else if (num1Len >= NUM_SHORT) {
            num1Short = num1.substring(num1Len - NUM_SHORT);
            logd("compareNumsInternal, 7: num1Short = " + num1Short);
            if (num2Len >= NUM_SHORT && num1Short.compareTo(num2.substring(num2Len - NUM_SHORT)) == 0) {
                logd("compareNumsInternal, 7: >= NUM_SHORT return true");
                return true;
            }
        } else {
            logd("compareNumsInternal, 5: do full compare");
            return num1.equals(num2);
        }
        return false;
    }

    private boolean isRoamingCountryNumberByPrefix(String numberPrefix, String netIso) {
        if (SystemProperties.getBoolean(HwTelephonyProperties.PROPERTY_NETWORK_ISROAMING, false)) {
            if (TextUtils.isEmpty(numberPrefix)) {
                return true;
            }
            numberPrefix = numberPrefix.substring(getIntlPrefixLength(numberPrefix));
            if (!(TextUtils.isEmpty(numberPrefix) || (TextUtils.isEmpty(netIso) ^ 1) == 0)) {
                return numberPrefix.equals(Integer.toString(sInstance.getCountryCodeForRegion(netIso.toUpperCase(Locale.US))));
            }
        }
        return false;
    }

    private boolean isValidData4Number(String data1Num, String data4Num) {
        logd("isValidData4Number, data1Num: " + data1Num + ", data4Num: " + data4Num);
        if (!(TextUtils.isEmpty(data1Num) || (TextUtils.isEmpty(data4Num) ^ 1) == 0 || !data4Num.startsWith("+"))) {
            int countryCodeLen = getIntlPrefixAndCCLen(data4Num);
            if (countryCodeLen > 0) {
                data4Num = data4Num.substring(countryCodeLen);
            }
            logd("isValidData4Number, data4Num after remove prefix: " + data4Num);
            if (data4Num.length() <= data1Num.length() && data4Num.equals(data1Num.substring(data1Num.length() - data4Num.length()))) {
                return true;
            }
        }
        return false;
    }

    private boolean isNormalPrefix(String number) {
        String sMcc = " ";
        if (this.mNetworkOperator != null && this.mNetworkOperator.length() > 3) {
            sMcc = this.mNetworkOperator.substring(0, 3);
        }
        if (number.startsWith("011")) {
            for (Object equals : NORMAL_PREFIX_MCC) {
                if (sMcc.equals(equals) && number.length() == 11) {
                    logd("those operator 011 are normal prefix");
                    return true;
                }
            }
        }
        return false;
    }

    private static String formatedForDualNumber(String compNum) {
        if (!IS_SUPPORT_DUAL_NUMBER) {
            return compNum;
        }
        if (isVirtualNum(compNum)) {
            compNum = compNum.substring(0, compNum.length() - 1);
        }
        if (compNum.startsWith("*230#")) {
            compNum = compNum.substring(5, compNum.length());
        } else if (compNum.startsWith("*23#")) {
            compNum = compNum.substring(4, compNum.length());
        }
        return compNum;
    }

    private static boolean isVirtualNum(String dialString) {
        if (!dialString.endsWith("#")) {
            return false;
        }
        String tempstring = dialString.substring(0, dialString.length() - 1).replace(" ", "").replace("+", "").replace("-", "");
        if (tempstring.startsWith("*230#")) {
            tempstring = tempstring.substring(5, tempstring.length());
        } else if (tempstring.startsWith("*23#")) {
            tempstring = tempstring.substring(4, tempstring.length());
        }
        if (tempstring.matches("[0-9]+")) {
            return true;
        }
        return false;
    }
}
