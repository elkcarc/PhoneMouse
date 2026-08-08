package com.example.phonemouse

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.*
import androidx.lifecycle.*
import androidx.recyclerview.widget.*
import com.example.phonemouse.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Main entry point. Handles UI setup and renders state from the ViewModel. */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private val perms by lazy { BluetoothPermissionManager(this) }
    private lateinit var adapter: ConfigsAdapter

    /** Initializes UI, starts state observation, and checks for system permissions. */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.uiState.collectLatest { render(it) } } }
        if (perms.hasPermissions()) {
            // Profile registration is handled by the service on creation
        } else {
            perms.requestPermissions()
        }
        if (!perms.isBluetoothEnabled()) perms.requestBluetoothEnable()
        setupInsets()
    }

    /** Sets up top-level click listeners and initializes child UI components. */
    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { binding.drawerLayout.openDrawer(GravityCompat.START) }
        binding.statusBtn.setOnClickListener { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
        binding.toggleBtn.setOnClickListener { viewModel.toggleAutomation() }
        binding.trackpad.setOnMoveListener { dx, dy -> viewModel.mouseHidService.value?.sendManualMove(dx, dy) }
        setupMouseButtons()
        setupList()
        setupDrawer()
        setupSettings()
    }

    /** Configures touch listeners for manual mouse clicks and scroll buttons. */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupMouseButtons() {
        val listener = View.OnTouchListener { v, e ->
            val m: Byte = when (v.id) { R.id.leftClickBtn -> 0x01; R.id.rightClickBtn -> 0x02; R.id.middleClickBtn -> 0x04; else -> 0x00 }
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.performClick()
                    if (m != 0x00.toByte()) {
                        viewModel.mouseHidService.value?.setButtonState(mask = m, pressed = true)
                    } else {
                        viewModel.mouseHidService.value?.sendManualScroll(delta = if (v.id == R.id.scrollUpBtn) 1 else -1)
                    }
                    v.isPressed = true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (m != 0x00.toByte()) {
                        viewModel.mouseHidService.value?.setButtonState(mask = m, pressed = false)
                    }
                    v.isPressed = false
                }
            }
            true
        }
        listOf(binding.leftClickBtn, binding.rightClickBtn, binding.middleClickBtn, binding.scrollUpBtn, binding.scrollDownBtn).forEach { it.setOnTouchListener(listener) }
    }

    /** Initializes the automation configurations list with drag-and-drop support. */
    private fun setupList() {
        val helper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean {
                viewModel.moveConfig(v.adapterPosition, t.adapterPosition)
                return true
            }
            override fun onSwiped(v: RecyclerView.ViewHolder, d: Int) {}
        })
        helper.attachToRecyclerView(binding.navDrawerMain.configsRecyclerView)
        adapter = ConfigsAdapter(
            list = emptyList(),
            selectedIndex = 0,
            onSelected = { viewModel.selectConfig(it) },
            onDeleted = { viewModel.deleteConfig(it) },
            onDrag = { helper.startDrag(it) }
        )
        binding.navDrawerMain.configsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    /** Handles navigation between the main variations drawer and settings drawer. */
    private fun setupDrawer() {
        binding.navDrawerMain.settingsBtn.setOnClickListener { viewModel.setSettingsVisible(true) }
        binding.navDrawerSettings.settingsBackBtn.setOnClickListener { viewModel.setSettingsVisible(false) }
    }

    /** Sets up input listeners for theme, language, mode, and sensitivity settings. */
    private fun setupSettings() {
        binding.navDrawerSettings.apply {
            themeDropdown.setOnItemClickListener { _, _, p, _ -> viewModel.setThemeMode(arrayOf("Auto", "Dark", "Light")[p]) }
            languageDropdown.setOnItemClickListener { _, _, p, _ -> viewModel.setLanguage(arrayOf("en", "es", "ja", "ru", "zh")[p]) }
            trackpadModeDropdown.setOnItemClickListener { _, _, p, _ -> viewModel.setTrackpadMode(arrayOf("Trackpad", "Trackpoint")[p]) }
            trailToggle.setOnCheckedChangeListener { _, c -> viewModel.setTrailEnabled(c) }
            trackpointAnimationToggle.setOnCheckedChangeListener { _, c -> viewModel.setTrackpointAnimationEnabled(c) }
            trackpadSensitivitySlider.addOnChangeListener { _, v, f -> if (f) viewModel.setTrackpadSensitivity(v) }
            trackpointSensitivitySlider.addOnChangeListener { _, v, f -> if (f) viewModel.setTrackpointSensitivity(v) }
        }
        binding.navDrawerMain.apply {
            addVariationBtn.setOnClickListener { addVariationBtn.isVisible = false; addConfigCard.isVisible = true }
            cancelAddBtn.setOnClickListener { resetAdd() }
            confirmAddBtn.setOnClickListener {
                viewModel.addConfig(AutomationConfig(minIntInput.text.toString().toIntOrNull() ?: getString(R.string.default_min_int).toInt(), maxIntInput.text.toString().toIntOrNull() ?: getString(R.string.default_max_int).toInt(), minPressInput.text.toString().toIntOrNull() ?: getString(R.string.default_min_press).toInt(), maxPressInput.text.toString().toIntOrNull() ?: getString(R.string.default_max_press).toInt(), minBreakInput.text.toString().toIntOrNull() ?: getString(R.string.default_min_break).toInt(), maxBreakInput.text.toString().toIntOrNull() ?: getString(R.string.default_max_break).toInt(), delayFreqInput.text.toString().toIntOrNull() ?: getString(R.string.default_freq).toInt()))
                resetAdd()
            }
        }
    }

    /** Clears the "Add Variation" form fields and resets visibility. */
    private fun resetAdd() {
        binding.navDrawerMain.apply {
            listOf(minIntInput to R.string.default_min_int, maxIntInput to R.string.default_max_int, minPressInput to R.string.default_min_press, maxPressInput to R.string.default_max_press, minBreakInput to R.string.default_min_break, maxBreakInput to R.string.default_max_break, delayFreqInput to R.string.default_freq).forEach { it.first.setText(getString(it.second)) }
            addVariationBtn.isVisible = true; addConfigCard.isVisible = false
        }
    }

    /** Updates all UI elements based on the unified [MainUiState]. */
    private fun render(s: MainUiState) {
        binding.navDrawerMain.root.isVisible = !s.isSettingsVisible
        binding.navDrawerSettings.root.isVisible = s.isSettingsVisible
        if (s.isSettingsVisible && !binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.openDrawer(GravityCompat.START, false)
        }

        binding.statusBtn.apply {
            backgroundTintList = ColorStateList.valueOf(if (s.isConnected) Color.GREEN else Color.GRAY)
            text = if (s.isConnected && (s.connectedDeviceName != null)) getString(R.string.connected_to, s.connectedDeviceName) else getString(s.statusTextRes)
        }
        binding.toggleBtn.apply {
            isEnabled = s.isConnected
            text = getString(if (s.isAutomationRunning) R.string.stop_automation else R.string.start_automation)
        }
        binding.trackpad.apply {
            mode = s.trackpadMode
            trackpadSensitivity = s.trackpadSensitivity
            trackpointSensitivity = s.trackpointSensitivity
            isTrailEnabled = s.isTrackpadMode && s.isTrailEnabled
            isTrackpointAnimationEnabled = (!s.isTrackpadMode) && s.isTrackpointAnimationEnabled
        }

        binding.navDrawerSettings.apply {
            setupSpinner(trackpadModeDropdown, arrayOf(getString(R.string.mode_trackpad), getString(R.string.mode_trackpoint)), if (s.isTrackpadMode) 0 else 1)
            setupSpinner(themeDropdown, arrayOf(getString(R.string.theme_auto), getString(R.string.theme_dark), getString(R.string.theme_light)), arrayOf("Auto", "Dark", "Light").indexOf(s.themeMode))
            setupSpinner(languageDropdown, arrayOf("English", "Español", "日本語", "Русский", "中文"), arrayOf("en", "es", "ja", "ru", "zh").indexOf(s.appLanguage))

            trackpadSensitivitySlider.isEnabled = s.isTrackpadSensitivityControlEnabled; trackpadSensitivityCard.alpha = s.trackpadSettingsAlpha
            if (trackpadSensitivitySlider.value != s.trackpadSensitivity) trackpadSensitivitySlider.value = s.trackpadSensitivity
            trackpadSensitivityValueText.text = String.format(java.util.Locale.US, "%.1fx", s.trackpadSensitivity)
            trackpadSensitivityLabel.text = getString(R.string.trackpad_sensitivity)

            trackpointSensitivitySlider.isEnabled = s.isTrackpointSensitivityControlEnabled; trackpointSensitivityCard.alpha = s.trackpointSettingsAlpha
            if (trackpointSensitivitySlider.value != s.trackpointSensitivity) trackpointSensitivitySlider.value = s.trackpointSensitivity
            trackpointSensitivityValueText.text = String.format(java.util.Locale.US, "%.1fx", s.trackpointSensitivity)
            trackpointSensitivityLabel.text = getString(R.string.trackpoint_sensitivity)

            trailToggle.isEnabled = s.isTrackpadTrailControlEnabled; trackpadTrailCard.alpha = s.trackpadSettingsAlpha
            if (trailToggle.isChecked != s.isTrailEnabled) trailToggle.isChecked = s.isTrailEnabled
            trailToggle.text = getString(R.string.trackpad_trail)

            trackpointAnimationToggle.isEnabled = s.isTrackpointAnimationControlEnabled; trackpointAnimationCard.alpha = s.trackpointSettingsAlpha
            if (trackpointAnimationToggle.isChecked != s.isTrackpointAnimationEnabled) trackpointAnimationToggle.isChecked = s.isTrackpointAnimationEnabled
            trackpointAnimationToggle.text = getString(R.string.trackpoint_animation)

            settingsTitleText.text = getString(R.string.settings); settingsBackBtn.text = getString(R.string.back)
        }
        binding.navDrawerMain.apply { automationVariationsTitle.text = getString(R.string.automation_variations); addVariationBtn.text = getString(R.string.add_new_variation); settingsBtn.text = getString(R.string.settings) }

        adapter.update(s.configs, s.selectedConfigIndex)

        applyTheme(s.themeMode); applyLanguage(s.appLanguage)
    }

    /** Helper to initialize a dropdown menu with correctly localized items. */
    private fun setupSpinner(v: android.widget.AutoCompleteTextView, items: Array<String>, idx: Int) {
        v.setAdapter(object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, items) {
            override fun getFilter() = object : android.widget.Filter() {
                override fun performFiltering(c: CharSequence?) = FilterResults().apply { values = items; count = items.size }
                override fun publishResults(c: CharSequence?, r: FilterResults?) { notifyDataSetChanged() }
            }
        })
        v.setText(items[idx.coerceAtLeast(0)], false)
    }

    /** Synchronizes the app's light/dark mode with user preference. */
    private fun applyTheme(m: String) {
        val n = when (m) { "Dark" -> AppCompatDelegate.MODE_NIGHT_YES; "Light" -> AppCompatDelegate.MODE_NIGHT_NO; else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM }
        if (AppCompatDelegate.getDefaultNightMode() != n) AppCompatDelegate.setDefaultNightMode(n)
    }

    /** Synchronizes the app's display language with user preference. */
    private fun applyLanguage(l: String) {
        val al = LocaleListCompat.forLanguageTags(l)
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != al.toLanguageTags()) AppCompatDelegate.setApplicationLocales(al)
    }

    /** Adjusts layout padding to account for system status and navigation bars. */
    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerLayout) { _, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val d = resources.displayMetrics.density; val p24 = (24 * d).toInt()
            binding.toolbar.updatePadding(top = sys.top); binding.bottomControls.updatePadding(bottom = sys.bottom)
            listOf(binding.navDrawerMain.mainDrawerPanel, binding.navDrawerSettings.settingsDrawerPanel).forEach { it.updatePadding(top = sys.top) }
            binding.navDrawerMain.bottomButtonsContainer.updatePadding(bottom = p24 + sys.bottom); binding.navDrawerSettings.settingsDrawerPanel.updatePadding(bottom = p24 + sys.bottom)
            insets
        }
    }

    /** Triggers profile registration if Bluetooth permissions were just granted. */
    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, gr: IntArray) {
        super.onRequestPermissionsResult(rc, p, gr)
        if (rc == BluetoothPermissionManager.REQUEST_CODE_BLUETOOTH_PERMISSIONS && (gr.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED })) {
            viewModel.mouseHidService.value?.registerProfile()
        }
    }
}