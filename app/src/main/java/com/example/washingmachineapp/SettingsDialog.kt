package de.moritz.waschzeitrechner

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat

/** Floating action button in the bottom-right corner that opens the settings dialog. */
@Composable
fun SettingsFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SmallFloatingActionButton(
        onClick = onClick,
        modifier = modifier.padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = stringResource(R.string.settings_button_description)
        )
    }
}

/**
 * Modal dialog for quickly switching the app language and theme.
 *
 * The theme change is applied immediately via [onThemeModeChange]; the
 * language selection is only committed when the user presses the confirm
 * button, since switching locales recreates the activity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    // Determine current language selection from AppCompat
    val currentLocaleList = AppCompatDelegate.getApplicationLocales()
    val currentLocaleTag = if (currentLocaleList.isEmpty) {
        ""
    } else {
        currentLocaleList[0]?.toLanguageTag() ?: ""
    }
    val currentLanguage = SupportedLanguages.firstOrNull { lang ->
        lang.tag.isNotEmpty() && currentLocaleTag.startsWith(lang.tag)
    } ?: SupportedLanguages[0]

    // Pending selection — only applied when the user presses OK
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_dialog_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Language section
                Text(text = stringResource(R.string.settings_language_label), fontSize = 14.sp)
                LanguageDropdown(
                    currentLanguage = selectedLanguage,
                    onLanguageSelected = { selected -> selectedLanguage = selected }
                )

                // Theme section
                Text(text = stringResource(R.string.settings_theme_label), fontSize = 14.sp)
                ThemeDropdown(
                    currentThemeMode = currentThemeMode,
                    onThemeModeChange = onThemeModeChange
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val newLocales = if (selectedLanguage.tag.isEmpty()) {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(selectedLanguage.tag)
                }
                onDismiss()
                AppCompatDelegate.setApplicationLocales(newLocales)
            }) {
                Text(stringResource(R.string.confirm))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDropdown(
    currentLanguage: SupportedLanguage,
    onLanguageSelected: (SupportedLanguage) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val systemDefaultLabel = stringResource(R.string.settings_language_system_default)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.width(260.dp)
    ) {
        TextField(
            value = if (currentLanguage.tag.isEmpty()) systemDefaultLabel else currentLanguage.nativeName,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SupportedLanguages.forEach { lang ->
                DropdownMenuItem(
                    text = {
                        Text(if (lang.tag.isEmpty()) systemDefaultLabel else lang.nativeName)
                    },
                    onClick = {
                        expanded = false
                        onLanguageSelected(lang)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeDropdown(
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val systemLabel = stringResource(R.string.settings_theme_system_default)
    val lightLabel = stringResource(R.string.settings_theme_light)
    val darkLabel = stringResource(R.string.settings_theme_dark)

    val themeOptions = listOf(
        ThemeMode.SYSTEM to systemLabel,
        ThemeMode.LIGHT to lightLabel,
        ThemeMode.DARK to darkLabel
    )

    val currentLabel = themeOptions.first { it.first == currentThemeMode }.second

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.width(260.dp)
    ) {
        TextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            themeOptions.forEach { (mode, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        onThemeModeChange(mode)
                    }
                )
            }
        }
    }
}
