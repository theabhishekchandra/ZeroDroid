package com.abhishek.zerodroid.features.celltower.domain

import android.annotation.SuppressLint
import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoTdscdma
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CellTowerAnalyzer(
    private val telephonyManager: TelephonyManager?
) {
    private var lastLac: Int? = null
    private var lastRssi: Int? = null
    private var lastCellType: CellType? = null
    private val alerts = mutableListOf<ImsiCatcherAlert>()

    @SuppressLint("MissingPermission")
    fun monitor(): Flow<CellTowerState> = flow {
        while (true) {
            try {
                val cellInfos = telephonyManager?.allCellInfo ?: emptyList()
                val towers = cellInfos.mapNotNull { it.toCellTowerInfo() }
                val current = towers.firstOrNull { it.isRegistered }
                val neighbors = towers.filter { !it.isRegistered }

                current?.let { checkImsiHeuristics(it) }

                emit(
                    CellTowerState(
                        currentCell = current,
                        neighbors = neighbors,
                        alerts = alerts.toList(),
                        isMonitoring = true
                    )
                )
            } catch (e: SecurityException) {
                emit(CellTowerState(error = "Permission denied: ${e.message}"))
            } catch (e: Exception) {
                emit(CellTowerState(error = e.message))
            }
            delay(3000)
        }
    }

    private fun checkImsiHeuristics(cell: CellTowerInfo) {
        // LAC change detection
        val currentLac = cell.lac
        if (currentLac != null && lastLac != null && currentLac != lastLac) {
            alerts.add(
                0,
                ImsiCatcherAlert(
                    type = AlertType.LAC_CHANGE,
                    description = "LAC changed from $lastLac to $currentLac without movement",
                    severity = AlertSeverity.MEDIUM
                )
            )
        }
        lastLac = currentLac

        // Signal spike detection
        val currentRssi = cell.rssi
        if (lastRssi != null) {
            val diff = currentRssi - lastRssi!!
            if (diff > 20) {
                alerts.add(
                    0,
                    ImsiCatcherAlert(
                        type = AlertType.SIGNAL_SPIKE,
                        description = "Signal jumped +${diff}dB (${lastRssi}→${currentRssi})",
                        severity = AlertSeverity.MEDIUM
                    )
                )
            }
        }
        lastRssi = currentRssi

        // 2G downgrade detection
        if (lastCellType != null && lastCellType != CellType.GSM && cell.type == CellType.GSM) {
            alerts.add(
                0,
                ImsiCatcherAlert(
                    type = AlertType.FORCED_2G_DOWNGRADE,
                    description = "Forced downgrade from ${lastCellType?.displayName} to 2G GSM",
                    severity = AlertSeverity.HIGH
                )
            )
        }
        lastCellType = cell.type

        // Keep only last 50 alerts
        while (alerts.size > 50) alerts.removeLast()
    }

    private fun CellInfo.toCellTowerInfo(): CellTowerInfo? = when (this) {
        is CellInfoLte -> CellTowerInfo(
            type = CellType.LTE,
            mcc = cellIdentity.mccString?.toIntOrNull(),
            mnc = cellIdentity.mncString?.toIntOrNull(),
            lac = cellIdentity.tac,
            cid = cellIdentity.ci.toLong(),
            rssi = cellSignalStrength.rsrp,
            arfcn = cellIdentity.earfcn,
            isRegistered = isRegistered
        )
        is CellInfoGsm -> CellTowerInfo(
            type = CellType.GSM,
            mcc = cellIdentity.mccString?.toIntOrNull(),
            mnc = cellIdentity.mncString?.toIntOrNull(),
            lac = cellIdentity.lac,
            cid = cellIdentity.cid.toLong(),
            rssi = cellSignalStrength.dbm,
            arfcn = cellIdentity.arfcn,
            isRegistered = isRegistered
        )
        is CellInfoWcdma -> CellTowerInfo(
            type = CellType.WCDMA,
            mcc = cellIdentity.mccString?.toIntOrNull(),
            mnc = cellIdentity.mncString?.toIntOrNull(),
            lac = cellIdentity.lac,
            cid = cellIdentity.cid.toLong(),
            rssi = cellSignalStrength.dbm,
            arfcn = cellIdentity.uarfcn,
            isRegistered = isRegistered
        )
        is CellInfoCdma -> CellTowerInfo(
            type = CellType.CDMA,
            mcc = null,
            mnc = cellIdentity.systemId,
            lac = cellIdentity.networkId,
            cid = cellIdentity.basestationId.toLong(),
            rssi = cellSignalStrength.dbm,
            arfcn = null,
            isRegistered = isRegistered
        )
        else -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                when (this) {
                    is CellInfoNr -> {
                        val id = cellIdentity as android.telephony.CellIdentityNr
                        val ss = cellSignalStrength as android.telephony.CellSignalStrengthNr
                        CellTowerInfo(
                            type = CellType.NR,
                            mcc = id.mccString?.toIntOrNull(),
                            mnc = id.mncString?.toIntOrNull(),
                            lac = id.tac,
                            cid = id.nci,
                            rssi = ss.ssRsrp,
                            arfcn = id.nrarfcn,
                            isRegistered = isRegistered
                        )
                    }
                    is CellInfoTdscdma -> CellTowerInfo(
                        type = CellType.TDSCDMA,
                        mcc = cellIdentity.mccString?.toIntOrNull(),
                        mnc = cellIdentity.mncString?.toIntOrNull(),
                        lac = cellIdentity.lac,
                        cid = cellIdentity.cid.toLong(),
                        rssi = cellSignalStrength.dbm,
                        arfcn = cellIdentity.uarfcn,
                        isRegistered = isRegistered
                    )
                    else -> null
                }
            } else null
        }
    }
}
