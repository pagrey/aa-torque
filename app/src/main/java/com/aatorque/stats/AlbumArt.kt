package com.aatorque.stats

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import timber.log.Timber

abstract class AlbumArt : CarFragment() {

    val registed = HashMap<Int, () -> Unit>()
    private val updateChannel = MutableSharedFlow<MediaMetadata?>()

    @OptIn(FlowPreview::class)
    override fun onStart() {
        super.onStart()
        registerMedia()
        lifecycleScope.launch {
            updateChannel.asSharedFlow()
                .debounce(300)
                .collect(this@AlbumArt::onMediaChanged)
        }
    }

    private fun registerMedia() {
        val mediaMan = ContextCompat.getSystemService(
            requireContext(),
            MediaSessionManager::class.java
        ) ?: return
        val component = ComponentName(requireContext(), NotiService::class.java)
        try {
            mediaMan.addOnActiveSessionsChangedListener({
                updateSessions(it ?: emptyList())
            }, component)
            updateSessions(mediaMan.getActiveSessions(component))
        } catch (e: SecurityException) {
            Timber.e("No permission to read media", e)
        }
    }

    override fun onStop() {
        super.onStop()
        updateSessions(emptyList())
    }

    private fun updateSessions(mediaControllers: List<MediaController>) {
        val found = mediaControllers.map {
            val token = it.sessionToken
            val code = token.hashCode()
            if (!registed.containsKey(code)) {
                val callback = object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(state: PlaybackState?) {
                        super.onPlaybackStateChanged(state)
                        Timber.i("Playback changed event ${state?.state}")
                        lifecycleScope.launch {
                            if (arrayOf(
                                    PlaybackState.STATE_PAUSED,
                                    PlaybackState.STATE_STOPPED
                                ).contains(state?.state)
                            ) {
                                updateChannel.emit(null)
                            } else if (isActive(it.playbackState)) {
                                updateChannel.emit(it.metadata)
                            }
                        }
                    }
                }
                it.registerCallback(callback)
                registed[code] = {
                    it.unregisterCallback(callback)
                    registed.remove(code)
                }
                if (isActive(it.playbackState)) {
                    lifecycleScope.launch {
                        updateChannel.emit(it.metadata)
                    }
                }
            }
            code
        }.toSet()
        registed.filterNot { found.contains(it.key) }.forEach {
            it.value()
        }
    }

    open fun isActive(state: PlaybackState?): Boolean {
        return when (state?.state) {
            PlaybackState.STATE_FAST_FORWARDING,
            PlaybackState.STATE_REWINDING,
            PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
            PlaybackState.STATE_SKIPPING_TO_NEXT,
            PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM,
            PlaybackState.STATE_BUFFERING,
            PlaybackState.STATE_CONNECTING,
            PlaybackState.STATE_PLAYING -> true

            else -> false
        }
    }

    abstract fun onMediaChanged(medadata: MediaMetadata?)
}