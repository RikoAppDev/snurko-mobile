package app.snurko

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import app.snurko.billing.data.PlayBillingGateway
import app.snurko.challenge.data.SafNotebookDocumentGateway
import app.snurko.challenge.domain.NOTEBOOK_MIME_TYPE
import app.snurko.reminders.data.AndroidNotificationPermissionGateway
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val documentGateway: SafNotebookDocumentGateway by inject()
    private val notificationPermissions: AndroidNotificationPermissionGateway by inject()
    private val billingGateway: PlayBillingGateway by inject()

    private val createDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument(NOTEBOOK_MIME_TYPE),
    ) { uri -> documentGateway.onDocumentPicked(uri) }

    private val openDocument = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> documentGateway.onDocumentPicked(uri) }

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted -> notificationPermissions.onPermissionResult(isGranted) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        forceDarkSystemBarIcons()
        documentGateway.attach(
            SafNotebookDocumentGateway.Launchers(
                createDocument = createDocument,
                openDocument = openDocument,
            ),
        )
        notificationPermissions.attach(requestNotificationPermission)
        billingGateway.attach(this)
        setContent {
            App()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) forceDarkSystemBarIcons()
    }

    /** The app is paper in every system theme, so the bars keep their dark icons even in night mode. */
    private fun forceDarkSystemBarIcons() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    override fun onDestroy() {
        documentGateway.detach()
        notificationPermissions.detach()
        billingGateway.detach()
        super.onDestroy()
    }
}
