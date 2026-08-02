import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
}

group = "me.rerere.rikkahub"
version = "2.4.3"

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
    implementation(libs.jna.platform)
    implementation("io.github.darkokoa:pangu-jvm:0.2.0")
    implementation("org.scilab.forge:jlatexmath:1.0.7")
    implementation("org.apache.pdfbox:pdfbox:3.0.3")
    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "me.rerere.rikkahub.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.Exe, TargetFormat.Msi)
            packageName = "rikkahub"
            packageVersion = project.version.toString()
            description = "RikkaHub desktop LLM chat client"
            vendor = "RikkaHub"
            linux {
                iconFile.set(project.file("src/main/resources/icon.png"))
                menuGroup = "Network"
            }
            windows {
                shortcut = true
                menu = true
                menuGroup = "RikkaHub"
                upgradeUuid = "4d59140b-31df-49e8-a119-281b19b07cf6"
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
