package com.pekaboo.opensource.toolbar.action;

import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Simple icon implementation that renders an emoji character.
 */
public class EmojiIcon implements Icon {

    private final String emoji;
    private final int size;

    /**
     * Creates an emoji icon with default size (16x16).
     *
     * @param emoji The emoji character to render
     */
    public EmojiIcon(@NotNull String emoji) {
        this(emoji, 16);
    }

    /**
     * Creates an emoji icon with specified size.
     *
     * @param emoji The emoji character to render
     * @param size  The icon size in pixels
     */
    public EmojiIcon(@NotNull String emoji, int size) {
        this.emoji = emoji;
        this.size = size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Use a logical font family so the platform falls back to its emoji
        // font (Apple Color Emoji / Segoe UI Emoji) on every OS.
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, size - 2);
        g2d.setFont(font);
        g2d.setColor(JBColor.BLACK);

        // Center the emoji in the icon bounds
        FontMetrics fm = g2d.getFontMetrics();
        int textX = x + (size - fm.stringWidth(emoji)) / 2;
        int textY = y + fm.getAscent() - (size - fm.getHeight()) / 2;

        g2d.drawString(emoji, textX, textY);
        g2d.dispose();
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }
}
