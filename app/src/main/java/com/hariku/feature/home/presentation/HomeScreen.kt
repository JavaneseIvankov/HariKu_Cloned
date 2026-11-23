package com.hariku.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.hariku.core.ui.components.Routes
import com.hariku.core.ui.theme.HariKuTheme
import org.koin.androidx.compose.koinViewModel

private val HomeCanvas = Color(0xFFFDF8F3)
private val HomeInk = Color(0xFF352C28)
private val HomeMuted = Color(0xFF74665E)
private val HomeAccent = Color(0xFFC97D50)
private val HomeSage = Color(0xFFEAF1E8)
private val HomeBlue = Color(0xFFE9F0F7)

@Composable
fun HomeScreen(
    navController: NavController,
    onOpenProfile: () -> Unit,
    onOpenSos: () -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val user = viewModel.currentUser
    HomeContent(
        displayName = user?.name?.takeIf(String::isNotBlank) ?: "Selamat datang",
        photoUrl = user?.photoUrl,
        onOpenProfile = onOpenProfile,
        onOpenSos = onOpenSos,
        onOpenChat = { navController.navigate(Routes.CustomizeCat.route) }
    )
}

@Composable
private fun HomeContent(
    displayName: String,
    photoUrl: String?,
    onOpenProfile: () -> Unit,
    onOpenSos: () -> Unit,
    onOpenChat: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeCanvas),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .clickable(onClick = onOpenProfile)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Profil",
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3B57B)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("HariKu", fontSize = 12.sp, color = HomeMuted)
                        Text(displayName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = HomeInk)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = onOpenSos,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF18476)),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                ) { Text("SOS", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Ruang untuk dirimu", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = HomeInk)
                Text("Luangkan waktu sejenak untuk mengenali perasaan dan mencari dukungan.", fontSize = 14.sp, color = HomeMuted, lineHeight = 21.sp)
                Button(
                    onClick = onOpenChat,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HomeAccent)
                ) {
                    Text("Mulai percakapan")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
        item {
            PlaceholderSection(
                title = "Suasana hati",
                owner = "Quick Mood Tracker · Raditya",
                message = "Pencatatan suasana hati akan tersedia di bagian ini.",
                tint = HomeSage
            )
        }
        item {
            PlaceholderSection(
                title = "Refleksi harian",
                owner = "Guided Journaling · Yoga",
                message = "Prompt dan jurnal harian akan tersedia di bagian ini.",
                tint = Color(0xFFF9EEE5)
            )
        }
        item {
            PlaceholderSection(
                title = "Ringkasan suasana hati",
                owner = "Mood Analytics & Reporting · Bagas",
                message = "Ringkasan dan laporan akan tersedia di bagian ini.",
                tint = HomeBlue
            )
        }
        item {
            Text(
                text = "Bagian ini akan terhubung ke fitur masing-masing setelah implementasi pemiliknya selesai.",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                fontSize = 12.sp,
                color = HomeMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PlaceholderSection(title: String, owner: String, message: String, tint: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = tint)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(owner, fontSize = 11.sp, color = HomeMuted, fontWeight = FontWeight.Medium)
            Text(title, fontSize = 18.sp, color = HomeInk, fontWeight = FontWeight.Bold)
            Text(message, fontSize = 13.sp, color = HomeMuted, lineHeight = 19.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    HariKuTheme {
        HomeContent(
            displayName = "Ayu",
            photoUrl = null,
            onOpenProfile = {},
            onOpenSos = {},
            onOpenChat = {}
        )
    }
}
