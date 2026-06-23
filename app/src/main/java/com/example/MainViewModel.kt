package com.example

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Note
import com.example.data.NoteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Simple data class representing music items
data class SongItem(
    val title: String,
    val artist: String,
    val durationText: String = "03:45",
    val uri: Uri? = null,
    val isLocal: Boolean = false
)

// Simple data class representing video items
data class VideoItem(
    val title: String,
    val durationText: String = "02:15",
    val uri: Uri? = null,
    val isLocal: Boolean = false
)

// Simple data class representing photo items
data class PhotoItem(
    val title: String,
    val uri: Uri? = null,
    // For online fallback images
    val url: String? = null,
    val isLocal: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val noteRepository: NoteRepository

    init {
        val database = AppDatabase.getDatabase(application)
        noteRepository = NoteRepository(database.noteDao())
    }

    // --- Tab Navigation ---
    var currentTab by mutableStateOf(0) // 0: Musique, 1: Vidéo, 2: Texte, 3: Photo
        private set

    fun selectTab(tab: Int) {
        currentTab = tab
    }

    // --- Dark Mode / Light Mode ---
    var isDarkMode by mutableStateOf(true)
        private set

    fun toggleDarkMode() {
        isDarkMode = !isDarkMode
    }

    // --- Notes Integration (Room Database) ---
    val allNotes: StateFlow<List<Note>> = noteRepository.allNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveNote(title: String, content: String) {
        viewModelScope.launch {
            noteRepository.insert(Note(title = title, content = content))
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            noteRepository.update(note)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteRepository.delete(note)
        }
    }

    // --- Musique state & playback simulation ---
    var songsList by mutableStateOf<List<SongItem>>(emptyList())
        private set

    var selectedMusicIndex by mutableStateOf(2)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var playbackProgress by mutableStateOf(0.35f) // progress slider (0.0 to 1.0)
        private set

    var playbackTimeText by mutableStateOf("01:21")
        private set

    private var playbackJob: Job? = null

    init {
        loadFallbackMedia()
    }

    private fun loadFallbackMedia() {
        songsList = listOf(
            SongItem("Paulz bertey", "Dardy Special"),
            SongItem("Ghetta girl", "Marley Beats"),
            SongItem("Paul-Walker", "Fast & Ambient"),
            SongItem("Annicha-Bombo", "Classic Afro"),
            SongItem("caliente - so", "Hot Rhythm"),
            SongItem("Kalu", "Sunset Melody"),
            SongItem("Rookie I", "Dardy & Co"),
            SongItem("Circus d'amour", "The Love Parade")
        )
        selectedMusicIndex = 2
    }

    fun selectSong(index: Int) {
        if (index in songsList.indices) {
            selectedMusicIndex = index
            playbackProgress = 0f
            playbackTimeText = "00:00"
            // Start playing when selected
            if (!isPlaying) {
                togglePlay()
            } else {
                restartPlaybackSimulation()
            }
        }
    }

    fun togglePlay() {
        isPlaying = !isPlaying
        if (isPlaying) {
            startPlaybackSimulation()
        } else {
            playbackJob?.cancel()
        }
    }

    private fun startPlaybackSimulation() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (isPlaying) {
                delay(1000)
                playbackProgress += 0.005f
                if (playbackProgress >= 1f) {
                    playbackProgress = 0f
                    // Auto advance
                    val nextIdx = (selectedMusicIndex + 1) % songsList.size
                    selectSong(nextIdx)
                    break
                }
                // Update time display text
                val totalSeconds = (225 * playbackProgress).toInt() // 3m 45s = 225s
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                playbackTimeText = String.format("%02d:%02d", minutes, seconds)
            }
        }
    }

    private fun restartPlaybackSimulation() {
        startPlaybackSimulation()
    }

    fun prevSong() {
        val prevIdx = if (selectedMusicIndex - 1 < 0) songsList.size - 1 else selectedMusicIndex - 1
        selectSong(prevIdx)
    }

    fun nextSong() {
        val nextIdx = (selectedMusicIndex + 1) % songsList.size
        selectSong(nextIdx)
    }

    fun seekTo(progress: Float) {
        playbackProgress = progress.coerceIn(0f, 1f)
        val totalSeconds = (225 * playbackProgress).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        playbackTimeText = String.format("%02d:%02d", minutes, seconds)
    }

    // --- Video State ---
    var videoList by mutableStateOf<List<VideoItem>>(emptyList())
        private set

    var activePlayingVideoUri by mutableStateOf<Uri?>(null)
        private set

    var isVideoPlayerActive by mutableStateOf(false)
        private set

    fun playVideo(video: VideoItem) {
        activePlayingVideoUri = video.uri
        isVideoPlayerActive = true
    }

    fun closeVideoPlayer() {
        isVideoPlayerActive = false
        activePlayingVideoUri = null
    }

    private fun loadFallbackVideos() {
        videoList = listOf(
            VideoItem("Clip Officiel - Paulz Bertey", "04:15"),
            VideoItem("Live Concert at Dardy Club", "12:30"),
            VideoItem("Visualizer - Ghetta Girl", "03:50"),
            VideoItem("Studio Recording - Caliente", "05:10")
        )
    }

    // --- Photos State ---
    var photosList by mutableStateOf<List<PhotoItem>>(emptyList())
        private set

    var activePhotoItem by mutableStateOf<PhotoItem?>(null)

    private fun loadFallbackPhotos() {
        photosList = List(9) { index ->
            PhotoItem(
                title = "Local Memory ${index + 1}",
                url = "https://picsum.photos/seed/zed_${index}/400/400"
            )
        }
    }

    // --- Real Media Loading (MediaStore Integration) ---
    var mediaLoadedSuccessfully by mutableStateOf(false)
        private set

    fun loadMediaFromDevice(context: Context) {
        viewModelScope.launch {
            try {
                // 1. Load Audio
                val audioList = mutableListOf<SongItem>()
                val audioProjection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.DURATION
                )
                val audioUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                context.contentResolver.query(
                    audioUri,
                    audioProjection,
                    "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                    null,
                    "${MediaStore.Audio.Media.TITLE} ASC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val title = cursor.getString(titleCol)
                        val artist = cursor.getString(artistCol)
                        val durationMs = cursor.getLong(durationCol)
                        
                        val fileUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                        val minutes = (durationMs / 1000) / 60
                        val seconds = (durationMs / 1000) % 60
                        val durStr = String.format("%02d:%02d", minutes, seconds)

                        audioList.add(
                            SongItem(
                                title = title,
                                artist = artist,
                                durationText = durStr,
                                uri = fileUri,
                                isLocal = true
                            )
                        )
                    }
                }

                if (audioList.isNotEmpty()) {
                    songsList = audioList
                    selectedMusicIndex = 0
                }

                // 2. Load Video
                val finalVideoList = mutableListOf<VideoItem>()
                val videoProjection = arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DURATION
                )
                val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                context.contentResolver.query(
                    videoUri,
                    videoProjection,
                    null,
                    null,
                    "${MediaStore.Video.Media.DISPLAY_NAME} ASC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val name = cursor.getString(nameCol)
                        val durationMs = cursor.getLong(durCol)
                        val fileUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                        val minutes = (durationMs / 1000) / 60
                        val seconds = (durationMs / 1000) % 60
                        val durStr = String.format("%02d:%02d", minutes, seconds)

                        finalVideoList.add(
                            VideoItem(
                                title = name,
                                durationText = durStr,
                                uri = fileUri,
                                isLocal = true
                            )
                        )
                    }
                }

                if (finalVideoList.isNotEmpty()) {
                    videoList = finalVideoList
                } else {
                    loadFallbackVideos()
                }

                // 3. Load Images
                val finalPhotosList = mutableListOf<PhotoItem>()
                val imagesProjection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME
                )
                val imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                context.contentResolver.query(
                    imagesUri,
                    imagesProjection,
                    null,
                    null,
                    "${MediaStore.Images.Media.DATE_ADDED} DESC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val name = cursor.getString(nameCol)
                        val fileUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                        finalPhotosList.add(
                            PhotoItem(
                                title = name,
                                uri = fileUri,
                                isLocal = true
                            )
                        )
                    }
                }

                if (finalPhotosList.isNotEmpty()) {
                    photosList = finalPhotosList
                } else {
                    loadFallbackPhotos()
                }

                mediaLoadedSuccessfully = true

            } catch (e: Exception) {
                e.printStackTrace()
                // Fall back in case of security exceptions or execution errors
                loadFallbackMedia()
                loadFallbackVideos()
                loadFallbackPhotos()
            }
        }
    }

    init {
        loadFallbackVideos()
        loadFallbackPhotos()
    }
}
