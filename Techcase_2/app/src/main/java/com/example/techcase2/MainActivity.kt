package com.example.techcase2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener{
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var inputEditText: EditText
    private lateinit var microphoneButton: ImageButton
    private lateinit var sendButton: ImageButton
    private lateinit var pulseAnimation: Animation // Animation declaration
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private var isListening = false
    private val quickReplies = listOf("Hello!", "Goodbye!", "Tell me a joke.", "How's the weather?")
    private lateinit var quickRepliesAdapter: QuickRepliesAdapter
    private lateinit var quickRepliesRecyclerView: RecyclerView
    private var isFirstMessageSent = false
    private lateinit var textToSpeech: TextToSpeech


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        inputEditText = findViewById(R.id.inputEditText)
        microphoneButton = findViewById(R.id.microphoneButton)
        sendButton = findViewById(R.id.sendButton)

        // Load the pulse animation
        pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_animation)

        chatAdapter = ChatAdapter(messages)
        chatRecyclerView.adapter = chatAdapter
        chatRecyclerView.layoutManager = LinearLayoutManager(this)

        requestMicrophonePermission()
        setupSpeechRecognizer()
        textToSpeech = TextToSpeech(this, this)

        microphoneButton.setOnClickListener {
            toggleListening()

        }

        quickRepliesRecyclerView = findViewById(R.id.quickRepliesRecyclerView)
        quickRepliesAdapter = QuickRepliesAdapter(quickReplies) { quickReply ->
            sendMessage(quickReply)
        }
        quickRepliesRecyclerView.adapter = quickRepliesAdapter
        quickRepliesRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        sendButton.setOnClickListener {
            val text = inputEditText.text.toString()
            if (text.isNotEmpty()) {
                sendMessage(text)
                inputEditText.text.clear()
            }
        }

        inputEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                chatRecyclerView.postDelayed({
                    chatRecyclerView.scrollToPosition(messages.size - 1)
                }, 100)
            }
        }

        inputEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val text = inputEditText.text.toString()
                if (text.isNotEmpty()) {
                    sendMessage(text)
                    inputEditText.text.clear()
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    resetListeningState()
                }

                override fun onError(error: Int) {
                    resetListeningState()
                }

                override fun onResults(results: Bundle) {
                    val data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spokenText = data?.get(0) ?: ""
                    runOnUiThread {
                        resetListeningState()
                        if (inputEditText.text.isEmpty()) {
                            inputEditText.setText(spokenText)
                            inputEditText.setSelection(inputEditText.text.length) // Move cursor to end
                        } else {
                            showEditDialog(spokenText) { editedText ->
                                inputEditText.setText(editedText)
                                inputEditText.setSelection(inputEditText.text.length) // Move cursor to end
                            }
                        }
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun resetListeningState() {
        isListening = false
        microphoneButton.setImageResource(R.drawable.ic_mic)
        microphoneButton.clearAnimation() // Stop the animation when not listening
    }

    private fun toggleListening() {
        if (!isListening) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
            }
            speechRecognizer.startListening(intent)
            microphoneButton.setImageResource(R.drawable.ic_mic_off)
            microphoneButton.startAnimation(pulseAnimation) // Start the animation
            isListening = true
        } else {
            speechRecognizer.stopListening()
            microphoneButton.setImageResource(R.drawable.ic_mic)
            microphoneButton.clearAnimation() // Stop the animation
            isListening = false
        }
    }

    private fun showEditDialog(spokenText: String, onTextEdited: (String) -> Unit) {
        val editText = EditText(this).apply {
            setText(spokenText)
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Text")
            .setView(editText)
            .setPositiveButton("Send") { dialog, which ->
                val editedText = editText.text.toString()
                onTextEdited(editedText)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendMessage(text: String) {
        if (!isFirstMessageSent) {
            hideQuickRepliesAndTitle()
            isFirstMessageSent = true
        }
        messages.add(ChatMessage(text, true))
        chatAdapter.notifyItemInserted(messages.size - 1)
        chatRecyclerView.scrollToPosition(messages.size - 1)
        queryOpenAI(text) { response ->
            runOnUiThread {
                messages.add(ChatMessage(response, false))
                chatAdapter.notifyItemInserted(messages.size - 1)
                chatRecyclerView.scrollToPosition(messages.size - 1)

                speakResponse(response)

            }
        }
    }

    private fun speakResponse(response: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeech.speak(response, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            textToSpeech.speak(response, TextToSpeech.QUEUE_FLUSH, null)
        }
    }
    private fun hideQuickRepliesAndTitle() {
        quickRepliesRecyclerView.visibility = View.GONE
        findViewById<TextView>(R.id.title2).visibility = View.GONE
        findViewById<TextView>(R.id.title3).visibility = View.GONE
    }

    class QuickRepliesAdapter(
        private val quickReplies: List<String>,
        private val onQuickReplyClicked: (String) -> Unit
    ) : RecyclerView.Adapter<QuickRepliesAdapter.QuickReplyViewHolder>() {

        class QuickReplyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val quickReplyText: TextView = view as TextView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuickReplyViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_quick_reply, parent, false)
            return QuickReplyViewHolder(view)
        }

        override fun onBindViewHolder(holder: QuickReplyViewHolder, position: Int) {
            holder.quickReplyText.text = quickReplies[position]
            holder.quickReplyText.setOnClickListener {
                onQuickReplyClicked(quickReplies[position])
            }
        }

        override fun getItemCount(): Int = quickReplies.size
    }

    //    permissions
    private fun requestMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                MY_PERMISSIONS_REQUEST_RECORD_AUDIO
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_PERMISSIONS_REQUEST_RECORD_AUDIO && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted
        } else {
            // Permission denied
        }
    }
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // TextToSpeech initialization successful
            val result = textToSpeech.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TextToSpeech", "Language not supported")
            }
        } else {
            Log.e("TextToSpeech", "Initialization failed")
        }
    }
    companion object {
        const val MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1
    }

    override fun onDestroy() {
        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }
        textToSpeech.shutdown()
        super.onDestroy()
        speechRecognizer.destroy()
    }
}

data class ChatMessage(val text: String, val isUser: Boolean)

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {
    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.messageTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = if (viewType == 1) R.layout.item_user_message else R.layout.item_bot_message
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.messageText.text = messages[position].text
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) 1 else 0
    }

    override fun getItemCount(): Int = messages.size
}
