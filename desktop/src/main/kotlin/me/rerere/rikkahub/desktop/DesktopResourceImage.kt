package me.rerere.rikkahub.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image
import org.jetbrains.skia.Surface
import org.jetbrains.skia.svg.SVGDOM

@Composable
internal fun rememberDesktopResourcePainter(path: String): Painter? =
    rememberDesktopResourceImage(path)?.let(::BitmapPainter)

@Composable
internal fun rememberDesktopResourceImage(path: String): ImageBitmap? = remember(path) {
    loadDesktopResourceImage(path)
}

internal fun loadDesktopResourceImage(path: String): ImageBitmap? =
    runCatching {
        DesktopResourceImageMarker::class.java.classLoader.getResourceAsStream(path)?.use { input ->
            val bytes = input.readBytes()
            if (path.endsWith(".svg", ignoreCase = true)) {
                bytes.toSvgImageBitmap()
            } else {
                Image.makeFromEncoded(bytes).toComposeImageBitmap()
            }
        }
    }.getOrNull()

private fun ByteArray.toSvgImageBitmap(): ImageBitmap {
    val surface = Surface.makeRasterN32Premul(64, 64)
    try {
        SVGDOM(Data.makeFromBytes(this)).apply {
            setContainerSize(64f, 64f)
            render(surface.canvas)
        }
        return surface.makeImageSnapshot().toComposeImageBitmap()
    } finally {
        surface.close()
    }
}

private object DesktopResourceImageMarker
