import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
}

group = "me.rerere.rikkahub"
version = "2.4.2"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

dependencies {
    implementation(compose.desktop.currentOs)
    @Suppress("DEPRECATION")
    implementation(compose.material3)
    implementation(libs.kotlinx.coroutines.core)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.11.0")
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.modelcontextprotocol.kotlin.sdk)
    implementation(libs.lucide.icons)
    implementation(libs.haze)
    implementation(libs.haze.blur)
    implementation(libs.jetbrains.markdown)
    implementation(libs.pebble)
    implementation("org.scilab.forge:jlatexmath:1.0.7")
    implementation("org.apache.pdfbox:pdfbox:3.0.3")
    testImplementation(kotlin("test"))
}

tasks.processResources {
    from(rootProject.file("docs/icon.png"))
    from(rootProject.file("app/src/main/assets/icons")) {
        into("icons")
    }
}

compose.desktop {
    application {
        mainClass = "me.rerere.rikkahub.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "rikkahub"
            packageVersion = project.version.toString()
            description = "RikkaHub Linux desktop client"
            vendor = "RikkaHub"
            linux {
                iconFile.set(rootProject.file("docs/icon.png"))
                menuGroup = "Network"
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
