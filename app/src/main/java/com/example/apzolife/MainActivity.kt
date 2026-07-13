package com.example.apzolife

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.apzolife.navigation.ApzoNavGraph
import com.example.apzolife.navigation.Screen
import com.example.apzolife.ui.theme.ApzoLifeTheme
import com.example.apzolife.ui.theme.ThemeManager

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Home.route, "Home", Icons.Rounded.HomeWork, Icons.Rounded.Home),
    BottomNavItem(Screen.Calendar.route, "Calendar", Icons.Rounded.CalendarMonth, Icons.Rounded.CalendarMonth),
    BottomNavItem(Screen.Analytics.route, "Insights", Icons.Rounded.Insights, Icons.Rounded.Insights),
    BottomNavItem(Screen.Completed.route, "Done", Icons.Rounded.CheckCircleOutline, Icons.Rounded.CheckCircle),
    BottomNavItem(Screen.Settings.route, "Profile", Icons.Rounded.Person, Icons.Rounded.Person)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ApzoLifeTheme(darkTheme = ThemeManager.isDarkMode) { ApzoApp() }
        }
    }
}

@Composable
fun ApzoApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomNav = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomNav)
                ApzoBottomNavBar(navController = navController, currentRoute = currentRoute)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            ApzoNavGraph(navController = navController)
        }
    }
}

@Composable
fun ApzoBottomNavBar(navController: NavHostController, currentRoute: String?) {
    val bg = if (ThemeManager.isDarkMode) Color(0xFF1A2F38).copy(alpha = 0.97f)
    else Color.White.copy(alpha = 0.97f)

    Surface(color = bg, shadowElevation = 0.dp) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(0.5.dp)
                    .background(if (ThemeManager.isDarkMode) Color.White.copy(0.08f) else Color.Black.copy(0.06f))
                    .align(Alignment.TopCenter)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomNavItems.forEach { item ->
                    val selected = currentRoute == item.route
                    ModernNavItem(item = item, selected = selected) {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernNavItem(item: BottomNavItem, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val iconColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        tween(200), label = ""
    )
    val labelColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
        tween(200), label = ""
    )
    val scale by animateFloatAsState(
        if (selected) 1.08f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = ""
    )
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 6.dp)
            .scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (selected)
                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)))
            Icon(if (selected) item.selectedIcon else item.icon, item.title,
                tint = iconColor, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.height(3.dp))
        Text(item.title, fontSize = 9.sp, lineHeight = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = labelColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Box(modifier = Modifier.size(if (selected) 4.dp else 0.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary))
    }
}