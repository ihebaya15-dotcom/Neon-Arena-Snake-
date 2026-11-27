package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.model.EyeStyle
import com.example.game.model.HubSection
import com.example.game.model.PlayerProgress
import com.example.game.model.SnakeSkin
import com.example.game.model.SnakeSkins
import com.example.game.model.StoreCategory
import com.example.game.model.TrailStyle
import com.example.game.model.UiAccentPreset
import com.example.ui.components.CoordinatedSnakePreviewCanvas
import com.example.ui.components.CustomizeSection
import com.example.ui.components.RecordSection
import com.example.ui.components.StoreSection
import com.example.ui.theme.ArenaDeepNavy
import com.example.ui.theme.GridLineBlue
import com.example.ui.theme.HudBorderStroke
import com.example.ui.theme.HudGlassSurface
import com.example.ui.theme.NeonSnakeTheme
import com.example.ui.theme.SolarGold
import com.example.ui.theme.SpaceObsidian
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.StoreValidationFeedback
import kotlin.math.PI

@Composable
fun NeonHubScreen(
    activeSection: HubSection,
    activeStoreCategory: StoreCategory,
    progress: PlayerProgress,
    storeValidationFeedback: StoreValidationFeedback?,
    onSelectSection: (HubSection) -> Unit,
    onSelectStoreCategory: (StoreCategory) -> Unit,
    onSelectSkin: (SnakeSkin) -> Unit,
    onSelectTrail: (TrailStyle) -> Unit,
    onSelectEye: (EyeStyle) -> Unit,
    onSelectAccentPreset: (UiAccentPreset) -> Unit,
    onPurchaseOrEquipSkin: (SnakeSkin) -> Unit,
    onPurchaseOrEquipTrail: (TrailStyle) -> Unit,
    onPurchaseOrEquipEye: (EyeStyle) -> Unit,
    onDismissStoreValidationFeedback: () -> Unit,
    onStartGame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hub_anim")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "hub_wave_phase",
    )

    val coordinatedAccent by animateColorAsState(
        targetValue = progress.coordinatedAccentColor,
        label = "hub_coordinated_accent",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceObsidian),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(ArenaDeepNavy, SpaceObsidian),
                    center = Offset(size.width * 0.5f, size.height * 0.30f),
                    radius = size.maxDimension * 0.85f,
                ),
            )
            val step = 42.dp.toPx()
            val scrollOffset = (wavePhase / (2f * PI.toFloat())) * step
            var x = 0f
            while (x <= size.width) {
                drawLine(
                    color = GridLineBlue.copy(alpha = 0.45f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f,
                )
                x += step
            }
            var y = -step + scrollOffset
            while (y <= size.height) {
                drawLine(
                    color = GridLineBlue.copy(alpha = 0.45f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
                y += step
            }
            drawCircle(
                color = coordinatedAccent.copy(alpha = 0.12f),
                radius = size.width * 0.55f,
                center = Offset(size.width * 0.5f, size.height * 0.32f),
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                HubTopStatusBar(
                    sparksBalance = progress.sparksBalance,
                    bestScore = progress.bestScore,
                    accentColor = coordinatedAccent,
                    onWalletClick = { onSelectSection(HubSection.STORE) },
                )
            },
            bottomBar = {
                HubBottomNavigationBar(
                    activeSection = activeSection,
                    accentColor = coordinatedAccent,
                    onSelectSection = onSelectSection,
                )
            },
        ) { innerPadding ->
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxSize(),
                ) {
                    Crossfade(
                        targetState = activeSection,
                        label = "hub_section_transition",
                    ) { section ->
                        when (section) {
                            HubSection.PLAY -> PlayHubSection(
                                progress = progress,
                                accentColor = coordinatedAccent,
                                wavePhase = wavePhase,
                                onSelectSkin = onSelectSkin,
                                onOpenCustomize = { onSelectSection(HubSection.CUSTOMIZE) },
                                onStartGame = onStartGame,
                            )

                            HubSection.STORE -> StoreSection(
                                progress = progress,
                                selectedCategory = activeStoreCategory,
                                accentColor = coordinatedAccent,
                                wavePhase = wavePhase,
                                validationFeedback = storeValidationFeedback,
                                onSelectCategory = onSelectStoreCategory,
                                onPurchaseOrEquipSkin = onPurchaseOrEquipSkin,
                                onPurchaseOrEquipTrail = onPurchaseOrEquipTrail,
                                onPurchaseOrEquipEye = onPurchaseOrEquipEye,
                                onDismissValidationFeedback = onDismissStoreValidationFeedback,
                            )

                            HubSection.CUSTOMIZE -> CustomizeSection(
                                progress = progress,
                                accentColor = coordinatedAccent,
                                wavePhase = wavePhase,
                                onSelectSkin = onSelectSkin,
                                onSelectTrail = onSelectTrail,
                                onSelectEye = onSelectEye,
                                onSelectAccentPreset = onSelectAccentPreset,
                            )

                            HubSection.RECORD -> RecordSection(
                                progress = progress,
                                accentColor = coordinatedAccent,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HubTopStatusBar(
    sparksBalance: Int,
    bestScore: Int,
    accentColor: Color,
    onWalletClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            text = "NEON SNAKE",
            style = MaterialTheme.typography.titleLarge,
            color = accentColor,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (bestScore > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(HudGlassSurface)
                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = bestScore.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(HudGlassSurface)
                    .border(1.dp, SolarGold.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .clickable(onClick = onWalletClick)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("hub_sparks_wallet"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = stringResource(R.string.sparks_currency_name),
                    tint = SolarGold,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    text = stringResource(R.string.sparks_balance_short, sparksBalance),
                    style = MaterialTheme.typography.labelLarge,
                    color = SolarGold,
                )
            }
        }
    }
}

@Composable
private fun PlayHubSection(
    progress: PlayerProgress,
    accentColor: Color,
    wavePhase: Float,
    onSelectSkin: (SnakeSkin) -> Unit,
    onOpenCustomize: () -> Unit,
    onStartGame: () -> Unit,
) {
    val equippedSkin = progress.equippedSkin
    val ownedSkins = SnakeSkins.all.filter { it.id in progress.unlockedSkinIds }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val minHeight = maxHeight
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = (minHeight - 16.dp).coerceAtLeast(0.dp)),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.lobby_subtitle),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Preview Card showing live Skin + Trail + Eyes
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(188.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(HudGlassSurface)
                        .border(1.5.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(28.dp))
                        .clickable(onClick = onOpenCustomize)
                        .testTag("play_snake_preview_card"),
                ) {
                    CoordinatedSnakePreviewCanvas(
                        skin = equippedSkin,
                        trailStyle = progress.equippedTrail,
                        eyeStyle = progress.equippedEye,
                        wavePhase = wavePhase,
                        isBoostPreview = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SpaceObsidian.copy(alpha = 0.72f))
                            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = equippedSkin.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Owned Skin Selector + Play Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.lobby_choose_skin),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        ownedSkins.forEach { skin ->
                            val isSelected = skin.id == equippedSkin.id
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(skin.primaryColor, skin.secondaryColor)),
                                    )
                                    .border(
                                        width = if (isSelected) 3.dp else 1.5.dp,
                                        color = if (isSelected) Color.White else HudBorderStroke,
                                        shape = CircleShape,
                                    )
                                    .clickable { onSelectSkin(skin) }
                                    .testTag("skin_option_${skin.id}"),
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = skin.displayName,
                                        tint = SpaceObsidian,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Button(
                        onClick = onStartGame,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = SpaceObsidian,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("start_game_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.lobby_start_play),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stringResource(R.string.lobby_controls_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HubBottomNavigationBar(
    activeSection: HubSection,
    accentColor: Color,
    onSelectSection: (HubSection) -> Unit,
) {
    NavigationBar(
        containerColor = HudGlassSurface,
        contentColor = TextPrimary,
        windowInsets = WindowInsets.navigationBars,
        modifier = Modifier.border(width = 1.dp, color = HudBorderStroke),
    ) {
        val itemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = accentColor,
            selectedTextColor = accentColor,
            indicatorColor = accentColor.copy(alpha = 0.18f),
            unselectedIconColor = TextSecondary,
            unselectedTextColor = TextSecondary,
        )

        NavigationBarItem(
            selected = activeSection == HubSection.PLAY,
            onClick = { onSelectSection(HubSection.PLAY) },
            icon = { Icon(Icons.Rounded.SportsEsports, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_play)) },
            colors = itemColors,
            modifier = Modifier.testTag("hub_tab_play"),
        )
        NavigationBarItem(
            selected = activeSection == HubSection.STORE,
            onClick = { onSelectSection(HubSection.STORE) },
            icon = { Icon(Icons.Rounded.Storefront, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_store)) },
            colors = itemColors,
            modifier = Modifier.testTag("hub_tab_store"),
        )
        NavigationBarItem(
            selected = activeSection == HubSection.CUSTOMIZE,
            onClick = { onSelectSection(HubSection.CUSTOMIZE) },
            icon = { Icon(Icons.Rounded.Tune, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_customize)) },
            colors = itemColors,
            modifier = Modifier.testTag("hub_tab_customize"),
        )
        NavigationBarItem(
            selected = activeSection == HubSection.RECORD,
            onClick = { onSelectSection(HubSection.RECORD) },
            icon = { Icon(Icons.Rounded.EmojiEvents, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_record)) },
            colors = itemColors,
            modifier = Modifier.testTag("hub_tab_record"),
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NeonHubScreenPreview() {
    NeonSnakeTheme {
        NeonHubScreen(
            activeSection = HubSection.PLAY,
            activeStoreCategory = StoreCategory.SKINS,
            progress = PlayerProgress(bestScore = 1480, sparksBalance = 310),
            storeValidationFeedback = null,
            onSelectSection = {},
            onSelectStoreCategory = {},
            onSelectSkin = {},
            onSelectTrail = {},
            onSelectEye = {},
            onSelectAccentPreset = {},
            onPurchaseOrEquipSkin = {},
            onPurchaseOrEquipTrail = {},
            onPurchaseOrEquipEye = {},
            onDismissStoreValidationFeedback = {},
            onStartGame = {},
        )
    }
}
