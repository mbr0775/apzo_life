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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.apzolife.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun SignupScreen(
    onBackToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        if (state.message != null && state.message!!.contains("Account created")) {
            showSuccess = true
            delay(2200)
            onBackToLogin()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF060E1A),
                        Color(0xFF0A1628),
                        Color(0xFF0F2744)
                    )
                )
            )
    ) {
        // Decorative orbs
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopEnd)
                .offset(x = 80.dp, y = (-60).dp)
                .clip(CircleShape)
                .background(Color(0xFF6C5CE7).copy(alpha = 0.22f))
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = (-60).dp, y = 200.dp)
                .clip(CircleShape)
                .background(Color(0xFF00B894).copy(alpha = 0.15f))
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

            // Back to login
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onBackToLogin)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("← Back", fontSize = 13.sp, color = Color.White.copy(alpha = 0.65f), fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Brand
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF6C5CE7), Color(0xFF1D546C))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("✦", fontSize = 28.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Create account",
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = (-1).sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Start building discipline today",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.45f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Card
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AuthTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = "Full name",
                    icon = { Icon(Icons.Rounded.Person, null, tint = Color(0xFF8DCDE5), modifier = Modifier.size(18.dp)) }
                )
                AuthTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email address",
                    icon = { Icon(Icons.Rounded.Email, null, tint = Color(0xFF8DCDE5), modifier = Modifier.size(18.dp)) },
                    keyboardType = KeyboardType.Email
                )
                AuthTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    icon = { Icon(Icons.Rounded.Lock, null, tint = Color(0xFF8DCDE5), modifier = Modifier.size(18.dp)) },
                    trailing = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                null,
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardType = KeyboardType.Password
                )

                // Error
                AnimatedVisibility(visible = state.error != null, enter = fadeIn() + slideInVertically(), exit = fadeOut()) {
                    state.error?.let {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE17055).copy(alpha = 0.12f))
                                .border(1.dp, Color(0xFFE17055).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚠  $it", color = Color(0xFFFF9A8B), fontSize = 13.sp)
                        }
                    }
                }

                GradientButton(
                    text = "Create Account",
                    isLoading = state.isLoading,
                    onClick = {
                        viewModel.signup(fullName.trim(), email.trim(), password)
                    },
                    modifier = Modifier
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                Text("  have an account?  ", fontSize = 12.sp, color = Color.White.copy(alpha = 0.35f))
                Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.08f)))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    .clickable(onClick = onBackToLogin),
                contentAlignment = Alignment.Center
            ) {
                Text("Sign in →", color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF6C5CE7), Color(0xFF1D546C)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = Color.White, modifier = Modifier.size(44.dp))
                    }
                    Text("Account Created!", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text("Check your email to verify ✉️", fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f))
                }
            }
        }
    }
}