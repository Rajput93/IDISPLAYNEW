package com.app.idisplaynew

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.idisplaynew.data.repository.Repository
import com.app.idisplaynew.data.viewmodel.LoginViewModel
import com.app.idisplaynew.data.viewmodel.LoginViewModelFactory
import com.app.idisplaynew.ui.screens.login.LoginScreen
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
            LoginScreen(viewModel = viewModel)
        }
    }
}

