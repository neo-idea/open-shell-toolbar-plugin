package com.pekaboo.opensource.toolbar.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the JSON import/export round-trip used by ToolbarConfigService.
 * A broken round-trip would silently lose user data (titles, commands,
 * icons, enabled/openInTerminal flags).
 */
public class ShellCommandConfigTest {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Test
    public void gsonRoundTripPreservesAllFields() {
        List<ShellCommandConfig> original = new ArrayList<>();
        original.add(new ShellCommandConfig("id-1", "Open Terminal", "open -na Terminal",
                "$ProjectFileDir$", "⌘", true, false));
        original.add(new ShellCommandConfig("id-2", "Dev Server", "npm run dev",
                "{{rootPath}}", "🚀", false, true));
        original.add(new ShellCommandConfig(null, "Emoji \uD83D\uDCBB & <quotes> \" '",
                "echo 'hello'", null, null, true, true));

        String json = GSON.toJson(original);
        Type listType = new TypeToken<List<ShellCommandConfig>>() {}.getType();
        List<ShellCommandConfig> parsed = GSON.fromJson(json, listType);

        assertEquals(original, parsed);
    }

    @Test
    public void defaultsAreSaneForNewConfigs() {
        ShellCommandConfig config = new ShellCommandConfig();

        assertNotNull(config.getId());
        assertTrue("new commands must be enabled by default", config.isEnabled());
        assertTrue("new commands must open in the built-in terminal by default",
                config.isOpenInTerminal());
        assertNotNull("icon must fall back to a default emoji", config.getIcon());
    }
}
