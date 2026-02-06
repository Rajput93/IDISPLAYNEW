package com.app.idisplaynew.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.idisplaynew.R
import com.app.idisplaynew.data.viewmodel.LoginViewModel
import com.app.idisplaynew.ui.theme.DisplayHubBackground
import com.app.idisplaynew.ui.theme.DisplayHubBlue
import com.app.idisplaynew.ui.theme.DisplayHubBorder
import com.app.idisplaynew.ui.theme.DisplayHubCardBackground
import com.app.idisplaynew.ui.theme.DisplayHubPlaceholder
import com.app.idisplaynew.ui.theme.DisplayHubTextSecondary
import com.app.idisplaynew.ui.utils.rememberResponsiveValues

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoading by viewModel.isLoading.observeAsState(initial = false)
    val error by viewModel.error.observeAsState(initial = null)
    val registerResponse by viewModel.registerResponse.observeAsState(initial = null)
    val snackbarHostState = remember { SnackbarHostState() }
    val responsive = rememberResponsiveValues()
    val scrollState = rememberScrollState()

    LaunchedEffect(registerResponse) {
        registerResponse?.let { response ->
            if (response.isSuccess) {
                snackbarHostState.showSnackbar(
                    message = response.message.ifBlank { "Device registered successfully" },
                    withDismissAction = true
                )
                delay(1500)
                viewModel.clearRegisterResponse()
                onLoginSuccess()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> Snackbar(snackbarData = data) } }
    ) { paddingValues ->
    Box(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .background(DisplayHubBackground)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = responsive.cardHorizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Image(
                painter = painterResource(R.drawable.ic_launcher),
                contentDescription = null,
                modifier = Modifier.size(responsive.logoSize),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.displayhub),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = (MaterialTheme.typography.headlineMedium.fontSize.value * responsive.titleFontScale).sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier
                    .widthIn(max = responsive.effectiveCardWidth)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DisplayHubCardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.sign_in),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = (MaterialTheme.typography.headlineLarge.fontSize.value * responsive.titleFontScale).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.sign_in_subtitle),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * responsive.bodyFontScale).sp
                        ),
                        color = DisplayHubTextSecondary
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(R.string.base_url),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = (MaterialTheme.typography.labelLarge.fontSize.value * responsive.bodyFontScale).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.baseUrl,
                        onValueChange = viewModel::updateBaseUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = responsive.inputMinHeight),
                        placeholder = {
                            Text(
                                stringResource(R.string.base_url_placeholder),
                                color = DisplayHubPlaceholder
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DisplayHubBlue,
                            unfocusedBorderColor = DisplayHubBorder,
                            focusedContainerColor = DisplayHubCardBackground,
                            unfocusedContainerColor = DisplayHubCardBackground,
                            cursorColor = DisplayHubBlue,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = stringResource(R.string.client_id),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = (MaterialTheme.typography.labelLarge.fontSize.value * responsive.bodyFontScale).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.clientId,
                        onValueChange = viewModel::updateClientId,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = responsive.inputMinHeight),
                        placeholder = {
                            Text(
                                stringResource(R.string.client_id_hint),
                                color = DisplayHubPlaceholder
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DisplayHubBlue,
                            unfocusedBorderColor = DisplayHubBorder,
                            focusedContainerColor = DisplayHubCardBackground,
                            unfocusedContainerColor = DisplayHubCardBackground,
                            cursorColor = DisplayHubBlue,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    error?.let { msg ->
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = { viewModel.register() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(responsive.buttonHeight),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DisplayHubBlue),
                        enabled = !isLoading,
                        contentPadding = ButtonDefaults.ContentPadding
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                        Text(
                            text = if (isLoading) "..." else stringResource(R.string.login),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = (MaterialTheme.typography.labelLarge.fontSize.value * responsive.bodyFontScale).sp
                            )
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    }
}
