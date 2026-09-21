package com.pekaboo.opensource.toolbar.ui;

import org.junit.Test;

import java.awt.Image;
import java.awt.image.BufferedImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Pure-logic tests for icon source detection and image scaling.
 */
public class CommandIconManagerTest {

    @Test
    public void emojiAndPlainTextAreNotImageSources() {
        assertFalse(CommandIconManager.isImageSource(null));
        assertFalse(CommandIconManager.isImageSource(""));
        assertFalse(CommandIconManager.isImageSource("💻"));
        assertFalse(CommandIconManager.isImageSource("🚀 deploy"));
        assertFalse(CommandIconManager.isImageSource("hello world"));
    }

    @Test
    public void urlsAreImageSources() {
        assertTrue(CommandIconManager.isImageSource("http://example.com/icon.png"));
        assertTrue(CommandIconManager.isImageSource("https://example.com/icon.svg?v=2"));
        assertTrue(CommandIconManager.isImageSource("file:///Users/me/icon.svg"));
    }

    @Test
    public void dataUrisAndInlineSvgAreImageSources() {
        assertTrue(CommandIconManager.isImageSource("data:image/svg+xml;base64,PHN2Zy8+"));
        assertTrue(CommandIconManager.isImageSource("data:image/png;base64,iVBOR..."));
        assertTrue(CommandIconManager.isImageSource("<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>"));
        assertTrue(CommandIconManager.isImageSource("  <!-- logo -->\n<svg>...</svg>"));
    }

    @Test
    public void fitScalesToTargetBoxPreservingAspect() {
        // 100x50 -> fit into 16 => scale 0.16 => 16x8
        BufferedImage wide = new BufferedImage(100, 50, BufferedImage.TYPE_INT_ARGB);
        Image fitted = CommandIconManager.fit(wide, 16);
        assertNotNull(fitted);
        assertEquals(16, fitted.getWidth(null));
        assertEquals(8, fitted.getHeight(null));

        // already target size => unchanged
        BufferedImage square = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        assertEquals(square, CommandIconManager.fit(square, 16));
    }
}
