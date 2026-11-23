package com.hariku.feature.auth.presentation.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hariku.core.ui.theme.HariKuTheme
import com.hariku.feature.auth.presentation.components.RegularTextField
import org.koin.androidx.compose.koinViewModel

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: RegisterScreenViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.registerSuccess) {
        if (state.registerSuccess) onRegistered()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Buat Akun", style = MaterialTheme.typography.headlineMedium)
        Text(text = "HariKu", style = MaterialTheme.typography.titleMedium)

        RegularTextField(
            text = state.email,
            onValueChange = viewModel::onEmailChange,
            placeholder = "Email",
            enabled = !state.isLoading
        )

        RegularTextField(
            text = state.name,
            onValueChange = viewModel::onNameChange,
            placeholder = "Nama",
            keyboardType = KeyboardType.Text,
            enabled = !state.isLoading
        )

        RegularTextField(
            text = state.password,
            onValueChange = viewModel::onPasswordChange,
            placeholder = "Kata Sandi (Minimal 8 Karakter)",
            isPassword = true,
            enabled = !state.isLoading
        )

        RegularTextField(
            text = state.confirmation,
            onValueChange = viewModel::onConfirmationChange,
            placeholder = "Konfirmasi Kata Sandi",
            isPassword = true,
            imeAction = ImeAction.Done,
            enabled = !state.isLoading
        )

        state.error?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(
            onClick = viewModel::onRegisterClicked,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B76A8))
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(2.dp))
            } else {
                Text(text = "Daftar")
            }
        }

        TextButton(onClick = onNavigateToLogin, enabled = !state.isLoading) {
            Text(text = "Sudah punya akun? Masuk")
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    HariKuTheme {
        RegisterScreen(onRegistered = {}, onNavigateToLogin = {})
    }
}
