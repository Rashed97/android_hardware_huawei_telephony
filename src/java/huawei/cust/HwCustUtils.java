package huawei.cust;

import android.os.SystemProperties;
import android.util.Log;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;

public final class HwCustUtils {
    static final String CUST_CLS_NULL_REPLACE = "-";
    static final String CUST_CLS_SUFFIX_DEF = "Impl:-";
    static final String CUST_CLS_SUFFIX_SEP = ":";
    static String FILE_ONLY_IN_CUST = "/system/etc/permissions/hwcustframework.xml";
    static boolean CUST_VERSION = new File(FILE_ONLY_IN_CUST).exists();
    static final boolean DEBUG_I = true;
    static final boolean EXCEPTION_WHEN_ERROR = true;
    static final String PROP_CUST_CLS_SUFFIX = "cust.cls.suffixes";
    static final String[] FACTORY_ARRAY = SystemProperties.get(PROP_CUST_CLS_SUFFIX, CUST_CLS_SUFFIX_DEF).split(":");
    static final String HWCUST_PREFIX = "HwCust";
    static final String TAG = "HwCust";
    private static HashMap<String, ClassInfo> mClassCache = new HashMap();
    private static HashMap<String, Constructor<?>> mConstructorCache = new HashMap();
    private static HashMap<Class<?>, Class<?>> mPrimitiveMap = new HashMap();

    static class ClassInfo {
        Class<?> mCls;
        Constructor<?>[] mCs;
        String mOrgClsName;

        ClassInfo(String orgName, Class<?> cls) {
            this.mOrgClsName = orgName;
            this.mCls = cls;
            this.mCs = cls.getDeclaredConstructors();
        }
    }

    static {
        mPrimitiveMap.put(Boolean.TYPE, Boolean.class);
        mPrimitiveMap.put(Byte.TYPE, Byte.class);
        mPrimitiveMap.put(Character.TYPE, Character.class);
        mPrimitiveMap.put(Short.TYPE, Short.class);
        mPrimitiveMap.put(Integer.TYPE, Integer.class);
        mPrimitiveMap.put(Long.TYPE, Long.class);
        mPrimitiveMap.put(Float.TYPE, Float.class);
        mPrimitiveMap.put(Double.TYPE, Double.class);
        int i = 0;
        while (i < FACTORY_ARRAY.length) {
            if (FACTORY_ARRAY[i] == null || FACTORY_ARRAY[i].equals("-")) {
                FACTORY_ARRAY[i] = "";
            }
            i++;
        }
    }

    public static Object createObj(String className, ClassLoader cl, Object... args) {
        Throwable ex;
        ClassInfo clsInfo = getClassByName(className, cl, FACTORY_ARRAY);
        if (clsInfo == null) {
            return null;
        }
        Constructor<?> useConstructor = findConstructor(clsInfo, args);
        if (useConstructor == null) {
            handle_exception("constructor not found for " + clsInfo.mCls, new NullPointerException());
            return null;
        }
        try {
            Object obj = useConstructor.newInstance(args);
            Log.d("HwCust", "Create obj success use " + clsInfo.mCls);
            return obj;
        } catch (Throwable e) {
            ex = e;
            handle_exception("create cust obj fail. Class = " + clsInfo.mCls + ", constructor = " + useConstructor, ex);
            return null;
        } catch (Throwable e2) {
            ex = e2;
            handle_exception("create cust obj fail. Class = " + clsInfo.mCls + ", constructor = " + useConstructor, ex);
            return null;
        } catch (Throwable e3) {
            ex = e3;
            handle_exception("create cust obj fail. Class = " + clsInfo.mCls + ", constructor = " + useConstructor, ex);
            return null;
        } catch (Throwable e4) {
            ex = e4;
            handle_exception("create cust obj fail. Class = " + clsInfo.mCls + ", constructor = " + useConstructor, ex);
            return null;
        } catch (Throwable e5) {
            ex = e5;
            handle_exception("create cust obj fail. Class = " + clsInfo.mCls + ", constructor = " + useConstructor, ex);
            return null;
        }
    }

    public static Object createObj(Class<?> classClass, Object... args) {
        return createObj(classClass.getName(), classClass.getClassLoader(), args);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static synchronized huawei.cust.HwCustUtils.ClassInfo getClassByName(java.lang.String r9, java.lang.ClassLoader r10, java.lang.String[] r11) {
        /*
        r8 = 0;
        r6 = huawei.cust.HwCustUtils.class;
        monitor-enter(r6);
        r5 = mClassCache;	 Catch:{ all -> 0x00d2 }
        r0 = r5.get(r9);	 Catch:{ all -> 0x00d2 }
        r0 = (huawei.cust.HwCustUtils.ClassInfo) r0;	 Catch:{ all -> 0x00d2 }
        if (r0 == 0) goto L_0x0010;
    L_0x000e:
        monitor-exit(r6);
        return r0;
    L_0x0010:
        if (r9 == 0) goto L_0x0018;
    L_0x0012:
        r5 = r9.length();	 Catch:{ all -> 0x00d2 }
        if (r5 != 0) goto L_0x0036;
    L_0x0018:
        r5 = new java.lang.StringBuilder;	 Catch:{ all -> 0x00d2 }
        r5.<init>();	 Catch:{ all -> 0x00d2 }
        r7 = "createCustImpl obj, className invalid: ";
        r5 = r5.append(r7);	 Catch:{ all -> 0x00d2 }
        r5 = r5.append(r9);	 Catch:{ all -> 0x00d2 }
        r5 = r5.toString();	 Catch:{ all -> 0x00d2 }
        r7 = new java.lang.Exception;	 Catch:{ all -> 0x00d2 }
        r7.<init>();	 Catch:{ all -> 0x00d2 }
        handle_exception(r5, r7);	 Catch:{ all -> 0x00d2 }
        monitor-exit(r6);
        return r8;
    L_0x0036:
        r5 = "$";
        r5 = r9.contains(r5);	 Catch:{ all -> 0x00d2 }
        if (r5 != 0) goto L_0x0018;
    L_0x003f:
        r5 = ".HwCust";
        r5 = r9.contains(r5);	 Catch:{ all -> 0x00d2 }
        r5 = r5 ^ 1;
        if (r5 != 0) goto L_0x0018;
    L_0x004a:
        r4 = 0;
        r1 = r0;
    L_0x004c:
        r5 = r11.length;	 Catch:{ all -> 0x00d2 }
        if (r4 >= r5) goto L_0x00d7;
    L_0x004f:
        r5 = new java.lang.StringBuilder;	 Catch:{ ClassNotFoundException -> 0x00cb }
        r5.<init>();	 Catch:{ ClassNotFoundException -> 0x00cb }
        r5 = r5.append(r9);	 Catch:{ ClassNotFoundException -> 0x00cb }
        r7 = r11[r4];	 Catch:{ ClassNotFoundException -> 0x00cb }
        r5 = r5.append(r7);	 Catch:{ ClassNotFoundException -> 0x00cb }
        r5 = r5.toString();	 Catch:{ ClassNotFoundException -> 0x00cb }
        r7 = 1;
        r2 = java.lang.Class.forName(r5, r7, r10);	 Catch:{ ClassNotFoundException -> 0x00cb }
        r0 = new huawei.cust.HwCustUtils$ClassInfo;	 Catch:{ ClassNotFoundException -> 0x00cb }
        r0.<init>(r9, r2);	 Catch:{ ClassNotFoundException -> 0x00cb }
        r5 = mClassCache;	 Catch:{ ClassNotFoundException -> 0x00d5 }
        r5.put(r9, r0);	 Catch:{ ClassNotFoundException -> 0x00d5 }
        r5 = CUST_VERSION;	 Catch:{ ClassNotFoundException -> 0x00d5 }
        if (r5 == 0) goto L_0x00c1;
    L_0x0075:
        r5 = r11.length;	 Catch:{ ClassNotFoundException -> 0x00d5 }
        r5 = r5 + -1;
        if (r4 != r5) goto L_0x00c1;
    L_0x007a:
        r5 = "HwCust";
        r7 = new java.lang.StringBuilder;	 Catch:{ ClassNotFoundException -> 0x00d5 }
        r7.<init>();	 Catch:{ ClassNotFoundException -> 0x00d5 }
        r8 = "CUST VERSION = ";
        r7 = r7.append(r8);	 Catch:{ ClassNotFoundException -> 0x00d5 }
        r8 = CUST_VERSION;	 Catch:{ ClassNotFoundException -> 0x00d5 }
        r7 = r7.append(r8);	 Catch:{ ClassNotFoundException -> 0x00d5 }
        r8 = ", use class = ";
        r7 = r7.append(r8);	 Catch:{ ClassNotFoundException -> 0x00d5 }
        r7 = r7.append(r2);	 Catch:{ ClassNotFoundException -> 0x00d5 }
        r7 = r7.toString();	 Catch:{ ClassNotFoundException -> 0x00d5 }
        android.util.Log.w(r5, r7);	 Catch:{ ClassNotFoundException -> 0x00d5 }
    L_0x00a1:
        if (r0 != 0) goto L_0x00bf;
    L_0x00a3:
        r5 = new java.lang.StringBuilder;	 Catch:{ all -> 0x00d2 }
        r5.<init>();	 Catch:{ all -> 0x00d2 }
        r7 = "Class / custClass not found for: ";
        r5 = r5.append(r7);	 Catch:{ all -> 0x00d2 }
        r5 = r5.append(r9);	 Catch:{ all -> 0x00d2 }
        r5 = r5.toString();	 Catch:{ all -> 0x00d2 }
        r7 = new java.lang.ClassNotFoundException;	 Catch:{ all -> 0x00d2 }
        r7.<init>();	 Catch:{ all -> 0x00d2 }
        handle_exception(r5, r7);	 Catch:{ all -> 0x00d2 }
    L_0x00bf:
        monitor-exit(r6);
        return r0;
    L_0x00c1:
        r5 = CUST_VERSION;	 Catch:{ ClassNotFoundException -> 0x00d5 }
        if (r5 != 0) goto L_0x00a1;
    L_0x00c5:
        r5 = r11.length;	 Catch:{ ClassNotFoundException -> 0x00d5 }
        r5 = r5 + -1;
        if (r4 == r5) goto L_0x00a1;
    L_0x00ca:
        goto L_0x007a;
    L_0x00cb:
        r3 = move-exception;
        r0 = r1;
    L_0x00cd:
        r4 = r4 + 1;
        r1 = r0;
        goto L_0x004c;
    L_0x00d2:
        r5 = move-exception;
        monitor-exit(r6);
        throw r5;
    L_0x00d5:
        r3 = move-exception;
        goto L_0x00cd;
    L_0x00d7:
        r0 = r1;
        goto L_0x00a1;
        */
        throw new UnsupportedOperationException("Method not decompiled: huawei.cust.HwCustUtils.getClassByName(java.lang.String, java.lang.ClassLoader, java.lang.String[]):huawei.cust.HwCustUtils$ClassInfo");
    }

    static synchronized Constructor<?> findConstructor(ClassInfo info, Object... args) {
        synchronized (HwCustUtils.class) {
            String tag = getArgsType(info.mOrgClsName, args);
            Constructor<?> useConstructor = (Constructor) mConstructorCache.get(tag);
            if (useConstructor != null) {
                return useConstructor;
            }
            for (Constructor<?> c : info.mCs) {
                Class<?>[] ptcs = c.getParameterTypes();
                if (!Modifier.isPrivate(c.getModifiers()) && ptcs.length == args.length) {
                    if (ptcs.length == 0) {
                        useConstructor = c;
                    } else {
                        for (int i = 0; i < args.length; i++) {
                            if (args[i] == null) {
                                if (ptcs[i].isPrimitive()) {
                                    break;
                                }
                            }
                            Class<?> argCls = args[i].getClass();
                            Class<?> ptcCls = ptcs[i];
                            if (argCls.isPrimitive()) {
                                argCls = (Class) mPrimitiveMap.get(argCls);
                            }
                            if (ptcCls.isPrimitive()) {
                                ptcCls = (Class) mPrimitiveMap.get(ptcCls);
                            }
                            if (!ptcCls.isAssignableFrom(argCls)) {
                                break;
                            }
                            if (i == args.length - 1) {
                                useConstructor = c;
                            }
                        }
                    }
                    if (useConstructor != null) {
                        log_info("Constructor found for " + info.mCls);
                        break;
                    }
                }
            }
            mConstructorCache.put(tag, useConstructor);
            return useConstructor;
        }
    }

    static String getArgsType(String clsName, Object... args) {
        StringBuilder sb = new StringBuilder(clsName + ":" + "-");
        for (Object arg : args) {
            if (arg == null) {
                sb.append(":null");
            } else {
                sb.append(":").append(arg.getClass());
            }
        }
        return sb.toString();
    }

    static void log_info(String msg) {
        Log.i("HwCust", msg);
    }

    static void handle_exception(String msg, Throwable th) {
        throw new RuntimeException(msg, th);
    }
}
