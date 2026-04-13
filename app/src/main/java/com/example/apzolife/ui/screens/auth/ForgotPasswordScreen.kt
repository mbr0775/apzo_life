package com.example.apzolife.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.apzolife.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        if (state.message != null && state.message!!.contains("sent")) {
            showSuccess = true
            delay(2500)
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF060E1A), Color(0xFF0A1628), Color(0xFF0F2744))
                )
            )
    ) {
        // Orbs
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(x = (-70).dp, y = 100.dp)
                .clip(CircleShape)
                .background(Color(0xFF1D546C).copy(alpha = 0.28f))
        )
        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .clip(CircleShape)
                .background(Color(0xFF00B894).copy(alpha = 0.18f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onBack)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("← Back", fontSize = 13.sp, color = Color.White.copy(alpha = 0.65f), fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(52.dp))

            // Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1D546C), Color(0xFF2E7A9A))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Email, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Reset password",
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = (-1).sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Enter your email and we'll send\nyou a reset link",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.45f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.03f))
                        ),
                        RoundedCornerShape(28.dp)
                    )
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AuthTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email address",
                    icon = { Icon(Icons.Rounded.Email, null, tint = Color(0xFF8DCDE5), modifier = Modifier.size(18.dp)) },
                    keyboardType = KeyboardType.Email
                )

                AnimatedVisibility(visible = state.error != null, enter = fadeIn() + slideInVertically(), exit = fadeOut()) {
                    state.error?.let {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE17055).copy(alpha = 0.12f))
                                .border(1.dp, Color(0xFFE17055).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text("⚠  $it", color = Color(0xFFFF9A8B), fontSize = 13.sp)
                        }
                    }
                }

                GradientButton(
                    text = "Send Reset Link",
                    icon = { Icon(Icons.Rounded.Send, null, tint = Color.White, modifier = Modifier.size(16.dp)) },
                    isLoading = state.isLoading,
                    onClick = { viewModel.sendReset(email.trim()) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Remember your password?",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.35f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text("Back to Sign In", color = Color.White.copy(alpha = 0.65f), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // Success overlay
        AnimatedVisibility(
            visible = showSuccess,
            enter = fadeIn(tween(400)) + scaleIn(tween(400)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF060E1A).copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF1D546C), Color(0xFF2E7A9A)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = Color.White, modifier = Modifier.size(44.dp))
                    }
                    Text("Email Sent!", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text("Check your inbox for the reset link 📬", fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f))
                }
            }
        }
    }
}