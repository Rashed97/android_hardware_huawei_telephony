package com.android.internal.telephony;

import android.content.Context;

public interface HwPhoneManager {
    public interface PhoneServiceInterface {
        void setPhone(Phone phone, Context context);

        void setPhone(Phone[] phoneArr, Context context);
    }
}
