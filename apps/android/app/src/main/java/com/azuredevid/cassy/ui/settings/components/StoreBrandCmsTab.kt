package com.azuredevid.cassy.ui.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.azuredevid.cassy.R
import com.azuredevid.cassy.ui.settings.StoreSettingsUiEvent
import com.azuredevid.cassy.ui.settings.StoreSettingsUiState
import com.azuredevid.cassy.ui.theme.CassyKunyit
import com.azuredevid.cassy.ui.theme.CassyPandan
import com.azuredevid.cassy.ui.theme.CassyTinta
import com.azuredevid.cassy.ui.theme.statusErrFg

private fun parseHexColor(hex: String, fallback: Color): Color {
    val clean = hex.trim().removePrefix("#")
    if (clean.length != 6 && clean.length != 8) return fallback
    return try {
        val colorInt = clean.toLong(16)
        if (clean.length == 6) Color(colorInt or 0xFF000000) else Color(colorInt)
    } catch (_: Exception) {
        fallback
    }
}

private data class BrandPreset(
    val labelRes: Int,
    val hex: String
)

private val COLOR_PRESETS = listOf(
    BrandPreset(R.string.cms_preset_neutral, ""),
    BrandPreset(R.string.cms_preset_pandan, "#1F5F52"),
    BrandPreset(R.string.cms_preset_kunyit, "#E9A23B"),
    BrandPreset(R.string.cms_preset_tinta, "#17211E"),
    BrandPreset(R.string.cms_preset_batu, "#3E4C47")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoreBrandCmsTab(
    state: StoreSettingsUiState,
    onEvent: (StoreSettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(0.6f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.cms_section_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                    )
                    Text(
                        text = stringResource(R.string.cms_section_desc),
                        style = MaterialTheme.typography.bodySmall.copy(color = colors.onSurfaceVariant)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.cms_header_card_title),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                    )

                    OutlinedTextField(
                        value = state.heroTitleId,
                        onValueChange = { onEvent(StoreSettingsUiEvent.UpdateHeroTitleId(it)) },
                        label = { Text(stringResource(R.string.cms_hero_title_id)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.outline
                        )
                    )

                    OutlinedTextField(
                        value = state.heroTitleEn,
                        onValueChange = { onEvent(StoreSettingsUiEvent.UpdateHeroTitleEn(it)) },
                        label = { Text(stringResource(R.string.cms_hero_title_en)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.outline
                        )
                    )

                    OutlinedTextField(
                        value = state.heroSubtitleId,
                        onValueChange = { onEvent(StoreSettingsUiEvent.UpdateHeroSubtitleId(it)) },
                        label = { Text(stringResource(R.string.cms_hero_subtitle_id)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.outline
                        )
                    )

                    OutlinedTextField(
                        value = state.heroSubtitleEn,
                        onValueChange = { onEvent(StoreSettingsUiEvent.UpdateHeroSubtitleEn(it)) },
                        label = { Text(stringResource(R.string.cms_hero_subtitle_en)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.outline
                        )
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.cms_colors_title),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                    )

                    Text(
                        text = stringResource(R.string.cms_presets_label),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = colors.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        COLOR_PRESETS.forEach { preset ->
                            val presetColor = parseHexColor(preset.hex, CassyTinta)
                            val isSelected = state.primaryHex.equals(preset.hex, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) colors.surfaceVariant else Color.Transparent,
                                border = BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) colors.primary else colors.outline
                                ),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(role = Role.RadioButton) {
                                        onEvent(StoreSettingsUiEvent.UpdatePrimaryHex(preset.hex))
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(presetColor)
                                    )
                                    Text(
                                        text = stringResource(preset.labelRes),
                                        style = MaterialTheme.typography.labelMedium.copy(color = colors.onSurface)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = state.primaryHex,
                            onValueChange = { onEvent(StoreSettingsUiEvent.UpdatePrimaryHex(it)) },
                            label = { Text(stringResource(R.string.cms_primary_hex_label)) },
                            isError = !state.isPrimaryHexValid,
                            supportingText = if (!state.isPrimaryHexValid) {
                                { Text(stringResource(R.string.cms_hex_error), color = statusErrFg()) }
                            } else {
                                null
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(parseHexColor(state.primaryHex, CassyTinta))
                                        .border(1.dp, colors.outline, CircleShape)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.outline
                            )
                        )

                        OutlinedTextField(
                            value = state.accentHex,
                            onValueChange = { onEvent(StoreSettingsUiEvent.UpdateAccentHex(it)) },
                            label = { Text(stringResource(R.string.cms_accent_hex_label)) },
                            isError = !state.isAccentHexValid,
                            supportingText = if (!state.isAccentHexValid) {
                                { Text(stringResource(R.string.cms_hex_error), color = statusErrFg()) }
                            } else {
                                null
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(parseHexColor(state.accentHex, CassyKunyit))
                                        .border(1.dp, colors.outline, CircleShape)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.outline
                            )
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.cms_banner_card_title),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.heroBannerUrl.isNotBlank() && state.isHeroBannerUrlValid) {
                            AsyncImage(
                                model = state.heroBannerUrl,
                                contentDescription = stringResource(R.string.cms_banner_preview_cd),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 120.dp, height = 64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, colors.outline, RoundedCornerShape(8.dp))
                            )
                        }

                        OutlinedTextField(
                            value = state.heroBannerUrl,
                            onValueChange = { onEvent(StoreSettingsUiEvent.UpdateHeroBannerUrl(it)) },
                            label = { Text(stringResource(R.string.cms_banner_url_label)) },
                            placeholder = { Text(stringResource(R.string.cms_banner_url_placeholder)) },
                            singleLine = true,
                            isError = !state.isHeroBannerUrlValid,
                            supportingText = if (!state.isHeroBannerUrlValid) {
                                {
                                    Text(
                                        text = stringResource(R.string.cms_banner_url_error),
                                        color = statusErrFg()
                                    )
                                }
                            } else {
                                null
                            },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.outline
                            )
                        )
                    }

                    HorizontalDivider(color = colors.outline)

                    val whiteLabelTitle = stringResource(R.string.cms_white_label_title)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = whiteLabelTitle,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurface
                                )
                            )
                            Text(
                                text = stringResource(R.string.cms_white_label_desc),
                                style = MaterialTheme.typography.bodySmall.copy(color = colors.onSurfaceVariant)
                            )
                        }
                        Switch(
                            checked = state.hidePoweredByBadge,
                            onCheckedChange = { onEvent(StoreSettingsUiEvent.ToggleWhiteLabel(it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.onPrimary,
                                checkedTrackColor = colors.primary
                            ),
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .semantics { contentDescription = whiteLabelTitle }
                        )
                    }
                }
            }

            Button(
                onClick = { onEvent(StoreSettingsUiEvent.SaveStoreCms) },
                enabled = !state.isSavingCms &&
                    state.isHeroBannerUrlValid &&
                    state.isPrimaryHexValid &&
                    state.isAccentHexValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                )
            ) {
                if (state.isSavingCms) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = colors.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.cms_btn_saving))
                } else {
                    Text(stringResource(R.string.cms_btn_save))
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.cms_preview_heading),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
            )

            val primaryColor = remember(state.primaryHex) {
                parseHexColor(state.primaryHex, CassyPandan)
            }
            val accentColor = remember(state.accentHex) {
                parseHexColor(state.accentHex, CassyKunyit)
            }

            Surface(
                modifier = Modifier
                    .width(310.dp)
                    .height(540.dp),
                shape = RoundedCornerShape(28.dp),
                color = colors.surface,
                border = BorderStroke(4.dp, colors.onSurface),
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(132.dp)
                            .background(primaryColor)
                    ) {
                        if (state.heroBannerUrl.isNotBlank() && state.isHeroBannerUrlValid) {
                            AsyncImage(
                                model = state.heroBannerUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(Color.Black.copy(alpha = 0.4f))
                            )
                        }

                        Column(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text = state.heroTitleId.ifBlank { stringResource(R.string.cms_hero_title_id) },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = state.heroSubtitleId.ifBlank { stringResource(R.string.cms_hero_subtitle_id) },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = accentColor,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = primaryColor
                        ) {
                            Text(
                                text = stringResource(R.string.cms_preview_cat_1),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.outline)
                        ) {
                            Text(
                                text = stringResource(R.string.cms_preview_cat_2),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = colors.onSurfaceVariant,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.cms_preview_item_name),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = colors.onSurface
                                        )
                                    )
                                    Text(
                                        text = stringResource(R.string.cms_preview_item_price),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = accentColor,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = primaryColor,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "+",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = primaryColor
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.cms_preview_cart_count),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                            Text(
                                text = stringResource(R.string.cms_preview_cart_cta),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(
                                if (!state.hidePoweredByBadge) {
                                    R.string.cms_preview_powered_by
                                } else {
                                    R.string.cms_preview_white_label
                                }
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.onSurfaceVariant,
                                fontSize = 9.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
