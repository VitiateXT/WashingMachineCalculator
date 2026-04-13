package de.moritz.waschzeitrechner

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import de.moritz.waschzeitrechner.ui.theme.WashingMachineAppTheme
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val LocalDateTimeSaver = Saver<LocalDateTime, String>(
    save = { it.toString() },
    restore = { LocalDateTime.parse(it) }
)

private val WashProgramSaver = Saver<WashProgram, String>(
    save = { it.storageKey },
    restore = { WashProgram.fromStorageKey(it) }
)

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsStore = AppSettingsStore(this)

        setContent {
            var themeMode by remember { mutableStateOf(settingsStore.getThemeMode()) }

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            WashingMachineAppTheme(darkTheme = darkTheme) {
                AppScaffold(
                    themeMode = themeMode,
                    settingsStore = settingsStore,
                    onThemeModeChange = { newMode ->
                        themeMode = newMode
                        settingsStore.setThemeMode(newMode)
                    }
                )
            }
        }
    }
}

@Composable
fun AppScaffold(
    themeMode: ThemeMode,
    settingsStore: AppSettingsStore,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var settingsVersion by remember { mutableIntStateOf(0) }
    val tabStateHolder = rememberSaveableStateHolder()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontSize = 30.sp
                    )
                }
                HorizontalDivider()
                TabRow(selectedTabIndex = selectedTabIndex) {
                    val tabTitles = listOf(
                        stringResource(R.string.tab_wash_calculator),
                        stringResource(R.string.tab_wash_settings)
                    )
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(text = title) }
                        )
                    }
                }

                when (selectedTabIndex) {
                    0 -> tabStateHolder.SaveableStateProvider(key = "calculator_tab") {
                        WaschzeitrechnerScreen(
                            settingsStore = settingsStore,
                            settingsVersion = settingsVersion
                        )
                    }

                    else -> tabStateHolder.SaveableStateProvider(key = "settings_tab") {
                        WaschzeiteinstellungenScreen(
                            settingsStore = settingsStore,
                            onSettingsChanged = { settingsVersion += 1 }
                        )
                    }
                }
            }

            SettingsFab(
                onClick = { showSettings = true },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }

    if (showSettings) {
        SettingsDialog(
            currentThemeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            onDismiss = { showSettings = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaschzeitrechnerScreen(
    settingsStore: AppSettingsStore,
    settingsVersion: Int
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dateTimeFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm") }
    val hoursShortLabel = stringResource(R.string.hours_short_label)
    val minutesShortLabel = stringResource(R.string.minutes_short_label)

    var now by remember { mutableStateOf(LocalDateTime.now().withSecond(0).withNano(0)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = LocalDateTime.now().withSecond(0).withNano(0)
        }
    }

    val initialProgram = settingsStore.getDefaultProgram()
    val initialDurationParts = splitDurationMinutes(
        settingsStore.getProgramDurationMinutes(initialProgram)
    )
    val savedDefaultProgram = remember(settingsVersion) { settingsStore.getDefaultProgram() }
    val savedPreferredFinishTime = remember(settingsVersion) { settingsStore.getPreferredFinishTime() }
    val savedProgramDurations = remember(settingsVersion) {
        WashProgram.entries.associateWith { program ->
            settingsStore.getProgramDurationMinutes(program)
        }
    }
    val smartRecommendationsEnabled = remember(settingsVersion) {
        settingsStore.getSmartRecommendationsEnabled()
    }

    var finishDateTime by rememberSaveable(stateSaver = LocalDateTimeSaver) {
        mutableStateOf(settingsStore.getInitialFinishDateTime(LocalDateTime.now().withSecond(0).withNano(0)))
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedProgram by rememberSaveable(stateSaver = WashProgramSaver) { mutableStateOf(initialProgram) }
    var washHours by rememberSaveable { mutableIntStateOf(initialDurationParts.first) }
    var washMinutes by rememberSaveable { mutableIntStateOf(initialDurationParts.second) }

    val currentDurationMinutes = composeDurationMinutes(washHours, washMinutes)
    val recommendation = remember(
        now,
        finishDateTime,
        selectedProgram,
        currentDurationMinutes,
        savedDefaultProgram,
        savedPreferredFinishTime,
        savedProgramDurations
    ) {
        buildSmartRecommendation(
            now = now,
            finishDateTime = finishDateTime,
            currentProgram = selectedProgram,
            currentDurationMinutes = currentDurationMinutes,
            defaultProgram = savedDefaultProgram,
            preferredFinishTime = savedPreferredFinishTime,
            programDurations = savedProgramDurations
        )
    }

    val schedule = remember(now, finishDateTime, currentDurationMinutes) {
        WashScheduleCalculator.calculate(
            now = now,
            finishDateTime = finishDateTime,
            washDurationMinutes = currentDurationMinutes
        )
    }
    val startTimeText = schedule.startDateTime?.let { startDateTime ->
        if (startDateTime.toLocalDate() == now.toLocalDate()) {
            startDateTime.format(timeFormatter)
        } else {
            startDateTime.format(dateTimeFormatter)
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = finishDateTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        finishDateTime = LocalDateTime.of(date, finishDateTime.toLocalTime())
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = finishDateTime.hour,
            initialMinute = finishDateTime.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    finishDateTime = finishDateTime
                        .withHour(timePickerState.hour)
                        .withMinute(timePickerState.minute)
                        .withSecond(0)
                    showTimePicker = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            text = { TimePicker(state = timePickerState) },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(text = stringResource(R.string.current_time_label), fontSize = 14.sp)
            Text(text = now.format(timeFormatter), fontSize = 22.sp)

            Text(text = stringResource(R.string.finish_time_label), fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Text(finishDateTime.format(dateFormatter))
                }
                OutlinedButton(onClick = { showTimePicker = true }) {
                    Text(finishDateTime.format(timeFormatter))
                }
            }

            Text(text = stringResource(R.string.program_label), fontSize = 14.sp)
            ProgramDropdown(
                selectedProgram = selectedProgram,
                onProgramSelected = { program ->
                    selectedProgram = program
                    val durationParts = splitDurationMinutes(
                        settingsStore.getProgramDurationMinutes(program)
                    )
                    washHours = durationParts.first
                    washMinutes = durationParts.second
                }
            )

            Text(text = stringResource(R.string.wash_duration_label), fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TimeDropdown(
                    label = hoursShortLabel,
                    value = washHours,
                    range = 0..5,
                    onValueChange = { updatedHours -> washHours = updatedHours }
                )
                TimeDropdown(
                    label = minutesShortLabel,
                    value = washMinutes,
                    range = 0..59,
                    onValueChange = { updatedMinutes -> washMinutes = updatedMinutes }
                )
            }

            if (shouldShowSmartDurationRecommendation(
                    smartRecommendationsEnabled = smartRecommendationsEnabled,
                    currentProgram = selectedProgram,
                    currentDurationMinutes = currentDurationMinutes,
                    recommendation = recommendation
                )) {
                SmartRecommendationCard(
                    recommendation = recommendation!!,
                    onApplyRecommendation = {
                        val durationParts = splitDurationMinutes(recommendation.durationMinutes)
                        selectedProgram = recommendation.program
                        washHours = durationParts.first
                        washMinutes = durationParts.second
                    }
                )
            }

            if (schedule.isValid && startTimeText != null) {
                Text(text = stringResource(R.string.start_time_label), fontSize = 14.sp)
                Text(text = startTimeText, fontSize = 22.sp)
            } else {
                Text(
                    text = stringResource(schedule.messageResId),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Text(
            text = stringResource(R.string.app_version_footer, BuildConfig.VERSION_NAME),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaschzeiteinstellungenScreen(
    settingsStore: AppSettingsStore,
    onSettingsChanged: () -> Unit
) {
    val savedFinishTime = remember { settingsStore.getPreferredFinishTime() }
    val fallbackFinishDateTime = remember {
        settingsStore.getInitialFinishDateTime(LocalDateTime.now().withSecond(0).withNano(0))
    }
    val hoursShortLabel = stringResource(R.string.hours_short_label)
    val minutesShortLabel = stringResource(R.string.minutes_short_label)

    var showTimePicker by remember { mutableStateOf(false) }
    var defaultFinishHour by remember {
        mutableIntStateOf(savedFinishTime?.hour ?: fallbackFinishDateTime.hour)
    }
    var defaultFinishMinute by remember {
        mutableIntStateOf(savedFinishTime?.minute ?: fallbackFinishDateTime.minute)
    }
    var defaultProgram by remember { mutableStateOf(settingsStore.getDefaultProgram()) }
    var smartRecommendationsEnabled by remember {
        mutableStateOf(settingsStore.getSmartRecommendationsEnabled())
    }
    var programDurations by remember {
        mutableStateOf(
            WashProgram.entries.associateWith { program ->
                settingsStore.getProgramDurationMinutes(program)
            }
        )
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = defaultFinishHour,
            initialMinute = defaultFinishMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    defaultFinishHour = timePickerState.hour
                    defaultFinishMinute = timePickerState.minute
                    settingsStore.setPreferredFinishTime(
                        hour = timePickerState.hour,
                        minute = timePickerState.minute
                    )
                    onSettingsChanged()
                    showTimePicker = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            text = { TimePicker(state = timePickerState) },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(text = stringResource(R.string.default_finish_time_label), fontSize = 14.sp)
            OutlinedButton(onClick = { showTimePicker = true }) {
                Text(text = "%02d:%02d".format(defaultFinishHour, defaultFinishMinute))
            }

            Text(text = stringResource(R.string.default_program_label), fontSize = 14.sp)
            ProgramDropdown(
                selectedProgram = defaultProgram,
                onProgramSelected = { program ->
                    defaultProgram = program
                    settingsStore.setDefaultProgram(program)
                    onSettingsChanged()
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.padding(end = 16.dp)) {
                    Text(text = stringResource(R.string.smart_recommendations_label), fontSize = 14.sp)
                    Text(
                        text = stringResource(R.string.smart_recommendations_description),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = smartRecommendationsEnabled,
                    onCheckedChange = { enabled ->
                        smartRecommendationsEnabled = enabled
                        settingsStore.setSmartRecommendationsEnabled(enabled)
                        onSettingsChanged()
                    }
                )
            }

            Text(text = stringResource(R.string.program_durations_label), fontSize = 14.sp)
            WashProgram.entries.forEach { program ->
                ProgramDurationRow(
                    program = program,
                    durationMinutes = programDurations.getValue(program),
                    hoursShortLabel = hoursShortLabel,
                    minutesShortLabel = minutesShortLabel,
                    onDurationChanged = { updatedMinutes ->
                        val safeDuration = updatedMinutes.coerceAtLeast(1)
                        programDurations = programDurations.toMutableMap().apply {
                            put(program, safeDuration)
                        }
                        settingsStore.setProgramDurationMinutes(program, safeDuration)
                        onSettingsChanged()
                    }
                )
            }
        }

        Text(
            text = stringResource(R.string.app_version_footer, BuildConfig.VERSION_NAME),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SmartRecommendationCard(
    recommendation: WashRecommendation,
    onApplyRecommendation: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.smart_recommendation_message,
                    stringResource(recommendation.program.labelResId),
                    formatDuration(recommendation.durationMinutes)
                )
            )
            Text(
                text = stringResource(recommendation.messageResId),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onApplyRecommendation) {
                Text(text = stringResource(R.string.apply_recommendation_button))
            }
        }
    }
}

@Composable
private fun ProgramDurationRow(
    program: WashProgram,
    durationMinutes: Int,
    hoursShortLabel: String,
    minutesShortLabel: String,
    onDurationChanged: (Int) -> Unit
) {
    val durationParts = splitDurationMinutes(durationMinutes)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = stringResource(program.labelResId), fontSize = 16.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TimeDropdown(
                label = hoursShortLabel,
                value = durationParts.first,
                range = 0..5,
                onValueChange = { updatedHours ->
                    onDurationChanged(
                        composeDurationMinutes(updatedHours, durationParts.second)
                    )
                }
            )
            TimeDropdown(
                label = minutesShortLabel,
                value = durationParts.second,
                range = 0..59,
                onValueChange = { updatedMinutes ->
                    onDurationChanged(
                        composeDurationMinutes(durationParts.first, updatedMinutes)
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDropdown(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.width(130.dp)
    ) {
        TextField(
            value = "%02d  %s".format(value, label),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            range.forEach { item ->
                DropdownMenuItem(
                    text = { Text("%02d".format(item)) },
                    onClick = {
                        onValueChange(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramDropdown(
    selectedProgram: WashProgram,
    onProgramSelected: (WashProgram) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.width(260.dp)
    ) {
        TextField(
            value = stringResource(selectedProgram.labelResId),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            WashProgram.entries.forEach { program ->
                DropdownMenuItem(
                    text = { Text(stringResource(program.labelResId)) },
                    onClick = {
                        expanded = false
                        onProgramSelected(program)
                    }
                )
            }
        }
    }
}

@Composable
private fun formatDuration(durationMinutes: Int): String {
    val durationParts = splitDurationMinutes(durationMinutes)
    return "%02d %s %02d %s".format(
        durationParts.first,
        stringResource(R.string.hours_short_label),
        durationParts.second,
        stringResource(R.string.minutes_short_label)
    )
}