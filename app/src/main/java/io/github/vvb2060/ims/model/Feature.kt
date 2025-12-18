package io.github.vvb2060.ims.model

import androidx.compose.runtime.Immutable
import io.github.vvb2060.ims.R

enum class Feature(
    val valueType: FeatureValueType,
    val showTitleRes: Int,
    val showDescriptionRes: Int,
    val defaultValue: Any,
) {
    CARRIER_NAME(
        FeatureValueType.STRING,
        R.string.carrier_name,
        R.string.carrier_name_desc,
        "",
    ),
    COUNTRY_ISO(
        FeatureValueType.STRING,
        R.string.country_iso,
        R.string.country_iso_desc,
        "",
    ),
    VOLTE(
        FeatureValueType.BOOLEAN,
        R.string.volte,
        R.string.volte_desc,
        true,
    ),
    VOWIFI(
        FeatureValueType.BOOLEAN,
        R.string.vowifi,
        R.string.vowifi_desc,
        true,
    ),
    VT(
        FeatureValueType.BOOLEAN,
        R.string.vt,
        R.string.vt_desc,
        true,
    ),
    VONR(
        FeatureValueType.BOOLEAN,
        R.string.vonr,
        R.string.vonr_desc,
        true,
    ),
    CROSS_SIM(
        FeatureValueType.BOOLEAN,
        R.string.cross_sim,
        R.string.cross_sim_desc,
        true,
    ),
    UT(
        FeatureValueType.BOOLEAN,
        R.string.ut,
        R.string.ut_desc,
        true,
    ),
    FIVE_G_NR(
        FeatureValueType.BOOLEAN,
        R.string._5g_nr,
        R.string._5g_nr_desc,
        true,
    ),
    FIVE_G_THRESHOLDS(
        FeatureValueType.BOOLEAN,
        R.string._5g_thresholds,
        R.string._5g_thresholds_desc,
        true,
    )
}

enum class FeatureValueType {
    BOOLEAN, STRING,
}

@Immutable
data class FeatureValue(
    val data: Any,
    val valueType: FeatureValueType,
)