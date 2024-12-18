package ijkl

import com.intellij.notification.*
import com.intellij.notification.NotificationListener.URL_OPENING_LISTENER
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.Application
import java.io.File
import java.io.InputStream

const val groupDisplayId = "IJKL Shortcuts"
private const val notificationTitle = "IJKL Shortcuts plugin"

fun resourceInputStream(fileName: String): InputStream {
    val file = File(fileName)
    return if (file.exists()) file.inputStream()
    else Main::class.java.classLoader.getResource(fileName)?.openStream() ?: error("Couldn't find '$fileName'")
}

fun Application.showNotification(message: String, listener: NotificationListener) {
    @Suppress("DEPRECATION") // Use deprecated setListener() because there isn't alternative to URL_OPENING_LISTENER
    messageBus
        .syncPublisher(Notifications.TOPIC)
        .notify(Notification(groupDisplayId, notificationTitle, message, INFORMATION)
            .setListener(listener))
}

fun ActionManager.actionText(actionId: String) =
    getAction(actionId)?.templatePresentation?.text ?: actionId
