package com.astrodham.astroagent

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.astrodham.astroagent.databinding.ActivityMainBinding
import com.astrodham.astroagent.ocr.ScreenCaptureService
import com.astrodham.astroagent.state.AgentPhase
import com.astrodham.astroagent.ui.ChatAdapter
import com.astrodham.astroagent.ui.ChatMessage
import com.astrodham.astroagent.ui.MainViewModel
import com.astrodham.astroagent.ui.PermissionHelper
import com.astrodham.astroagent.util.ApiKeyManager
import com.astrodham.astroagent.util.Logger
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * Main activity for AstroAgent.
 *
 * Provides:
 * - Command input (text + voice)
 * - Agent status display
 * - Live log view
 * - Permission management (Accessibility, Audio, Notifications, Screen Capture)
 * - Start/stop controls
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter

    // ── Permission Launchers ──

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        results.forEach { (permission, granted) ->
            Logger.i("Permission $permission: ${if (granted) "granted" else "denied"}")
        }
    }

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val intent = ScreenCaptureService.createStartIntent(
                this, result.resultCode, result.data!!
            )
            startForegroundService(intent)
            Logger.i("Screen capture service started")
        } else {
            Logger.w("Screen capture permission denied")
            Toast.makeText(this, "Screen capture permission is required for OCR", Toast.LENGTH_LONG).show()
        }
    }

    // ── Lifecycle ──

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeState()
        requestPermissions()
        initializeVoice()
        
        checkApiKey()
    }

    private fun checkApiKey() {
        if (!ApiKeyManager.hasApiKey()) {
            showApiKeyDialog(requireNow = true)
        }
    }

    private fun showApiKeyDialog(requireNow: Boolean = false) {
        val input = TextInputEditText(this).apply {
            hint = getString(R.string.hint_api_key)
            setText(ApiKeyManager.getApiKey())
            setPadding(48, 48, 48, 48)
        }

        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.title_api_key)
            .setMessage(R.string.desc_api_key)
            .setView(input)
            .setPositiveButton(R.string.action_save) { dialog, _ ->
                val key = input.text?.toString()?.trim() ?: ""
                if (key.isNotBlank()) {
                    ApiKeyManager.saveApiKey(key)
                    Toast.makeText(this, R.string.api_key_saved, Toast.LENGTH_SHORT).show()
                } else if (requireNow) {
                    Toast.makeText(this, R.string.api_key_required, Toast.LENGTH_LONG).show()
                    showApiKeyDialog(true)
                }
                dialog.dismiss()
            }

        if (!requireNow) {
            builder.setNegativeButton(R.string.action_cancel) { dialog, _ ->
                dialog.dismiss()
            }
        } else {
            builder.setCancelable(false)
        }

        builder.show()
    }

    override fun onResume() {
        super.onResume()
        updateAccessibilityWarning()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.voiceManager.destroy()
        viewModel.ttsManager.shutdown()
    }

    // ── Setup ──

    private fun setupUI() {
        // Toolbar Menu Setup
        binding.toolbar.inflateMenu(R.menu.menu_main)
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_settings) {
                showApiKeyDialog()
                true
            } else {
                false
            }
        }

        // Chat RecyclerView
        chatAdapter = ChatAdapter()
        binding.rvChat.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true // New logs appear at bottom
            }
            adapter = chatAdapter
        }

        // Execute button
        binding.btnExecute.setOnClickListener {
            val command = binding.etCommand.text?.toString()?.trim() ?: ""
            if (command.isNotBlank()) {
                binding.etCommand.text?.clear()
                viewModel.executeCommand(command)
            } else {
                Toast.makeText(this, "Enter a command first", Toast.LENGTH_SHORT).show()
            }
        }

        // Enter key on keyboard triggers execute
        binding.etCommand.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.btnExecute.performClick()
                true
            } else false
        }

        // Voice button
        binding.btnVoice.setOnClickListener {
            if (viewModel.voiceManager.isListening.value) {
                viewModel.voiceManager.stopListening()
            } else {
                if (PermissionHelper.hasAudioPermission(this)) {
                    viewModel.voiceManager.startListening()
                } else {
                    permissionLauncher.launch(arrayOf(android.Manifest.permission.RECORD_AUDIO))
                }
            }
        }

        // Stop button
        binding.btnStop.setOnClickListener {
            viewModel.cancelWorkflow()
        }

        // Accessibility warning tap → open settings
        binding.cardAccessibilityWarning.setOnClickListener {
            PermissionHelper.openAccessibilitySettings(this)
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe agent state
                launch {
                    viewModel.agentState.collect { state ->
                        binding.tvStatus.text = state.statusMessage

                        // Progress bar
                        binding.progressBar.isVisible = state.isExecuting || state.isPlanning

                        // Stop button
                        binding.btnStop.isVisible = state.isExecuting || state.isPlanning

                        // Disable input during execution
                        binding.btnExecute.isEnabled = !state.isExecuting && !state.isPlanning
                        binding.etCommand.isEnabled = !state.isExecuting && !state.isPlanning
                    }
                }

                // Observe Chat Messages
                launch {
                    viewModel.chatMessages.collect { messages ->
                        chatAdapter.submitList(messages) {
                            if (messages.isNotEmpty()) {
                                binding.rvChat.scrollToPosition(messages.size - 1)
                            }
                        }
                    }
                }

                // Observe voice listening state
                launch {
                    viewModel.voiceManager.isListening.collect { isListening ->
                        if (isListening) {
                            binding.btnVoice.setImageResource(android.R.drawable.ic_media_pause)
                        } else {
                            binding.btnVoice.setImageResource(android.R.drawable.ic_btn_speak_now)
                        }
                    }
                }

                // Observe voice partial results for UI feedback
                launch {
                    viewModel.voiceManager.partialResults.collect { partial ->
                        binding.etCommand.setText(partial)
                    }
                }
            }
        }
    }

    private fun updateAccessibilityWarning() {
        val isEnabled = PermissionHelper.isAccessibilityServiceEnabled(this)
        binding.cardAccessibilityWarning.isVisible = !isEnabled
    }

    private fun requestPermissions() {
        val missing = PermissionHelper.getMissingPermissions(this)
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun initializeVoice() {
        viewModel.voiceManager.initialize()
        viewModel.ttsManager.initialize()
    }

    /**
     * Call this to start screen capture for OCR.
     * Triggers the system consent dialog.
     */
    fun requestScreenCapture() {
        if (ScreenCaptureService.isActive) {
            Logger.i("Screen capture already active")
            return
        }

        val projectionManager = getSystemService(MediaProjectionManager::class.java)
        screenCaptureLauncher.launch(projectionManager.createScreenCaptureIntent())
    }
}
