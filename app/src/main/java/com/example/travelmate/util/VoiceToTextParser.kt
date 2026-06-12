package com.example.travelmate.util

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class VoiceToTextParser @Inject constructor(
    private val app: Application
) : RecognitionListener {

    companion object {
        private const val TAG = "VoiceToTextParser"
    }

    private val _state = MutableStateFlow(VoiceToTextParserState())
    val state = _state.asStateFlow()

    private val recognizer = SpeechRecognizer.createSpeechRecognizer(app)

    fun startListening(languageCode: String = "vi-VN") {
        _state.update { VoiceToTextParserState() }
        Log.d(TAG, "Bắt đầu yêu cầu lắng nghe với ngôn ngữ: $languageCode")

        if (!SpeechRecognizer.isRecognitionAvailable(app)) {
            val errorMsg = "LỖI: Speech Recognition không khả dụng trên thiết bị này. Hãy kiểm tra Google Speech Services trong Cài đặt."
            Log.e(TAG, errorMsg)
            _state.update {
                it.copy(error = errorMsg)
            }
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Thêm prompt để thông báo cho hệ thống (tùy engine)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Đang lắng nghe...")
        }

        try {
            recognizer.setRecognitionListener(this)
            recognizer.startListening(intent)
            Log.d(TAG, "Đã gọi recognizer.startListening(intent) thành công")
            _state.update { it.copy(isListening = true) }
        } catch (e: Exception) {
            val errorMsg = "LỖI khi khởi chạy: ${e.localizedMessage}"
            Log.e(TAG, errorMsg, e)
            _state.update { it.copy(error = errorMsg, isListening = false) }
        }
    }

    fun stopListening() {
        Log.d(TAG, "Yêu cầu dừng lắng nghe thủ công")
        _state.update { it.copy(isListening = false) }
        recognizer.stopListening()
    }

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "Sẵn sàng nhận giọng nói (onReadyForSpeech)")
        _state.update { it.copy(error = null) }
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "Đã phát hiện bắt đầu nói (onBeginningOfSpeech)")
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Cập nhật âm lượng nếu cần hiển thị sóng âm
        _state.update { it.copy(rmsDb = rmsdB) }
    }

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEndOfSpeech() {
        Log.d(TAG, "Kết thúc nói (onEndOfSpeech)")
        _state.update { it.copy(isListening = false) }
    }

    override fun onError(error: Int) {
        val message = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Lỗi audio (Kết nối micro thất bại)"
            SpeechRecognizer.ERROR_CLIENT -> "Lỗi client (Ứng dụng bị ngắt kết nối)"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Lỗi: Thiếu quyền ghi âm (RECORD_AUDIO)"
            SpeechRecognizer.ERROR_NETWORK -> "Lỗi mạng (Hãy kiểm tra Internet)"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Lỗi mạng: Hết thời gian chờ"
            SpeechRecognizer.ERROR_NO_MATCH -> "Không nhận diện được giọng nói. Hãy thử lại."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Dịch vụ đang bận. Hãy đợi vài giây."
            SpeechRecognizer.ERROR_SERVER -> "Lỗi từ Server nhận diện của Google"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Không nghe thấy tiếng. Hãy bắt đầu nói sớm hơn."
            else -> "Lỗi không xác định: $error"
        }
        
        Log.e(TAG, "onError: Code $error -> $message")
        
        // Nếu lỗi bận hoặc client thì không hiện lên UI để tránh gây phiền
        if (error == SpeechRecognizer.ERROR_CLIENT) return

        _state.update {
            it.copy(
                error = message,
                isListening = false
            )
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        Log.d(TAG, "Kết quả cuối cùng (onResults): $matches")
        matches?.getOrNull(0)?.let { result ->
            _state.update { it.copy(spokenText = result, isListening = false) }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        Log.d(TAG, "Kết quả tạm thời (onPartialResults): $partial")
        partial?.getOrNull(0)?.let { result ->
            _state.update { it.copy(spokenText = result) }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        Log.d(TAG, "Sự kiện Speech: $eventType")
    }
}

data class VoiceToTextParserState(
    val spokenText: String = "",
    val isListening: Boolean = false,
    val error: String? = null,
    val rmsDb: Float = 0f
)
