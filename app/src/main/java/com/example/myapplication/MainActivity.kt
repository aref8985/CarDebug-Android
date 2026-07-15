package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.settings.AppLanguage
import com.example.myapplication.data.settings.AppTheme
import com.example.myapplication.data.settings.SettingsRepository
import com.example.myapplication.ui.MainScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.util.LocaleManager
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val repository = remember { SettingsRepository(context) }
            
            // Core States
            val theme by repository.themeFlow.collectAsState(initial = null)
            val language by repository.languageFlow.collectAsState(initial = null)
            
            var showSplash by remember { mutableStateOf(true) }

            // Splash Logic: Artificial delay for "Professional Feel"
            LaunchedEffect(theme, language) {
                if (theme != null && language != null) {
                    delay(3000) // Calmer 3-second delay
                    showSplash = false
                }
            }

            val darkTheme = when (theme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            val localeContext = remember(language) {
                if (language != null) {
                    LocaleManager.updateLocale(context, if (language == AppLanguage.EN) "en" else "fa")
                } else context
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (showSplash) {
                        ManualSplashScreen()
                    } else {
                        MainScreen(localeContext = localeContext)
                    }
                }
            }
        }
    }
}

@Composable
fun ManualSplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val messages = listOf(
        "در حال فراخوانی تنظیمات کاربر...",
        "بررسی یکپارچگی دیتابیس خودرو...",
        "راه‌اندازی ماژول ارتباطی بلوتوث...",
        "آماده‌باش کامل..."
    )
    var currentMessageIndex by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        while(true) {
            delay(750) // Slower, more readable cycle
            currentMessageIndex = (currentMessageIndex + 1) % messages.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)), // Solid professional dark
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(130.dp)
                    .scale(scale),
                shape = CircleShape,
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFF9800).copy(alpha = 0.3f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(110.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "RF CarDiag",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                modifier = Modifier.width(160.dp).height(3.dp).clip(CircleShape),
                color = Color(0xFFFF9800),
                trackColor = Color.White.copy(alpha = 0.05f)
            )
            
            // Fixed alignment and centered dynamic message
            Text(
                text = messages[currentMessageIndex],
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth(),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFFF9800).copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center, // Explicitly centered
                letterSpacing = 0.5.sp
            )
        }
        
        Text(
            text = "INDUSTRIAL GRADE OBD SOLUTION",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            ),
            color = Color.Gray.copy(alpha = 0.3f)
        )
    }
}
