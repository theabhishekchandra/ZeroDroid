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
    private companion object {
        /** Same value as [UNAVAILABLE] (API 29); inlined so minSdk 26 builds read it too. */
        const val UNAVAILABLE = Int.MAX_VALUE
    }

    private var lastLac: Int? = null
    private var lastRssi: Int? = null
    private var lastCellType: CellType? = null
    private val alerts = mutableListOf<ImsiCatcherAlert>()

    @SuppressLint("MissingPermission")
    fun monitor(): Flow<CellTowerState> = flow {
        // This analyzer is a singleton, so a fresh monitoring session must not compare against a
        // baseline left over from a previous Stop/Start — that would flag an ordinary cell change
        // after being away as a false LAC-change/signal-spike/downgrade "attack" alert.
        lastLac = null
        lastRssi = null
        lastCellType = null
        alerts.clear()

        while (true) {
            // Build the state inside the try, but emit outside it: catching around emit() would
            // swallow the collector's cancellation (e.g. Stop, or a first()) and then emit into a
            // cancelled flow, which the Flow contract rejects.
            val state = try {
                val cellInfos = telephonyManager?.allCellInfo ?: emptyList()
                val towers = cellInfos.mapNotNull { it.toCellTowerInfo() }
                val current = towers.firstOrNull { it.isRegistered }
                val neighbors = towers.filter { !it.isRegistered }

                current?.let { checkImsiHeuristics(it) }

                CellTowerState(
                    currentCell = current,
                    neighbors = neighbors,
                    alerts = alerts.toList(),
                    isMonitoring = true,
                    simAbsent = telephonyManager?.simState == TelephonyManager.SIM_STATE_ABSENT
                )
            } catch (e: SecurityException) {
                CellTowerState(error = "Permission denied: ${e.message}")
            } catch (e: Exception) {
                CellTowerState(error = e.message)
            }
            emit(state)
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
        while (alerts.size > 50) alerts.removeAt(alerts.lastIndex)
    }

    // 3GPP reserves the all-ones value of a field's bit width to mean "unavailable" (65535 for
    // 16-bit LAC/TAC, 268435455 for 28-bit CID/CI) — neighbor cells frequently report these
    // placeholders rather than a real identity, so they must be filtered like MCC/MNC already are,
    // or the UI ends up displaying a fake-looking tower ID that was never actually broadcast.
    private fun Int.orNullIfUnavailable(): Int? =
        takeIf { it != UNAVAILABLE && it != 65535 && it != 268435455 }

    private fun Long.orNullIfUnavailable(): Long? =
        takeIf { it != UNAVAILABLE.toLong() && it != 65535L && it != 268435455L }

    private fun Int.taOrNull(): Int? = takeIf { it != UNAVAILABLE && it in 0..1282 }

    // Distance from Timing Advance: TA is a round-trip-time unit specific to each radio generation
    // (LTE: 16*Ts ≈ 78.12m/step, GSM: one bit period ≈ 553.85m/step) — a real security signal, since
    // a rogue/portable tower sitting unusually close often shows an implausibly small TA.
    private fun lteTaToMeters(ta: Int): Int = (ta * 78.12).toInt()
    private fun gsmTaToMeters(ta: Int): Int = (ta * 553.85).toInt()

    @Suppress("DEPRECATION")
    private fun CellInfo.toCellTowerInfo(): CellTowerInfo? = when (this) {
        is CellInfoLte -> {
            val mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                cellIdentity.mccString?.toIntOrNull()
            } else {
                cellIdentity.mcc.takeIf { it != UNAVAILABLE }
            }
            val mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                cellIdentity.mncString?.toIntOrNull()
            } else {
                cellIdentity.mnc.takeIf { it != UNAVAILABLE }
            }
            CellTowerInfo(
                type = CellType.LTE,
                mcc = mcc,
                mnc = mnc,
                carrierName = CarrierLookup.lookup(mcc, mnc),
                lac = cellIdentity.tac.orNullIfUnavailable(),
                cid = cellIdentity.ci.toLong().orNullIfUnavailable(),
                rssi = cellSignalStrength.rsrp,
                arfcn = cellIdentity.earfcn,
                isRegistered = isRegistered,
                pci = cellIdentity.pci.takeIf { it != UNAVAILABLE },
                rsrq = cellSignalStrength.rsrq.takeIf { it != UNAVAILABLE },
                snr = cellSignalStrength.rssnr.takeIf { it != UNAVAILABLE },
                timingAdvance = cellSignalStrength.timingAdvance.taOrNull(),
                distanceMeters = cellSignalStrength.timingAdvance.taOrNull()?.let { lteTaToMeters(it) },
                bandwidthKhz = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    cellIdentity.bandwidth.takeIf { it != UNAVAILABLE }
                } else {
                    null
                }
            )
        }
        is CellInfoGsm -> {
            val mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                cellIdentity.mccString?.toIntOrNull()
            } else {
                cellIdentity.mcc.takeIf { it != UNAVAILABLE }
            }
            val mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                cellIdentity.mncString?.toIntOrNull()
            } else {
                cellIdentity.mnc.takeIf { it != UNAVAILABLE }
            }
            CellTowerInfo(
                type = CellType.GSM,
                mcc = mcc,
                mnc = mnc,
                carrierName = CarrierLookup.lookup(mcc, mnc),
                lac = cellIdentity.lac.orNullIfUnavailable(),
                cid = cellIdentity.cid.toLong().orNullIfUnavailable(),
                rssi = cellSignalStrength.dbm,
                arfcn = cellIdentity.arfcn,
                isRegistered = isRegistered,
                timingAdvance = cellSignalStrength.timingAdvance.taOrNull(),
                distanceMeters = cellSignalStrength.timingAdvance.taOrNull()?.let { gsmTaToMeters(it) }
            )
        }
        is CellInfoWcdma -> {
            val mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                cellIdentity.mccString?.toIntOrNull()
            } else {
                cellIdentity.mcc.takeIf { it != UNAVAILABLE }
            }
            val mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                cellIdentity.mncString?.toIntOrNull()
            } else {
                cellIdentity.mnc.takeIf { it != UNAVAILABLE }
            }
            CellTowerInfo(
                type = CellType.WCDMA,
                mcc = mcc,
                mnc = mnc,
                carrierName = CarrierLookup.lookup(mcc, mnc),
                lac = cellIdentity.lac.orNullIfUnavailable(),
                cid = cellIdentity.cid.toLong().orNullIfUnavailable(),
                rssi = cellSignalStrength.dbm,
                arfcn = cellIdentity.uarfcn,
                isRegistered = isRegistered,
                pci = cellIdentity.psc.takeIf { it != UNAVAILABLE }
            )
        }
        is CellInfoCdma -> CellTowerInfo(
            type = CellType.CDMA,
            mcc = null,
            mnc = cellIdentity.systemId,
            carrierName = CarrierLookup.lookup(null, cellIdentity.systemId),
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
                        val mcc = id.mccString?.toIntOrNull()
                        val mnc = id.mncString?.toIntOrNull()
                        CellTowerInfo(
                            type = CellType.NR,
                            mcc = mcc,
                            mnc = mnc,
                            carrierName = CarrierLookup.lookup(mcc, mnc),
                            lac = id.tac,
                            cid = id.nci,
                            rssi = ss.ssRsrp,
                            arfcn = id.nrarfcn,
                            isRegistered = isRegistered,
                            pci = id.pci.takeIf { it != UNAVAILABLE },
                            rsrq = ss.ssRsrq.takeIf { it != UNAVAILABLE },
                            snr = ss.ssSinr.takeIf { it != UNAVAILABLE }
                        )
                    }
                    is CellInfoTdscdma -> {
                        val mcc = cellIdentity.mccString?.toIntOrNull()
                        val mnc = cellIdentity.mncString?.toIntOrNull()
                        CellTowerInfo(
                            type = CellType.TDSCDMA,
                            mcc = mcc,
                            mnc = mnc,
                            carrierName = CarrierLookup.lookup(mcc, mnc),
                            lac = cellIdentity.lac.orNullIfUnavailable(),
                            cid = cellIdentity.cid.toLong().orNullIfUnavailable(),
                            rssi = cellSignalStrength.dbm,
                            arfcn = cellIdentity.uarfcn,
                            isRegistered = isRegistered
                        )
                    }
                    else -> null
                }
            } else null
        }
    }
}
