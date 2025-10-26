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

/**
 * Professional audio session manager for voice conversations.
 *
 * Features:
 * - Intelligent device routing (BT/USB/Wired/Speaker)
 * - Mixed mode: BT A2DP (output) + Built-in mic (input)
 * - Audio focus management with ducking prevention
 * - Dynamic audio enhancement (LoudnessEnhancer + DynamicsProcessing)
 * - Automatic volume optimization
 * - Mode-aware session management
 *
 * @property context Application context for audio services
 */
class AudioSessionManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as SystemAudioManager

    // Audio focus management
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    // Audio enhancement
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var dynamicsProcessor: DynamicsProcessing? = null
    private var currentBoostDb: Int = DEFAULT_BOOST_DB

    // Session state
    private var preferSpeaker = false

    /**
     * Enters voice conversation mode with microphone support.
     *
     * Configures:
     * - MODE_IN_COMMUNICATION for echo cancellation
     * - Voice routing priority: BT SCO → BT A2DP + Built-in mic → USB → Wired → Speaker
     * - Maximum voice call and music stream volumes
     * - Audio enhancement boosters
     *
     * @param maxBoostDb Loudness boost in dB (0-24)
     * @param preferSpeakerForMax Prefer built-in speaker for maximum volume
     */
    fun enterVoiceSession(maxBoostDb: Int = 18, preferSpeakerForMax: Boolean = false) {
        preferSpeaker = preferSpeakerForMax
        currentBoostDb = maxBoostDb.coerceIn(MIN_BOOST_DB, MAX_BOOST_DB)

        Log.i(TAG, "Entering voice session (boost=${currentBoostDb}dB, preferSpeaker=$preferSpeaker)")

        runCatching {
            audioManager.mode = SystemAudioManager.MODE_IN_COMMUNICATION
            requestAudioFocus(AudioAttributes.USAGE_VOICE_COMMUNICATION, AudioAttributes.CONTENT_TYPE_SPEECH)
            routeForVoiceCall(maximize = true, preferSpeaker = preferSpeaker)
            logVolumeState("enterVoiceSession")
        }.onFailure { e ->
            Log.e(TAG, "Failed to enter voice session", e)
        }
    }

    /**
     * Enters mixed mode: Bluetooth A2DP for audio output + Built-in mic for input.
     * Use this for Bluetooth speakers without microphone.
     *
     * @param maxBoostDb Loudness boost in dB (0-24)
     */
    fun enterMixedBluetoothMode(maxBoostDb: Int = 18) {
        currentBoostDb = maxBoostDb.coerceIn(MIN_BOOST_DB, MAX_BOOST_DB)

        Log.i(TAG, "Entering mixed BT mode (A2DP output + Built-in mic, boost=${currentBoostDb}dB)")

        runCatching {
            // ✅ Use MODE_IN_COMMUNICATION to keep built-in mic active
            audioManager.mode = SystemAudioManager.MODE_IN_COMMUNICATION

            requestAudioFocus(AudioAttributes.USAGE_VOICE_COMMUNICATION, AudioAttributes.CONTENT_TYPE_SPEECH)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                routeForMixedMode()
            } else {
                routeForMixedModeLegacy()
            }

            setStreamsToMaxVolume(SystemAudioManager.STREAM_MUSIC, SystemAudioManager.STREAM_VOICE_CALL)
            enableAudioBoosters()

            logVolumeState("enterMixedBluetoothMode")
        }.onFailure { e ->
            Log.e(TAG, "Failed to enter mixed BT mode", e)
        }
    }

    /**
     * Exits current audio session and restores defaults.
     */
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

    /**
     * Adjusts boost gain dynamically during session.
     *
     * @param db Boost level in dB (0-24)
     */
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

    // ==================== Audio Routing ====================

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
        val devices = audioManager.availableCommunicationDevices
        Log.d(TAG, "Available devices: ${devices.map { "${getDeviceTypeName(it.type)} (${it.productName})" }}")

        val targetDevice = if (preferSpeaker) {
            audioManager.availableCommunicationDevices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            }
        } else {
            findPreferredCommunicationDevice()
        }

        targetDevice?.let { device ->
            val success = audioManager.setCommunicationDevice(device)
            Log.d(TAG, "setCommunicationDevice(${getDeviceTypeName(device.type)}): $success")

            val currentDevice = audioManager.communicationDevice
            Log.d(TAG, "Current device after set: ${currentDevice?.let { getDeviceTypeName(it.type) } ?: "null"}")
        } ?: run {
            audioManager.isSpeakerphoneOn = true
            Log.w(TAG, "No device found, defaulted to speakerphone")
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun findPreferredCommunicationDevice(): AudioDeviceInfo? {
        // ✅ Priority 1: Bluetooth SCO (headset with mic)
        val btScoDevice = audioManager.availableCommunicationDevices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        }
        if (btScoDevice != null) {
            Log.d(TAG, "Found BT SCO device (with mic)")
            return btScoDevice
        }

        // ✅ Priority 2: Bluetooth A2DP (speaker without mic) - will use built-in mic
        val btA2dpDevice = audioManager.availableCommunicationDevices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
        }
        if (btA2dpDevice != null) {
            Log.d(TAG, "Found BT A2DP device (music only, will use built-in mic)")
            return btA2dpDevice
        }

        // ✅ Priority 3: USB Headset (with mic)
        val usbDevice = audioManager.availableCommunicationDevices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_USB_DEVICE
        }
        if (usbDevice != null) {
            Log.d(TAG, "Found USB device")
            return usbDevice
        }

        // ✅ Priority 4: Wired Headset (with mic)
        val wiredDevice = audioManager.availableCommunicationDevices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
        }
        if (wiredDevice != null) {
            Log.d(TAG, "Found Wired headset")
            return wiredDevice
        }

        // ✅ Priority 5: Built-in speaker + mic
        Log.d(TAG, "No external device, using built-in")
        return audioManager.availableCommunicationDevices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun routeForMixedMode() {
        // ✅ Find Bluetooth A2DP for audio output
        val btA2dpDevice = audioManager.availableCommunicationDevices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
        }

        if (btA2dpDevice != null) {
            // Route audio output to Bluetooth speaker
            val success = audioManager.setCommunicationDevice(btA2dpDevice)
            Log.d(TAG, "Mixed mode: BT A2DP for output ($success), built-in mic for input")

            // Verify
            val currentDevice = audioManager.communicationDevice
            Log.d(TAG, "Current device: ${currentDevice?.let { getDeviceTypeName(it.type) } ?: "null"}")
        } else {
            // Fallback to speaker if no BT A2DP
            val speaker = audioManager.availableCommunicationDevices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            }
            speaker?.let { audioManager.setCommunicationDevice(it) }
            Log.d(TAG, "Mixed mode: No BT A2DP, using built-in speaker + mic")
        }

        // ✅ Mic will automatically use built-in since we're in MODE_IN_COMMUNICATION
    }

    @Suppress("DEPRECATION")
    private fun routeForVoiceCallLegacy(preferSpeaker: Boolean) {
        if (preferSpeaker) {
            audioManager.stopBluetoothSco()
            audioManager.isBluetoothScoOn = false
            audioManager.isSpeakerphoneOn = true
            Log.d(TAG, "Legacy: Force speakerphone ON")
        } else {
            // Check BT availability
            if (audioManager.isBluetoothScoAvailableOffCall) {
                audioManager.isSpeakerphoneOn = false
                audioManager.isBluetoothScoOn = true
                audioManager.startBluetoothSco()
                Log.d(TAG, "Legacy: BT SCO started")

                // Wait for BT connection
                Thread.sleep(500)
            } else {
                Log.w(TAG, "Legacy: BT SCO not available")
                audioManager.isSpeakerphoneOn = true
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun routeForMixedModeLegacy() {
        // Enable Bluetooth audio (for A2DP)
        audioManager.isBluetoothA2dpOn = true
        audioManager.isSpeakerphoneOn = false

        // Don't use SCO - use A2DP for audio output
        audioManager.isBluetoothScoOn = false

        Log.d(TAG, "Legacy mixed mode: A2DP ON, SCO OFF")
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

    // ==================== Audio Focus Management ====================

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

    // ==================== Audio Enhancement ====================

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
            Log.d(TAG, "LoudnessEnhancer enabled @ ${currentBoostDb}dB")
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
                    attackTime = 1f
                    releaseTime = 50f
                    ratio = 10f
                    threshold = -1f
                }

                channel.limiter = limiter
                setChannelTo(0, channel)
            }

            Log.d(TAG, "DynamicsProcessing limiter enabled")
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
    private fun updateDynamicsProcessingGain() {
        // Future: Map currentBoostDb to limiter threshold if needed
    }

    // ==================== Volume Management ====================

    private fun setStreamsToMaxVolume(vararg streams: Int) {
        streams.forEach { stream ->
            val maxVolume = audioManager.getStreamMaxVolume(stream)
            val currentVolume = audioManager.getStreamVolume(stream)

            if (currentVolume != maxVolume) {
                audioManager.setStreamVolume(stream, maxVolume, 0)
                Log.d(TAG, "Set stream $stream to max volume: $maxVolume")
            }
        }
    }

    private fun restoreMaxVolume(tag: String) {
        if (audioManager.mode == SystemAudioManager.MODE_IN_COMMUNICATION) {
            setStreamsToMaxVolume(SystemAudioManager.STREAM_VOICE_CALL, SystemAudioManager.STREAM_MUSIC)
        } else {
            setStreamsToMaxVolume(SystemAudioManager.STREAM_MUSIC)
        }
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
        AudioDeviceInfo.TYPE_BUILTIN_MIC -> "BUILTIN_MIC"
        else -> "TYPE_$type"
    }

    companion object {
        private const val TAG = "AudioSessionManager"
        private const val MIN_BOOST_DB = 0
        private const val MAX_BOOST_DB = 24
        private const val DEFAULT_BOOST_DB = 18
    }
}
