package com.ltebandslock.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ltebandslock.data.model.AppLanguage
import com.ltebandslock.data.model.AppSettings
import com.ltebandslock.data.model.AppThemeMode
import com.ltebandslock.data.model.SpeedUnit
import com.ltebandslock.ui.theme.JoyConBlue
import com.ltebandslock.ui.theme.LocalAppStrings
import com.ltebandslock.ui.theme.LocalCustomColors
import com.ltebandslock.ui.theme.NintendoRed

@Composable
fun SettingsDialog(
    settings: AppSettings,
    onUpdateTheme: (AppThemeMode) -> Unit,
    onUpdateLanguage: (AppLanguage) -> Unit,
    onUpdateSpeedUnit: (SpeedUnit) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalCustomColors.current
    val strings = LocalAppStrings.current

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.cardBg)
                .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NintendoRed)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "CONFIG",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.settingsTitle,
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.close,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: Appearance Theme
                Text(
                    text = strings.themeSection,
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        val isSelected = settings.themeMode == mode
                        val label = when (mode) {
                            AppThemeMode.DARK -> strings.themeDark
                            AppThemeMode.LIGHT -> strings.themeLight
                            AppThemeMode.SYSTEM -> strings.themeSystem
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) NintendoRed else colors.cardBgSubtle
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) NintendoRed else colors.cardBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onUpdateTheme(mode) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else colors.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Language
                Text(
                    text = strings.languageSection,
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppLanguage.entries.forEach { lang ->
                        val isSelected = settings.language == lang
                        val label = when (lang) {
                            AppLanguage.TRADITIONAL_CHINESE -> strings.langZh
                            AppLanguage.ENGLISH -> strings.langEn
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) JoyConBlue else colors.cardBgSubtle
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) JoyConBlue else colors.cardBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onUpdateLanguage(lang) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else colors.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Text(
                    text = if (settings.language == AppLanguage.TRADITIONAL_CHINESE) {
                        "註：RSRP、SINR、RSRQ、CA、Band 等無線射頻專有名詞一律維持標準英文。"
                    } else {
                        "Note: RF technical terms (RSRP, SINR, RSRQ, CA, Band) remain in English."
                    },
                    color = colors.textSecondary,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: Speed Unit
                Text(
                    text = strings.speedUnitSection,
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SpeedUnit.entries.forEach { unit ->
                        val isSelected = settings.speedUnit == unit
                        val label = when (unit) {
                            SpeedUnit.MBPS -> strings.unitMbpsDesc
                            SpeedUnit.MB_S -> strings.unitMbsDesc
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) NintendoRed else colors.cardBgSubtle
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) NintendoRed else colors.cardBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onUpdateSpeedUnit(unit) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else colors.textPrimary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.cardBorder,
                        contentColor = colors.textPrimary
                    )
                ) {
                    Text(
                        text = strings.close,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
