package com.aatorque.stats
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.aatorque.datastore.Display
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class TorqueRefresher {
    val data = HashMap<Int, TorqueData>()
    private val executor = ScheduledThreadPoolExecutor(7)
    val handler = Handler(Looper.getMainLooper())
    var lastConnectStatus: Boolean? = null
    var conWatcher: ((Boolean) -> Unit)? = null

    companion object {
        const val TAG = "TorqueRefresher"
        const val REFRESH_INTERVAL = 300L
    }

    fun populateQuery(pos: Int, query: Display): TorqueData {
        data[pos]?.stopRefreshing(true)
        val td = TorqueData(query)
        data[pos] = td
        Log.d(TAG, "Setting query: $query for pos $pos")
        return td
    }

    fun makeExecutors(service: TorqueService) {
        data.values.forEachIndexed { index, torqueData ->
            val refreshOffset = (REFRESH_INTERVAL / data.size) * index
            if (torqueData.pid != null && torqueData.refreshTimer == null) {
                Log.d(TAG, "Scheduled item in position $index with $refreshOffset delay")
                doRefresh(service, torqueData)
                torqueData.refreshTimer = executor.scheduleWithFixedDelay({
                    try {
                        doRefresh(service, torqueData)
                    } catch (e: Exception) {
                        Log.e(TAG, "Refresh failed in pos $index", e)
                    }
                }, refreshOffset, REFRESH_INTERVAL, TimeUnit.MILLISECONDS)
            } else {
                Log.i(TAG, "No reason to schedule item in position $index")
            }
        }
    }

    fun doRefresh(service: TorqueService, torqueData: TorqueData) {
        service.runIfConnected { ts ->
            val value = ts.getPIDValuesAsDouble(arrayOf( torqueData.pid!!))[0]
            torqueData.lastData = value
            Log.d(TAG, "Got valid $value from torque for ${torqueData.display.label}")
            if (value != 0.0 || torqueData.hasReceivedNonZero) {
                torqueData.hasReceivedNonZero = true
                handler.post {
                    torqueData.sendNotifyUpdate()
                    if (value != 0.0 && lastConnectStatus != true) {
                        lastConnectStatus = true
                        conWatcher?.let { it(true) }
                    }
                }
            }
        }
    }

    fun stopExecutors() {
        Log.i(TAG, "Telling Torque refreshers to stop")
        for (td in data.values) {
            td.stopRefreshing()
        }
    }

    fun hasChanged(idx: Int, otherScreen: Display?): Boolean {
        if (!data.containsKey(idx)) return true
        return data[idx]?.display?.equals(otherScreen) != true
    }

    fun watchConnection(service: TorqueService, notifyConState: (connected: Boolean?) -> Unit) {
        notifyConState(lastConnectStatus)
        service.addConnectCallback {
            if (lastConnectStatus == null) {
                lastConnectStatus = null
                notifyConState(false)
            }
            conWatcher = notifyConState
        }
    }

}