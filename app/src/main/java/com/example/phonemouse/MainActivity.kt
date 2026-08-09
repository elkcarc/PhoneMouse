package com.example.phonemouse

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.*
import androidx.drawerlayout.widget.DrawerLayout
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
    private val dialogs by lazy { MainDialogHelper(this, viewModel) }
    private lateinit var configAdapter: ConfigsAdapter
    private lateinit var recordingAdapter: RecordingsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
        setupBackNavigation()
        lifecycleScope.launch { 
            repeatOnLifecycle(Lifecycle.State.STARTED) { 
                viewModel.uiState.collectLatest { render(it) } 
            } 
        }
        // Auto-start service only if we already have full permissions and hardware is active
        if (perms.hasPermissions() && perms.isBluetoothEnabled()) {
            viewModel.startService()
        }
        setupInsets()
    }

    override fun onResume() {
        super.onResume()
        viewModel.updatePermissionState(perms.hasPermissions())
        viewModel.updateBluetoothState(perms.isBluetoothEnabled())
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(enabled = true) {
                override fun handleOnBackPressed() {
                    val s = viewModel.uiState.value
                    when {
                        binding.drawerLayout.isDrawerOpen(GravityCompat.START) -> {
                            if (s.isSettingsVisible || (s.activePanel != "Main")) {
                                viewModel.setSettingsVisible(v = false)
                                viewModel.setActivePanel("Main")
                            } else {
                                binding.drawerLayout.closeDrawer(GravityCompat.START)
                            }
                        }
                        else -> {
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                        }
                    }
                }
            }
        )
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { binding.drawerLayout.openDrawer(GravityCompat.START) }
        
        binding.statusBtn.setOnClickListener { 
            when {
                !perms.hasPermissions() -> perms.requestPermissions()
                !perms.isBluetoothEnabled() -> perms.requestBluetoothEnable()
                !viewModel.uiState.value.isConnected -> viewModel.startService()
                else -> startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            }
        }
        
        binding.autoclickerBtn.setOnClickListener { 
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            viewModel.toggleAutoclicker() 
        }
        binding.recordBtn.setOnClickListener { 
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            viewModel.toggleRecording() 
        }
        binding.playbackBtn.setOnClickListener { 
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            viewModel.togglePlayback() 
        }

        binding.trackpad.setOnMoveListener { dx, dy -> viewModel.mouseHidService.value?.sendManualMove(dx, dy) }

        binding.drawerLayout.addDrawerListener(
            object : DrawerLayout.SimpleDrawerListener() {
                override fun onDrawerClosed(drawerView: View) {
                    viewModel.setSettingsVisible(v = false)
                    viewModel.setActivePanel("Main")
                }
            },
        )
        
        setupMouseButtons()
        setupList()
        setupDrawer()
        setupSettings()
    }

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
                        viewModel.mouseHidService.value?.sendManualScroll(if (v.id == R.id.scrollUpBtn) 1 else -1)
                    }
                    v.isPressed = true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (m != 0x00.toByte()) viewModel.mouseHidService.value?.setButtonState(mask = m, pressed = false)
                    v.isPressed = false
                }
            }
            true
        }
        listOf(binding.leftClickBtn, binding.rightClickBtn, binding.middleClickBtn, binding.scrollUpBtn, binding.scrollDownBtn).forEach { it.setOnTouchListener(listener) }
    }

    private fun setupList() {
        val disallowIntercept = object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (e.action == MotionEvent.ACTION_MOVE) {
                    rv.findChildViewUnder(e.x, e.y)?.let { rv.parent.requestDisallowInterceptTouchEvent(true) }
                }
                return false
            }
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(d: Boolean) {}
        }
        binding.navDrawerMain.configsRecyclerView.addOnItemTouchListener(disallowIntercept)
        binding.navDrawerMain.recordingsRecyclerView.addOnItemTouchListener(disallowIntercept)

        // Profiles List
        val configTouchHelper = ItemTouchHelper(
            object : SwipeCallback() {
                override fun isLongPressDragEnabled() = false
                override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = true.also { viewModel.moveConfig(v.adapterPosition, t.adapterPosition) }
                override fun onSwiped(v: RecyclerView.ViewHolder, d: Int) {
                    val pos = v.adapterPosition
                    if (pos == RecyclerView.NO_POSITION) return
                    if (d == ItemTouchHelper.LEFT) dialogs.confirmDelete { viewModel.deleteConfig(pos) }
                    else dialogs.showEditProfileDialog(pos)
                    configAdapter.notifyItemChanged(pos)
                }
            }
        )
        configTouchHelper.attachToRecyclerView(binding.navDrawerMain.configsRecyclerView)
        configAdapter = ConfigsAdapter(
            list = emptyList(), 
            selectedIndex = 0, 
            onClick = { pos -> viewModel.selectConfig(pos) }
        ) { configTouchHelper.startDrag(it) }
        binding.navDrawerMain.configsRecyclerView.apply { layoutManager = LinearLayoutManager(this@MainActivity); adapter = configAdapter }

        // Input Recordings
        val recordingTouchHelper = ItemTouchHelper(
            object : SwipeCallback() {
                override fun isLongPressDragEnabled() = false
                override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = true.also { viewModel.moveRecording(v.adapterPosition, t.adapterPosition) }
                override fun onSwiped(v: RecyclerView.ViewHolder, d: Int) {
                    val pos = v.adapterPosition
                    if (pos == RecyclerView.NO_POSITION) return
                    if (d == ItemTouchHelper.LEFT) dialogs.confirmDelete { viewModel.deleteRecording(pos) }
                    else dialogs.showEditRecordingDialog(pos)
                    recordingAdapter.notifyItemChanged(pos)
                }
            }
        )
        recordingTouchHelper.attachToRecyclerView(binding.navDrawerMain.recordingsRecyclerView)
        recordingAdapter = RecordingsAdapter(
            list = emptyList(), 
            selectedIndex = 0, 
            onClick = { pos -> viewModel.selectRecording(pos) }
        ) { recordingTouchHelper.startDrag(it) }
        binding.navDrawerMain.recordingsRecyclerView.apply { layoutManager = LinearLayoutManager(this@MainActivity); adapter = recordingAdapter }
    }

    /** Base class for swipe actions with visual background reveals. */
    private abstract inner class SwipeCallback : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        private val deleteColor = 0xFFE53935.toInt()
        private val editColor = 0xFF43A047.toInt()
        private val deleteIcon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_close)?.apply { setTint(Color.WHITE) }
        private val editIcon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_edit)?.apply { setTint(Color.WHITE) }
        private val cornerRadius = 12 * resources.displayMetrics.density

        override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
            val itemView = viewHolder.itemView
            val iconSize = deleteIcon?.intrinsicHeight ?: 0
            val iconMargin = (itemView.height - iconSize) / 2
            val iconTop = itemView.top + iconMargin
            val iconBottom = iconTop + iconSize

            val paint = android.graphics.Paint().apply { isAntiAlias = true }
            
            if (dX > 0) { // Swipe Right (Edit)
                paint.color = editColor
                val background = RectF(
                    (itemView.left.toFloat() + (16 * resources.displayMetrics.density)),
                    (itemView.top.toFloat() + (8 * resources.displayMetrics.density)),
                    (itemView.left.toFloat() + dX + cornerRadius),
                    (itemView.bottom.toFloat() - (8 * resources.displayMetrics.density)),
                )
                c.drawRoundRect(background, cornerRadius, cornerRadius, paint)
                
                editIcon?.let { icon ->
                    val iconLeft = itemView.left + iconMargin + (16 * resources.displayMetrics.density).toInt()
                    val iconRight = iconLeft + icon.intrinsicWidth
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    if (dX > (iconMargin + (16 * resources.displayMetrics.density))) icon.draw(c)
                }
            } else if (dX < 0) { // Swipe Left (Delete)
                paint.color = deleteColor
                val background = RectF(
                    ((itemView.right.toFloat() + dX) - cornerRadius),
                    (itemView.top.toFloat() + (8 * resources.displayMetrics.density)),
                    (itemView.right.toFloat() - (16 * resources.displayMetrics.density)),
                    (itemView.bottom.toFloat() - (8 * resources.displayMetrics.density)),
                )
                c.drawRoundRect(background, cornerRadius, cornerRadius, paint)
                
                deleteIcon?.let { icon ->
                    val iconRight = itemView.right - iconMargin - (16 * resources.displayMetrics.density).toInt()
                    val iconLeft = iconRight - icon.intrinsicWidth
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    if (kotlin.math.abs(dX) > (iconMargin + (16 * resources.displayMetrics.density))) icon.draw(c)
                }
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    private fun setupDrawer() {
        binding.navDrawerMain.apply {
            profilesBtn.setOnClickListener { viewModel.setActivePanel("Profiles") }
            recordingsBtn.setOnClickListener { viewModel.setActivePanel("Recordings") }
            profilesBackBtn.setOnClickListener { viewModel.setActivePanel("Main") }
            recordingsBackBtn.setOnClickListener { viewModel.setActivePanel("Main") }
            settingsBtn.setOnClickListener { viewModel.setSettingsVisible(v = true) }
        }
        binding.navDrawerSettings.settingsBackBtn.setOnClickListener { viewModel.setSettingsVisible(v = false) }
    }

    private fun setupSettings() {
        binding.navDrawerSettings.apply {
            themeDropdown.setOnItemClickListener { _, _, p, _ -> viewModel.setThemeMode(arrayOf("Auto", "Dark", "Light")[p]) }
            languageDropdown.setOnItemClickListener { _, _, p, _ -> viewModel.setLanguage(arrayOf("en", "es", "ja", "ru", "zh")[p]) }
            trackpadModeDropdown.setOnItemClickListener { _, _, p, _ -> viewModel.setTrackpadMode(arrayOf("Trackpad", "Trackpoint")[p]) }
            trailToggle.setOnCheckedChangeListener { _, c -> viewModel.setTrailEnabled(c) }
            trackpointAnimationToggle.setOnCheckedChangeListener { _, c -> viewModel.setTrackpointAnimationEnabled(c) }
            confirmDeleteToggle.setOnCheckedChangeListener { _, c -> viewModel.setConfirmDelete(c) }
            trackpadSensitivitySlider.addOnChangeListener { _, v, f -> if (f) viewModel.setTrackpadSensitivity(v) }
            trackpadAccelerationSlider.addOnChangeListener { _, v, f -> if (f) viewModel.setTrackpadAcceleration(v) }
            trackpointSensitivitySlider.addOnChangeListener { _, v, f -> if (f) viewModel.setTrackpointSensitivity(v) }
            trackpointCurveDropdown.setOnItemClickListener { _, _, p, _ -> viewModel.setTrackpointCurve(arrayOf("Linear", "Quadratic", "Cubic")[p]) }
        }
        binding.navDrawerMain.apply {
            addVariationBtn.setOnClickListener { dialogs.showEditProfileDialog(index = null) }
        }
    }

    private fun setupSpinner(v: AutoCompleteTextView, items: Array<String>, idx: Int) {
        v.setAdapter(
            object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, items) {
                override fun getFilter() = object : Filter() {
                    override fun performFiltering(c: CharSequence?) = FilterResults().apply { values = items; count = items.size }
                    override fun publishResults(c: CharSequence?, r: FilterResults?) { notifyDataSetChanged() }
                }
            },
        )
        v.setText(items[idx.coerceAtLeast(0)], false)
    }

    private fun render(s: MainUiState) {
        if (!::configAdapter.isInitialized || !::recordingAdapter.isInitialized) return
        
        binding.navDrawerMain.root.isVisible = !s.isSettingsVisible
        binding.navDrawerSettings.root.isVisible = s.isSettingsVisible
        if (s.isSettingsVisible && !binding.drawerLayout.isDrawerOpen(GravityCompat.START)) binding.drawerLayout.openDrawer(GravityCompat.START, false)

        binding.navDrawerMain.apply {
            mainNavPanel.isVisible = s.activePanel == "Main"
            profilesPanel.isVisible = s.activePanel == "Profiles"
            recordingsPanel.isVisible = s.activePanel == "Recordings"
        }

        binding.statusBtn.apply {
            val statusColor = when {
                !s.hasPermissions || !s.isBluetoothEnabled -> Color.RED
                s.isConnected -> Color.GREEN
                else -> Color.GRAY
            }
            backgroundTintList = ColorStateList.valueOf(statusColor)
            text = if (s.isConnected && (s.connectedDeviceName != null)) getString(R.string.connected_to, s.connectedDeviceName) else getString(s.statusTextRes)
        }
        
        val typedVal = android.util.TypedValue()
        theme.resolveAttribute(R.attr.controlIconColor, typedVal, true)
        val defaultIconColor = typedVal.data
        
        val bgVal = android.util.TypedValue()
        theme.resolveAttribute(R.attr.controlBackgroundColor, bgVal, true)
        val defaultBgColor = bgVal.data

        binding.autoclickerBtn.apply {
            isEnabled = s.isConnected && !s.isRecording && !s.isPlaying
            setIconResource(if (s.isAutoclickerRunning) R.drawable.ic_stop_circle else R.drawable.ic_autoplay)
            val iconColor = if (s.isAutoclickerRunning) Color.WHITE else defaultIconColor
            iconTint = ColorStateList.valueOf(iconColor)
            backgroundTintList = ColorStateList.valueOf(if (s.isAutoclickerRunning) Color.RED else defaultBgColor).withAlpha(if (isEnabled) 255 else 128)
        }
        
        binding.recordBtn.apply {
            isEnabled = s.isConnected && !s.isAutoclickerRunning && !s.isPlaying
            setIconResource(if (s.isRecording) R.drawable.ic_stop_circle else R.drawable.ic_screen_record)
            val iconColor = if (s.isRecording) Color.WHITE else defaultIconColor
            iconTint = ColorStateList.valueOf(iconColor)
            backgroundTintList = ColorStateList.valueOf(if (s.isRecording) Color.RED else defaultBgColor).withAlpha(if (isEnabled) 255 else 128)
        }
        
        binding.playbackBtn.apply {
            isEnabled = s.isConnected && s.hasRecording && !s.isAutoclickerRunning && !s.isRecording
            setIconResource(if (s.isPlaying) R.drawable.ic_stop_circle else R.drawable.ic_play_circle)
            val iconColor = if (s.isPlaying) Color.WHITE else defaultIconColor
            iconTint = ColorStateList.valueOf(iconColor)
            backgroundTintList = ColorStateList.valueOf(if (s.isPlaying) Color.RED else defaultBgColor).withAlpha(if (isEnabled) 255 else 128)
        }

        binding.trackpad.apply {
            mode = s.trackpadMode; trackpadSensitivity = s.trackpadSensitivity; trackpadAcceleration = s.trackpadAcceleration
            trackpointSensitivity = s.trackpointSensitivity; trackpointCurve = s.trackpointCurve
            isTrailEnabled = s.isTrackpadMode && s.isTrailEnabled; isTrackpointAnimationEnabled = (!s.isTrackpadMode) && s.isTrackpointAnimationEnabled
        }

        binding.navDrawerSettings.apply {
            setupSpinner(trackpadModeDropdown, arrayOf(getString(R.string.mode_trackpad), getString(R.string.mode_trackpoint)), if (s.isTrackpadMode) 0 else 1)
            setupSpinner(themeDropdown, arrayOf(getString(R.string.theme_auto), getString(R.string.theme_dark), getString(R.string.theme_light)), arrayOf("Auto", "Dark", "Light").indexOf(s.themeMode))
            setupSpinner(languageDropdown, arrayOf("English", "Español", "日本語", "Русский", "中文"), arrayOf("en", "es", "ja", "ru", "zh").indexOf(s.appLanguage))

            trackpadSensitivitySlider.isEnabled = s.isTrackpadSensitivityControlEnabled; trackpadSensitivityCard.alpha = s.trackpadSettingsAlpha
            if (trackpadSensitivitySlider.value != s.trackpadSensitivity) trackpadSensitivitySlider.value = s.trackpadSensitivity
            trackpadSensitivityValueText.text = String.format(java.util.Locale.US, "%.1fx", s.trackpadSensitivity)
            trackpadSensitivityLabel.text = getString(R.string.trackpad_sensitivity)

            trackpadAccelerationSlider.isEnabled = s.isTrackpadSensitivityControlEnabled; trackpadAccelerationCard.alpha = s.trackpadSettingsAlpha
            if (trackpadAccelerationSlider.value != s.trackpadAcceleration) trackpadAccelerationSlider.value = s.trackpadAcceleration
            trackpadAccelerationValueText.text = String.format(java.util.Locale.US, "%.1fx", s.trackpadAcceleration)

            trackpointSensitivitySlider.isEnabled = s.isTrackpointSensitivityControlEnabled; trackpointSensitivityCard.alpha = s.trackpointSettingsAlpha
            if (trackpointSensitivitySlider.value != s.trackpointSensitivity) trackpointSensitivitySlider.value = s.trackpointSensitivity
            trackpointSensitivityValueText.text = String.format(java.util.Locale.US, "%.1fx", s.trackpointSensitivity)
            trackpointSensitivityLabel.text = getString(R.string.trackpoint_sensitivity)
            
            trackpointCurveCard.alpha = s.trackpointSettingsAlpha
            setupSpinner(trackpointCurveDropdown, arrayOf(getString(R.string.curve_linear), getString(R.string.curve_quadratic), getString(R.string.curve_cubic)), arrayOf("Linear", "Quadratic", "Cubic").indexOf(s.trackpointCurve))

            trailToggle.isEnabled = s.isTrackpadTrailControlEnabled; trackpadTrailCard.alpha = s.trackpadSettingsAlpha
            if (trailToggle.isChecked != s.isTrailEnabled) trailToggle.isChecked = s.isTrailEnabled
            trailToggle.text = getString(R.string.trackpad_trail)

            trackpointAnimationToggle.isEnabled = s.isTrackpointAnimationControlEnabled; trackpointAnimationCard.alpha = s.trackpointSettingsAlpha
            if (trackpointAnimationToggle.isChecked != s.isTrackpointAnimationEnabled) trackpointAnimationToggle.isChecked = s.isTrackpointAnimationEnabled
            trackpointAnimationToggle.text = getString(R.string.trackpoint_animation)
            
            if (confirmDeleteToggle.isChecked != s.confirmDelete) confirmDeleteToggle.isChecked = s.confirmDelete

            settingsTitleText.text = getString(R.string.settings); settingsBackBtn.text = getString(R.string.back)
        }
        binding.navDrawerMain.apply { profilesBtn.text = getString(R.string.autoclicker_profiles); recordingsBtn.text = getString(R.string.input_recordings); settingsBtn.text = getString(R.string.settings) }

        configAdapter.update(s.configs, s.selectedConfigIndex)
        recordingAdapter.update(s.recordings, s.selectedRecordingIndex)

        applyTheme(s.themeMode); applyLanguage(s.appLanguage)
    }

    private fun applyTheme(m: String) {
        val n = when (m) { "Dark" -> AppCompatDelegate.MODE_NIGHT_YES; "Light" -> AppCompatDelegate.MODE_NIGHT_NO; else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM }
        if (AppCompatDelegate.getDefaultNightMode() != n) AppCompatDelegate.setDefaultNightMode(n)
    }

    private fun applyLanguage(l: String) {
        val al = LocaleListCompat.forLanguageTags(l)
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != al.toLanguageTags()) AppCompatDelegate.setApplicationLocales(al)
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerLayout) { _, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val d = resources.displayMetrics.density; val p24 = (24 * d).toInt()
            binding.toolbar.updatePadding(top = sys.top); binding.bottomControls.updatePadding(bottom = sys.bottom)
            binding.navDrawerMain.mainDrawerPanel.updatePadding(top = sys.top)
            binding.navDrawerMain.mainNavPanel.updatePadding(bottom = sys.bottom + p24)
            binding.navDrawerMain.profilesPanel.updatePadding(bottom = sys.bottom + p24)
            binding.navDrawerMain.recordingsPanel.updatePadding(bottom = sys.bottom + p24)
            binding.navDrawerSettings.settingsDrawerPanel.updatePadding(top = sys.top, bottom = sys.bottom + p24)
            insets
        }
    }

    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, gr: IntArray) {
        super.onRequestPermissionsResult(rc, p, gr)
        if (rc == BluetoothPermissionManager.REQUEST_CODE_BLUETOOTH_PERMISSIONS) {
            if (gr.isNotEmpty() && gr.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }) {
                if (perms.isBluetoothEnabled()) viewModel.startService()
                else perms.requestBluetoothEnable()
            }
        }
    }
}