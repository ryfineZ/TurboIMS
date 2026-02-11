package io.github.vvb2060.ims.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Immutable
data class SimSelection(
    val subId: Int,
    val displayName: String,
    val carrierName: String,
    val simSlotIndex: Int,
    val showTitle: String = buildString {
        append("SIM ")
        append(simSlotIndex + 1)
        append(": ")
        append(displayName)
        append(" (${carrierName})")
    },
    val countryIso: String = "",
    val mcc: String = "",
    val mnc: String = "",
)
