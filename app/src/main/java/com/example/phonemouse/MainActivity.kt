package com.example.phonemouse

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.os.LocaleListCompat
import com.example.phonemouse.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * The entry point activity for PhoneMouse.
 * Responsible for UI setup, event wiring, and observing the [MainViewModel].
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var permissionManager: BluetoothPermissionManager
    private lateinit var configsAdapter: ConfigsAdapter
    private val prefs by lazy { getSharedPreferences("PhoneMousePrefs", MODE_PRIVATE) }

    private var hidService: BluetoothHidService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothHidService.LocalBinder
            val instance = binder.getService()
            hidService = instance
            viewModel.setMouseHidService(instance.mouseHidService)
            observeHidService(instance.mouseHidService)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            hidService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme and language early to ensure consistent layout inflation
        applySavedLanguage()
        applySavedTheme()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        permissionManager = BluetoothPermissionManager(this)

        setupUI()
        observeViewModel()
        
        // Start and bind the Foreground Service to keep connection alive in background
        val intent = Intent(this, BluetoothHidService::class.java)
        ContextCompat.startForegroundService(this, intent)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)

        if (!permissionManager.isBluetoothEnabled()) {
            permissionManager.requestBluetoothEnable()
        }

        setupWindowInsets()
    }

    /**
     * Observes real-time status updates from the HID service.
     */
    private fun observeHidService(service: MouseHidService) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    service.isConnected.collect { isConnected ->
                        binding.statusBtn.backgroundTintList = ColorStateList.valueOf(if (isConnected) Color.GREEN else Color.GRAY)
                        binding.toggleBtn.isEnabled = isConnected
                        if (!isConnected) {
                            binding.statusBtn.text = getString(R.string.disconnected_tap_to_open_bluetooth_settings)
                        }
                    }
                }
                launch {
                    service.connectedDeviceName.collect { name ->
                        if (name != null) {
                            binding.statusBtn.text = getString(R.string.connected_to, name)
                        }
                    }
                }
                launch {
                    service.isAutomationRunning.collect { isRunning ->
                        binding.toggleBtn.text = if (isRunning) getString(R.string.stop_automation) else getString(R.string.start_automation)
                    }
                }
            }
        }
    }

    /**
     * Loads the theme preference and sets the appropriate night mode.
     */
    private fun applySavedTheme() {
        val themeMode = prefs.getString("theme_mode", "Auto") ?: "Auto"
        val nightMode = when (themeMode) {
            "Dark" -> AppCompatDelegate.MODE_NIGHT_YES
            "Light" -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    /**
     * Applies the saved language preference using AppCompatDelegate.
     */
    private fun applySavedLanguage() {
        val languageCode = prefs.getString("app_language", "en") ?: "en"
        applyLanguage(languageCode)
    }

    /**
     * Updates the application locale dynamically.
     */
    private fun applyLanguage(languageCode: String) {
        val appLocales = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocales)
    }

    /**
     * Initializes all UI components and click listeners.
     */
    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.statusBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }

        binding.toggleBtn.setOnClickListener {
            viewModel.toggleAutomation()
        }

        setupTrackpad()
        setupMouseButtons()
        setupAutomationList()
        setupDrawerNavigation()
        setupSettingsPanel()
    }

    /**
     * Configures the trackpad to send movement reports and display a touch trail.
     */
    private fun setupTrackpad() {
        binding.trackpad.setOnMoveListener { dx, dy ->
            viewModel.mouseHidService.value?.sendManualMove(dx, dy)
        }
    }

    /**
     * Configures listeners for manual scroll and click buttons.
     */
    private fun setupMouseButtons() {
        val buttonTouchListener = View.OnTouchListener { v, event ->
            val mask: Byte = when (v.id) {
                R.id.leftClickBtn -> 0x01
                R.id.rightClickBtn -> 0x02
                R.id.middleClickBtn -> 0x04
                else -> 0x00
            }

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performClick()
                    if (mask != 0x00.toByte()) {
                        viewModel.mouseHidService.value?.setButtonState(mask, true)
                    } else {
                        // Handle scroll buttons
                        val delta = if (v.id == R.id.scrollUpBtn) 1 else -1
                        viewModel.mouseHidService.value?.sendManualScroll(delta)
                    }
                    v.isPressed = true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (mask != 0x00.toByte()) {
                        viewModel.mouseHidService.value?.setButtonState(mask, false)
                    }
                    v.isPressed = false
                }
            }
            true
        }

        binding.leftClickBtn.setOnTouchListener(buttonTouchListener)
        binding.rightClickBtn.setOnTouchListener(buttonTouchListener)
        binding.middleClickBtn.setOnTouchListener(buttonTouchListener)
        binding.scrollUpBtn.setOnTouchListener(buttonTouchListener)
        binding.scrollDownBtn.setOnTouchListener(buttonTouchListener)
    }

    /**
     * Configures the variation list with drag-and-drop and swipe-to-delete support.
     */
    private fun setupAutomationList() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                viewModel.moveConfig(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        })
        itemTouchHelper.attachToRecyclerView(binding.navDrawerMain.configsRecyclerView)

        configsAdapter = ConfigsAdapter(
            viewModel.configs.value,
            viewModel.selectedConfigIndex.value,
            { viewModel.selectConfig(it) },
            { viewModel.deleteConfig(it) },
            { itemTouchHelper.startDrag(it) }
        )

        binding.navDrawerMain.configsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = configsAdapter
        }
    }

    /**
     * Handles switching between the main variations panel and the settings panel in the drawer.
     */
    private fun setupDrawerNavigation() {
        binding.navDrawerMain.settingsBtn.setOnClickListener {
            binding.navDrawerMain.root.isVisible = false
            binding.navDrawerSettings.root.isVisible = true
        }

        binding.navDrawerSettings.settingsBackBtn.setOnClickListener {
            binding.navDrawerSettings.root.isVisible = false
            binding.navDrawerMain.root.isVisible = true
        }
    }

    /**
     * Initializes the settings panel components, including the theme selection dropdown.
     */
    private fun setupSettingsPanel() {
        val themeNames = arrayOf(getString(R.string.theme_auto), getString(R.string.theme_dark), getString(R.string.theme_light))
        val themeValues = arrayOf("Auto", "Dark", "Light")
        
        // Custom adapter that disables filtering to prevent the "one entry" bug
        val themesAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, themeNames) {
            override fun getFilter(): android.widget.Filter {
                return object : android.widget.Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        val results = FilterResults()
                        results.values = themeNames
                        results.count = themeNames.size
                        return results
                    }
                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        notifyDataSetChanged()
                    }
                }
            }
        }

        binding.navDrawerSettings.themeDropdown.apply {
            setAdapter(themesAdapter)
            val currentTheme = prefs.getString("theme_mode", "Auto") ?: "Auto"
            val currentIdx = themeValues.indexOf(currentTheme).coerceAtLeast(0)
            setText(themeNames[currentIdx], false)
            setOnItemClickListener { _, _, position, _ ->
                val selected = themeValues[position]
                prefs.edit { putString("theme_mode", selected) }
                applySavedTheme()
            }
        }

        val languages = arrayOf("English", "Español", "日本語", "Русский", "中文")
        val langCodes = arrayOf("en", "es", "ja", "ru", "zh")
        val langAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, languages) {
            override fun getFilter(): android.widget.Filter {
                return object : android.widget.Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        val results = FilterResults()
                        results.values = languages
                        results.count = languages.size
                        return results
                    }
                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        notifyDataSetChanged()
                    }
                }
            }
        }

        binding.navDrawerSettings.languageDropdown.apply {
            setAdapter(langAdapter)
            val currentLang = prefs.getString("app_language", "en") ?: "en"
            val currentIdx = langCodes.indexOf(currentLang).coerceAtLeast(0)
            setText(languages[currentIdx], false)
            setOnItemClickListener { _, _, position, _ ->
                val selectedCode = langCodes[position]
                viewModel.setLanguage(selectedCode)
                applyLanguage(selectedCode)
            }
        }

        val modes = arrayOf(getString(R.string.mode_trackpad), getString(R.string.mode_trackpoint))
        val modeValues = arrayOf("Trackpad", "Trackpoint")
        val modesAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, modes) {
            override fun getFilter(): android.widget.Filter {
                return object : android.widget.Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        val results = FilterResults()
                        results.values = modes
                        results.count = modes.size
                        return results
                    }
                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        notifyDataSetChanged()
                    }
                }
            }
        }

        binding.navDrawerSettings.trackpadModeDropdown.apply {
            setAdapter(modesAdapter)
            val currentMode = prefs.getString("trackpad_mode", "Trackpad") ?: "Trackpad"
            val currentIdx = modeValues.indexOf(currentMode).coerceAtLeast(0)
            setText(modes[currentIdx], false)
            setOnItemClickListener { _, _, position, _ ->
                viewModel.setTrackpadMode(modeValues[position])
            }
        }

        binding.navDrawerSettings.trailToggle.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setTrailEnabled(isChecked)
        }

        binding.navDrawerSettings.trackpadSensitivitySlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) viewModel.setTrackpadSensitivity(value)
        }

        binding.navDrawerSettings.trackpointSensitivitySlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) viewModel.setTrackpointSensitivity(value)
        }

        binding.navDrawerSettings.trackpointAnimationToggle.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setTrackpointAnimationEnabled(isChecked)
        }

        binding.navDrawerMain.addVariationBtn.setOnClickListener {
            binding.navDrawerMain.addVariationBtn.isVisible = false
            binding.navDrawerMain.addConfigCard.isVisible = true
        }

        binding.navDrawerMain.cancelAddBtn.setOnClickListener {
            resetAddConfigInputs()
            binding.navDrawerMain.addVariationBtn.isVisible = true
            binding.navDrawerMain.addConfigCard.isVisible = false
        }

        binding.navDrawerMain.confirmAddBtn.setOnClickListener {
            val config = AutomationConfig(
                binding.navDrawerMain.minIntInput.text.toString().toIntOrNull() ?: getString(R.string.default_min_int).toInt(),
                binding.navDrawerMain.maxIntInput.text.toString().toIntOrNull() ?: getString(R.string.default_max_int).toInt(),
                binding.navDrawerMain.minPressInput.text.toString().toIntOrNull() ?: getString(R.string.default_min_press).toInt(),
                binding.navDrawerMain.maxPressInput.text.toString().toIntOrNull() ?: getString(R.string.default_max_press).toInt(),
                binding.navDrawerMain.minBreakInput.text.toString().toIntOrNull() ?: getString(R.string.default_min_break).toInt(),
                binding.navDrawerMain.maxBreakInput.text.toString().toIntOrNull() ?: getString(R.string.default_max_break).toInt(),
                binding.navDrawerMain.delayFreqInput.text.toString().toIntOrNull() ?: getString(R.string.default_freq).toInt()
            )
            viewModel.addConfig(config)
            resetAddConfigInputs()
            binding.navDrawerMain.addVariationBtn.isVisible = true
            binding.navDrawerMain.addConfigCard.isVisible = false
        }
    }

    /**
     * Resets the automation variation input fields to their default values.
     */
    private fun resetAddConfigInputs() {
        binding.navDrawerMain.apply {
            minIntInput.setText(getString(R.string.default_min_int))
            maxIntInput.setText(getString(R.string.default_max_int))
            minPressInput.setText(getString(R.string.default_min_press))
            maxPressInput.setText(getString(R.string.default_max_press))
            minBreakInput.setText(getString(R.string.default_min_break))
            maxBreakInput.setText(getString(R.string.default_max_break))
            delayFreqInput.setText(getString(R.string.default_freq))
        }
    }

    /**
     * Connects UI elements to StateFlows in the ViewModel.
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Selection index updates
                launch {
                    viewModel.selectedConfigIndex.collect { index ->
                        configsAdapter.updateSelection(index)
                    }
                }
                // Language updates
                launch {
                    viewModel.appLanguage.collect { languageCode ->
                        val languages = arrayOf("English", "Español", "日本語", "Русский", "中文")
                        val langCodes = arrayOf("en", "es", "ja", "ru", "zh")
                        val index = langCodes.indexOf(languageCode).coerceAtLeast(0)
                        if (binding.navDrawerSettings.languageDropdown.text.toString() != languages[index]) {
                            binding.navDrawerSettings.languageDropdown.setText(languages[index], false)
                        }
                    }
                }
                // Trail toggle updates
                launch {
                    viewModel.isTrailEnabled.collect { isEnabled ->
                        val isTrackpad = viewModel.trackpadMode.value == "Trackpad"
                        binding.trackpad.isTrailEnabled = isTrackpad && isEnabled
                        if (binding.navDrawerSettings.trailToggle.isChecked != isEnabled) {
                            binding.navDrawerSettings.trailToggle.isChecked = isEnabled
                        }
                    }
                }
                // Sensitivity updates
                launch {
                    viewModel.trackpadSensitivity.collect { value ->
                        binding.trackpad.trackpadSensitivity = value
                        binding.navDrawerSettings.trackpadSensitivityValueText.text = String.format(java.util.Locale.US, "%.1fx", value)
                        if (binding.navDrawerSettings.trackpadSensitivitySlider.value != value) {
                            binding.navDrawerSettings.trackpadSensitivitySlider.value = value
                        }
                    }
                }
                launch {
                    viewModel.trackpointSensitivity.collect { value ->
                        binding.trackpad.trackpointSensitivity = value
                        binding.navDrawerSettings.trackpointSensitivityValueText.text = String.format(java.util.Locale.US, "%.1fx", value)
                        if (binding.navDrawerSettings.trackpointSensitivitySlider.value != value) {
                            binding.navDrawerSettings.trackpointSensitivitySlider.value = value
                        }
                    }
                }
                // Mode updates
                launch {
                    viewModel.trackpadMode.collect { mode ->
                        binding.trackpad.mode = mode
                        val isTrackpad = mode == "Trackpad"
                        
                        // Grey out and disable settings that don't apply to the current mode
                        // We NO LONGER call setTrailEnabled(false) etc here, to preserve user memory
                        
                        binding.navDrawerSettings.trackpadTrailCard.alpha = if (isTrackpad) 1.0f else 0.5f
                        binding.navDrawerSettings.trailToggle.isEnabled = isTrackpad
                        // Force TrackpadView to only show trail if in Trackpad mode AND user enabled it
                        binding.trackpad.isTrailEnabled = isTrackpad && viewModel.isTrailEnabled.value

                        binding.navDrawerSettings.trackpointAnimationCard.alpha = if (!isTrackpad) 1.0f else 0.5f
                        binding.navDrawerSettings.trackpointAnimationToggle.isEnabled = !isTrackpad
                        // Force TrackpadView to only animate if in Trackpoint mode AND user enabled it
                        binding.trackpad.isTrackpointAnimationEnabled = !isTrackpad && viewModel.isTrackpointAnimationEnabled.value
                        
                        // Grey out sensitivity sliders based on mode
                        binding.navDrawerSettings.trackpadSensitivityCard.alpha = if (isTrackpad) 1.0f else 0.5f
                        binding.navDrawerSettings.trackpadSensitivitySlider.isEnabled = isTrackpad
                        
                        binding.navDrawerSettings.trackpointSensitivityCard.alpha = if (isTrackpad) 0.5f else 1.0f
                        binding.navDrawerSettings.trackpointSensitivitySlider.isEnabled = !isTrackpad
                        
                        val modes = arrayOf(getString(R.string.mode_trackpad), getString(R.string.mode_trackpoint))
                        val modeValues = arrayOf("Trackpad", "Trackpoint")
                        val index = modeValues.indexOf(mode).coerceAtLeast(0)
                        if (binding.navDrawerSettings.trackpadModeDropdown.text.toString() != modes[index]) {
                            binding.navDrawerSettings.trackpadModeDropdown.setText(modes[index], false)
                        }
                    }
                }
                // Trackpoint Animation updates
                launch {
                    viewModel.isTrackpointAnimationEnabled.collect { isEnabled ->
                        val isTrackpoint = viewModel.trackpadMode.value == "Trackpoint"
                        binding.trackpad.isTrackpointAnimationEnabled = isTrackpoint && isEnabled
                        if (binding.navDrawerSettings.trackpointAnimationToggle.isChecked != isEnabled) {
                            binding.navDrawerSettings.trackpointAnimationToggle.isChecked = isEnabled
                        }
                    }
                }
                // Variations list updates
                launch {
                    viewModel.configs.collect { configs ->
                        configsAdapter.currentList = configs
                        @SuppressLint("NotifyDataSetChanged")
                        configsAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    /**
     * Adjusts layout padding to account for system bars (status and navigation).
     */
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerLayout) { _, insets ->
            val sysInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val density = resources.displayMetrics.density
            val padding24 = (24 * density).toInt()

            binding.toolbar.updatePadding(top = sysInsets.top)
            binding.bottomControls.updatePadding(bottom = sysInsets.bottom)
            
            // Apply system top insets to the drawer content
            binding.navDrawerMain.mainDrawerPanel.updatePadding(top = sysInsets.top)
            binding.navDrawerSettings.settingsDrawerPanel.updatePadding(top = sysInsets.top)
            
            // Apply system bottom insets to ensure buttons aren't cut off
            binding.navDrawerMain.bottomButtonsContainer.updatePadding(bottom = padding24 + sysInsets.bottom)
            binding.navDrawerSettings.settingsDrawerPanel.updatePadding(bottom = padding24 + sysInsets.bottom)
            insets
        }
    }

    override fun onDestroy() {
        try { unbindService(serviceConnection) } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == BluetoothPermissionManager.REQUEST_CODE_BLUETOOTH_PERMISSIONS) {
            if (grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }) {
                hidService?.mouseHidService?.registerProfile()
            }
        }
    }
}