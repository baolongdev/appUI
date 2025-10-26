package com.example.appui.core.audio.session

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager as SystemAudioManager
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

class AudioSessionManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as SystemAudioManager

    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var dynamicsProcessor: DynamicsProcessing? = null
    private var currentBoostDb: Int = DEFAULT_BOOST_DB
    private var preferSpeaker = false

    fun enterVoiceSession(maxBoostDb: Int = 24, preferSpeakerForMax: Boolean = false) {
        preferSpeaker = preferSpeakerForMax
        currentBoostDb = maxBoostDb.coerceIn(MIN_BOOST_DB, MAX_BOOST_DB)

        Log.i(TAG, "🔊 Entering voice session (boost=${currentBoostDb}dB, preferSpeaker=$preferSpeaker)")

        runCatching {
            audioManager.mode = SystemAudioManager.MODE_IN_COMMUNICATION
            requestAudioFocus(AudioAttributes.USAGE_VOICE_COMMUNICATION, AudioAttributes.CONTENT_TYPE_SPEECH)
            routeForVoiceCall(maximize = true, preferSpeaker = preferSpeaker)
            logVolumeState("enterVoiceSession")
        }.onFailure { e ->
            Log.e(TAG, "Failed to enter voice session", e)
        }
    }

    fun enterMediaLoudSession(maxBoostDb: Int = 24, preferSpeakerForMax: Boolean = false) {
        preferSpeaker = preferSpeakerForMax
        currentBoostDb = maxBoostDb.coerceIn(MIN_BOOST_DB, MAX_BOOST_DB)

        Log.i(TAG, "🔊 Entering media loud session (boost=${currentBoostDb}dB)")

        runCatching {
            // Keep MODE_IN_COMMUNICATION to maintain routing
            requestAudioFocus(AudioAttributes.USAGE_MEDIA, AudioAttributes.CONTENT_TYPE_MUSIC)

            // Re-apply boosters with new gain
            loudnessEnhancer?.apply {
                setTargetGain(currentBoostDb * 100)
                if (!enabled) enabled = true
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dynamicsProcessor?.let { processor ->
                    val channel = processor.getChannelByChannelIndex(0)
                    val limiter = channel.limiter

                    limiter.apply {
                        setEnabled(true)
                        attackTime = 0.5f
                        releaseTime = 30f
                        ratio = 20f
                        threshold = -2f
                        postGain = currentBoostDb.toFloat()
                    }

                    channel.limiter = limiter
                    processor.setChannelTo(0, channel)
                }
            }

            setStreamsToMaxVolume(SystemAudioManager.STREAM_MUSIC, SystemAudioManager.STREAM_VOICE_CALL)

            logVolumeState("enterMediaLoudSession")
        }.onFailure { e ->
            Log.e(TAG, "Failed to enter media loud session", e)
        }
    }

    fun exitVoiceSession() {
        Log.i(TAG, "Exiting audio session")

        runCatching {
            abandonAudioFocus()
            audioManager.mode = SystemAudioManager.MODE_NORMAL
            clearDeviceRouting()
            disableAudioBoosters()
        }.onFailure { e ->
            Log.e(TAG, "Failed to exit session", e)
        }
    }

    fun setBoostGainDb(db: Int) {
        currentBoostDb = db.coerceIn(MIN_BOOST_DB, MAX_BOOST_DB)

        runCatching {
            loudnessEnhancer?.setTargetGain(currentBoostDb * 100)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                updateDynamicsProcessingGain()
            }

            Log.d(TAG, "Boost gain updated to ${currentBoostDb}dB")
        }.onFailure { e ->
            Log.w(TAG, "Failed to update boost gain", e)
        }
    }

    // ==================== Audio routing ====================

    @Suppress("DEPRECATION")
    private fun routeForVoiceCall(maximize: Boolean, preferSpeaker: Boolean) {
        audioManager.mode = SystemAudioManager.MODE_IN_COMMUNICATION

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            routeForVoiceCallModern(preferSpeaker)
        } else {
            routeForVoiceCallLegacy(preferSpeaker)
        }

        if (maximize) {
            setStreamsToMaxVolume(SystemAudioManager.STREAM_VOICE_CALL, SystemAudioManager.STREAM_MUSIC)
            enableAudioBoosters()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun routeForVoiceCallModern(preferSpeaker: Boolean) {
        val targetDevice = if (preferSpeaker) {
            audioManager.availableCommunicationDevices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            }
        } else {
            findPreferredCommunicationDevice()
        }

        targetDevice?.let {
            val success = audioManager.setCommunicationDevice(it)
            Log.d(TAG, "setCommunicationDevice(${getDeviceTypeName(it.type)}): $success")
        } ?: run {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = true
            Log.d(TAG, "Fallback: isSpeakerphoneOn=true")
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun findPreferredCommunicationDevice(): AudioDeviceInfo? {
        return audioManager.availableCommunicationDevices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
        } ?: audioManager.availableCommunicationDevices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        }
    }

    @Suppress("DEPRECATION")
    private fun routeForVoiceCallLegacy(preferSpeaker: Boolean) {
        if (preferSpeaker) {
            audioManager.isSpeakerphoneOn = true
            runCatching {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
            }
            Log.d(TAG, "Legacy: Forced speaker (isSpeakerphoneOn=true)")
        } else {
            // ✅ FIXED: Try BT first, fallback to speaker
            if (audioManager.isBluetoothScoAvailableOffCall) {
                audioManager.startBluetoothSco()
                audioManager.isBluetoothScoOn = true
                Log.d(TAG, "Legacy: BT SCO enabled")
            } else {
                audioManager.isSpeakerphoneOn = true
                Log.d(TAG, "Legacy: Fallback to speaker")
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun routeForMedia(maximize: Boolean, preferSpeaker: Boolean) {
        // ✅ FIXED: Keep MODE_IN_COMMUNICATION to maintain routing
        // Don't switch to MODE_NORMAL as it may reset device routing

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Device already set by routeForVoiceCall, don't change
            Log.d(TAG, "Media: Keeping current device routing")
        } else {
            // For legacy, keep current speaker/BT setting
            Log.d(TAG, "Media: Keeping legacy routing")
        }

        if (maximize) {
            setStreamsToMaxVolume(SystemAudioManager.STREAM_MUSIC, SystemAudioManager.STREAM_VOICE_CALL)

            // Re-enable boosters if needed
            if (loudnessEnhancer == null || dynamicsProcessor == null) {
                enableAudioBoosters()
            }
        }
    }

    private fun clearDeviceRouting() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        } else {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = false
            runCatching {
                @Suppress("DEPRECATION")
                audioManager.stopBluetoothSco()
                @Suppress("DEPRECATION")
                audioManager.isBluetoothScoOn = false
            }
        }
    }

    // ==================== Audio focus management ====================

    private fun requestAudioFocus(usage: Int, contentType: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requestAudioFocusModern(usage, contentType)
        } else {
            requestAudioFocusLegacy(usage)
        }

        Log.d(TAG, "Audio focus granted: $hasAudioFocus")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun requestAudioFocusModern(usage: Int, contentType: Int) {
        val attributes = AudioAttributes.Builder()
            .setUsage(usage)
            .setContentType(contentType)
            .build()

        audioFocusRequest = AudioFocusRequest.Builder(SystemAudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener { focusChange ->
                handleAudioFocusChange(focusChange)
            }
            .build()

        hasAudioFocus = audioManager.requestAudioFocus(audioFocusRequest!!) ==
                SystemAudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    @Suppress("DEPRECATION")
    private fun requestAudioFocusLegacy(usage: Int) {
        val stream = if (usage == AudioAttributes.USAGE_VOICE_COMMUNICATION)
            SystemAudioManager.STREAM_VOICE_CALL
        else
            SystemAudioManager.STREAM_MUSIC

        hasAudioFocus = audioManager.requestAudioFocus(
            { focusChange -> handleAudioFocusChange(focusChange) },
            stream,
            SystemAudioManager.AUDIOFOCUS_GAIN
        ) == SystemAudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            SystemAudioManager.AUDIOFOCUS_GAIN,
            SystemAudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                restoreMaxVolume("focus=$focusChange")
            }
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
        hasAudioFocus = false
    }

    // ==================== Audio enhancement ====================

    private fun enableAudioBoosters() {
        enableLoudnessEnhancer()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            enableDynamicsProcessing()
        }
    }

    private fun disableAudioBoosters() {
        disableLoudnessEnhancer()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            disableDynamicsProcessing()
        }
    }

    private fun enableLoudnessEnhancer() {
        runCatching {
            if (loudnessEnhancer == null) {
                loudnessEnhancer = LoudnessEnhancer(0)
            }
            loudnessEnhancer?.apply {
                setTargetGain(currentBoostDb * 100)
                enabled = true
            }
            Log.d(TAG, "✅ LoudnessEnhancer enabled @ ${currentBoostDb}dB")
        }.onFailure { e ->
            Log.w(TAG, "Failed to enable LoudnessEnhancer", e)
        }
    }

    private fun disableLoudnessEnhancer() {
        runCatching {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
            loudnessEnhancer = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun enableDynamicsProcessing() {
        runCatching {
            if (dynamicsProcessor == null) {
                val config = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    1, false, 0, false, 0, false, 0, true
                ).build()

                dynamicsProcessor = DynamicsProcessing(0, 0, config)
            }

            dynamicsProcessor?.apply {
                setEnabled(true)

                val channel = getChannelByChannelIndex(0)
                val limiter = channel.limiter

                limiter.apply {
                    setEnabled(true)
                    attackTime = 0.5f
                    releaseTime = 30f
                    ratio = 20f
                    threshold = -2f
                    postGain = currentBoostDb.toFloat()
                }

                channel.limiter = limiter
                setChannelTo(0, channel)
            }

            Log.d(TAG, "✅ DynamicsProcessing limiter enabled")
        }.onFailure { e ->
            Log.w(TAG, "Failed to enable DynamicsProcessing", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun disableDynamicsProcessing() {
        runCatching {
            dynamicsProcessor?.setEnabled(false)
            dynamicsProcessor?.release()
            dynamicsProcessor = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun updateDynamicsProcessingGain() {}

    // ==================== Volume management ====================

    private fun setStreamsToMaxVolume(vararg streams: Int) {
        streams.forEach { stream ->
            val maxVolume = audioManager.getStreamMaxVolume(stream)
            audioManager.setStreamVolume(stream, maxVolume, 0)
        }
    }

    private fun restoreMaxVolume(tag: String) {
        setStreamsToMaxVolume(SystemAudioManager.STREAM_VOICE_CALL, SystemAudioManager.STREAM_MUSIC)
        logVolumeState("restore@$tag")
    }

    // ==================== Utilities ====================

    private fun logVolumeState(phase: String) {
        val voiceVol = audioManager.getStreamVolume(SystemAudioManager.STREAM_VOICE_CALL)
        val voiceMax = audioManager.getStreamMaxVolume(SystemAudioManager.STREAM_VOICE_CALL)
        val musicVol = audioManager.getStreamVolume(SystemAudioManager.STREAM_MUSIC)
        val musicMax = audioManager.getStreamMaxVolume(SystemAudioManager.STREAM_MUSIC)

        Log.d(TAG, "$phase: mode=${audioManager.mode}, voice=$voiceVol/$voiceMax, " +
                "music=$musicVol/$musicMax, preferSpeaker=$preferSpeaker")
    }

    private fun getDeviceTypeName(type: Int): String = when (type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "BT_A2DP"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "BT_SCO"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "USB_HEADSET"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "WIRED_HEADSET"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "SPEAKER"
        else -> "TYPE_$type"
    }

    companion object {
        private const val TAG = "AudioSessionManager"
        private const val MIN_BOOST_DB = 0
        private const val MAX_BOOST_DB = 22
        private const val DEFAULT_BOOST_DB = 22
    }
}
