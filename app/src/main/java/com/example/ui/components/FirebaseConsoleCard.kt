package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.app

object FirebaseConsoleLinks {
    const val DEFAULT_PROJECT_ID = "gen-lang-client-0270493025"
    const val OWNER_ADMIN_EMAIL = "youngskiiicey@gmail.com"

    fun resolveProjectId(): String =
        runCatching { Firebase.app.options.projectId }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_PROJECT_ID

    fun resolveDatabaseId(context: Context): String =
        runCatching { context.getString(R.string.firestore_database_id) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "ai-studio-android-battlebo-f9f1c6c4-69cd-46ca-9bcc-c54ee93a8e1b"

    fun authUsersUrl(projectId: String = resolveProjectId()): String =
        "https://console.firebase.google.com/project/$projectId/authentication/users"

    fun authProvidersUrl(projectId: String = resolveProjectId()): String =
        "https://console.firebase.google.com/project/$projectId/authentication/providers"

    fun firestoreDataUrl(
        projectId: String = resolveProjectId(),
        databaseId: String = "ai-studio-android-battlebo-f9f1c6c4-69cd-46ca-9bcc-c54ee93a8e1b"
    ): String =
        "https://console.firebase.google.com/project/$projectId/firestore/databases/$databaseId/data"

    fun projectOverviewUrl(projectId: String = resolveProjectId()): String =
        "https://console.firebase.google.com/project/$projectId/overview"

    fun openUrlOrCopy(
        context: Context,
        clipboard: ClipboardManager,
        url: String,
        label: String,
        onFeedback: (String) -> Unit
    ) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val launched = runCatching {
            context.startActivity(intent)
        }.isSuccess
        if (launched) {
            onFeedback("Opening Firebase Console: $label")
        } else {
            clipboard.setText(AnnotatedString(url))
            onFeedback("Copied $label Console link to clipboard.")
        }
    }
}

@Composable
fun FirebaseConsoleCard(
    modifier: Modifier = Modifier,
    onFeedback: (String) -> Unit = {}
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val projectId = remember { FirebaseConsoleLinks.resolveProjectId() }
    val databaseId = remember(context) { FirebaseConsoleLinks.resolveDatabaseId(context) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.large,
        modifier = modifier
            .fillMaxWidth()
            .testTag("firebase_console_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudQueue,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Firebase Console & Auth Control",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Manage Admin Login ID, Passwords, Sign-In Providers & Firestore",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "OWNER ADMIN LOGIN ID",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = FirebaseConsoleLinks.OWNER_ADMIN_EMAIL,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(FirebaseConsoleLinks.OWNER_ADMIN_EMAIL))
                                onFeedback("Copied Owner Admin ID: ${FirebaseConsoleLinks.OWNER_ADMIN_EMAIL}")
                            },
                            modifier = Modifier.testTag("copy_owner_email_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy owner admin email"
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FIREBASE PROJECT ID",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = projectId,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "DB: $databaseId",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(projectId))
                                onFeedback("Copied Firebase Project ID: $projectId")
                            },
                            modifier = Modifier.testTag("copy_project_id_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Firebase project ID"
                            )
                        }
                    }

                    Text(
                        text = "Tip: Google Sign-In is provisioned and active for 1-tap authentication. Use the Firebase Console links below to inspect Auth users, review Sign-In providers, or browse Firestore data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = {
                    FirebaseConsoleLinks.openUrlOrCopy(
                        context = context,
                        clipboard = clipboard,
                        url = FirebaseConsoleLinks.authUsersUrl(projectId),
                        label = "Auth Users (Login ID & Password)",
                        onFeedback = onFeedback
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("firebase_console_users_button")
            ) {
                Icon(Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    text = "Open Auth Users (Login ID & Password)",
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        FirebaseConsoleLinks.openUrlOrCopy(
                            context = context,
                            clipboard = clipboard,
                            url = FirebaseConsoleLinks.authProvidersUrl(projectId),
                            label = "Sign-In Providers",
                            onFeedback = onFeedback
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("firebase_console_providers_button")
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Providers", modifier = Modifier.padding(start = 6.dp))
                }

                OutlinedButton(
                    onClick = {
                        FirebaseConsoleLinks.openUrlOrCopy(
                            context = context,
                            clipboard = clipboard,
                            url = FirebaseConsoleLinks.firestoreDataUrl(projectId, databaseId),
                            label = "Firestore Data",
                            onFeedback = onFeedback
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("firebase_console_firestore_button")
                ) {
                    Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Firestore", modifier = Modifier.padding(start = 6.dp))
                }
            }

            OutlinedButton(
                onClick = {
                    FirebaseConsoleLinks.openUrlOrCopy(
                        context = context,
                        clipboard = clipboard,
                        url = FirebaseConsoleLinks.projectOverviewUrl(projectId),
                        label = "Firebase Overview",
                        onFeedback = onFeedback
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("firebase_console_overview_button")
            ) {
                Text("Open Full Firebase Console Project")
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(16.dp)
                )
            }
        }
    }
}
