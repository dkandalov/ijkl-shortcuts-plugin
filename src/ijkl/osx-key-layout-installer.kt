package ijkl

import com.intellij.openapi.application.Application
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import java.io.FileOutputStream

fun initOsxKeyLayoutInstaller(
    bundleName: String,
    systemPathToBundle: String,
    userPathToBundle: String,
    application: Application,
    logger: Logger
) {
    if (!SystemInfo.isMac || userPathToBundle.dirExists() || systemPathToBundle.dirExists()) return

    application.invokeLater {
        val message =
            "Because of JDK limitations alt+ijkl shortcuts need keyboard input source with no dead key mapping for alt+[letter] shortcuts. " +
            "<a href=''>Click here</a> to install bundle with 'U.S. - IJKL' and 'British - IJKL' input sources. "
        application.showNotification(message) { notification, _ ->
            try {
                copyKeyLayoutTo(bundleName, userPathToBundle)
                notification.expire()
                application.showNotification(
                    "The bundle with input sources was copied to '$userPathToBundle'. " +
                        "You will need to add it manually in OSX 'System Preferences -> Keyboard -> Input Sources'. " +
                        "See <a href='https://github.com/dkandalov/ijkl-shortcuts-plugin/blob/master/readme.md#how-to-install-osx-input-source'>plugin readme</a> for more details."
                                            )
            } catch (e: Exception) {
                logger.error(e)
            }
        }
    }
}

fun copyKeyLayoutTo(fromResource: String, toDir: String) {
    // List directories and files manually because there seems to be no easy way to list files/dirs in classloader resources.
    FileUtil.createDirectory(File("$toDir/Contents/Resources/en.lproj"))
    listOf(
        "Contents/Info.plist",
        "Contents/version.plist",
        "Contents/Resources/British - IJKL.icns",
        "Contents/Resources/British - IJKL.keylayout",
        "Contents/Resources/U.S. - IJKL.icns",
        "Contents/Resources/U.S. - IJKL.keylayout",
        "Contents/Resources/en.lproj/InfoPlist.strings"
    ).forEach { fileName ->
        FileUtil.copy(
            resourceInputStream("$fromResource/$fileName"),
            FileOutputStream("$toDir/$fileName")
        )
    }
}

private fun String.dirExists() = File(this).let { it.exists() && it.isDirectory }
