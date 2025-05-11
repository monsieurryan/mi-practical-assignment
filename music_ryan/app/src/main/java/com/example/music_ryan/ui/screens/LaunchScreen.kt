package com.example.music_ryan.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.music_ryan.R
import kotlinx.coroutines.delay
import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.example.music_ryan.ui.components.TermsAndConditions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import com.example.music_ryan.data.UserPreferences
import androidx.compose.ui.zIndex
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.DisposableEffect

private val MiSans = FontFamily(
    Font(R.font.mi_sans_regular, FontWeight.Normal),
    Font(R.font.mi_sans_medium, FontWeight.Medium),
    Font(R.font.mi_sans_bold, FontWeight.Bold)
)

@Composable
fun LaunchScreen(
    modifier: Modifier = Modifier,
    onLaunchComplete: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    var startExitAnimation by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var shouldStartLaunchSequence by remember { mutableStateOf(false) }
    var hasCheckedTerms by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val hasAcceptedTerms by userPreferences.hasAcceptedTerms.collectAsState(initial = false)
    
    val fadeInAnimation by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "fadeIn"
    )

    val fadeOutAnimation by animateFloatAsState(
        targetValue = if (startExitAnimation) 0f else 1f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "fadeOut"
    )

    val scaleAnimation by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    // 自动暂停检测应用是否在前台
    var isPaused by remember { mutableStateOf(false) }
    
    DisposableEffect(Unit) {
//        val activity = context as? Activity
        
        onDispose {
            // 组件被销毁时设置暂停标志，防止退出过程中的回调执行
            isPaused = true
            println("LaunchScreen: 设置暂停标志，防止退出过程中执行回调")
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 启动页内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF6F7F8))
                .graphicsLayer {
                    alpha = fadeInAnimation * fadeOutAnimation
                    scaleX = scaleAnimation
                    scaleY = scaleAnimation
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 210.dp, bottom = 210.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        color = Color(0xFF3325CD).copy(alpha = 0.9f)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_music_logo),
                            contentDescription = "Music Community Logo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Text(
                        text = "音乐社区",
                        color = Color.Black,
                        fontSize = 20.sp,
                        fontFamily = MiSans,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "听你想听",
                    color = Color.Black.copy(alpha = 0.4f),
                    fontSize = 15.sp,
                    fontFamily = MiSans,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 3.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 用户许可页面
        if (hasCheckedTerms && showTermsDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .zIndex(1f)
            ) {
                TermsAndConditions(
                    onUserAgreementClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.mi.com"))
                        context.startActivity(intent)
                    },
                    onPrivacyPolicyClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.xiaomiev.com/"))
                        context.startActivity(intent)
                    },
                    onDisagreeClick = {
                        (context as? Activity)?.finish()
                    },
                    onAgreeClick = {
                        shouldStartLaunchSequence = true
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.85f)
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }

    // 启动动画
    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(1000L) // 等待启动动画完成
        hasCheckedTerms = true
        if (!hasAcceptedTerms) {
            showTermsDialog = true
        } else {
            delay(1000L) // 显示1秒
            // 检查是否已经暂停
            if (!isPaused) {
                startExitAnimation = true
                delay(800L) // 等待淡出动画完成
                // 再次检查是否已经暂停
                if (!isPaused) {
                    onLaunchComplete()
                } else {
                    println("LaunchScreen: 应用已暂停，不执行启动完成回调")
                }
            } else {
                println("LaunchScreen: 应用已暂停，不执行退出动画")
            }
        }
    }

    // 用户同意后的启动序列
    LaunchedEffect(shouldStartLaunchSequence) {
        if (shouldStartLaunchSequence) {
            showTermsDialog = false
            delay(1000L) // 显示1秒
            // 检查是否已经暂停
            if (!isPaused) {
                startExitAnimation = true
                delay(800L) // 等待淡出动画完成
                // 再次检查是否已经暂停
                if (!isPaused) {
                    onLaunchComplete()
                } else {
                    println("LaunchScreen: 应用已暂停，不执行启动完成回调")
                }
            } else {
                println("LaunchScreen: 应用已暂停，不执行退出动画")
            }
        }
    }
} 