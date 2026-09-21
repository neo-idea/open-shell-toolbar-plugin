package com.pekaboo.opensource.toolbar.ui;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ui.JBUI;
import com.pekaboo.opensource.toolbar.action.EmojiIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves command icons from the user-supplied icon field.
 *
 * <p>Supported inputs:</p>
 * <ul>
 *   <li>emoji / short text — rendered by {@link EmojiIcon} (existing behavior)</li>
 *   <li>image URL — {@code http://}, {@code https://}, {@code file://}</li>
 *   <li>data URI — {@code data:image/svg+xml;base64,...} (or plain base64 PNG/JPEG)</li>
 *   <li>inline SVG — any string containing an {@code <svg ...>} document</li>
 * </ul>
 *
 * <p>Images are downloaded on a pooled thread and cached; callers on the EDT
 * get {@code null} while loading (and should fall back to a default icon).
 * Once an icon is ready, a config-changed event re-renders the toolbar,
 * dropdown and status-bar popup. Rasterization uses the platform SVG
 * renderer ({@code com.intellij.ui.svg.SvgKt.renderSvg}) via reflection so
 * the plugin keeps compiling/running across IDE versions; if it is missing,
 * the icon degrades to the default emoji instead of failing.</p>
 */
public final class CommandIconManager {

    private static final Logger LOG = Logger.getInstance(CommandIconManager.class);

    /** Logical icon size in points; scaled to the monitor by {@link JBUI}. */
    private static final int TARGET_SIZE = 16;
    private static final int MAX_BYTES = 2 * 1024 * 1024; // 2 MB safety cap

    private static final Map<String, Icon> ICONS = new ConcurrentHashMap<>();
    private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();
    private static final Set<String> IN_FLIGHT = ConcurrentHashMap.newKeySet();

    private CommandIconManager() {
    }

    /**
     * @return true when the raw icon field references an image (URL, data URI
     * or inline SVG) rather than an emoji.
     */
    public static boolean isImageSource(@Nullable String raw) {
        return raw != null && (raw.contains("<svg")
                || raw.startsWith("http://")
                || raw.startsWith("https://")
                || raw.startsWith("file://")
                || raw.startsWith("data:image/"));
    }

    /**
     * EDT-safe lookup: returns the cached icon for the given source, or
     * {@code null} while it is still loading (a background load is kicked
     * off). Never blocks, never fetches on the calling thread.
     */
    public static @Nullable Icon resolve(@Nullable String raw) {
        if (!isImageSource(raw)) {
            return null;
        }
        Icon cached = ICONS.get(raw);
        if (cached != null) {
            return cached;
        }
        if (!FAILED.contains(raw) && IN_FLIGHT.add(raw)) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> loadInBackground(raw));
        }
        return null;
    }

    private static void loadInBackground(@NotNull String raw) {
        Icon icon = null;
        try {
            icon = doLoad(raw);
        } catch (Throwable t) {
            LOG.warn("Failed to load command icon: " + shorten(raw), t);
        }
        if (icon != null) {
            ICONS.put(raw, icon);
        } else {
            FAILED.add(raw);
        }
        IN_FLIGHT.remove(raw);
        // Re-render every surface (toolbar, dropdown, status-bar popup) so the
        // freshly cached icon becomes visible.
        Application application = ApplicationManager.getApplication();
        if (application != null && !application.isDisposed()) {
            com.pekaboo.opensource.toolbar.service.ToolbarConfigService.fireConfigsChangedPublic();
        }
    }

    @Nullable
    private static Icon doLoad(@NotNull String raw) throws Exception {
        byte[] bytes = readBytes(raw);
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES) {
            return null;
        }
        int target = Math.max(16, JBUI.scale(TARGET_SIZE));
        if (looksSvg(raw, bytes)) {
            Image image = renderSvg(bytes);
            if (image != null) {
                return new ImageIcon(fit(image, target));
            }
        }
        BufferedImage raster = ImageIO.read(new ByteArrayInputStream(bytes));
        return raster != null ? new ImageIcon(fit(raster, target)) : null;
    }

    private static byte @Nullable [] readBytes(@NotNull String raw) throws Exception {
        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            // Plain URLConnection: HttpRequests lives in a product jar that is
            // not on the plugin compile classpath.
            URLConnection connection = new URL(raw).openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            try (InputStream in = connection.getInputStream()) {
                return in.readAllBytes();
            }
        }
        if (raw.startsWith("file://")) {
            return Files.readAllBytes(Paths.get(new java.net.URI(raw.trim())));
        }
        if (raw.startsWith("data:image/")) {
            int comma = raw.indexOf(',');
            if (comma < 0) return null;
            String data = raw.substring(comma + 1).trim();
            if (raw.substring(0, comma).contains("base64")) {
                return Base64.getDecoder().decode(data);
            }
            return java.net.URLDecoder.decode(data, StandardCharsets.UTF_8)
                    .getBytes(StandardCharsets.UTF_8);
        }
        if (raw.contains("<svg")) {
            return raw.getBytes(StandardCharsets.UTF_8);
        }
        return null;
    }

    private static boolean looksSvg(@NotNull String raw, byte @NotNull [] bytes) {
        if (raw.startsWith("data:image/svg") || raw.contains("<svg")) {
            return true;
        }
        String path = raw.split("\\?", 2)[0].toLowerCase();
        if (path.endsWith(".svg")) {
            return true;
        }
        String head = new String(bytes, 0, Math.min(bytes.length, 512), StandardCharsets.UTF_8).trim();
        return head.startsWith("<svg") || head.startsWith("<?xml") && head.contains("<svg");
    }

    /** Renders SVG bytes via the platform renderer (reflection-tolerant). */
    private static @Nullable Image renderSvg(byte @NotNull [] bytes) {
        try {
            Class<?> svgKt = Class.forName("com.intellij.ui.svg.SvgKt");
            Method render = svgKt.getMethod("renderSvg", byte[].class, float.class);
            Object result = render.invoke(null, bytes, 1.0f);
            return result instanceof Image ? (Image) result : null;
        } catch (Throwable t) {
            LOG.warn("Platform SVG renderer unavailable; falling back to default icon", t);
            return null;
        }
    }

    /** Scales an image to fit a target square, preserving aspect ratio. */
    static @NotNull Image fit(@NotNull Image src, int target) {
        int w = src.getWidth(null);
        int h = src.getHeight(null);
        if (w <= 0 || h <= 0) {
            return src;
        }
        float scale = Math.min((float) target / w, (float) target / h);
        if (Math.abs(scale - 1f) < 0.01f) {
            return src;
        }
        int nw = Math.max(1, Math.round(w * scale));
        int nh = Math.max(1, Math.round(h * scale));
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(src, 0, 0, nw, nh, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    private static String shorten(@NotNull String raw) {
        return raw.length() > 120 ? raw.substring(0, 120) + "..." : raw;
    }
}
