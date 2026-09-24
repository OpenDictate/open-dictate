@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.opendictate.app.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.SettingsAccessibility
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.relocation.BringIntoViewModifierNode
import androidx.compose.ui.relocation.bringIntoView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.os.LocaleListCompat
import com.opendictate.app.R
import com.opendictate.app.data.normalizeDictionaryTerms
import com.opendictate.app.model.AppLanguage
import com.opendictate.app.model.DictationLanguage
import com.opendictate.app.model.TextTransformationModel
import com.opendictate.app.model.TranscriptionModel
import com.opendictate.app.model.TranscriptionResponseTimeout
import com.opendictate.app.service.DictationPhase
import com.opendictate.app.service.DictationStateBus

internal val Ink = Color(0xFF080808)
internal val Panel = Color(0xFF171717)
internal val PanelLight = Color(0xFF262626)
internal val White = Color(0xFFFFFFFF)
internal val Silver = Color(0xFFE3E3E3)
internal val Fog = Color(0xFFC6C6C6)

@Composable
fun OpenDictateApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val historyState by viewModel.historyState.collectAsState()
    val dictation by DictationStateBus.state.collectAsState()
    val context = LocalContext.current
    val clipboardTranscriptLabel = stringResource(R.string.clipboard_transcript_label)
    val lifecycleOwner = LocalLifecycleOwner.current
    var showKeyDialog by remember { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    val settingsScrollState = rememberScrollState()
    val maximumDisplayRefreshRate = LocalView.current.display
        ?.supportedModes
        ?.maxOfOrNull { it.refreshRate }
        ?: 0f
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.refreshPermissions() }

    LaunchedEffect(Unit) { viewModel.refreshPermissions() }
    LaunchedEffect(showHistory) {
        if (showHistory) viewModel.refreshHistory()
    }
    BackHandler(enabled = showHistory) { showHistory = false }
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    OpenDictateTheme {
        Scaffold(
            containerColor = Ink,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { padding ->
            if (showHistory) {
                TranscriptHistoryScreen(
                    state = historyState,
                    onBack = { showHistory = false },
                    onQueryChange = viewModel::updateHistoryQuery,
                    onAiSearch = viewModel::runAiHistorySearch,
                    onCopy = { transcript ->
                        val clip = ClipData.newPlainText(
                            clipboardTranscriptLabel,
                            transcript,
                        )
                        context.getSystemService(ClipboardManager::class.java)
                            .setPrimaryClip(clip)
                    },
                    onDelete = viewModel::deleteHistoryItem,
                    modifier = Modifier.padding(padding),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(settingsScrollState)
                        .preferredFrameRate(
                            if (settingsScrollState.isScrollInProgress) {
                                maximumDisplayRefreshRate
                            } else {
                                0f
                            },
                        )
                        .padding(horizontal = 20.dp),
                ) {
                    Header(onHistoryClick = { showHistory = true })
                    Hero(dictation.phase)
                    Spacer(Modifier.height(26.dp))
                    SectionLabel(stringResource(R.string.section_mode))
                    Spacer(Modifier.height(10.dp))
                    ModelDeck(state.model, viewModel::selectModel)
                    Spacer(Modifier.height(26.dp))
                    SectionLabel(stringResource(R.string.section_transformation))
                    Spacer(Modifier.height(10.dp))
                    TransformationModelCard(
                        selected = state.transformationModel,
                        buttonEnabled = state.transformationButtonEnabled,
                        settingEnabled = !dictation.isActive,
                        onSelect = viewModel::selectTransformationModel,
                        onButtonEnabledChange = viewModel::setTransformationButtonEnabled,
                    )
                    Spacer(Modifier.height(26.dp))
                    SectionLabel(stringResource(R.string.section_readiness))
                    Spacer(Modifier.height(10.dp))
                    SetupCard(
                        icon = Icons.Outlined.Key,
                        title = stringResource(R.string.api_key_title),
                        subtitle = stringResource(
                            if (state.hasApiKey) R.string.api_key_saved else R.string.api_key_needed,
                        ),
                        complete = state.hasApiKey,
                        action = stringResource(
                            if (state.hasApiKey) R.string.action_change else R.string.action_add,
                        ),
                        onClick = { showKeyDialog = true },
                    )
                    Spacer(Modifier.height(10.dp))
                    SetupCard(
                        icon = Icons.Outlined.Mic,
                        title = stringResource(R.string.microphone_title),
                        subtitle = stringResource(
                            if (state.microphoneGranted) {
                                R.string.microphone_granted
                            } else {
                                R.string.microphone_usage
                            },
                        ),
                        complete = state.microphoneGranted,
                        action = stringResource(
                            if (state.microphoneGranted) R.string.action_ready else R.string.action_allow,
                        ),
                        onClick = {
                            val permissions = buildList {
                                add(Manifest.permission.RECORD_AUDIO)
                                if (Build.VERSION.SDK_INT >= 33) {
                                    add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                            permissionLauncher.launch(permissions.toTypedArray())
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                    SetupCard(
                        icon = Icons.Outlined.SettingsAccessibility,
                        title = stringResource(R.string.keyboard_button_title),
                        subtitle = stringResource(
                            if (state.accessibilityEnabled) {
                                R.string.accessibility_enabled
                            } else {
                                R.string.accessibility_enable_hint
                            },
                        ),
                        complete = state.accessibilityEnabled,
                        action = stringResource(
                            if (state.accessibilityEnabled) {
                                R.string.action_ready
                            } else {
                                R.string.action_enable
                            },
                        ),
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                    )
                    Spacer(Modifier.height(26.dp))
                    SectionLabel(stringResource(R.string.section_accuracy))
                    Spacer(Modifier.height(10.dp))
                    PreferencesCard(
                        languages = state.languages,
                        initialPrompt = viewModel.prompt,
                        keepTrailingPeriod = state.keepTrailingPeriod,
                        onToggleLanguage = viewModel::toggleLanguage,
                        onAutomaticLanguageDetection = viewModel::useAutomaticLanguageDetection,
                        onPrompt = viewModel::savePrompt,
                        onKeepTrailingPeriod = viewModel::setKeepTrailingPeriod,
                    )
                    Spacer(Modifier.height(10.dp))
                    AdvancedSettingsCard(
                        responseTimeoutSeconds = state.transcriptionResponseTimeoutSeconds,
                        onResponseTimeoutChange = viewModel::setTranscriptionResponseTimeout,
                    )
                    Spacer(Modifier.height(10.dp))
                    TestField()
                    Spacer(Modifier.height(20.dp))
                    PrivacyNote()
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
        if (showKeyDialog) {
            ApiKeyDialog(
                hasKey = state.hasApiKey,
                loadKey = viewModel::getApiKey,
                onDismiss = { showKeyDialog = false },
                onSave = {
                    viewModel.saveApiKey(it)
                    showKeyDialog = false
                },
                onDelete = {
                    viewModel.clearApiKey()
                    showKeyDialog = false
                },
            )
        }
    }
}

@Composable
private fun Header(onHistoryClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WaveLogo()
        Spacer(Modifier.width(12.dp))
        Text(
            "OPENDICTATE",
            color = White,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            fontSize = 14.sp,
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onHistoryClick) {
            Icon(
                Icons.Outlined.History,
                contentDescription = stringResource(R.string.history_open),
                tint = Fog,
            )
        }
        AppLanguageSwitcher()
    }
}

@Composable
private fun AppLanguageSwitcher() {
    var expanded by remember { mutableStateOf(false) }
    val selected = AppLanguage.fromLanguageTags(
        AppCompatDelegate.getApplicationLocales().toLanguageTags(),
    )
    Box {
        TextButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.textButtonColors(
                containerColor = White.copy(alpha = 0.12f),
                contentColor = White,
            ),
            contentPadding = PaddingValues(horizontal = 10.dp),
        ) {
            Icon(
                Icons.Outlined.Language,
                contentDescription = stringResource(R.string.language_switcher_description),
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                selected.shortLabel(),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            AppLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.title()) },
                    onClick = {
                        expanded = false
                        AppCompatDelegate.setApplicationLocales(
                            LocaleListCompat.forLanguageTags(language.languageTag),
                        )
                    },
                    leadingIcon = {
                        if (language == selected) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = White,
                            )
                        } else {
                            Spacer(Modifier.size(24.dp))
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun Hero(phase: DictationPhase) {
    val active = phase == DictationPhase.CONNECTING || phase == DictationPhase.LISTENING
    Column {
        Text(
            text = stringResource(if (active) R.string.hero_active else R.string.hero_idle),
            color = White,
            fontSize = 42.sp,
            lineHeight = 44.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.2).sp,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(R.string.hero_subtitle),
            color = Fog,
            fontSize = 16.sp,
            lineHeight = 23.sp,
        )
    }
}

@Composable
private fun WaveLogo() {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(Ink, CircleShape)
            .border(1.dp, PanelLight, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.Mic,
            contentDescription = null,
            tint = White,
            modifier = Modifier.size(23.dp),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = Fog,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
    )
}

@Composable
private fun ModelDeck(selected: TranscriptionModel, onSelect: (TranscriptionModel) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ModelOption(
            selected = selected == TranscriptionModel.ACCURATE,
            eyebrow = "GPT TRANSCRIBE",
            title = stringResource(R.string.model_accurate_title),
            description = stringResource(R.string.model_accurate_description),
            badge = stringResource(R.string.model_accurate_badge),
            onClick = { onSelect(TranscriptionModel.ACCURATE) },
        )
        ModelOption(
            selected = selected == TranscriptionModel.LIVE,
            eyebrow = "GPT LIVE TRANSCRIBE",
            title = stringResource(R.string.model_live_title),
            description = stringResource(R.string.model_live_description),
            badge = stringResource(R.string.model_live_badge),
            onClick = { onSelect(TranscriptionModel.LIVE) },
        )
    }
}

@Composable
private fun TransformationModelCard(
    selected: TextTransformationModel,
    buttonEnabled: Boolean,
    settingEnabled: Boolean,
    onSelect: (TextTransformationModel) -> Unit,
    onButtonEnabledChange: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val buttonToggleDescription = stringResource(R.string.transformation_button_toggle)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).background(PanelLight, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.AutoFixHigh, contentDescription = null, tint = White)
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.transformation_model_title),
                        color = White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                    Text(
                        stringResource(R.string.transformation_model_subtitle),
                        color = Fog,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = buttonEnabled,
                    onCheckedChange = onButtonEnabledChange,
                    enabled = settingEnabled,
                    modifier = Modifier.semantics {
                        contentDescription = buttonToggleDescription
                    },
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PanelLight, RoundedCornerShape(12.dp)),
            ) {
                TextButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        selected.title(),
                        color = White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Outlined.ArrowDropDown,
                        contentDescription = null,
                        tint = Fog,
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    TextTransformationModel.entries.forEach { model ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(model.title(), fontWeight = FontWeight.SemiBold)
                                    Text(
                                        stringResource(model.descriptionRes()),
                                        color = Fog,
                                        fontSize = 12.sp,
                                    )
                                }
                            },
                            onClick = {
                                onSelect(model)
                                expanded = false
                            },
                            leadingIcon = {
                                if (model == selected) {
                                    Icon(Icons.Outlined.Check, contentDescription = null, tint = White)
                                } else {
                                    Spacer(Modifier.size(24.dp))
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelOption(
    selected: Boolean,
    eyebrow: String,
    title: String,
    description: String,
    badge: String,
    onClick: () -> Unit,
) {
    val border by animateColorAsState(if (selected) White else Color.Transparent, label = "model-border")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (selected) PanelLight else Panel),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .background(if (selected) White else PanelLight, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (selected) Icons.Outlined.Speed else Icons.Outlined.Check,
                    contentDescription = null,
                    tint = if (selected) Ink else Fog,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    eyebrow,
                    color = if (selected) White else Fog,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp,
                )
                Text(title, color = White, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                Text(description, color = Fog, fontSize = 13.sp, lineHeight = 18.sp)
            }
            Text(
                badge,
                color = if (selected) Ink else Fog,
                modifier = Modifier
                    .background(if (selected) White else PanelLight, RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 5.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SetupCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    complete: Boolean,
    action: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(PanelLight, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = if (complete) White else Silver)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    subtitle,
                    color = Fog,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                action,
                color = if (complete) White else Silver,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun PreferencesCard(
    languages: Set<DictationLanguage>,
    initialPrompt: String,
    keepTrailingPeriod: Boolean,
    onToggleLanguage: (DictationLanguage) -> Unit,
    onAutomaticLanguageDetection: () -> Unit,
    onPrompt: (String) -> Unit,
    onKeepTrailingPeriod: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var promptText by rememberSaveable { mutableStateOf(initialPrompt) }
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.dictation_languages_title),
                        color = White,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.dictation_languages_subtitle),
                        color = Fog,
                        fontSize = 12.sp,
                    )
                }
                Box {
                    TextButton(onClick = { expanded = true }) {
                        Text(languages.summary(), color = White)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.dictation_languages_automatic)) },
                            onClick = onAutomaticLanguageDetection,
                            leadingIcon = {
                                Checkbox(
                                    checked = languages.isEmpty(),
                                    onCheckedChange = null,
                                    colors = languageCheckboxColors(),
                                )
                            },
                        )
                        DictationLanguage.entries.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.title()) },
                                onClick = { onToggleLanguage(item) },
                                leadingIcon = {
                                    Checkbox(
                                        checked = item in languages,
                                        onCheckedChange = null,
                                        colors = languageCheckboxColors(),
                                    )
                                },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.trailing_period_title),
                        color = White,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.trailing_period_subtitle),
                        color = Fog,
                        fontSize = 12.sp,
                    )
                }
                Switch(
                    checked = keepTrailingPeriod,
                    onCheckedChange = onKeepTrailingPeriod,
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = promptText,
                onValueChange = {
                    promptText = normalizeDictionaryTerms(it).take(300)
                    onPrompt(promptText)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .keepFullBoundsInView(),
                label = { Text(stringResource(R.string.dictionary_label)) },
                placeholder = { Text(stringResource(R.string.dictionary_placeholder)) },
                supportingText = { Text(stringResource(R.string.dictionary_supporting)) },
                minLines = 1,
                maxLines = 4,
                shape = RoundedCornerShape(14.dp),
            )
        }
    }
}

@Composable
private fun AdvancedSettingsCard(
    responseTimeoutSeconds: Int?,
    onResponseTimeoutChange: (Int?) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showTimeoutDialog by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.advanced_settings_title),
                    color = Fog,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = Fog)
            }
            if (expanded) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.transcription_timeout_title),
                            color = White,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            stringResource(R.string.transcription_timeout_subtitle),
                            color = Fog,
                            fontSize = 12.sp,
                        )
                    }
                    TextButton(onClick = { showTimeoutDialog = true }) {
                        Text(
                            if (responseTimeoutSeconds == null) {
                                stringResource(R.string.transcription_timeout_unlimited)
                            } else {
                                stringResource(R.string.transcription_timeout_value, responseTimeoutSeconds)
                            },
                            color = White,
                        )
                    }
                }
            }
        }
    }
    if (showTimeoutDialog) {
        TranscriptionTimeoutDialog(
            currentSeconds = responseTimeoutSeconds,
            onDismiss = { showTimeoutDialog = false },
            onSave = {
                onResponseTimeoutChange(it)
                showTimeoutDialog = false
            },
        )
    }
}

@Composable
private fun TranscriptionTimeoutDialog(
    currentSeconds: Int?,
    onDismiss: () -> Unit,
    onSave: (Int?) -> Unit,
) {
    var limitEnabled by rememberSaveable { mutableStateOf(currentSeconds != null) }
    var secondsText by rememberSaveable {
        mutableStateOf((currentSeconds ?: TranscriptionResponseTimeout.DEFAULT_SECONDS).toString())
    }
    val seconds = secondsText.toIntOrNull()?.takeIf(TranscriptionResponseTimeout::isValid)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.transcription_timeout_title)) },
        text = {
            Column {
                Text(stringResource(R.string.transcription_timeout_explanation))
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.transcription_timeout_limit),
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = limitEnabled, onCheckedChange = { limitEnabled = it })
                }
                if (limitEnabled) {
                    OutlinedTextField(
                        value = secondsText,
                        onValueChange = { secondsText = it.filter(Char::isDigit).take(3) },
                        label = { Text(stringResource(R.string.transcription_timeout_seconds)) },
                        supportingText = {
                            Text(stringResource(R.string.transcription_timeout_range))
                        },
                        isError = seconds == null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(if (limitEnabled) seconds else null) },
                enabled = !limitEnabled || seconds != null,
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun TestField() {
    var text by remember { mutableStateOf("") }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier
            .fillMaxWidth()
            .keepFullBoundsInView(),
        label = { Text(stringResource(R.string.test_field_label)) },
        placeholder = { Text(stringResource(R.string.test_field_placeholder)) },
        minLines = 3,
        maxLines = 3,
        shape = RoundedCornerShape(18.dp),
    )
}

/** Keeps cursor relocation from moving the parent settings list after the field is focused. */
private fun Modifier.keepFullBoundsInView(): Modifier =
    this then FullBoundsBringIntoViewElement

private object FullBoundsBringIntoViewElement : ModifierNodeElement<FullBoundsBringIntoViewNode>() {
    override fun create() = FullBoundsBringIntoViewNode()

    override fun update(node: FullBoundsBringIntoViewNode) = Unit

    override fun equals(other: Any?) = other === this

    override fun hashCode() = javaClass.hashCode()
}

private class FullBoundsBringIntoViewNode :
    Modifier.Node(),
    BringIntoViewModifierNode,
    LayoutAwareModifierNode {
    private var size = IntSize.Zero

    override fun onRemeasured(size: IntSize) {
        this.size = size
    }

    override suspend fun bringIntoView(
        childCoordinates: LayoutCoordinates,
        boundsProvider: () -> Rect?,
    ) {
        bringIntoView {
            if (size == IntSize.Zero) {
                boundsProvider()
            } else {
                Rect(
                    left = 0f,
                    top = 0f,
                    right = size.width.toFloat(),
                    bottom = size.height.toFloat(),
                )
            }
        }
    }
}

@Composable
private fun PrivacyNote() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(15.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Outlined.Lock, contentDescription = null, tint = White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(R.string.privacy_note),
            color = Fog,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
    }
}

@Composable
private fun ApiKeyDialog(
    hasKey: Boolean,
    loadKey: () -> String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var value by remember { mutableStateOf(if (hasKey) loadKey() else "") }
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (hasKey) R.string.api_key_dialog_replace else R.string.api_key_dialog_connect,
                ),
            )
        },
        text = {
            Column {
                Text(stringResource(R.string.api_key_dialog_description))
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.trim().take(256) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.api_key_title)) },
                    placeholder = { Text("sk-…") },
                    visualTransformation = PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val clip = ClipData.newPlainText("OpenAI API key", value).apply {
                                    description.extras = PersistableBundle().apply {
                                        putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                                    }
                                }
                                context.getSystemService(ClipboardManager::class.java)
                                    .setPrimaryClip(clip)
                            },
                            enabled = value.isNotBlank(),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = stringResource(R.string.api_key_copy),
                            )
                        }
                    },
                    singleLine = true,
                )
                if (hasKey) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.api_key_dialog_delete), color = Silver)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(value) },
                enabled = value.length >= 20,
                colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Ink),
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        containerColor = Panel,
        titleContentColor = White,
        textContentColor = Fog,
    )
}

@Composable
private fun Set<DictationLanguage>.summary(): String = when (size) {
    0 -> stringResource(R.string.dictation_languages_summary_auto)
    1 -> first().title()
    else -> stringResource(R.string.dictation_languages_summary_count, size)
}

@Composable
private fun AppLanguage.shortLabel(): String = when (this) {
    AppLanguage.SYSTEM -> stringResource(R.string.language_system_short)
    AppLanguage.RUSSIAN -> "RU"
    AppLanguage.ENGLISH -> "EN"
}

@Composable
private fun AppLanguage.title(): String = when (this) {
    AppLanguage.SYSTEM -> stringResource(R.string.language_system)
    AppLanguage.RUSSIAN -> stringResource(R.string.language_russian)
    AppLanguage.ENGLISH -> stringResource(R.string.language_english)
}

private fun DictationLanguage.title(): String = when (this) {
    DictationLanguage.RUSSIAN -> "Русский"
    DictationLanguage.ENGLISH -> "English"
    DictationLanguage.UKRAINIAN -> "Українська"
    DictationLanguage.GERMAN -> "Deutsch"
    DictationLanguage.FRENCH -> "Français"
    DictationLanguage.SPANISH -> "Español"
    DictationLanguage.ITALIAN -> "Italiano"
    DictationLanguage.PORTUGUESE -> "Português"
    DictationLanguage.POLISH -> "Polski"
    DictationLanguage.TURKISH -> "Türkçe"
    DictationLanguage.CHINESE -> "中文"
    DictationLanguage.JAPANESE -> "日本語"
    DictationLanguage.KOREAN -> "한국어"
    DictationLanguage.ARABIC -> "العربية"
    DictationLanguage.HINDI -> "हिन्दी"
}

private fun TextTransformationModel.title(): String = when (this) {
    TextTransformationModel.LUNA -> "GPT-6 Luna"
    TextTransformationModel.SOL -> "GPT-6 Sol"
}

private fun TextTransformationModel.descriptionRes(): Int = when (this) {
    TextTransformationModel.LUNA -> R.string.transformation_model_luna_description
    TextTransformationModel.SOL -> R.string.transformation_model_sol_description
}

@Composable
private fun languageCheckboxColors() = CheckboxDefaults.colors(
    checkedColor = White,
    checkmarkColor = Ink,
    uncheckedColor = Fog,
)

@Composable
private fun OpenDictateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            primary = White,
            secondary = Silver,
            tertiary = Fog,
            background = Ink,
            surface = Panel,
            surfaceVariant = PanelLight,
            primaryContainer = PanelLight,
            secondaryContainer = PanelLight,
            error = Silver,
            onPrimary = Ink,
            onSecondary = Ink,
            onPrimaryContainer = White,
            onSecondaryContainer = White,
            onSurfaceVariant = Fog,
            onError = Ink,
            onBackground = White,
            onSurface = White,
            outline = Fog.copy(alpha = 0.45f),
            surfaceTint = Color.Transparent,
        ),
        typography = MaterialTheme.typography.copy(
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.SansSerif),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.SansSerif),
        ),
        content = content,
    )
}
