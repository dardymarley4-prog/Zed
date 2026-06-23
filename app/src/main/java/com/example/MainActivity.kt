package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.net.Uri
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.Note
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemMainViewModel: MainViewModel = viewModel()
            MyApplicationTheme(darkTheme = systemMainViewModel.isDarkMode) {
                val context = LocalContext.current

                // Launcher for runtime media permissions
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { result ->
                    val isGranted = result.values.any { it }
                    if (isGranted) {
                        systemMainViewModel.loadMediaFromDevice(context)
                    }
                }

                // Request appropriate permissions based on OS Version on start
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_MEDIA_AUDIO,
                                Manifest.permission.READ_MEDIA_VIDEO,
                                Manifest.permission.READ_MEDIA_IMAGES
                            )
                        )
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                        )
                    }
                    // Attempt initial load anyways loadMediaFromDevice has try-catch
                    systemMainViewModel.loadMediaFromDevice(context)
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ZedAppContent(viewModel = systemMainViewModel)
                }
            }
        }
    }
}

@Composable
fun ZedAppContent(viewModel: MainViewModel) {
    val currentTab = viewModel.currentTab
    val isDarkMode = viewModel.isDarkMode

    val bgGradientColors = if (isDarkMode) {
        listOf(
            Color(0xFF0F1014),
            Color(0xFF13141C),
            Color(0xFF1E202B)
        )
    } else {
        listOf(
            Color(0xFFF1F5F9),
            Color(0xFFE2E8F0),
            Color(0xFFCBD5E1)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = bgGradientColors
                )
            )
    ) {
        // Safe drawing content container leaving bottom padding for floating bar
        Scaffold(
            bottomBar = { Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) },
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
                    .padding(bottom = 90.dp) // Leave exact space for floating bar
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "TabContent"
                ) { activeTab ->
                    when (activeTab) {
                        0 -> MusicTabScreen(viewModel = viewModel)
                        1 -> VideoTabScreen(viewModel = viewModel)
                        2 -> NotesTabScreen(viewModel = viewModel)
                        3 -> PhotoTabScreen(viewModel = viewModel)
                    }
                }
            }
        }

        // --- Translucent Glassmorphic Custom Bottom Navigation Bar ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 16.dp, start = 20.dp, end = 20.dp)
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.85f))
                .border(
                    width = 1.dp,
                    color = if (isDarkMode) Color.White.copy(alpha = 0.15f) else Color(0xFFCBD5E1),
                    shape = RoundedCornerShape(26.dp)
                )
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassTabButton(
                    icon = Icons.Filled.MusicNote,
                    label = "Musique",
                    isSelected = currentTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    modifier = Modifier.weight(1f).testTag("music_tab_button")
                )
                GlassTabButton(
                    icon = Icons.Filled.PlayArrow,
                    label = "Vidéo",
                    isSelected = currentTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    modifier = Modifier.weight(1f).testTag("video_tab_button")
                )
                GlassTabButton(
                    icon = Icons.Filled.Notes,
                    label = "Texte",
                    isSelected = currentTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    modifier = Modifier.weight(1f).testTag("notes_tab_button")
                )
                GlassTabButton(
                    icon = Icons.Filled.PhotoLibrary,
                    label = "Photo",
                    isSelected = currentTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    modifier = Modifier.weight(1f).testTag("photos_tab_button")
                )
            }
        }

        // Immersive video player dialog if active
        if (viewModel.isVideoPlayerActive && viewModel.activePlayingVideoUri != null) {
            VideoPlayerOverlay(
                videoUri = viewModel.activePlayingVideoUri!!,
                title = viewModel.videoList.find { it.uri == viewModel.activePlayingVideoUri }?.title ?: "Lecture Vidéo",
                onClose = { viewModel.closeVideoPlayer() }
            )
        }

        // Fullscreen photo viewer dialog if active
        if (viewModel.activePhotoItem != null) {
            FullscreenPhotoViewer(
                photo = viewModel.activePhotoItem!!,
                onClose = { viewModel.activePhotoItem = null }
            )
        }
    }
}

@Composable
fun GlassTabButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(if (isSelected) 1.15f else 1.0f, label = "iconScale")
    val alpha by animateFloatAsState(if (isSelected) 1f else 0.5f, label = "iconAlpha")
    val tintColor = if (isSelected) ZedAccentPurple else MaterialTheme.colorScheme.onBackground

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                onClick = onClick,
                indication = ripple(bounded = false, radius = 28.dp),
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tintColor.copy(alpha = alpha),
                modifier = Modifier
                    .size(26.dp)
                    .rotate(if (isSelected && icon == Icons.Filled.MusicNote) -12f else 0f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = tintColor.copy(alpha = alpha),
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

// ==========================================
// 1. MUSIC PLAYER TAB SCREEN
// ==========================================
@Composable
fun MusicTabScreen(viewModel: MainViewModel) {
    var activeUpperTab by remember { mutableStateOf(0) } // 0: Music, 1: Playlist, 2: Dossiers
    val songsList = viewModel.songsList
    val selectedIndex = viewModel.selectedMusicIndex
    val rotationState = rememberInfiniteTransition(label = "discRotate")
    val angle by rotationState.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "disc"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Upper Tabs Layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                listOf("Music", "Playlist", "Dossiers").forEachIndexed { index, title ->
                    val isActive = index == activeUpperTab
                    val textColorActive = if (viewModel.isDarkMode) Color.White else Color(0xFF0F172A)
                    val textColorInactive = if (viewModel.isDarkMode) Color.White.copy(alpha = 0.5f) else Color(0xFF0F172A).copy(alpha = 0.5f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { activeUpperTab = index }
                            .padding(bottom = 2.dp)
                    ) {
                        Text(
                            text = if (title == "Dossiers") "DOSSIERS" else title,
                            fontSize = if (isActive) 19.sp else 17.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isActive) textColorActive else textColorInactive,
                            letterSpacing = if (title == "Dossiers") 1.sp else 0.sp,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .height(2.dp)
                                .width(if (isActive) 22.dp else 0.dp)
                                .background(ZedAccentPurple)
                        )
                    }
                }
            }
            IconButton(
                onClick = { viewModel.toggleDarkMode() },
                modifier = Modifier.size(40.dp).testTag("theme_toggle_button")
            ) {
                Icon(
                    imageVector = if (viewModel.isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Toggle color theme",
                    tint = if (viewModel.isDarkMode) Color.White else Color(0xFF0F172A)
                )
            }
        }

        // Split: Main vinyl disc layout & Curved ListWheel simulation
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Disc vinyl placeholder representing active player
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .border(4.dp, ZedAccentPurple.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Load generated album art banner
                    Image(
                        painter = painterResource(id = R.drawable.img_media_banner_1782217867981),
                        contentDescription = "Album Vinyl Record Artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize(0.9f)
                            .clip(CircleShape)
                            .rotate(if (viewModel.isPlaying) angle else 0f)
                    )
                    // Disk center hole
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F0F26))
                            .border(2.dp, Color.Black, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Song playing label
                if (selectedIndex in songsList.indices) {
                    val currentTrack = songsList[selectedIndex]
                    Text(
                        text = currentTrack.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentTrack.artist,
                        color = ZedTextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Magical Arc Wheels Curved Song List in Compose
            Box(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                // Sophisticated Dark glowing circular node & vertical timeline line
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = 10.dp)
                        .fillMaxHeight(0.7f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .weight(1f)
                            .background(ZedAccentPurple.copy(alpha = 0.2f))
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(ZedAccentPurple.copy(alpha = 0.25f))
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(ZedAccentPurple)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .weight(1f)
                            .background(ZedAccentPurple.copy(alpha = 0.2f))
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    contentPadding = PaddingValues(vertical = 40.dp)
                ) {
                    items(songsList.size) { index ->
                        val isSelected = index == selectedIndex
                        // Beautiful dynamic offsets mimicking ListWheelScrollView
                        val animatePaddingLeft by animateDpAsState(
                            targetValue = if (isSelected) 36.dp else 16.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                            label = "pad"
                        )
                        val sizeMultiplier by animateFloatAsState(
                            targetValue = if (isSelected) 1.25f else 0.95f,
                            label = "sizeScale"
                        )
                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.45f),
                            label = "colorSel"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .offset(x = animatePaddingLeft)
                                .clickable(
                                    onClick = { viewModel.selectSong(index) },
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Playing index indicator",
                                    tint = ZedAccentPurple,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(end = 4.dp)
                                )
                            }
                            Text(
                                text = songsList[index].title,
                                fontSize = (15 * sizeMultiplier).sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Unified Audio Deck Controllers (At the bottom of Section 1)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(20.dp))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                .padding(14.dp)
        ) {
            // Slider tracker
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = viewModel.playbackTimeText,
                    fontSize = 11.sp,
                    color = ZedTextSecondary
                )
                Slider(
                    value = viewModel.playbackProgress,
                    onValueChange = { viewModel.seekTo(it) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = ZedAccentPurple,
                        activeTrackColor = ZedAccentPurple,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
                Text(
                    text = if (selectedIndex in songsList.indices) songsList[selectedIndex].durationText else "00:00",
                    fontSize = 11.sp,
                    color = ZedTextSecondary
                )
            }

            // Audio button controllers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.prevSong() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Musique précédente",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                FloatingActionButton(
                    onClick = { viewModel.togglePlay() },
                    shape = CircleShape,
                    containerColor = ZedAccentPurple,
                    contentColor = Color.Black,
                    modifier = Modifier.size(54.dp).testTag("play_pause_fab")
                ) {
                    Icon(
                        imageVector = if (viewModel.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Lancer ou mettre en pause",
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(
                    onClick = { viewModel.nextSong() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Musique suivante",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// 2. VIDEO DIRECTORY TAB SCREEN
// ==========================================
@Composable
fun VideoTabScreen(viewModel: MainViewModel) {
    val videos = viewModel.videoList

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Vos Dossiers Vidéos",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (viewModel.isDarkMode) Color.White else Color(0xFF0F172A)
                )
                Text(
                    text = "${videos.size} dossiers vidéos identifiés",
                    fontSize = 13.sp,
                    color = if (viewModel.isDarkMode) ZedTextSecondary else ZedLightTextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )
            }
            IconButton(
                onClick = { viewModel.toggleDarkMode() },
                modifier = Modifier.size(40.dp).testTag("theme_toggle_button")
            ) {
                Icon(
                    imageVector = if (viewModel.isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Toggle color theme",
                    tint = if (viewModel.isDarkMode) Color.White else Color(0xFF0F172A)
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(videos) { video ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .clickable { viewModel.playVideo(video) }
                ) {
                    // Visual background thumbnail
                    Image(
                        painter = painterResource(id = R.drawable.img_media_banner_1782217867981),
                        contentDescription = "Video preview thumbnail background",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.35f)
                    )

                    // Overlay content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.End)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = video.durationText,
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Launch video",
                                tint = Color.Black,
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(ZedAccentPurple, CircleShape)
                                    .padding(4.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = video.title,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (video.isLocal) "Média Local" else "Simulé",
                                    fontSize = 10.sp,
                                    color = ZedTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Inline native VideoPlayer overlay to play actual video files
@Composable
fun VideoPlayerOverlay(
    videoUri: Uri,
    title: String,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val context = LocalContext.current
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(videoUri)
                        val controller = MediaController(ctx)
                        controller.setAnchorView(this)
                        setMediaController(controller)
                        setOnPreparedListener {
                            start()
                        }
                    }
                },
                update = { view ->
                    view.setVideoURI(videoUri)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .align(Alignment.Center)
            )

            // Upper Title bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Fermer le lecteur vidéo",
                        tint = Color.White
                    )
                }
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

// ==========================================
// 3. TEXTS / LYRICS RECORDER TAB SCREEN
// ==========================================
@Composable
fun NotesTabScreen(viewModel: MainViewModel) {
    val notes by viewModel.allNotes.collectAsStateWithLifecycle()
    var titleInput by remember { mutableStateOf("") }
    var contentInput by remember { mutableStateOf("") }
    var searchInput by remember { mutableStateOf("") }
    var currentlyEditingNote by remember { mutableStateOf<Note?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Écrire un Texte / Paroles",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (viewModel.isDarkMode) Color.White else Color(0xFF0F172A)
                )
                Text(
                    text = "Sauvegardez vos pensées, poèmes et paroles de musique localement",
                    fontSize = 13.sp,
                    color = if (viewModel.isDarkMode) ZedTextSecondary else ZedLightTextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
            }
            IconButton(
                onClick = { viewModel.toggleDarkMode() },
                modifier = Modifier.size(40.dp).testTag("theme_toggle_button")
            ) {
                Icon(
                    imageVector = if (viewModel.isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Toggle color theme",
                    tint = if (viewModel.isDarkMode) Color.White else Color(0xFF0F172A)
                )
            }
        }

        // Write Form Block
        val isDark = viewModel.isDarkMode
        val formBg = if (isDark) Color.White.copy(alpha = 0.05f) else Color.White
        val formBorder = if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0)
        val textInputColor = if (isDark) Color.White else Color(0xFF0F172A)
        val textPlaceholderColor = if (isDark) ZedTextSecondary else ZedLightTextSecondary
        val textInputBorder = if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFCBD5E1)
        val textInputBgUnfocused = if (isDark) Color.White.copy(alpha = 0.03f) else Color(0xFFF8FAFC)
        val textInputBgFocused = if (isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFF1F5F9)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = formBg),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, formBorder)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (currentlyEditingNote == null) "Nouveau Document" else "Mode Édition",
                    color = ZedAccentPurple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                TextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    placeholder = { Text("Titre de la chanson / Note...", color = textPlaceholderColor) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textInputColor,
                        unfocusedTextColor = textInputColor,
                        focusedBorderColor = ZedAccentPurple,
                        unfocusedBorderColor = textInputBorder,
                        unfocusedContainerColor = textInputBgUnfocused,
                        focusedContainerColor = textInputBgFocused,
                        focusedPlaceholderColor = textPlaceholderColor,
                        unfocusedPlaceholderColor = textPlaceholderColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("note_title_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                TextField(
                    value = contentInput,
                    onValueChange = { contentInput = it },
                    placeholder = { Text("Tape ton texte ici...", color = textPlaceholderColor) },
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textInputColor,
                        unfocusedTextColor = textInputColor,
                        focusedBorderColor = ZedAccentPurple,
                        unfocusedBorderColor = textInputBorder,
                        unfocusedContainerColor = textInputBgUnfocused,
                        focusedContainerColor = textInputBgFocused,
                        focusedPlaceholderColor = textPlaceholderColor,
                        unfocusedPlaceholderColor = textPlaceholderColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("note_content_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ElevatedButton(
                        onClick = {
                            if (contentInput.isNotBlank()) {
                                val finalTitle = if (titleInput.isBlank()) "Sans Titre" else titleInput
                                val noteToEdit = currentlyEditingNote
                                if (noteToEdit == null) {
                                    viewModel.saveNote(finalTitle, contentInput)
                                } else {
                                    viewModel.updateNote(
                                        noteToEdit.copy(
                                            title = finalTitle,
                                            content = contentInput,
                                            timestamp = System.currentTimeMillis()
                                        )
                                    )
                                    currentlyEditingNote = null
                                }
                                titleInput = ""
                                contentInput = ""
                            }
                        },
                        colors = ButtonDefaults.elevatedButtonColors(containerColor = ZedAccentPurple),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_note_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentlyEditingNote == null) "Sauvegarder" else "Mettre à jour",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (currentlyEditingNote != null) {
                        OutlinedButton(
                            onClick = {
                                currentlyEditingNote = null
                                titleInput = ""
                                contentInput = ""
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Annuler", color = Color.White)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar & Filtered List of saved items
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.White)
                .then(
                    if (!isDark) Modifier.border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                    else Modifier
                )
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Recherche",
                tint = if (isDark) ZedTextSecondary else ZedLightTextSecondary
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextField(
                value = searchInput,
                onValueChange = { searchInput = it },
                placeholder = { Text("Rechercher dans les enregistrements...", color = if (isDark) ZedTextMuted else ZedLightTextMuted) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                    unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_notes_input")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Textes enregistrés (${notes.size}) :",
            color = if (isDark) Color.White else Color(0xFF0F172A),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val filteredNotes = notes.filter {
            it.title.contains(searchInput, ignoreCase = true) ||
                    it.content.contains(searchInput, ignoreCase = true)
        }

        if (filteredNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aucun texte ne correspond à votre recherche.",
                    color = ZedTextMuted,
                    fontSize = 13.sp
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredNotes.forEach { note ->
                    SavedNoteListItem(
                        note = note,
                        onEdit = {
                            currentlyEditingNote = note
                            titleInput = note.title
                            contentInput = note.content
                        },
                        onDelete = { viewModel.deleteNote(note) }
                    )
                }
            }
        }
    }
}

@Composable
fun SavedNoteListItem(
    note: Note,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateText = remember(note.timestamp) {
        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        sdf.format(Date(note.timestamp))
    }

    val isDark = MaterialTheme.colorScheme.background == ZedDarkBackGround
    val cardColor = if (isDark) Color.White.copy(alpha = 0.03f) else Color.White
    val borderColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFE2E8F0)
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val bodyColor = if (isDark) ZedTextSecondary else ZedLightTextSecondary

    Card(
        modifier = Modifier.fillMaxWidth().testTag("saved_note_card"),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = note.title,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = dateText,
                        fontSize = 10.sp,
                        color = ZedTextMuted
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.content,
                    fontSize = 12.sp,
                    color = bodyColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Éditer cette note",
                        tint = ZedAccentPurple,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Supprimer cette note",
                        tint = Color.Red.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// 4. PHOTO GALLERY TAB SCREEN
// ==========================================
@Composable
fun PhotoTabScreen(viewModel: MainViewModel) {
    val photos = viewModel.photosList

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Galerie Photos",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (viewModel.isDarkMode) Color.White else Color(0xFF0F172A)
                )
                Text(
                    text = "${photos.size} images détectées sur l'appareil",
                    fontSize = 13.sp,
                    color = if (viewModel.isDarkMode) ZedTextSecondary else ZedLightTextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )
            }
            IconButton(
                onClick = { viewModel.toggleDarkMode() },
                modifier = Modifier.size(40.dp).testTag("theme_toggle_button")
            ) {
                Icon(
                    imageVector = if (viewModel.isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Toggle color theme",
                    tint = if (viewModel.isDarkMode) Color.White else Color(0xFF0F172A)
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(photos) { photo ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .clickable { viewModel.activePhotoItem = photo }
                ) {
                    if (photo.isLocal && photo.uri != null) {
                        // Render devices actual gallery photo using Coil
                        AsyncImage(
                            model = photo.uri,
                            contentDescription = photo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (photo.url != null) {
                        // Fallback randomized seeds Picsum photocard loaded via Coil
                        AsyncImage(
                            model = photo.url,
                            contentDescription = photo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

// Immersive full-screen Photo viewer dialog
@Composable
fun FullscreenPhotoViewer(
    photo: PhotoItem,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (photo.isLocal && photo.uri != null) {
                AsyncImage(
                    model = photo.uri,
                    contentDescription = photo.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                )
            } else if (photo.url != null) {
                AsyncImage(
                    model = photo.url,
                    contentDescription = photo.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                )
            }

            // Close controls floating at top
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Fermer le visualiseur photo",
                        tint = Color.White
                    )
                }
                Text(
                    text = photo.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}
