package ijkl

import com.intellij.notification.Notification
import com.intellij.notification.NotificationDisplayType.STICKY_BALLOON
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationListener.URL_OPENING_LISTENER
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.notification.Notifications
import com.intellij.notification.NotificationsConfiguration
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.Application
import java.io.File
import java.io.InputStream

private val notificationTitle = "IJKL Shortcuts plugin"
private val groupDisplayId = "IJKL Shortcuts".apply {
    NotificationsConfiguration.getNotificationsConfiguration().register(this, STICKY_BALLOON, true)
}

fun resourceInputStream(fileName: String): InputStream =
    if (File(fileName).exists()) File(fileName).inputStream()
    else AppComponent::class.java.classLoader.getResource(fileName).openStream()

fun Application.showNotification(message: String, listener: NotificationListener = URL_OPENING_LISTENER) {
    messageBus
        .syncPublisher(Notifications.TOPIC)
        .notify(Notification(groupDisplayId, notificationTitle, message, INFORMATION, listener))
}

fun ActionManager.actionText(actionId: String) =
    getAction(actionId)?.templatePresentation?.text ?: actionId