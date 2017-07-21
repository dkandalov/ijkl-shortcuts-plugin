package ijkl

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationListener.URL_OPENING_LISTENER
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.Application
import java.io.File
import java.io.InputStream

fun resourceInputStream(fileName: String): InputStream =
    if (File(fileName).exists()) File(fileName).inputStream()
    else AppComponent::class.java.classLoader.getResource(fileName).openStream()

fun Application.showNotification(message: String, listener: NotificationListener = URL_OPENING_LISTENER) {
    val title = "IJKL Shortcuts plugin"
    val groupDisplayId = "IJKL Shortcuts"
    messageBus
        .syncPublisher(Notifications.TOPIC)
        .notify(Notification(groupDisplayId, title, message, INFORMATION, listener))
}

fun ActionManager.actionText(actionId: String) =
    getAction(actionId)?.templatePresentation?.text ?: actionId