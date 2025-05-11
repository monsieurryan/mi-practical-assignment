package com.example.music_ryan.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextDecoration
import com.example.music_ryan.R
import com.example.music_ryan.data.UserPreferences
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.text.ClickableText

private val MiSans = FontFamily(
    Font(R.font.mi_sans_regular, FontWeight.Normal),
    Font(R.font.mi_sans_medium, FontWeight.Medium)
)

@Composable
fun TermsAndConditions(
    onUserAgreementClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onDisagreeClick: () -> Unit,
    onAgreeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    var shouldSaveTerms by remember { mutableStateOf(false) }

    LaunchedEffect(shouldSaveTerms) {
        if (shouldSaveTerms) {
            userPreferences.setAcceptedTerms(true)
            onAgreeClick()
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "声明与条款",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 25.dp, bottom = 16.dp),
                    color = Color.Black,
                    fontSize = 21.sp,
                    fontFamily = MiSans,
                    fontWeight = FontWeight(380),
                    textAlign = TextAlign.Center
                )

                val annotatedString = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = Color(0xCC000000),
                            fontSize = 15.sp,
                            fontFamily = MiSans,
                            fontWeight = FontWeight(380)
                        )
                    ) {
                        append("欢迎使用音乐社区，我们将严格遵守相关法律和隐私政策保护您的个人隐私，请您阅读并同意")
                    }
                    pushStringAnnotation(tag = "agreement", annotation = "")
                    withStyle(
                        style = SpanStyle(
                            color = Color(0xFF3482FF),
                            textDecoration = TextDecoration.None
                        )
                    ) {
                        append("《用户协议》")
                    }
                    pop()
                    withStyle(
                        style = SpanStyle(
                            color = Color(0xCC000000)
                        )
                    ) {
                        append("与")
                    }
                    pushStringAnnotation(tag = "privacy", annotation = "")
                    withStyle(
                        style = SpanStyle(
                            color = Color(0xFF3482FF),
                            textDecoration = TextDecoration.None
                        )
                    ) {
                        append("《隐私政策》")
                    }
                    pop()
                    withStyle(
                        style = SpanStyle(
                            color = Color(0xCC000000)
                        )
                    ) {
                        append("。")
                    }
                }

                ClickableText(
                    text = annotatedString,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    onClick = { offset ->
                        val annotations = annotatedString.getStringAnnotations(start = offset, end = offset)
                        annotations.firstOrNull()?.let {
                            when (it.tag) {
                                "agreement" -> onUserAgreementClick()
                                "privacy" -> onPrivacyPolicyClick()
                            }
                        }
                    }
                )

                Text(
                    text = "不同意",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .clickable { onDisagreeClick() },
                    color = Color(0x33000000),
                    fontSize = 14.sp,
                    fontFamily = MiSans,
                    fontWeight = FontWeight(380),
                    textAlign = TextAlign.Center
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(
                        color = Color(0xFF3482FF),
                        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                    )
                    .clickable { shouldSaveTerms = true }
            ) {
                Text(
                    text = "同意",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = MiSans,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

