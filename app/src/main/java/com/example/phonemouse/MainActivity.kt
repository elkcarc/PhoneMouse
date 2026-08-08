package com.example.phonemouse

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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
import android.widget.ArrayAdapter
import androidx.core.os.LocaleListCompat
import com.example.phonemouse.databinding.ActivityMainBinding
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
        
        // Handle Bluetooth lifecycle and permissions
        if (permissionManager.hasPermissions()) {
            viewModel.mouseHidService.registerProfile()
        } else {
            permissionManager.requestPermissions()
        }

        if (!permissionManager.isBluetoothEnabled()) {
            permissionManager.requestBluetoothEnable()
        }

        setupWindowInsets()
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
            viewModel.mouseHidService.sendManualMove(dx, dy)
        }
    }

    /**
     * Configures listeners for manual scroll and click buttons.
     */
    private fun setupMouseButtons() {
        binding.scrollUpBtn.setOnClickListener { viewModel.mouseHidService.sendManualScroll(1) }
        binding.middleClickBtn.setOnClickListener { viewModel.mouseHidService.sendManualClick(0x04) }
        binding.scrollDownBtn.setOnClickListener { viewModel.mouseHidService.sendManualScroll(-1) }
        binding.leftClickBtn.setOnClickListener { viewModel.mouseHidService.sendManualClick(0x01) }
        binding.rightClickBtn.setOnClickListener { viewModel.mouseHidService.sendManualClick(0x02) }
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
        val themes = arrayOf("Auto", "Dark", "Light")
        
        // Custom adapter that disables filtering to prevent the "one entry" bug
        val themesAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, themes) {
            override fun getFilter(): android.widget.Filter {
                return object : android.widget.Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        val results = FilterResults()
                        results.values = themes
                        results.count = themes.size
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
            setText(prefs.getString("theme_mode", "Auto"), false)
            setOnItemClickListener { _, _, position, _ ->
                val selected = themes[position]
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

        binding.navDrawerSettings.trailToggle.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setTrailEnabled(isChecked)
        }

        binding.navDrawerMain.addVariationBtn.setOnClickListener {
            binding.navDrawerMain.addVariationBtn.isVisible = false
            binding.navDrawerMain.addConfigCard.isVisible = true
        }

        binding.navDrawerMain.cancelAddBtn.setOnClickListener {
            binding.navDrawerMain.addVariationBtn.isVisible = true
            binding.navDrawerMain.addConfigCard.isVisible = false
        }

        binding.navDrawerMain.confirmAddBtn.setOnClickListener {
            val config = AutomationConfig(
                binding.navDrawerMain.minIntInput.text.toString().toIntOrNull() ?: 5000,
                binding.navDrawerMain.maxIntInput.text.toString().toIntOrNull() ?: 6000,
                binding.navDrawerMain.minPressInput.text.toString().toIntOrNull() ?: 1000,
                binding.navDrawerMain.maxPressInput.text.toString().toIntOrNull() ?: 1500,
                binding.navDrawerMain.minBreakInput.text.toString().toIntOrNull() ?: 15000,
                binding.navDrawerMain.maxBreakInput.text.toString().toIntOrNull() ?: 20000,
                binding.navDrawerMain.delayFreqInput.text.toString().toIntOrNull() ?: 10
            )
            viewModel.addConfig(config)
            binding.navDrawerMain.addVariationBtn.isVisible = true
            binding.navDrawerMain.addConfigCard.isVisible = false
        }
    }

    /**
     * Connects UI elements to StateFlows in the ViewModel.
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Connection status updates
                launch {
                    viewModel.mouseHidService.isConnected.collect { isConnected ->
                        binding.statusBtn.text = if (isConnected) getString(R.string.connected) else getString(R.string.disconnected_tap_to_open_bluetooth_settings)
                        binding.statusBtn.backgroundTintList = ColorStateList.valueOf(if (isConnected) Color.GREEN else Color.GRAY)
                        binding.toggleBtn.isEnabled = isConnected
                    }
                }
                // Automation status updates
                launch {
                    viewModel.mouseHidService.isAutomationRunning.collect { isRunning ->
                        binding.toggleBtn.text = if (isRunning) getString(R.string.stop_automation) else getString(R.string.start_automation)
                    }
                }
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
                        binding.trackpad.isTrailEnabled = isEnabled
                        if (binding.navDrawerSettings.trailToggle.isChecked != isEnabled) {
                            binding.navDrawerSettings.trailToggle.isChecked = isEnabled
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
        super.onDestroy()
        // Clean up the Bluetooth registration to prevent profile leaks
        viewModel.mouseHidService.unregisterProfile()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == BluetoothPermissionManager.REQUEST_CODE_BLUETOOTH_PERMISSIONS) {
            if (grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }) {
                viewModel.mouseHidService.registerProfile()
            }
        }
    }
}