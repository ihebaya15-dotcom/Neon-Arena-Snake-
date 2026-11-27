package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.model.EyeStyle
import com.example.game.model.PlayerProgress
import com.example.game.model.SnakeSkin
import com.example.game.model.SnakeSkins
import com.example.game.model.StoreCategory
import com.example.game.model.TrailStyle
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.HudBorderStroke
import com.example.ui.theme.HudGlassSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonSnakeTheme
import com.example.ui.theme.SolarGold
import com.example.ui.theme.SpaceObsidian
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.StoreValidationFeedback

@Composable
fun StoreSection(
    progress: PlayerProgress,
    selectedCategory: StoreCategory,
    accentColor: Color,
    wavePhase: Float,
    validationFeedback: StoreValidationFeedback?,
    onSelectCategory: (StoreCategory) -> Unit,
    onPurchaseOrEquipSkin: (SnakeSkin) -> Unit,
    onPurchaseOrEquipTrail: (TrailStyle) -> Unit,
    onPurchaseOrEquipEye: (EyeStyle) -> Unit,
    onDismissValidationFeedback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = stringResource(R.string.store_title),
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.store_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            StoreCategoryChip(
                label = stringResource(R.string.store_cat_skins),
                icon = Icons.Rounded.Palette,
                selected = selectedCategory == StoreCategory.SKINS,
                accentColor = accentColor,
                onClick = { onSelectCategory(StoreCategory.SKINS) },
                tag = "store_cat_skins",
                modifier = Modifier.weight(1f),
            )
            StoreCategoryChip(
                label = stringResource(R.string.store_cat_trails),
                icon = Icons.Rounded.AutoAwesome,
                selected = selectedCategory == StoreCategory.TRAILS,
                accentColor = accentColor,
                onClick = { onSelectCategory(StoreCategory.TRAILS) },
                tag = "store_cat_trails",
                modifier = Modifier.weight(1f),
            )
            StoreCategoryChip(
                label = stringResource(R.string.store_cat_eyes),
                icon = Icons.Rounded.Visibility,
                selected = selectedCategory == StoreCategory.EYES,
                accentColor = accentColor,
                onClick = { onSelectCategory(StoreCategory.EYES) },
                tag = "store_cat_eyes",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        AnimatedVisibility(
            visible = validationFeedback != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            if (validationFeedback != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DangerCrimson.copy(alpha = 0.16f))
                        .border(1.dp, DangerCrimson.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("store_validation_banner"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = DangerCrimson,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(
                            R.string.store_insufficient_sparks,
                            validationFeedback.missingSparks,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = onDismissValidationFeedback,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cd_close_overlay),
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 152.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            when (selectedCategory) {
                StoreCategory.SKINS -> {
                    items(SnakeSkins.all, key = { it.id }) { skin ->
                        val isUnlocked = skin.id in progress.unlockedSkinIds
                        val isEquipped = skin.id == progress.equippedSkinId
                        StoreItemCard(
                            title = skin.displayName,
                            priceSparks = skin.priceSparks,
                            sparksBalance = progress.sparksBalance,
                            isUnlocked = isUnlocked,
                            isEquipped = isEquipped,
                            itemAccent = skin.primaryColor,
                            onAction = { onPurchaseOrEquipSkin(skin) },
                            tag = "store_item_${skin.id}",
                            preview = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(SpaceObsidian.copy(alpha = 0.7f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        repeat(4) { idx ->
                                            val c = if (idx % 2 == 0) skin.primaryColor else skin.secondaryColor
                                            Box(
                                                modifier = Modifier
                                                    .size(if (idx == 3) 26.dp else 20.dp)
                                                    .clip(CircleShape)
                                                    .background(c)
                                                    .border(1.5.dp, skin.coreColor.copy(alpha = 0.6f), CircleShape),
                                            )
                                        }
                                    }
                                }
                            },
                        )
                    }
                }

                StoreCategory.TRAILS -> {
                    items(TrailStyle.entries, key = { it.id }) { trail ->
                        val isUnlocked = trail.id in progress.unlockedTrailIds
                        val isEquipped = trail.id == progress.equippedTrailId
                        StoreItemCard(
                            title = stringResource(trail.nameResId),
                            priceSparks = trail.priceSparks,
                            sparksBalance = progress.sparksBalance,
                            isUnlocked = isUnlocked,
                            isEquipped = isEquipped,
                            itemAccent = accentColor,
                            onAction = { onPurchaseOrEquipTrail(trail) },
                            tag = "store_item_${trail.id}",
                            preview = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(SpaceObsidian.copy(alpha = 0.7f)),
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val r = 10.dp.toPx()
                                        for (idx in 0..3) {
                                            val cx = size.width * (0.22f + idx * 0.18f)
                                            val cy = size.height * 0.5f
                                            drawSnakeTrailSegment(
                                                center = Offset(cx, cy),
                                                radius = r,
                                                segmentIndex = idx,
                                                totalSegments = 4,
                                                skin = progress.equippedSkin,
                                                trailStyle = trail,
                                                isBoosting = true,
                                                worldTime = wavePhase,
                                                shieldAlpha = 1f,
                                            )
                                            drawCircle(
                                                color = progress.equippedSkin.primaryColor,
                                                radius = r * 0.72f,
                                                center = Offset(cx, cy),
                                            )
                                        }
                                    }
                                }
                            },
                        )
                    }
                }

                StoreCategory.EYES -> {
                    items(EyeStyle.entries, key = { it.id }) { eye ->
                        val isUnlocked = eye.id in progress.unlockedEyeIds
                        val isEquipped = eye.id == progress.equippedEyeId
                        StoreItemCard(
                            title = stringResource(eye.nameResId),
                            priceSparks = eye.priceSparks,
                            sparksBalance = progress.sparksBalance,
                            isUnlocked = isUnlocked,
                            isEquipped = isEquipped,
                            itemAccent = accentColor,
                            onAction = { onPurchaseOrEquipEye(eye) },
                            tag = "store_item_${eye.id}",
                            preview = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(SpaceObsidian.copy(alpha = 0.7f)),
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val headR = 20.dp.toPx()
                                        val center = Offset(size.width * 0.5f, size.height * 0.5f)
                                        drawCircle(
                                            color = progress.equippedSkin.glowColor.copy(alpha = 0.25f),
                                            radius = headR * 1.45f,
                                            center = center,
                                        )
                                        drawCircle(
                                            color = progress.equippedSkin.primaryColor,
                                            radius = headR,
                                            center = center,
                                        )
                                        drawSnakeEyes(
                                            headCenter = center,
                                            headRadius = headR,
                                            angleRadians = 0f,
                                            eyeStyle = eye,
                                            skin = progress.equippedSkin,
                                        )
                                    }
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
private fun StoreCategoryChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accentColor.copy(alpha = 0.20f) else HudGlassSurface)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accentColor else HudBorderStroke,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .testTag(tag),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) accentColor else TextSecondary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
            color = if (selected) accentColor else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StoreItemCard(
    title: String,
    priceSparks: Int,
    sparksBalance: Int,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    itemAccent: Color,
    onAction: () -> Unit,
    tag: String,
    preview: @Composable () -> Unit,
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isEquipped -> itemAccent
            isUnlocked -> itemAccent.copy(alpha = 0.4f)
            else -> HudBorderStroke
        },
        label = "store_card_border",
    )
    val canAfford = sparksBalance >= priceSparks

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(HudGlassSurface, SpaceObsidian.copy(alpha = 0.85f)),
                ),
            )
            .border(if (isEquipped) 2.dp else 1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(12.dp)
            .testTag(tag),
    ) {
        preview()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (isEquipped) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = stringResource(R.string.store_status_equipped),
                    tint = itemAccent,
                    modifier = Modifier.size(18.dp),
                )
            } else if (!isUnlocked && !canAfford) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        if (isEquipped) {
            OutlinedButton(
                onClick = {},
                enabled = false,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            ) {
                Text(
                    text = stringResource(R.string.store_status_equipped),
                    style = MaterialTheme.typography.labelLarge,
                    color = itemAccent,
                )
            }
        } else if (isUnlocked) {
            OutlinedButton(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("${tag}_equip"),
            ) {
                Text(
                    text = stringResource(R.string.store_action_equip),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                )
            }
        } else {
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canAfford) SolarGold else HudBorderStroke,
                    contentColor = if (canAfford) SpaceObsidian else TextSecondary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("${tag}_buy"),
            ) {
                Icon(
                    imageVector = if (canAfford) Icons.Rounded.Bolt else Icons.Rounded.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.store_action_buy, priceSparks),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 700, backgroundColor = 0xFF060812)
@Composable
private fun StoreSectionPreview() {
    NeonSnakeTheme {
        Box(modifier = Modifier.background(SpaceObsidian)) {
            StoreSection(
                progress = PlayerProgress(sparksBalance = 240),
                selectedCategory = StoreCategory.SKINS,
                accentColor = NeonCyan,
                wavePhase = 1.2f,
                validationFeedback = null,
                onSelectCategory = {},
                onPurchaseOrEquipSkin = {},
                onPurchaseOrEquipTrail = {},
                onPurchaseOrEquipEye = {},
                onDismissValidationFeedback = {},
            )
        }
    }
}
