package app.snurko

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.snurko.billing.data.PlayBillingGateway
import app.snurko.challenge.data.SafNotebookDocumentGateway
import app.snurko.challenge.domain.NOTEBOOK_MIME_TYPE
import app.snurko.reminders.data.AndroidNotificationPermissionGateway
import org.koin.android.ext.android.inject

const val EXTRA_OPEN_UNLOCK = "app.snurko.extra.OPEN_UNLOCK"

class MainActivity : ComponentActivity() {

    private val documentGateway: SafNotebookDocumentGateway by inject()
    private val notificationPermissions: AndroidNotificationPermissionGateway by inject()
    private val billingGateway: PlayBillingGateway by inject()

    private var isUnlockRequested by mutableStateOf(false)

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
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        documentGateway.attach(
            SafNotebookDocumentGateway.Launchers(
                createDocument = createDocument,
                openDocument = openDocument,
            ),
        )
        notificationPermissions.attach(requestNotificationPermission)
        billingGateway.attach(this)
        isUnlockRequested = intent.opensUnlock()
        setContent {
            App(
                isUnlockRequested = isUnlockRequested,
                onUnlockOpened = { isUnlockRequested = false },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        isUnlockRequested = intent.opensUnlock()
    }

    private fun Intent?.opensUnlock(): Boolean =
        this?.getBooleanExtra(EXTRA_OPEN_UNLOCK, false) == true

    override fun onDestroy() {
        documentGateway.detach()
        notificationPermissions.detach()
        billingGateway.detach()
        super.onDestroy()
    }
}
