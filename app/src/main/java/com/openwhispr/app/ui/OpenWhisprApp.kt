@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.openwhispr.app.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Key
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openwhispr.app.data.normalizeDictionaryTerms
import com.openwhispr.app.model.DictationLanguage
import com.openwhispr.app.model.TranscriptionModel
import com.openwhispr.app.service.DictationPhase
import com.openwhispr.app.service.DictationStateBus
import kotlin.math.PI
import kotlin.math.sin

private val Ink = Color(0xFF101426)
private val Panel = Color(0xFF191F36)
private val PanelLight = Color(0xFF222A46)
private val Mint = Color(0xFF7DE4C4)
private val Coral = Color(0xFFFF6A6E)
private val Fog = Color(0xFFAAB2CB)
private val White = Color(0xFFF6F7FC)

@Composable
fun OpenWhisprApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val dictation by DictationStateBus.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showKeyDialog by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.refreshPermissions() }

    LaunchedEffect(Unit) { viewModel.refreshPermissions() }
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    OpenWhisprTheme {
        Scaffold(
            containerColor = Ink,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp),
            ) {
                Header()
                Hero(dictation.phase)
                Spacer(Modifier.height(26.dp))
                SectionLabel("РЕЖИМ")
                Spacer(Modifier.height(10.dp))
                ModelDeck(state.model, viewModel::selectModel)
                Spacer(Modifier.height(26.dp))
                SectionLabel("ГОТОВНОСТЬ")
                Spacer(Modifier.height(10.dp))
                SetupCard(
                    icon = Icons.Outlined.Key,
                    title = "OpenAI API key",
                    subtitle = if (state.hasApiKey) "Сохранён в Android Keystore" else "Нужен ваш личный ключ",
                    complete = state.hasApiKey,
                    action = if (state.hasApiKey) "ИЗМЕНИТЬ" else "ДОБАВИТЬ",
                    onClick = { showKeyDialog = true },
                )
                Spacer(Modifier.height(10.dp))
                SetupCard(
                    icon = Icons.Outlined.Mic,
                    title = "Микрофон",
                    subtitle = if (state.microphoneGranted) "Разрешение выдано" else "Только во время диктовки",
                    complete = state.microphoneGranted,
                    action = if (state.microphoneGranted) "ГОТОВО" else "РАЗРЕШИТЬ",
                    onClick = {
                        val permissions = buildList {
                            add(Manifest.permission.RECORD_AUDIO)
                            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        permissionLauncher.launch(permissions.toTypedArray())
                    },
                )
                Spacer(Modifier.height(10.dp))
                SetupCard(
                    icon = Icons.Outlined.SettingsAccessibility,
                    title = "Кнопка над клавиатурой",
                    subtitle = if (state.accessibilityEnabled) "Сервис специальных возможностей включён" else "Включите OpenWhispr в настройках",
                    complete = state.accessibilityEnabled,
                    action = if (state.accessibilityEnabled) "ГОТОВО" else "ВКЛЮЧИТЬ",
                    onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                )
                Spacer(Modifier.height(26.dp))
                SectionLabel("ТОЧНОСТЬ")
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
                Spacer(Modifier.height(20.dp))
                TestField()
                Spacer(Modifier.height(20.dp))
                PrivacyNote()
                Spacer(Modifier.height(28.dp))
            }
        }
        if (showKeyDialog) {
            ApiKeyDialog(
                hasKey = state.hasApiKey,
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
private fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WaveLogo()
        Spacer(Modifier.width(12.dp))
        Text(
            "OPENWHISPR",
            color = White,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            fontSize = 14.sp,
        )
        Spacer(Modifier.weight(1f))
        Surface(color = Mint.copy(alpha = 0.12f), shape = RoundedCornerShape(50)) {
            Text(
                "ON DEVICE",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                color = Mint,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 0.8.sp,
            )
        }
    }
}

@Composable
private fun Hero(phase: DictationPhase) {
    val active = phase == DictationPhase.CONNECTING || phase == DictationPhase.LISTENING
    Column {
        Text(
            text = if (active) "Говорите.\nЯ уже пишу." else "Говорите.\nТекст уже там.",
            color = White,
            fontSize = 42.sp,
            lineHeight = 44.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.2).sp,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Одна кнопка над клавиатурой — в любом приложении.",
            color = Fog,
            fontSize = 16.sp,
            lineHeight = 23.sp,
        )
    }
}

@Composable
private fun WaveLogo() {
    Canvas(
        modifier = Modifier
            .size(38.dp)
            .background(Mint, CircleShape)
            .padding(8.dp),
    ) {
        val path = Path()
        repeat(20) { index ->
            val f = index / 19f
            val x = size.width * f
            val envelope = sin(PI * f).toFloat()
            val y = size.height / 2 + sin(f * PI.toFloat() * 4) * size.height * 0.3f * envelope
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, Ink, style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round))
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
            title = "Точнее",
            description = "Сначала слушает, затем вставляет готовый текст.",
            badge = "ПО УМОЛЧАНИЮ",
            onClick = { onSelect(TranscriptionModel.ACCURATE) },
        )
        ModelOption(
            selected = selected == TranscriptionModel.LIVE,
            eyebrow = "GPT LIVE TRANSCRIBE",
            title = "Мгновенно",
            description = "Показывает слова прямо во время речи.",
            badge = "ВО ВРЕМЯ РЕЧИ",
            onClick = { onSelect(TranscriptionModel.LIVE) },
        )
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
    val border by animateColorAsState(if (selected) Mint else Color.Transparent, label = "model-border")
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
                    .background(if (selected) Mint else PanelLight, CircleShape),
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
                    color = if (selected) Mint else Fog,
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
                    .background(if (selected) Mint else PanelLight, RoundedCornerShape(6.dp))
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
                Icon(icon, contentDescription = null, tint = if (complete) Mint else Coral)
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
                color = if (complete) Mint else Coral,
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
                    Text("Языки диктовки", color = White, fontWeight = FontWeight.SemiBold)
                    Text("Выберите один или несколько", color = Fog, fontSize = 12.sp)
                }
                Box {
                    TextButton(onClick = { expanded = true }) {
                        Text(languages.summary(), color = Mint)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Определять автоматически") },
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
                    Text("Точка в конце", color = White, fontWeight = FontWeight.SemiBold)
                    Text("Добавлять точку после последней фразы", color = Fog, fontSize = 12.sp)
                }
                Switch(
                    checked = keepTrailingPeriod,
                    onCheckedChange = onKeepTrailingPeriod,
                )
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = promptText,
                onValueChange = {
                    promptText = normalizeDictionaryTerms(it).take(300)
                    onPrompt(promptText)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Словарь (необязательно)") },
                placeholder = { Text("Kotlin\nOpenWhispr\nCompose") },
                supportingText = { Text("Каждое имя, термин или редкое слово — с новой строки") },
                minLines = 2,
                shape = RoundedCornerShape(14.dp),
            )
        }
    }
}

@Composable
private fun TestField() {
    var text by remember { mutableStateOf("") }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Попробуйте здесь") },
        placeholder = { Text("Откройте клавиатуру — кнопка появится над ней") },
        minLines = 3,
        shape = RoundedCornerShape(18.dp),
    )
}

@Composable
private fun PrivacyNote() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Mint.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(15.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Outlined.Lock, contentDescription = null, tint = Mint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "Ключ и записи не проходят через сервер OpenWhispr. Аудио отправляется напрямую в OpenAI только во время диктовки.",
            color = Fog,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
    }
}

@Composable
private fun ApiKeyDialog(
    hasKey: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hasKey) "Заменить API key" else "Подключить OpenAI") },
        text = {
            Column {
                Text("Ключ шифруется Android Keystore и остаётся на этом устройстве.")
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.trim().take(256) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("OpenAI API key") },
                    placeholder = { Text("sk-…") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                if (hasKey) {
                    TextButton(onClick = onDelete) { Text("Удалить сохранённый ключ", color = Coral) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(value) },
                enabled = value.length >= 20,
                colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Ink),
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        containerColor = Panel,
        titleContentColor = White,
        textContentColor = Fog,
    )
}

private fun Set<DictationLanguage>.summary(): String = when (size) {
    0 -> "Авто"
    1 -> first().title()
    else -> "Выбрано: $size"
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

@Composable
private fun languageCheckboxColors() = CheckboxDefaults.colors(
    checkedColor = Mint,
    checkmarkColor = Ink,
    uncheckedColor = Fog,
)

@Composable
private fun OpenWhisprTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            primary = Mint,
            secondary = Coral,
            background = Ink,
            surface = Panel,
            onPrimary = Ink,
            onBackground = White,
            onSurface = White,
            outline = Fog.copy(alpha = 0.45f),
        ),
        typography = MaterialTheme.typography.copy(
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.SansSerif),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.SansSerif),
        ),
        content = content,
    )
}
