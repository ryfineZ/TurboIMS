package io.github.vvb2060.ims.model

import android.os.Build
import android.os.Bundle
import android.telephony.CarrierConfigManager

object FeatureConfigMapper {
    private const val KEY_SHOW_4G_FOR_LTE = "show_4g_for_lte_data_icon_bool"
    private val FIVE_G_THRESHOLDS = intArrayOf(-128, -118, -108, -98)

    val readKeys: Array<String> = linkedSetOf(
        CarrierConfigManager.KEY_CARRIER_NAME_OVERRIDE_BOOL,
        CarrierConfigManager.KEY_CARRIER_NAME_STRING,
        CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL,
        CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL,
        CarrierConfigManager.KEY_CARRIER_VT_AVAILABLE_BOOL,
        CarrierConfigManager.KEY_CARRIER_SUPPORTS_SS_OVER_UT_BOOL,
        CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY,
        CarrierConfigManager.KEY_5G_NR_SSRSRP_THRESHOLDS_INT_ARRAY,
        CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL,
        KEY_SHOW_4G_FOR_LTE,
        CarrierConfigManager.KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL,
        CarrierConfigManager.KEY_ENABLE_CROSS_SIM_CALLING_ON_OPPORTUNISTIC_DATA_BOOL,
        CarrierConfigManager.KEY_VONR_ENABLED_BOOL,
        CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL,
        CarrierConfigManager.Ims.KEY_IMS_USER_AGENT_STRING,
        CarrierConfigManager.KEY_SIM_COUNTRY_ISO_OVERRIDE_STRING,
    ).toTypedArray()

    fun fromBundle(bundle: Bundle): Map<Feature, FeatureValue> {
        val map = linkedMapOf<Feature, FeatureValue>()

        val carrierNameOverride = bundle.getBooleanOrDefault(
            CarrierConfigManager.KEY_CARRIER_NAME_OVERRIDE_BOOL,
            false
        )
        val carrierName = if (carrierNameOverride) {
            bundle.getStringOrDefault(CarrierConfigManager.KEY_CARRIER_NAME_STRING, "")
        } else {
            ""
        }
        map[Feature.CARRIER_NAME] = FeatureValue(carrierName, FeatureValueType.STRING)

        val countryIso = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            bundle.getStringOrDefault(CarrierConfigManager.KEY_SIM_COUNTRY_ISO_OVERRIDE_STRING, "")
        } else {
            ""
        }
        map[Feature.COUNTRY_ISO] = FeatureValue(countryIso, FeatureValueType.STRING)

        val imsUserAgent = bundle.getStringOrDefault(
            CarrierConfigManager.Ims.KEY_IMS_USER_AGENT_STRING,
            ""
        )
        map[Feature.IMS_USER_AGENT] = FeatureValue(imsUserAgent, FeatureValueType.STRING)

        map[Feature.VOLTE] = FeatureValue(
            bundle.getBooleanOrDefault(
                CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL,
                Feature.VOLTE.defaultValue as Boolean
            ),
            FeatureValueType.BOOLEAN
        )

        map[Feature.VOWIFI] = FeatureValue(
            bundle.getBooleanOrDefault(
                CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL,
                Feature.VOWIFI.defaultValue as Boolean
            ),
            FeatureValueType.BOOLEAN
        )

        map[Feature.VT] = FeatureValue(
            bundle.getBooleanOrDefault(
                CarrierConfigManager.KEY_CARRIER_VT_AVAILABLE_BOOL,
                Feature.VT.defaultValue as Boolean
            ),
            FeatureValueType.BOOLEAN
        )

        val vonrEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            bundle.getBooleanOrDefault(CarrierConfigManager.KEY_VONR_ENABLED_BOOL, false) &&
                bundle.getBooleanOrDefault(CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL, false)
        } else {
            Feature.VONR.defaultValue as Boolean
        }
        map[Feature.VONR] = FeatureValue(vonrEnabled, FeatureValueType.BOOLEAN)

        val crossSimEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getBooleanOrDefault(
                CarrierConfigManager.KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL,
                false
            ) && bundle.getBooleanOrDefault(
                CarrierConfigManager.KEY_ENABLE_CROSS_SIM_CALLING_ON_OPPORTUNISTIC_DATA_BOOL,
                false
            )
        } else {
            Feature.CROSS_SIM.defaultValue as Boolean
        }
        map[Feature.CROSS_SIM] = FeatureValue(crossSimEnabled, FeatureValueType.BOOLEAN)

        map[Feature.UT] = FeatureValue(
            bundle.getBooleanOrDefault(
                CarrierConfigManager.KEY_CARRIER_SUPPORTS_SS_OVER_UT_BOOL,
                Feature.UT.defaultValue as Boolean
            ),
            FeatureValueType.BOOLEAN
        )

        val nrEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val arr = bundle.getIntArray(CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY)
            arr?.contains(CarrierConfigManager.CARRIER_NR_AVAILABILITY_NSA) == true &&
                arr.contains(CarrierConfigManager.CARRIER_NR_AVAILABILITY_SA)
        } else {
            Feature.FIVE_G_NR.defaultValue as Boolean
        }
        map[Feature.FIVE_G_NR] = FeatureValue(nrEnabled, FeatureValueType.BOOLEAN)

        val thresholds = bundle.getIntArray(CarrierConfigManager.KEY_5G_NR_SSRSRP_THRESHOLDS_INT_ARRAY)
        val thresholdEnabled = thresholds?.contentEquals(FIVE_G_THRESHOLDS) == true
        map[Feature.FIVE_G_THRESHOLDS] = FeatureValue(thresholdEnabled, FeatureValueType.BOOLEAN)

        val show4g = bundle.getBooleanOrDefault(
            CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL,
            bundle.getBooleanOrDefault(KEY_SHOW_4G_FOR_LTE, Feature.SHOW_4G_FOR_LTE.defaultValue as Boolean)
        )
        map[Feature.SHOW_4G_FOR_LTE] = FeatureValue(show4g, FeatureValueType.BOOLEAN)

        return map
    }

    private fun Bundle.getBooleanOrDefault(key: String, default: Boolean): Boolean {
        return if (containsKey(key)) getBoolean(key) else default
    }

    private fun Bundle.getStringOrDefault(key: String, default: String): String {
        return if (containsKey(key)) getString(key, default) else default
    }
}
