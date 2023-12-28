package com.aatorque.stats
import android.os.Handler
import android.os.Looper
import com.aatorque.datastore.Display
import timber.log.Timber
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

enum class ConnectStatus {
    CONNECTING_TORQUE, CONNECTING_ECU, CONNECTED, SETUP_GAUGE
}

typealias ConStatusFn = ((ConnectStatus) -> Unit)

class TorqueRefresher {
    val data = HashMap<Int, TorqueData>()
    private val executor = ScheduledThreadPoolExecutor(7)
    val handler = Handler(Looper.getMainLooper())
    var lastConnectStatus = ConnectStatus.CONNECTING_TORQUE
    var conWatcher: ConStatusFn? = null
    val cache = mutableMapOf<Int, HashMap<Int, TorqueData>>()

    companion object {
        const val REFRESH_INTERVAL = 300L
    }


    fun populateQuery(pos: Int, screen: Int, query: Display): TorqueData {
        data[pos]?.stopRefreshing(true)
        val cacheItem = cache[screen]?.get(pos)
        val torqueData = if (cacheItem?.display?.equals(query) == true) {
            cacheItem
        } else {
            TorqueData(query)
        }
        cache.getOrPut(screen) {
            HashMap()
        }[pos] = torqueData
        data[pos] = torqueData
        Timber.i("Setting query: $query for pos $pos on screen $screen")
        return torqueData
    }

    fun makeExecutors(service: TorqueService) {
        var foundValid = false
        data.values.forEachIndexed { index, torqueData ->
            val refreshOffset = (REFRESH_INTERVAL / data.size) * index
            if (torqueData.pid != null) {
                foundValid = true
                if (torqueData.refreshTimer == null) {
                    Timber.i("Scheduled item in position $index with $refreshOffset delay")
                    doRefresh(service, torqueData)
                    torqueData.refreshTimer = executor.scheduleWithFixedDelay({
                        try {
                            doRefresh(service, torqueData)
                        } catch (e: Exception) {
                            Timber.e("Refresh failed in pos $index", e)
                        }
                    }, refreshOffset, REFRESH_INTERVAL, TimeUnit.MILLISECONDS)
                }
            } else {
                Timber.i("No reason to schedule item in position $index")
            }
        }
        conWatcher?.invoke(if (foundValid) lastConnectStatus else ConnectStatus.SETUP_GAUGE)
    }

    fun doRefresh(service: TorqueService, torqueData: TorqueData) {
        service.runIfConnected { ts ->
            if (torqueData.repeatCounter.flushIfOver(30)) {
                Timber.i("Got repeated data, attempting to use old api for hard refresh on ${torqueData.display.label}")
                torqueData.pidLong?.let {
                    // Use old API to force refresh if value is stuck
                    @Suppress("DEPRECATION")
                    ts.getValueForPid(it, true)
                }
            }
            val value = try {
                ts.getPIDValuesAsDouble(arrayOf(torqueData.pid!!))[0]
            } catch (e: ArrayIndexOutOfBoundsException) {
                Timber.e("Torque returned invalid data for ${torqueData.display.label}")
                return@runIfConnected
            }
            torqueData.lastData = value
            Timber.d("Got valid $value from torque for ${torqueData.display.label}")
            if (value != 0.0 || torqueData.hasReceivedNonZero) {
                handler.post {
                    torqueData.sendNotifyUpdate()
                    if (value != 0.0 && lastConnectStatus != ConnectStatus.CONNECTED) {
                        lastConnectStatus = ConnectStatus.CONNECTED
                        conWatcher?.let { it(ConnectStatus.CONNECTED) }
                    }
                }
            }
        }
    }

    fun stopExecutors() {
        Timber.i("Telling Torque refreshers to stop")
        for (td in data.values) {
            td.stopRefreshing()
        }
    }

    fun hasChanged(idx: Int, otherScreen: Display?): Boolean {
        if (!data.containsKey(idx)) return true
        return data[idx]?.display?.equals(otherScreen) != true
    }

    fun updateIfNeeded(pos: Int, screen: Int, query: Display): TorqueData {
        return if (hasChanged(pos, query)) {
            populateQuery(pos, screen, query)
        } else {
            data[pos]!!
        }
    }

    fun watchConnection(service: TorqueService, notifyConState: ConStatusFn) {
        notifyConState(lastConnectStatus)
        service.addConnectCallback {
            if (lastConnectStatus == ConnectStatus.CONNECTING_TORQUE) {
                lastConnectStatus = ConnectStatus.CONNECTING_ECU
                notifyConState(lastConnectStatus)
            }
            conWatcher = notifyConState
        }
    }

}
