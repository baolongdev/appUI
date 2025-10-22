package com.example.appui.core.audio.capture

import android.annotation.SuppressLint
import android.content.Context
import android.media.*
import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * PCM audio data from a source.
 */
data class PcmData(
    val timestamp: Long,
    val samples: ShortArray,
    val sampleRate: Int,
    val channelCount: Int,
    val source: AudioSource
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PcmData
        return timestamp == other.timestamp &&
                samples.contentEquals(other.samples) &&
                sampleRate == other.sampleRate &&
                channelCount == other.channelCount &&
                source == other.source
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + samples.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + channelCount
        result = 31 * result + source.hashCode()
        return result
    }
}

/**
 * Audio source type.
 */
enum class AudioSource {
    MICROPHONE,
    PLAYBACK
}

/**
 * Manages PCM audio capture from microphone and playback with pause/resume support.
 */
class AudioPcmCaptureManager(
    private val context: Context
) {
    // Configuration
    private var micSampleRate = DEFAULT_SAMPLE_RATE
    private var micChannelConfig = AudioFormat.CHANNEL_IN_MONO

    // Audio recording
    private var audioRecord: AudioRecord? = null
    private var micCaptureJob: Job? = null

    // Audio playback visualization
    private var visualizer: Visualizer? = null

    // PCM data streams
    private val _micPcmFlow = MutableStateFlow<PcmData?>(null)
    val micPcmFlow: StateFlow<PcmData?> = _micPcmFlow.asStateFlow()

    private val _playbackPcmFlow = MutableStateFlow<PcmData?>(null)
    val playbackPcmFlow: StateFlow<PcmData?> = _playbackPcmFlow.asStateFlow()

    // State
    private val _isMicCapturing = MutableStateFlow(false)
    val isMicCapturing: StateFlow<Boolean> = _isMicCapturing.asStateFlow()

    private val _isPlaybackCapturing = MutableStateFlow(false)
    val isPlaybackCapturing: StateFlow<Boolean> = _isPlaybackCapturing.asStateFlow()

    // Recording to file
    private var micRecordingStream: FileOutputStream? = null
    private var playbackRecordingStream: FileOutputStream? = null

    /**
     * Starts microphone PCM capture.
     */
    @SuppressLint("MissingPermission")
    fun startMicCapture(
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
        channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
        recordToFile: File? = null
    ) {
        if (_isMicCapturing.value) {
            Log.w(TAG, "Mic capture already running")
            return
        }

        micSampleRate = sampleRate
        micChannelConfig = channelConfig

        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            channelConfig,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (bufferSize <= 0) {
            Log.e(TAG, "Invalid buffer size: $bufferSize")
            return
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                audioRecord?.release()
                audioRecord = null
                return
            }

            // Setup WAV recording if needed
            recordToFile?.let { file ->
                micRecordingStream = FileOutputStream(file)
                writeWavHeader(
                    micRecordingStream!!,
                    sampleRate,
                    getChannelCount(channelConfig)
                )
            }

            audioRecord?.startRecording()
            _isMicCapturing.value = true

            micCaptureJob = CoroutineScope(Dispatchers.IO).launch {
                captureMicPcm(bufferSize)
            }

            Log.i(TAG, "Mic capture started (sampleRate=$sampleRate, bufferSize=$bufferSize)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start mic capture", e)
            stopMicCapture()
        }
    }

    /**
     * Pauses microphone capture (stops emitting PCM data but keeps recording active).
     */
    fun pauseMicCapture() {
        if (!_isMicCapturing.value) {
            Log.w(TAG, "Mic capture not running, cannot pause")
            return
        }

        // Stop emitting data by canceling the job, but keep AudioRecord alive
        micCaptureJob?.cancel()
        micCaptureJob = null

        Log.i(TAG, "Mic capture paused")
    }

    /**
     * Resumes microphone capture.
     */
    fun resumeMicCapture() {
        if (!_isMicCapturing.value) {
            Log.w(TAG, "Mic capture not running, cannot resume")
            return
        }

        if (micCaptureJob != null) {
            Log.w(TAG, "Mic capture already active")
            return
        }

        // Resume capture job
        val bufferSize = AudioRecord.getMinBufferSize(
            micSampleRate,
            micChannelConfig,
            AudioFormat.ENCODING_PCM_16BIT
        )

        micCaptureJob = CoroutineScope(Dispatchers.IO).launch {
            captureMicPcm(bufferSize)
        }

        Log.i(TAG, "Mic capture resumed")
    }

    /**
     * Starts playback PCM capture using Visualizer.
     */
    fun startPlaybackCapture(
        captureSize: Int = 1024,
        recordToFile: File? = null
    ) {
        if (_isPlaybackCapturing.value) {
            Log.w(TAG, "Playback capture already running")
            return
        }

        try {
            // Get audio session ID (0 for output mix - all audio)
            val audioSessionId = 0

            Log.d(TAG, "Initializing Visualizer (session=$audioSessionId)")

            visualizer = Visualizer(audioSessionId).apply {
                // Set capture size (must be power of 2)
                val captureSizeRange = Visualizer.getCaptureSizeRange()
                val validCaptureSize = when {
                    captureSize < captureSizeRange[0] -> captureSizeRange[0]
                    captureSize > captureSizeRange[1] -> captureSizeRange[1]
                    (captureSize and (captureSize - 1)) != 0 -> {
                        // Not power of 2, find nearest
                        var size = 128
                        while (size < captureSize && size < 1024) size *= 2
                        size
                    }
                    else -> captureSize
                }

                Log.d(TAG, "Setting capture size: $validCaptureSize (range: ${captureSizeRange[0]}-${captureSizeRange[1]})")
                setCaptureSize(validCaptureSize)

                val rate = samplingRate
                Log.d(TAG, "Visualizer sampling rate: $rate Hz")

                // Setup data capture callback
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
                            waveform?.let {
                                processPlaybackWaveform(it, samplingRate)
                            }
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            // FFT data not used for PCM capture
                        }
                    },
                    Visualizer.getMaxCaptureRate(),
                    true,  // waveform
                    false  // fft
                )

                // Enable visualizer
                val result = setEnabled(true)
                if (result != Visualizer.SUCCESS) {
                    Log.e(TAG, "Failed to enable Visualizer, result code: $result")
                    release()
                    return
                }

                Log.i(TAG, "Visualizer enabled successfully")
            }

            // Setup WAV recording if needed
            recordToFile?.let { file ->
                playbackRecordingStream = FileOutputStream(file)
                writeWavHeader(
                    playbackRecordingStream!!,
                    visualizer!!.samplingRate,
                    1 // Visualizer is always mono
                )
            }

            _isPlaybackCapturing.value = true
            Log.i(TAG, "Playback capture started (captureSize=$captureSize)")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Visualizer not available (device may not support it)", e)
            stopPlaybackCapture()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start playback capture", e)
            stopPlaybackCapture()
        }
    }

    /**
     * Stops microphone capture.
     */
    fun stopMicCapture() {
        micCaptureJob?.cancel()
        micCaptureJob = null

        audioRecord?.apply {
            if (state == AudioRecord.STATE_INITIALIZED) {
                stop()
            }
            release()
        }
        audioRecord = null

        // Finalize WAV file
        micRecordingStream?.let { stream ->
            finalizeWavFile(stream)
            stream.close()
            micRecordingStream = null
        }

        _isMicCapturing.value = false
        Log.i(TAG, "Mic capture stopped")
    }

    /**
     * Stops playback capture.
     */
    fun stopPlaybackCapture() {
        visualizer?.apply {
            try {
                enabled = false
                release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing Visualizer", e)
            }
        }
        visualizer = null

        // Finalize WAV file
        playbackRecordingStream?.let { stream ->
            finalizeWavFile(stream)
            stream.close()
            playbackRecordingStream = null
        }

        _isPlaybackCapturing.value = false
        Log.i(TAG, "Playback capture stopped")
    }

    /**
     * Stops all capture.
     */
    fun stopAllCapture() {
        stopMicCapture()
        stopPlaybackCapture()
        Log.i(TAG, "All capture stopped")
    }

    // Private methods

    private suspend fun captureMicPcm(bufferSize: Int) = withContext(Dispatchers.IO) {
        val buffer = ShortArray(bufferSize)

        while (isActive && _isMicCapturing.value) {
            val readCount = audioRecord?.read(buffer, 0, bufferSize) ?: -1

            if (readCount > 0) {
                val pcmData = PcmData(
                    timestamp = System.currentTimeMillis(),
                    samples = buffer.copyOf(readCount),
                    sampleRate = micSampleRate,
                    channelCount = getChannelCount(micChannelConfig),
                    source = AudioSource.MICROPHONE
                )

                _micPcmFlow.value = pcmData

                // Write to WAV file if recording
                micRecordingStream?.let { stream ->
                    writePcmToStream(stream, buffer, readCount)
                }
            } else if (readCount < 0) {
                Log.e(TAG, "AudioRecord read error: $readCount")
                break
            }
        }
    }

    private fun processPlaybackWaveform(waveform: ByteArray, samplingRate: Int) {
        // Convert unsigned byte waveform (0-255) to signed short PCM (-32768 to 32767)
        val samples = ShortArray(waveform.size) { i ->
            // Normalize from [0, 255] to [-128, 127] then scale to 16-bit
            val normalized = (waveform[i].toInt() and 0xFF) - 128
            (normalized * 256).toShort()
        }

        val pcmData = PcmData(
            timestamp = System.currentTimeMillis(),
            samples = samples,
            sampleRate = samplingRate,
            channelCount = 1, // Visualizer is always mono
            source = AudioSource.PLAYBACK
        )

        _playbackPcmFlow.value = pcmData

        // Write to WAV file if recording
        playbackRecordingStream?.let { stream ->
            writePcmToStream(stream, samples, samples.size)
        }
    }

    // WAV file utilities

    private fun writeWavHeader(
        stream: FileOutputStream,
        sampleRate: Int,
        channelCount: Int
    ) {
        val header = ByteArray(44)
        val byteBuffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF header
        byteBuffer.put("RIFF".toByteArray())
        byteBuffer.putInt(0) // File size (will update later)
        byteBuffer.put("WAVE".toByteArray())

        // fmt subchunk
        byteBuffer.put("fmt ".toByteArray())
        byteBuffer.putInt(16) // Subchunk size
        byteBuffer.putShort(1) // Audio format (PCM)
        byteBuffer.putShort(channelCount.toShort())
        byteBuffer.putInt(sampleRate)
        byteBuffer.putInt(sampleRate * channelCount * 2) // Byte rate
        byteBuffer.putShort((channelCount * 2).toShort()) // Block align
        byteBuffer.putShort(16) // Bits per sample

        // data subchunk
        byteBuffer.put("data".toByteArray())
        byteBuffer.putInt(0) // Data size (will update later)

        stream.write(header)
    }

    private fun writePcmToStream(stream: FileOutputStream, samples: ShortArray, count: Int) {
        val buffer = ByteBuffer.allocate(count * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until count) {
            buffer.putShort(samples[i])
        }
        stream.write(buffer.array())
    }

    private fun finalizeWavFile(stream: FileOutputStream) {
        val fileSize = stream.channel.position()
        val dataSize = fileSize - 44

        stream.channel.position(4)
        stream.write(intToByteArrayLE((fileSize - 8).toInt()))

        stream.channel.position(40)
        stream.write(intToByteArrayLE(dataSize.toInt()))
    }

    private fun intToByteArrayLE(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }

    private fun getChannelCount(channelConfig: Int): Int = when (channelConfig) {
        AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_OUT_MONO -> 1
        AudioFormat.CHANNEL_IN_STEREO, AudioFormat.CHANNEL_OUT_STEREO -> 2
        else -> 1
    }

    companion object {
        private const val TAG = "AudioPcmCaptureManager"
        private const val DEFAULT_SAMPLE_RATE = 16000
    }
}
