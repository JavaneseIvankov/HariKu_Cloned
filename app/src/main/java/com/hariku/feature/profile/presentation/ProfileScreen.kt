package com.hariku.feature.profile.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.hariku.feature.auth.domain.model.AuthUser
import org.koin.androidx.compose.koinViewModel

private val ProfileBackground = Color(0xFFF2F2F2)
private val ProfileAccent = Color(0xFFC97D50)
private val ProfileText = Color(0xFF242424)

@Composable
fun ProfileScreen(
    navController: NavController,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel()
) {
    ProfileContent(
        user = viewModel.currentUser,
        onBack = { navController.popBackStack() },
        onLogout = {
            viewModel.logout()
            onLogout()
        }
    )
}

@Composable
private fun ProfileContent(
    user: AuthUser?,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Keluar dari Akun?",
                    fontWeight = FontWeight.Bold,
                    color = ProfileText
                )
            },
            text = {
                Text(
                    text = "Apakah kamu yakin ingin keluar dari akun ini?",
                    color = Color(0xFF6B4E3D)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD98585))
                ) {
                    Text("Keluar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal", color = Color(0xFF6B4E3D))
                }
            }
        )
    }

    Scaffold(
        containerColor = ProfileBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .statusBarsPadding()
                    .height(70.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = ProfileAccent,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable(onClick = onBack)
                )
                Text(
                    text = "Profile",
                    color = ProfileText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.size(28.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = user?.photoUrl,
                        contentDescription = "Foto Profil",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3B57B)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = user?.name?.takeIf(String::isNotBlank) ?: "Nama belum tersedia",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfileText
                    )
                    Text(
                        text = user?.email?.takeIf(String::isNotBlank) ?: "Email belum tersedia",
                        fontSize = 14.sp,
                        color = Color(0xFF6B4E3D)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            ProfileMenuRow("Notifikasi", Icons.Filled.NotificationsNone)
            ProfileMenuRow("Dark Mode", Icons.Filled.DarkMode, showSwitch = true)
            ProfileMenuRow("Ketentuan Pengguna", Icons.Filled.Policy)
            ProfileMenuRow("Bantuan", Icons.AutoMirrored.Filled.HelpOutline)
            ProfileMenuRow(
                text = "Keluar",
                icon = Icons.AutoMirrored.Filled.Logout,
                color = Color(0xFFD98585),
                onClick = { showLogoutDialog = true }
            )
        }
    }
}

@Composable
private fun ProfileMenuRow(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color = ProfileText,
    showSwitch: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(enabled = text == "Keluar", onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = text, tint = color, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(14.dp))
                Text(text = text, fontSize = 16.sp, color = color, fontWeight = FontWeight.Medium)
            }
            if (showSwitch) {
                Switch(
                    checked = false,
                    onCheckedChange = {},
                    modifier = Modifier.size(48.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ProfileAccent,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFE1E1E1)
                    )
                )
            }
        }
    }
}

@Composable
fun ProfileEntryButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text("Profile") }
}
