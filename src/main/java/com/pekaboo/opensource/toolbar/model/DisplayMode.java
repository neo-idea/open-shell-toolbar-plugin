package com.pekaboo.opensource.toolbar.model;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * How the shell commands are displayed in the main toolbar.
 */
public enum DisplayMode {

    /** Single toolbar icon; clicking it opens a dropdown with the commands. */
    POPUP("Popup — single icon with dropdown"),

    /** One individual toolbar button per enabled command. */
    FLAT("Flat — one button per command");

    private final String displayName;

    DisplayMode(@NotNull @Nls String displayName) {
        this.displayName = displayName;
    }

    public @NotNull @Nls String getDisplayName() {
        return displayName;
    }

    /** Parses a persisted mode name, falling back to {@link #POPUP}. */
    public static @NotNull DisplayMode parse(@org.jetbrains.annotations.Nullable String name) {
        if (name != null) {
            for (DisplayMode mode : values()) {
                if (mode.name().equals(name)) {
                    return mode;
                }
            }
        }
        return POPUP;
    }
}
