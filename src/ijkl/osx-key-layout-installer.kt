package ijkl

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.Application
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import java.io.FileOutputStream

private val ijklBundleName = "ijkl-keys.bundle"
private val systemPathToBundle = "/Library/Keyboard Layouts/$ijklBundleName"
private val userPathToBundle = "${System.getProperty("user.home")}/Library/Keyboard Layouts/$ijklBundleName"

fun initOsxKeyLayoutInstaller(application: Application) {
    if (!SystemInfo.isMac || userPathToBundle.dirExists() || systemPathToBundle.dirExists()) return

    application.invokeLater {
        val listener = NotificationListener { notification, _ ->
            try {
                copyKeyLayoutTo(userPathToBundle)
                notification.expire()
                TODO("notification")
            } catch(e: Exception) {
                TODO()
            }
        }

        val groupDisplayId = "IJKL Shortcuts"
        val title = ""
        val message =
            "Because of JDK limitations alt+ijkl shortcuts need keyboard input source with no dead key mapping for alt+[letter] shortcuts. " +
            "<a href=''>Click here</a> to install bundle with 'U.S. - IJKL' and 'British - IJKL' input sources. " +
            "You will need to add it manually in OSX 'System Preferences -> Keyboard -> Input Sources'. " +
            "See <a href=''>plugin readme</a> for more details."
        application.messageBus
            .syncPublisher(Notifications.TOPIC)
            .notify(Notification(groupDisplayId, title, message, NotificationType.INFORMATION, listener))
    }
}

private fun String.dirExists() = File(this).let { it.exists() && it.isDirectory }

fun copyKeyLayoutTo(dir: String) {
    // List directories and files manually because there seems to be no easy way to list files/dirs in classloader resources.
    FileUtil.createDirectory(File("$dir/Contents/Resources/English.lproj"))
    val files = listOf(
        "Contents/Info.plist",
        "Contents/version.plist",
        "Contents/Resources/British - IJKL.icns",
        "Contents/Resources/British - IJKL.keylayout",
        "Contents/Resources/U.S. - IJKL.icns",
        "Contents/Resources/U.S. - IJKL.keylayout",
        "Contents/Resources/English.lproj/InfoPlist.strings"
    )
    files.forEach { fileName ->
        FileUtil.copy(
            resourceInputStream("$ijklBundleName/$fileName"),
            FileOutputStream("$dir/$fileName")
        )
    }
}
