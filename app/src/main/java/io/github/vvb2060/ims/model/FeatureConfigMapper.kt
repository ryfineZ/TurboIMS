package io.github.vvb2060.ims.model

import android.os.Bundle
import android.telephony.CarrierConfigManager

object FeatureConfigMapper {
    private const val KEY_SHOW_4G_FOR_LTE = "show_4g_for_lte_data_icon_bool"
    private const val KEY_INCLUDE_LTE_FOR_NR_ADVANCED_THRESHOLD_BANDWIDTH =
        "include_lte_for_nr_advanced_threshold_bandwidth_bool"
    private const val KEY_NR_ADVANCED_THRESHOLD_BANDWIDTH_KHZ = "nr_advanced_threshold_bandwidth_khz_int"
    private const val KEY_ADDITIONAL_NR_ADVANCED_BANDS = "additional_nr_advanced_bands_int_array"
    private const val KEY_5G_ICON_CONFIGURATION = "5g_icon_configuration_string"
    private const val KEY_NR_ADVANCED_CAPABLE_PCO_ID = "nr_advanced_capable_pco_id_int"
    private const val KEY_VONR_ENABLED = "vonr_enabled_bool"
    private const val KEY_VONR_SETTING_VISIBILITY = "vonr_setting_visibility_bool"
    private const val KEY_SIM_COUNTRY_ISO_OVERRIDE = "sim_country_iso_override_string"
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
        KEY_NR_ADVANCED_THRESHOLD_BANDWIDTH_KHZ,
        KEY_ADDITIONAL_NR_ADVANCED_BANDS,
        KEY_5G_ICON_CONFIGURATION,
        KEY_NR_ADVANCED_CAPABLE_PCO_ID,
        KEY_INCLUDE_LTE_FOR_NR_ADVANCED_THRESHOLD_BANDWIDTH,
        CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL,
        KEY_SHOW_4G_FOR_LTE,
        CarrierConfigManager.KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL,
        CarrierConfigManager.KEY_ENABLE_CROSS_SIM_CALLING_ON_OPPORTUNISTIC_DATA_BOOL,
        KEY_VONR_ENABLED,
        KEY_VONR_SETTING_VISIBILITY,
        KEY_SIM_COUNTRY_ISO_OVERRIDE,
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

        val countryIso = bundle.getStringOrDefault(KEY_SIM_COUNTRY_ISO_OVERRIDE, "")
        map[Feature.COUNTRY_ISO] = FeatureValue(countryIso, FeatureValueType.STRING)
        map[Feature.TIKTOK_NETWORK_FIX] = FeatureValue(
            countryIso.isNotBlank() && countryIso.all { it.isDigit() },
            FeatureValueType.BOOLEAN
        )

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

        val vonrEnabled = bundle.getBooleanOrDefault(KEY_VONR_ENABLED, false) &&
            bundle.getBooleanOrDefault(KEY_VONR_SETTING_VISIBILITY, false)
        map[Feature.VONR] = FeatureValue(vonrEnabled, FeatureValueType.BOOLEAN)

        val crossSimEnabled = bundle.getBooleanOrDefault(
            CarrierConfigManager.KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL,
            false
        ) && bundle.getBooleanOrDefault(
            CarrierConfigManager.KEY_ENABLE_CROSS_SIM_CALLING_ON_OPPORTUNISTIC_DATA_BOOL,
            false
        )
        map[Feature.CROSS_SIM] = FeatureValue(crossSimEnabled, FeatureValueType.BOOLEAN)

        map[Feature.UT] = FeatureValue(
            bundle.getBooleanOrDefault(
                CarrierConfigManager.KEY_CARRIER_SUPPORTS_SS_OVER_UT_BOOL,
                Feature.UT.defaultValue as Boolean
            ),
            FeatureValueType.BOOLEAN
        )

        val arr = bundle.getIntArray(CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY)
        val nrEnabled = arr?.contains(CarrierConfigManager.CARRIER_NR_AVAILABILITY_NSA) == true ||
            arr?.contains(CarrierConfigManager.CARRIER_NR_AVAILABILITY_SA) == true
        map[Feature.FIVE_G_NR] = FeatureValue(nrEnabled, FeatureValueType.BOOLEAN)

        val thresholds = bundle.getIntArray(CarrierConfigManager.KEY_5G_NR_SSRSRP_THRESHOLDS_INT_ARRAY)
        val thresholdEnabled = thresholds?.contentEquals(FIVE_G_THRESHOLDS) == true
        map[Feature.FIVE_G_THRESHOLDS] = FeatureValue(thresholdEnabled, FeatureValueType.BOOLEAN)

        val fiveGPlusIconEnabled = bundle.containsKey(KEY_NR_ADVANCED_THRESHOLD_BANDWIDTH_KHZ) ||
            bundle.containsKey(KEY_ADDITIONAL_NR_ADVANCED_BANDS) ||
            bundle.containsKey(KEY_5G_ICON_CONFIGURATION) ||
            bundle.containsKey(KEY_NR_ADVANCED_CAPABLE_PCO_ID) ||
            bundle.containsKey(
                KEY_INCLUDE_LTE_FOR_NR_ADVANCED_THRESHOLD_BANDWIDTH
            )
        map[Feature.FIVE_G_PLUS_ICON] = FeatureValue(fiveGPlusIconEnabled, FeatureValueType.BOOLEAN)

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
