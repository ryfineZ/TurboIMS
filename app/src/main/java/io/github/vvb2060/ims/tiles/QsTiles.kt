package io.github.vvb2060.ims.tiles

import android.content.Intent
import android.content.pm.PackageManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.telephony.CarrierConfigManager
import android.util.Log
import io.github.vvb2060.ims.ShizukuProvider
import io.github.vvb2060.ims.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

private const val TAG = "TurboIMS-QS"

abstract class BaseSimTileService : TileService() {
    protected abstract val simSlotIndex: Int
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    protected fun launch(block: suspend () -> Unit) {
        serviceScope.launch { block() }
    }

    protected fun isShizukuReady(): Boolean {
        return Shizuku.pingBinder() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }

    protected suspend fun resolveSubId(): Int? {
        val sims = ShizukuProvider.readSimInfoList(applicationContext)
        return sims.firstOrNull { it.simSlotIndex == simSlotIndex }?.subId
    }

    protected suspend fun updateTileState(state: Int, subtitle: String? = null) {
        withContext(Dispatchers.Main) {
            qsTile?.state = state
            qsTile?.subtitle = subtitle
            qsTile?.updateTile()
        }
    }

    protected fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivityAndCollapse(intent)
    }
}

abstract class BaseVoLTETileService : BaseSimTileService() {
    override fun onStartListening() {
        super.onStartListening()
        launch { refreshTile() }
    }

    override fun onClick() {
        super.onClick()
        launch { toggleVoLTE() }
    }

    private suspend fun refreshTile() {
        if (!isShizukuReady()) {
            updateTileState(Tile.STATE_UNAVAILABLE)
            return
        }
        val subId = resolveSubId()
        if (subId == null) {
            updateTileState(Tile.STATE_UNAVAILABLE)
            return
        }
        val current = readVoLTEEnabled(subId)
        when (current) {
            true -> updateTileState(Tile.STATE_ACTIVE)
            false -> updateTileState(Tile.STATE_INACTIVE)
            null -> updateTileState(Tile.STATE_UNAVAILABLE)
        }
    }

    private suspend fun toggleVoLTE() {
        if (!isShizukuReady()) {
            openMainActivity()
            return
        }
        val subId = resolveSubId()
        if (subId == null) {
            updateTileState(Tile.STATE_UNAVAILABLE)
            return
        }
        val current = readVoLTEEnabled(subId) ?: run {
            updateTileState(Tile.STATE_UNAVAILABLE)
            return
        }
        val target = !current
        val result = ShizukuProvider.updateCarrierConfigBoolean(
            applicationContext,
            subId,
            CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL,
            target,
        )
        if (result != null) {
            Log.w(TAG, "toggle volte failed: $result")
        } else {
            ShizukuProvider.restartImsRegistration(applicationContext, subId)
        }
        refreshTile()
    }

    private suspend fun readVoLTEEnabled(subId: Int): Boolean? {
        val bundle = ShizukuProvider.readCarrierConfig(
            applicationContext,
            subId,
            arrayOf(CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL)
        ) ?: return null
        return if (bundle.containsKey(CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL)) {
            bundle.getBoolean(CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL)
        } else {
            null
        }
    }
}

abstract class BaseIMSStatusTileService : BaseSimTileService() {
    override fun onStartListening() {
        super.onStartListening()
        launch { refreshTile() }
    }

    private suspend fun refreshTile() {
        if (!isShizukuReady()) {
            updateTileState(Tile.STATE_UNAVAILABLE)
            return
        }
        val subId = resolveSubId()
        if (subId == null) {
            updateTileState(Tile.STATE_UNAVAILABLE)
            return
        }
        val registered = ShizukuProvider.readImsRegistrationStatus(applicationContext, subId)
        when (registered) {
            true -> updateTileState(Tile.STATE_ACTIVE)
            false -> updateTileState(Tile.STATE_INACTIVE)
            null -> updateTileState(Tile.STATE_UNAVAILABLE)
        }
    }
}

class SIM1VoLTETileService : BaseVoLTETileService() {
    override val simSlotIndex: Int = 0
}

class SIM2VoLTETileService : BaseVoLTETileService() {
    override val simSlotIndex: Int = 1
}

class SIM1IMSStatusTileService : BaseIMSStatusTileService() {
    override val simSlotIndex: Int = 0
}

class SIM2IMSStatusTileService : BaseIMSStatusTileService() {
    override val simSlotIndex: Int = 1
}
