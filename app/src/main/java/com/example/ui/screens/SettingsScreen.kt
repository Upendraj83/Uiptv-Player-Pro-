package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.ParentalSettings
import com.example.data.model.ThemePalette
import com.example.ui.ChannelHealthScanState
import com.example.ui.components.ChannelHealthScanBanner
import com.example.ui.theme.LocalTiweeColors
import com.example.ui.theme.TiweeAmber
import com.example.ui.theme.TiweeCyan
import com.example.ui.theme.TiweeEmerald
import com.example.ui.theme.TiweeFavoriteRose
import com.example.ui.theme.TiweeLiveRed
import com.example.ui.theme.TiweePurple

@Composable
fun SettingsScreen(
  userDisplayName: String?,
  userEmail: String?,
  userPhotoUrl: String?,
  isCloudSynced: Boolean,
  cloudSyncError: String?,
  themePalette: ThemePalette,
  favoriteCount: Int,
  sleepRemainingMinutes: Int?,
  parentalSettings: ParentalSettings,
  channelCount: Int,
  countryCount: Int,
  categoryCount: Int,
  brokenChannelCount: Int = 0,
  healthScanState: ChannelHealthScanState = ChannelHealthScanState.Idle,
  onNavigateFavorites: () -> Unit,
  onToggleTheme: () -> Unit,
  onSetSleepTimer: (Int?) -> Unit,
  onSetAllowAdultChannels: (Boolean) -> Unit,
  onSetParentalPin: (String?) -> Unit,
  onRefreshCatalog: () -> Unit,
  onScanBrokenChannels: () -> Unit = {},
  onCancelHealthScan: () -> Unit = {},
  onDismissHealthScan: () -> Unit = {},
  onRestoreBrokenChannels: () -> Unit = {},
  onSignOut: () -> Unit,
) {
  val extColors = LocalTiweeColors.current
  var showSleepDialog by remember { mutableStateOf(false) }
  var showParentalDialog by remember { mutableStateOf(false) }

  if (showSleepDialog) {
    SleepTimerDialog(
      currentMinutes = sleepRemainingMinutes,
      onSelectMinutes = { mins ->
        onSetSleepTimer(mins)
        showSleepDialog = false
      },
      onDismiss = { showSleepDialog = false },
    )
  }

  if (showParentalDialog) {
    ParentalControlDialog(
      parentalSettings = parentalSettings,
      onSetAllowAdult = onSetAllowAdultChannels,
      onSetPin = onSetParentalPin,
      onDismiss = { showParentalDialog = false },
    )
  }

  val listState = rememberLazyListState()

  LazyColumn(
    state = listState,
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier
      .fillMaxSize()
      .testTag("settings_screen"),
  ) {
    // Cloud Sync Account Card
    item(key = "account_card", contentType = "account_card") {
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extColors.cardBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              if (!userPhotoUrl.isNullOrBlank()) {
                var photoFailed by remember(userPhotoUrl) { mutableStateOf(false) }
                if (!photoFailed) {
                  AsyncImage(
                    model = userPhotoUrl,
                    contentDescription = userDisplayName ?: "Profile avatar",
                    contentScale = ContentScale.Crop,
                    onError = { photoFailed = true },
                    modifier = Modifier
                      .size(48.dp)
                      .clip(CircleShape),
                  )
                } else {
                  ProfileAvatarFallback()
                }
              } else {
                ProfileAvatarFallback()
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = userDisplayName ?: "UiPTv Viewer",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = userEmail ?: "Google Account",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(999.dp),
              color = (if (isCloudSynced) TiweeEmerald else TiweeAmber).copy(alpha = 0.16f),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              ) {
                Icon(
                  imageVector = if (isCloudSynced) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                  contentDescription = null,
                  tint = if (isCloudSynced) TiweeEmerald else TiweeAmber,
                  modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                  text = if (isCloudSynced) "Synced" else "Connecting",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = if (isCloudSynced) TiweeEmerald else TiweeAmber,
                )
              }
            }
          }

          if (!cloudSyncError.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Sync notice: $cloudSyncError",
              style = MaterialTheme.typography.bodySmall,
              color = extColors.warningAmber,
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          OutlinedButton(
            onClick = onSignOut,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, TiweeLiveRed.copy(alpha = 0.45f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TiweeLiveRed),
            modifier = Modifier
              .fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("settings_sign_out_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.Logout,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out")
          }
        }
      }
    }

    // Settings Action Tiles
    item(key = "saved_channels_tile", contentType = "action_tile") {
      SettingsActionTile(
        icon = Icons.Rounded.Favorite,
        accent = TiweeFavoriteRose,
        title = "Saved Channels",
        subtitle = "$favoriteCount channels synced across devices",
        trailingText = "Open",
        testTag = "settings_saved_channels_tile",
        onClick = onNavigateFavorites,
      )
    }

    item(key = "theme_tile", contentType = "action_tile") {
      SettingsActionTile(
        icon = Icons.Rounded.DarkMode,
        accent = TiweePurple,
        title = "Theme Palette",
        subtitle = if (themePalette == ThemePalette.AMOLED) "AMOLED True Black (#000000)" else "Dark Purple (#1C1829)",
        trailingText = "Switch",
        testTag = "settings_theme_tile",
        onClick = onToggleTheme,
      )
    }

    item(key = "sleep_timer_tile", contentType = "action_tile") {
      SettingsActionTile(
        icon = Icons.Rounded.Bedtime,
        accent = TiweeCyan,
        title = "Sleep Timer",
        subtitle = if (sleepRemainingMinutes != null) "Stops playback in $sleepRemainingMinutes min" else "Off",
        trailingText = if (sleepRemainingMinutes != null) "${sleepRemainingMinutes}m" else "Off",
        testTag = "settings_sleep_timer_tile",
        onClick = { showSleepDialog = true },
      )
    }

    item(key = "parental_tile", contentType = "action_tile") {
      SettingsActionTile(
        icon = Icons.Rounded.Shield,
        accent = TiweeAmber,
        title = "Parental Control",
        subtitle = buildString {
          append(if (parentalSettings.allowAdultChannels) "18+ channels enabled" else "18+ channels hidden")
          if (!parentalSettings.pin.isNullOrBlank()) append(" • PIN locked")
        },
        trailingText = if (parentalSettings.pin.isNullOrBlank()) "Configure" else "PIN Set",
        testTag = "settings_parental_tile",
        onClick = { showParentalDialog = true },
      )
    }

    item(key = "refresh_catalog_tile", contentType = "action_tile") {
      SettingsActionTile(
        icon = Icons.Rounded.Refresh,
        accent = TiweeEmerald,
        title = "Refresh IPTV Catalog",
        subtitle = "Re-fetch latest streams and channel logos",
        trailingText = "Sync Now",
        testTag = "settings_refresh_catalog_tile",
        onClick = onRefreshCatalog,
      )
    }

    item(key = "clean_broken_channels_tile", contentType = "action_tile") {
      val isScanning = healthScanState is ChannelHealthScanState.Scanning
      SettingsActionTile(
        icon = Icons.Rounded.CleaningServices,
        accent = TiweeAmber,
        title = "Remove Not Working Channels",
        subtitle = if (brokenChannelCount > 0) {
          "$brokenChannelCount offline channel(s) currently hidden"
        } else {
          "Check India, Pakistan, Bangladesh & top streams"
        },
        trailingText = if (isScanning) "Checking..." else "Scan Now",
        testTag = "settings_clean_channels_tile",
        onClick = { if (!isScanning) onScanBrokenChannels() },
      )
    }

    if (healthScanState !is ChannelHealthScanState.Idle) {
      item(key = "health_scan_banner", contentType = "scan_banner") {
        ChannelHealthScanBanner(
          scanState = healthScanState,
          onCancel = onCancelHealthScan,
          onDismiss = onDismissHealthScan,
        )
      }
    }

    if (brokenChannelCount > 0) {
      item(key = "restore_broken_channels_tile", contentType = "action_tile") {
        SettingsActionTile(
          icon = Icons.Rounded.Restore,
          accent = TiweeCyan,
          title = "Restore Hidden Channels",
          subtitle = "Re-enable $brokenChannelCount previously removed channel(s)",
          trailingText = "Restore",
          testTag = "settings_restore_channels_tile",
          onClick = onRestoreBrokenChannels,
        )
      }
    }

    // Catalog Statistics Card
    item(key = "catalog_stats", contentType = "stats_card") {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extColors.cardBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Live Catalog Summary",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            StatPill("Channels", channelCount.toString(), Modifier.weight(1f))
            StatPill("Countries", countryCount.toString(), Modifier.weight(1f))
            StatPill("Genres", categoryCount.toString(), Modifier.weight(1f))
          }
        }
      }
    }
  }
}

@Composable
private fun ProfileAvatarFallback() {
  Box(
    modifier = Modifier
      .size(48.dp)
      .clip(CircleShape)
      .background(TiweePurple.copy(alpha = 0.2f)),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = Icons.Rounded.Person,
      contentDescription = null,
      tint = TiweePurple,
      modifier = Modifier.size(26.dp),
    )
  }
}

@Composable
private fun SettingsActionTile(
  icon: ImageVector,
  accent: Color,
  title: String,
  subtitle: String,
  trailingText: String,
  testTag: String,
  onClick: () -> Unit,
) {
  val extColors = LocalTiweeColors.current
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, extColors.cardBorder),
    modifier = Modifier
      .fillMaxWidth()
      .minimumInteractiveComponentSize()
      .testTag(testTag),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.16f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accent,
            modifier = Modifier.size(22.dp),
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      Spacer(modifier = Modifier.width(10.dp))
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
      ) {
        Text(
          text = trailingText,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
      }
    }
  }
}

@Composable
private fun StatPill(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    modifier = modifier,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
    ) {
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.secondary,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun SleepTimerDialog(
  currentMinutes: Int?,
  onSelectMinutes: (Int?) -> Unit,
  onDismiss: () -> Unit,
) {
  val options = listOf<Int?>(null, 15, 30, 45, 60, 90, 120)
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Sleep Timer") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Automatically stop live playback after a set duration:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        options.forEach { mins ->
          val label = if (mins == null) "Off" else "$mins minutes"
          val isSelected = currentMinutes == mins
          Surface(
            onClick = { onSelectMinutes(mins) },
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) TiweePurple.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(
              1.dp,
              if (isSelected) TiweePurple else Color.Transparent,
            ),
            modifier = Modifier
              .fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("sleep_option_${mins ?: "off"}"),
          ) {
            Text(
              text = label,
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    },
  )
}

@Composable
private fun ParentalControlDialog(
  parentalSettings: ParentalSettings,
  onSetAllowAdult: (Boolean) -> Unit,
  onSetPin: (String?) -> Unit,
  onDismiss: () -> Unit,
) {
  var pinInput by remember { mutableStateOf("") }
  var verifyPinInput by remember { mutableStateOf("") }
  var pinError by remember { mutableStateOf<String?>(null) }
  var isUnlocked by remember(parentalSettings.pin) {
    mutableStateOf(parentalSettings.pin.isNullOrBlank())
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Lock, contentDescription = null, tint = TiweeAmber)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Parental Control")
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!isUnlocked && !parentalSettings.pin.isNullOrBlank()) {
          Text(
            text = "Enter your 4-digit PIN to unlock parental settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          OutlinedTextField(
            value = verifyPinInput,
            onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) verifyPinInput = it },
            label = { Text("Current 4-digit PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("parental_verify_pin_input"),
          )
          if (pinError != null) {
            Text(
              text = pinError!!,
              style = MaterialTheme.typography.bodySmall,
              color = TiweeLiveRed,
            )
          }
          Button(
            onClick = {
              if (verifyPinInput == parentalSettings.pin) {
                isUnlocked = true
                pinError = null
              } else {
                pinError = "Incorrect PIN. Please try again."
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("parental_unlock_button"),
          ) {
            Text("Unlock Settings")
          }
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Show 18+ channels",
                style = MaterialTheme.typography.titleSmall,
              )
              Text(
                text = "Include adult-tagged streams in catalog & search",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = parentalSettings.allowAdultChannels,
              onCheckedChange = onSetAllowAdult,
              modifier = Modifier.testTag("parental_adult_switch"),
            )
          }

          OutlinedTextField(
            value = pinInput,
            onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pinInput = it },
            label = { Text(if (parentalSettings.pin.isNullOrBlank()) "Set 4-digit PIN" else "Change 4-digit PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("parental_new_pin_input"),
          )

          if (pinError != null) {
            Text(
              text = pinError!!,
              style = MaterialTheme.typography.bodySmall,
              color = TiweeLiveRed,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = {
                if (pinInput.length == 4) {
                  onSetPin(pinInput)
                  pinInput = ""
                  pinError = null
                } else {
                  pinError = "PIN must be exactly 4 digits."
                }
              },
              modifier = Modifier
                .weight(1f)
                .testTag("parental_save_pin_button"),
            ) {
              Text("Save PIN")
            }

            if (!parentalSettings.pin.isNullOrBlank()) {
              OutlinedButton(
                onClick = {
                  onSetPin(null)
                  pinError = null
                },
                modifier = Modifier
                  .weight(1f)
                  .testTag("parental_remove_pin_button"),
              ) {
                Text("Remove PIN")
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Done")
      }
    },
  )
}
