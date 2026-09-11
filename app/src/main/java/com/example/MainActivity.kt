package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.model.MediaItem
import com.example.editor.ui.PhotoEditorScreen
import com.example.ui.screens.FullScreenViewerScreen
import com.example.ui.screens.GalleryHomeScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.EditorViewModel
import com.example.ui.viewmodel.GalleryViewModel
import com.example.ui.viewmodel.ThemeMode

class MainActivity : ComponentActivity() {

    private val galleryViewModel: GalleryViewModel by viewModels()
    private val editorViewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by galleryViewModel.uiState.collectAsState()

            val isDarkTheme = when (uiState.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        galleryViewModel = galleryViewModel,
                        editorViewModel = editorViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    galleryViewModel: GalleryViewModel,
    editorViewModel: EditorViewModel
) {
    val navController = rememberNavController()

    // State for Fullscreen viewer
    var viewerMediaList by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var viewerInitialIndex by remember { mutableIntStateOf(0) }

    // State for Photo Editor
    var editingMediaItem by remember { mutableStateOf<MediaItem?>(null) }

    // Request permissions on first launch
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            galleryViewModel.loadMedia()
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissionsToRequest)
    }

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            GalleryHomeScreen(
                viewModel = galleryViewModel,
                onOpenMedia = { clickedItem, list ->
                    viewerMediaList = list
                    viewerInitialIndex = list.indexOfFirst { it.id == clickedItem.id }.coerceAtLeast(0)
                    navController.navigate("viewer")
                },
                onOpenSearch = {
                    navController.navigate("search")
                }
            )
        }

        composable("search") {
            SearchScreen(
                viewModel = galleryViewModel,
                onBack = { navController.popBackStack() },
                onOpenMedia = { clickedItem, list ->
                    viewerMediaList = list
                    viewerInitialIndex = list.indexOfFirst { it.id == clickedItem.id }.coerceAtLeast(0)
                    navController.navigate("viewer")
                }
            )
        }

        composable("viewer") {
            FullScreenViewerScreen(
                mediaList = viewerMediaList,
                initialIndex = viewerInitialIndex,
                onBack = { navController.popBackStack() },
                onEditMedia = { item ->
                    editingMediaItem = item
                    editorViewModel.initEditor(item.uri, item.name)
                    navController.navigate("editor")
                },
                viewModel = galleryViewModel
            )
        }

        composable("editor") {
            PhotoEditorScreen(
                viewModel = editorViewModel,
                onClose = { navController.popBackStack() },
                onSaved = { savedUri ->
                    galleryViewModel.loadMedia()
                    navController.popBackStack()
                }
            )
        }
    }
}

