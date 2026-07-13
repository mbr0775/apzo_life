package com.example.apzolife.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PendingActions
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.apzolife.data.model.TaskStatus
import com.example.apzolife.data.repository.AuthRepository
import com.example.apzolife.ui.theme.ApzoError
import com.example.apzolife.ui.theme.ApzoSuccess
import com.example.apzolife.ui.theme.ApzoTertiary
import com.example.apzolife.ui.theme.ThemeManager
import com.example.apzolife.viewmodel.ApzoViewModel
import com.example.apzolife.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLoggedOut: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel(),
    apzoViewModel: ApzoViewModel = viewModel()
) {
    val isDark = ThemeManager.isDarkMode
    var showLogoutDialog by remember { mutableStateOf(false) }
    val homeState by apzoViewModel.homeState.collectAsState()

    LaunchedEffect(Unit) { apzoViewModel.loadHomeData() }

    val savedEmail = AuthRepository.getSavedEmail()
    val displayName = savedEmail
        .takeIf { it.isNotBlank() }
        ?.substringBefore("@")
        ?.replaceFirstChar { it.uppercase() }
        ?: "Apzo Life User"

    val totalTasks = homeState.allTasks.size
    val doneTasks = homeState.allTasks.count { it.status == TaskStatus.DONE.name }
    val pendingTasks = homeState.allTasks.count { it.status == TaskStatus.PENDING.name }
    val completionRate = if (totalTasks == 0) 0 else (doneTasks * 100) / totalTasks

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ApzoError.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Logout, null, tint = ApzoError, modifier = Modifier.size(24.dp))
                }
            },
            title = {
                Text("Sign out?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text(
                    "You'll need to sign in again to access your tasks.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(ApzoError)
                        .clickable {
                            showLogoutDialog = false
                            authViewModel.logout()
                            onLoggedOut()
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text("Sign out", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            // ── Hero header with avatar ─────────────────────────────
            item {
                ProfileHero(displayName = displayName, email = savedEmail)
            }

            // ── Quick stats strip ───────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .offset(y = (-28).dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfileStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.TaskAlt,
                        value = totalTasks.toString(),
                        label = "Total",
                        accent = MaterialTheme.colorScheme.primary
                    )
                    ProfileStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.PendingActions,
                        value = pendingTasks.toString(),
                        label = "Pending",
                        accent = Color(0xFFFDCB6E)
                    )
                    ProfileStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.AutoAwesome,
                        value = "$completionRate%",
                        label = "Complete",
                        accent = ApzoSuccess
                    )
                }
            }

            // ── Appearance section ──────────────────────────────────
            item {
                SectionLabel("Appearance")
            }

            item {
                ModernCard {
                    ThemeToggleRow(isDark = isDark)
                }
            }

            // ── Account section ──────────────────────────────────────
            item {
                SectionLabel("Account")
            }

            item {
                ModernCard {
                    Column {
                        ProfileInfoRow(
                            icon = Icons.Rounded.Email,
                            iconBg = ApzoTertiary.copy(alpha = 0.14f),
                            iconTint = ApzoTertiary,
                            title = "Email",
                            subtitle = savedEmail.ifBlank { "Not available" }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 70.dp)
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        )
                        ProfileInfoRow(
                            icon = Icons.Rounded.Person,
                            iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Display name",
                            subtitle = displayName
                        )
                    }
                }
            }

            // ── About section ─────────────────────────────────────────
            item {
                SectionLabel("About")
            }

            item {
                ModernCard {
                    ProfileInfoRow(
                        icon = Icons.Rounded.Info,
                        iconBg = Color(0xFF6C5CE7).copy(alpha = 0.15f),
                        iconTint = Color(0xFFA29BFE),
                        title = "Apzo Life",
                        subtitle = "Version 1.0.0 · Task Tracker"
                    )
                }
            }

            // ── Sign out ─────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(ApzoError.copy(alpha = 0.10f), ApzoError.copy(alpha = 0.04f))
                            )
                        )
                        .border(1.dp, ApzoError.copy(alpha = 0.20f), RoundedCornerShape(20.dp))
                        .clickable { showLogoutDialog = true }
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(ApzoError.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Logout, null, tint = ApzoError, modifier = Modifier.size(21.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sign out", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ApzoError)
                            Text(
                                "Logged out from all sessions",
                                fontSize = 12.sp,
                                color = ApzoError.copy(alpha = 0.65f)
                            )
                        }
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = ApzoError.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

// ── Hero header ──────────────────────────────────────────────────────

@Composable
private fun ProfileHero(displayName: String, email: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A1628),
                        Color(0xFF0F2744),
                        Color(0xFF1D546C)
                    )
                )
            )
    ) {
        // Decorative orbs
        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = (-40).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
        )
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-40).dp, y = 20.dp)
                .clip(CircleShape)
                .background(Color(0xFF00B894).copy(alpha = 0.14f))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Profile",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier
                    .align(Alignment.Start)
            )

            Spacer(Modifier.height(20.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1D546C), Color(0xFF00B894))
                        )
                    )
                    .border(3.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.take(1).uppercase(),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                displayName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (email.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    email,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── Stat card (floats over the hero bottom edge) ─────────────────────

@Composable
private fun ProfileStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    accent: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
            )
        }
    }
}

// ── Section label ─────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        modifier = Modifier.padding(start = 24.dp, top = 22.dp, bottom = 8.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        letterSpacing = 1.4.sp
    )
}

// ── Generic modern card wrapper ───────────────────────────────────────

@Composable
private fun ModernCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        content()
    }
}

// ── Theme toggle row (segmented, modern) ─────────────────────────────

@Composable
private fun ThemeToggleRow(isDark: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    if (isDark) Color(0xFF1D546C).copy(alpha = 0.20f)
                    else Color(0xFFFDCB6E).copy(alpha = 0.20f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isDark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                null,
                tint = if (isDark) Color(0xFF8DCDE5) else Color(0xFFFDCB6E),
                modifier = Modifier.size(21.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (isDark) "Dark Mode" else "Light Mode",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                if (isDark) "Switch to light theme" else "Switch to dark theme",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
            )
        }
        Switch(
            checked = isDark,
            onCheckedChange = { ThemeManager.setDark(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF1D546C),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )
    }
}

// ── Info row (used for account / about) ───────────────────────────────

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(19.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(
                subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}