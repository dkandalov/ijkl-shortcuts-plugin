package ijkl

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.io.InputStream

class AppComponent: ApplicationComponent {
    override fun initComponent() {
        initOsxKeyLayoutInstaller(
            ApplicationManager.getApplication()
        )
        initCurrentKeymapModifier(
            resourceInputStream("ijkl-keymap.xml"),
            ApplicationManager.getApplication(),
            Logger.getInstance(javaClass.canonicalName),
            System.getProperty("ijkl.log.shortcut.conflicts").toBoolean()
        )
    }
}

fun resourceInputStream(fileName: String): InputStream =
    if (File(fileName).exists()) File(fileName).inputStream()
    else AppComponent::class.java.classLoader.getResource(fileName).openStream()
