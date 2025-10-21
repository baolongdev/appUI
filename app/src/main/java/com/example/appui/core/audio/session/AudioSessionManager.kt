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
 * Max-loud AudioSessionManager:
 * - Route ưu tiên thiết bị ngoài (BT/USB/Wired) hoặc ép loa ngoài.
 * - MODE_IN_COMMUNICATION cho hội thoại (AEC/NS) + đặt max VOICE_CALL & MUSIC.
 * - LoudnessEnhancer (API 19+) + DynamicsProcessing (API 28+) để tăng gain có kiểm soát.
 * - Audio focus (không duck) + tự khôi phục max volume.
 * - Re-route khi thiết bị audio thay đổi.
 */
class AudioSessionManager(private val context: Context) {

    private val TAG = "AudioSessionManager"
    private val am = context.getSystemService(Context.AUDIO_SERVICE) as SystemAudioManager

    // Focus
    private var afr: AudioFocusRequest? = null
    private var hasFocus = false

    // Boosters
    private var le: LoudnessEnhancer? = null
    private var dp: DynamicsProcessing? = null
    private var boostDb: Int = 18 // 0..24 dB

    // Cache state
    private var _preferSpeakerCache = false

    /** Bắt đầu phiên ElevenLabs (có mic). */
    fun enterVoiceSession(maxBoostDb: Int = 18, preferSpeakerForMax: Boolean = false) {
        _preferSpeakerCache = preferSpeakerForMax
        boostDb = maxBoostDb.coerceIn(0, 24)
        try {
            am.mode = SystemAudioManager.MODE_IN_COMMUNICATION
            requestFocus(AudioAttributes.USAGE_VOICE_COMMUNICATION, AudioAttributes.CONTENT_TYPE_SPEECH)
            routeForVoiceCall(maximize = true, preferSpeakerForMax = preferSpeakerForMax)
            logVol("enterVoiceSession")
        } catch (e: Exception) {
            Log.w(TAG, "enterVoiceSession failed: ${e.message}")
        }
    }

    /** Chỉ phát media (không cần mic BT) – to nhất. */
    fun enterMediaLoudSession(maxBoostDb: Int = 20, preferSpeakerForMax: Boolean = true) {
        _preferSpeakerCache = preferSpeakerForMax
        boostDb = maxBoostDb.coerceIn(0, 24)
        try {
            am.mode = SystemAudioManager.MODE_NORMAL
            requestFocus(AudioAttributes.USAGE_MEDIA, AudioAttributes.CONTENT_TYPE_MUSIC)
            routeForMedia(maximize = true, preferSpeakerForMax = preferSpeakerForMax)
            logVol("enterMediaLoudSession")
        } catch (e: Exception) {
            Log.w(TAG, "enterMediaLoudSession failed: ${e.message}")
        }
    }

    /** Kết thúc phiên – khôi phục mặc định. */
    fun exitVoiceSession() {
        try {
            abandonFocus()
            am.mode = SystemAudioManager.MODE_NORMAL
            clearRouting()
            disableBoosters()
            Log.d(TAG, "exitVoiceSession → MODE_NORMAL")
        } catch (e: Exception) {
            Log.w(TAG, "exitVoiceSession failed: ${e.message}")
        }
    }

    fun setBoostGainDb(db: Int) {
        boostDb = db.coerceIn(0, 24)
        runCatching { le?.setTargetGain(boostDb * 100) }
        if (Build.VERSION.SDK_INT >= 28) updateDynamicsProcessingGain()
    }

    // ---------------- Routing ----------------

    @Suppress("DEPRECATION")
    fun routeForVoiceCall(maximize: Boolean, preferSpeakerForMax: Boolean) {
        try {
            am.mode = SystemAudioManager.MODE_IN_COMMUNICATION

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val target = if (preferSpeakerForMax) {
                    am.availableCommunicationDevices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                } else {
                    am.availableCommunicationDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                                it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                    } ?: am.availableCommunicationDevices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                }
                if (target != null) am.setCommunicationDevice(target) else am.isSpeakerphoneOn = true
            } else {
                if (preferSpeakerForMax) {
                    am.isSpeakerphoneOn = true
                    runCatching { am.stopBluetoothSco(); am.isBluetoothScoOn = false }
                } else if (am.isBluetoothScoAvailableOffCall) {
                    am.startBluetoothSco()
                    am.isBluetoothScoOn = true
                }
            }

            if (maximize) {
                setStreamsToMax(SystemAudioManager.STREAM_VOICE_CALL, SystemAudioManager.STREAM_MUSIC)
                enableBoosters()
            }
        } catch (e: Exception) {
            Log.w(TAG, "routeForVoiceCall failed: ${e.message}")
        }
    }

    @Suppress("DEPRECATION")
    fun routeForMedia(maximize: Boolean, preferSpeakerForMax: Boolean) {
        try {
            am.mode = SystemAudioManager.MODE_NORMAL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (preferSpeakerForMax) {
                    val spk = am.availableCommunicationDevices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                    if (spk != null) am.setCommunicationDevice(spk) else am.clearCommunicationDevice()
                } else am.clearCommunicationDevice()
            } else {
                am.isSpeakerphoneOn = preferSpeakerForMax
            }

            if (maximize) {
                setStreamsToMax(SystemAudioManager.STREAM_MUSIC)
                enableBoosters()
            }
        } catch (e: Exception) {
            Log.w(TAG, "routeForMedia failed: ${e.message}")
        }
    }

    private fun clearRouting() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.clearCommunicationDevice()
        } else {
            am.isSpeakerphoneOn = false
            runCatching { am.stopBluetoothSco(); am.isBluetoothScoOn = false }
        }
    }

    // ---------------- Focus ----------------

    private fun requestFocus(usage: Int, content: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder().setUsage(usage).setContentType(content).build()
            afr = AudioFocusRequest.Builder(SystemAudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener {
                    if (it == SystemAudioManager.AUDIOFOCUS_GAIN ||
                        it == SystemAudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                        maybeRestoreMax("focus=$it")
                    }
                }
                .build()
            hasFocus = am.requestAudioFocus(afr!!) == SystemAudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            val stream = if (usage == AudioAttributes.USAGE_VOICE_COMMUNICATION)
                SystemAudioManager.STREAM_VOICE_CALL else SystemAudioManager.STREAM_MUSIC
            @Suppress("DEPRECATION")
            hasFocus = am.requestAudioFocus({ fc ->
                if (fc == SystemAudioManager.AUDIOFOCUS_GAIN ||
                    fc == SystemAudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                    maybeRestoreMax("focus(legacy)=$fc")
                }
            }, stream, SystemAudioManager.AUDIOFOCUS_GAIN) == SystemAudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        Log.d(TAG, "requestFocus granted=$hasFocus")
    }

    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            afr?.let { am.abandonAudioFocusRequest(it) }
        else @Suppress("DEPRECATION") am.abandonAudioFocus(null)
        hasFocus = false
    }

    // ---------------- Boosters ----------------

    private fun enableBoosters() {
        enableLE()
        if (Build.VERSION.SDK_INT >= 28) enableDP()
    }

    private fun disableBoosters() {
        disableLE()
        if (Build.VERSION.SDK_INT >= 28) disableDP()
    }

    /** LoudnessEnhancer trên session 0 (output mix). */
    private fun enableLE() {
        runCatching {
            if (le == null) le = LoudnessEnhancer(0)
            le?.setTargetGain(boostDb * 100)
            le?.enabled = true
            Log.d(TAG, "LE ON @ ${boostDb}dB")
        }.onFailure { Log.w(TAG, "LE enable failed: ${it.message}") }
    }

    private fun disableLE() {
        runCatching { le?.enabled = false; le?.release() }
        le = null
    }

    /** DynamicsProcessing chỉ bật Limiter (ổn định, không lỗi API). */
    @RequiresApi(28)
    private fun enableDP() {
        runCatching {
            if (dp == null) {
                val cfg = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    1, // 1 channel
                    false, 0, // preEq off
                    false, 0, // mbc off
                    false, 0, // postEq off
                    true      // limiter on
                ).build()
                dp = DynamicsProcessing(0, 0, cfg)
            }

            // Bật DynamicsProcessing tổng
            dp?.setEnabled(true)

            // Lấy channel 0 để cấu hình limiter
            val ch = dp!!.getChannelByChannelIndex(0)
            val lim = ch.limiter

            // Bật limiter và cấu hình
            lim.setEnabled(true)
            lim.attackTime = 1f
            lim.releaseTime = 50f
            lim.ratio = 10f
            lim.threshold = -1f

            // Ghi lại limiter vào channel và apply
            ch.limiter = lim
            dp!!.setChannelTo(0, ch)

            Log.d(TAG, "DynamicsProcessing limiter ON")
        }.onFailure {
            Log.w(TAG, "DP enable failed: ${it.message}")
        }
    }

    @RequiresApi(28)
    private fun updateDynamicsProcessingGain() {
        // Có thể map boostDb → threshold nếu muốn
    }

    @RequiresApi(28)
    private fun disableDP() {
        runCatching {
            dp?.setEnabled(false)
            dp?.release()
        }
        dp = null
    }


    // ---------------- Volume ----------------

    private fun setStreamsToMax(vararg streams: Int) {
        streams.forEach { s ->
            val max = am.getStreamMaxVolume(s)
            if (am.getStreamVolume(s) != max) am.setStreamVolume(s, max, 0)
        }
    }

    private fun maybeRestoreMax(tag: String) {
        if (am.mode == SystemAudioManager.MODE_IN_COMMUNICATION)
            setStreamsToMax(SystemAudioManager.STREAM_VOICE_CALL, SystemAudioManager.STREAM_MUSIC)
        else setStreamsToMax(SystemAudioManager.STREAM_MUSIC)
        logVol("restore@$tag")
    }

    private fun logVol(phase: String) {
        val v = am.getStreamVolume(SystemAudioManager.STREAM_VOICE_CALL)
        val mv = am.getStreamMaxVolume(SystemAudioManager.STREAM_VOICE_CALL)
        val m = am.getStreamVolume(SystemAudioManager.STREAM_MUSIC)
        val mm = am.getStreamMaxVolume(SystemAudioManager.STREAM_MUSIC)
        Log.d(TAG, "$phase: mode=${am.mode}, voice=$v/$mv, music=$m/$mm, preferSpeaker=$_preferSpeakerCache")
    }

    private fun typeName(t: Int) = when (t) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "BT_A2DP"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "BT_SCO"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "USB_HEADSET"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "WIRED_HEADSET"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "SPEAKER"
        else -> "T$t"
    }
}
