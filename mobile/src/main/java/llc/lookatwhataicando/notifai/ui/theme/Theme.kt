package llc.lookatwhataicando.notifai.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary          = Brand80,
    onPrimary        = Color(0xFF002E6B),
    primaryContainer = Brand40,
    onPrimaryContainer = Color(0xFFD6E3FF),

    secondary        = Slate80,
    onSecondary      = Color(0xFF1C2531),
    secondaryContainer = Slate40,
    onSecondaryContainer = Color(0xFFD8E2F5),

    tertiary         = Amber80,
    onTertiary       = Color(0xFF5C3200),
    tertiaryContainer = Amber40,
    onTertiaryContainer = Color(0xFFFFDDB3),

    background       = Neutral10,
    onBackground     = Neutral90,
    surface          = Neutral10,
    onSurface        = Neutral90,
    surfaceVariant   = Neutral20,
    onSurfaceVariant = Color(0xFFC2C7D0),
    surfaceContainer = Neutral20,
    outline          = Color(0xFF8C9099),
)

private val LightColorScheme = lightColorScheme(
    primary          = Brand40,
    onPrimary        = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001947),

    secondary        = Slate40,
    onSecondary      = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD8E2F5),
    onSecondaryContainer = Color(0xFF0D1B2E),

    tertiary         = Amber40,
    onTertiary       = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDDB3),
    onTertiaryContainer = Color(0xFF2C1600),

    background       = Neutral99,
    onBackground     = Color(0xFF1A1C1E),
    surface          = Neutral99,
    onSurface        = Color(0xFF1A1C1E),
    surfaceVariant   = Neutral90,
    onSurfaceVariant = Color(0xFF44474E),
    surfaceContainer = Color(0xFFECEEF4),
    outline          = Color(0xFF74777F),
)

@Composable
fun NotifAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,          // disabled — wallpaper seed was overriding brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
