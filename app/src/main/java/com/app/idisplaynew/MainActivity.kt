package com.app.idisplaynew

import android.app.Application
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.app.idisplaynew.data.local.AppDatabase
import com.app.idisplaynew.data.local.MediaDownloadManager
import com.app.idisplaynew.data.repository.Repository
import com.app.idisplaynew.data.repository.ScheduleRepository
import com.app.idisplaynew.data.viewmodel.HomeViewModel
import com.app.idisplaynew.data.viewmodel.HomeViewModelFactory
import com.app.idisplaynew.data.viewmodel.LoginViewModel
import com.app.idisplaynew.data.viewmodel.LoginViewModelFactory
import com.app.idisplaynew.ui.screens.login.LoginScreen
import com.app.idisplaynew.ui.screens.home.HomeScreen
import com.app.idisplaynew.ui.utils.DataStoreManager
import com.app.idisplaynew.ui.screens.SplashScreen
import com.app.idisplaynew.ui.theme.IDisplayNewTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { false }
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            IDisplayNewTheme {
                DisplayHubApp()
            }
        }
    }
}

@Composable
private fun DisplayHubApp() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            val context = LocalContext.current
            val dataStoreManager = remember { DataStoreManager(context.applicationContext) }
            SplashScreen(
                dataStoreManager = dataStoreManager,
                onSplashFinished = { isLoggedIn ->
                    if (isLoggedIn) {
                        navController.navigate("home") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        navController.navigate("login") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("login") {
            val context = LocalContext.current
            val app = context.applicationContext as Application
            val dataStoreManager = remember { DataStoreManager(context.applicationContext) }
            val viewModel: LoginViewModel = viewModel(
                factory = LoginViewModelFactory(Repository, dataStoreManager, app)
            )
            LoginScreen(
                viewModel = viewModel,
                dataStoreManager = dataStoreManager,
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            val view = LocalView.current
            val context = LocalContext.current
            val appContext = context.applicationContext
            val dataStoreManager = remember { DataStoreManager(appContext) }
            val db = remember {
                Room.databaseBuilder(appContext, AppDatabase::class.java, "idisplay_db")
                    .build()
            }
            val downloadManager = remember { MediaDownloadManager(appContext) }
            val scheduleRepository = remember(db, downloadManager, dataStoreManager) {
                ScheduleRepository(Repository, db, downloadManager, dataStoreManager)
            }
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModelFactory(scheduleRepository)
            )

            DisposableEffect(Unit) {
                val window = (view.context as? ComponentActivity)?.window
                    ?: return@DisposableEffect onDispose { }

                WindowCompat.setDecorFitsSystemWindows(window, false)
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    window.attributes.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
                onDispose { }
            }

            HomeScreen(viewModel = homeViewModel)
        }
    }
}

