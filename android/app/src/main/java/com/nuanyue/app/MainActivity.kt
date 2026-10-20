package com.nuanyue.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nuanyue.app.ui.NuanYueRoot
import com.nuanyue.app.ui.theme.NuanYueTheme
import com.nuanyue.app.update.AppUpdate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        routeUpdate(intent)
        // The app is always light. auto() would follow the phone's dark theme and
        // draw white status icons on our blush background.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            NuanYueTheme {
                NuanYueRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeUpdate(intent)
    }

    override fun onResume() {
        super.onResume()
        (application as NuanYueApp).updates.onHostResume(this)
    }

    private fun routeUpdate(intent: Intent?) {
        if (intent?.getBooleanExtra(AppUpdate.EXTRA_OPEN_UPDATE, false) == true) {
            (application as NuanYueApp).updates.present()
            intent.removeExtra(AppUpdate.EXTRA_OPEN_UPDATE)
        }
    }
}
