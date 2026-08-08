package app.snurko

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import app.snurko.challenge.data.SafNotebookDocumentGateway
import app.snurko.challenge.domain.NOTEBOOK_MIME_TYPE
import app.snurko.reminders.data.AndroidNotificationPermissionGateway
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val documentGateway: SafNotebookDocumentGateway by inject()
    private val notificationPermissions: AndroidNotificationPermissionGateway by inject()

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
        setContent {
            App()
        }
    }

    override fun onDestroy() {
        documentGateway.detach()
        notificationPermissions.detach()
        super.onDestroy()
    }
}
