package com.app.idisplaynew

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
import com.app.idisplaynew.data.repository.Repository
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
            SplashScreen(
                onSplashFinished = {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("login") {
            val context = LocalContext.current
            val dataStoreManager = remember { DataStoreManager(context.applicationContext) }
            val viewModel: LoginViewModel = viewModel(
                factory = LoginViewModelFactory(Repository, dataStoreManager)
            )
            LoginScreen(
                viewModel = viewModel,
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
            val dataStoreManager = remember { DataStoreManager(context.applicationContext) }
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModelFactory(Repository, dataStoreManager)
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

