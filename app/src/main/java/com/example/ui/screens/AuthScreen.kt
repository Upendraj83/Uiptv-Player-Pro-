package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBackgroundEnd
import com.example.ui.theme.TiweeEmerald
import com.example.ui.theme.TiweeLiveRed
import com.example.ui.theme.TiweePurple

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isCheckingAutoSignIn by remember { mutableStateOf(true) }
  var isSigningIn by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = {
        isCheckingAutoSignIn = false
        onAuthSuccess()
      },
      onUnauthenticated = {
        isCheckingAutoSignIn = false
      },
      scope = scope,
    )
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Brush.verticalGradient(listOf(DarkBackground, DarkBackgroundEnd))),
  ) {
    // Atmospheric hero art backdrop
    Image(
      painter = painterResource(id = R.drawable.img_hero_banner),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      alpha = 0.45f,
      modifier = Modifier
        .fillMaxWidth()
        .height(340.dp),
    )

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(340.dp)
        .background(
          Brush.verticalGradient(
            listOf(Color.Transparent, DarkBackground.copy(alpha = 0.85f), DarkBackground)
          )
        ),
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Column(
        modifier = Modifier.widthIn(max = 460.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Surface(
          shape = RoundedCornerShape(26.dp),
          color = Color(0xFF161D27),
          border = BorderStroke(1.5.dp, TiweePurple.copy(alpha = 0.5f)),
          shadowElevation = 16.dp,
          modifier = Modifier.size(92.dp),
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_launcher_fg),
            contentDescription = "UiPTv Player Pro+ App Icon",
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxSize()
              .clip(RoundedCornerShape(26.dp)),
          )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
          text = "UiPTv Player Pro+",
          style = MaterialTheme.typography.displaySmall,
          color = Color.White,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Global Live IPTV Player",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.secondary,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "Explore thousands of live channels worldwide by country or genre, stream seamlessly in HLS, and sync your favorites across devices.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(28.dp))

        FeatureHighlightRow(
          icon = Icons.Rounded.Public,
          accent = TiweeLiveRed,
          title = "Worldwide Live Channels",
          subtitle = "Browse curated streams across 150+ countries and 20+ TV genres.",
        )

        Spacer(modifier = Modifier.height(12.dp))

        FeatureHighlightRow(
          icon = Icons.Rounded.LiveTv,
          accent = TiweePurple,
          title = "Adaptive HLS Playback",
          subtitle = "Powered by Media3 ExoPlayer with automatic multi-source failover.",
        )

        Spacer(modifier = Modifier.height(12.dp))

        FeatureHighlightRow(
          icon = Icons.Rounded.CloudSync,
          accent = TiweeEmerald,
          title = "Cloud-Synced Favorites & History",
          subtitle = "Save channels, recently viewed streams, and AMOLED theme to your account.",
        )

        Spacer(modifier = Modifier.height(28.dp))

        if (errorMessage != null) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = TiweeLiveRed.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, TiweeLiveRed.copy(alpha = 0.35f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 16.dp),
          ) {
            Text(
              text = errorMessage!!,
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFFCA5A5),
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(12.dp),
            )
          }
        }

        Button(
          onClick = {
            errorMessage = null
            isSigningIn = true
            onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                isSigningIn = false
                onAuthSuccess()
              },
              onAuthError = { msg ->
                isSigningIn = false
                errorMessage = msg
              },
              scope = scope,
              onAuthCancelled = {
                isSigningIn = false
              },
            )
          },
          enabled = !isSigningIn && !isCheckingAutoSignIn,
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = TiweePurple,
            contentColor = Color.White,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("google_sign_in_button"),
        ) {
          if (isSigningIn || isCheckingAutoSignIn) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              color = Color.White,
              strokeWidth = 2.5.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = if (isCheckingAutoSignIn) "Checking session..." else "Connecting...",
              style = MaterialTheme.typography.labelLarge,
            )
          } else {
            Icon(
              imageVector = Icons.Rounded.Login,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Sign in with Google",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FeatureHighlightRow(
  icon: ImageVector,
  accent: Color,
  title: String,
  subtitle: String,
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(accent.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accent,
          modifier = Modifier.size(22.dp),
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          color = Color.White,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
