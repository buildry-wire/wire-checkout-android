// Root build script. Plugins are declared (apply false) here and applied in the module.
plugins {
    id("com.android.library") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.vanniktech.maven.publish") version "0.30.0" apply false
}
