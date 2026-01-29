package android.os;

public final class TelephonyServiceManager {
    public static final class ServiceRegisterer {
        public IBinder get() {
            return null;
        }
    }

    public ServiceRegisterer telephonyServiceRegisterer;
    public ServiceRegisterer subscriptionServiceRegisterer;
    public ServiceRegisterer carrierConfigServiceRegisterer;
    public ServiceRegisterer phoneSubServiceRegisterer;

    public ServiceRegisterer getTelephonyServiceRegisterer() {
        return null;
    }

    public ServiceRegisterer getSubscriptionServiceRegisterer() {
        return null;
    }

    public ServiceRegisterer getCarrierConfigServiceRegisterer() {
        return null;
    }

    public ServiceRegisterer getPhoneSubServiceRegisterer() {
        return null;
    }
}
