import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    // Handles Maven Central upload via the Sonatype Central Portal + GPG signing.
    id("com.vanniktech.maven.publish")
}

group = "mn.wire"
version = "1.0.0"
description = "Wire hosted-checkout SDK for Android"

android {
    namespace = "mn.wire.checkout"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // The published "release" variant + sources/javadoc jars are configured by
    // the vanniktech plugin's AndroidSingleVariantLibrary below; declaring an
    // AGP `publishing { singleVariant("release") }` here as well makes Gradle
    // fail with "Using singleVariant publishing DSL multiple times".

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    jvmToolchain(17)
    // Public API stability: require explicit visibility modifiers on the published surface.
    explicitApi()
}

dependencies {
    implementation("androidx.browser:browser:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // org.json is provided by the Android platform at runtime; only needed to compile.
    compileOnly("org.json:json:20240303")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.24")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    // org.json is NOT on the plain-JVM unit-test classpath, so add it for tests.
    testImplementation("org.json:json:20240303")
}

tasks.withType<Test> {
    testLogging {
        events("passed", "skipped", "failed")
    }
}

mavenPublishing {
    configure(AndroidSingleVariantLibrary(variant = "release", sourcesJar = true, publishJavadocJar = true))

    // Publishes to Maven Central through the Sonatype Central Portal.
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)

    // GPG signing. The plugin reads signingInMemoryKey / signingInMemoryKeyPassword from
    // Gradle properties or the ORG_GRADLE_PROJECT_* env vars set in CI; only signs when present.
    if (project.findProperty("signingInMemoryKey") != null) {
        signAllPublications()
    }

    coordinates("mn.wire", "wire-checkout-android", version.toString())

    pom {
        name.set("wire-checkout-android")
        description.set("Wire hosted-checkout SDK for Android")
        url.set("https://github.com/buildry-wire/wire-checkout-android")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("buildry-wire")
                name.set("Wire")
                url.set("https://wire.mn")
            }
        }
        scm {
            connection.set("scm:git:https://github.com/buildry-wire/wire-checkout-android.git")
            developerConnection.set("scm:git:ssh://git@github.com/buildry-wire/wire-checkout-android.git")
            url.set("https://github.com/buildry-wire/wire-checkout-android")
        }
    }
}
