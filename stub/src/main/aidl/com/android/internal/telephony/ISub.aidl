package com.android.internal.telephony;

import android.telephony.SubscriptionInfo;

interface ISub {
    List<SubscriptionInfo> getActiveSubscriptionInfoList(String callingPackage, String callingFeatureId, boolean isForAllProfiles);
    int getSlotIndex(int subId);
}
