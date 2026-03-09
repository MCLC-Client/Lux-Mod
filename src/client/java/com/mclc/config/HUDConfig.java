package com.lux.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lux.LuxModClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HUDConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(),
            "lux_hud.json");

    private Map<String, ModuleData> modules = new HashMap<>();

    // Singleton instance
    private static HUDConfig instance;

    public static HUDConfig getInstance() {
        if (instance == null) {
            instance = new HUDConfig();
            instance.load();
        }
        return instance;
    }

    private HUDConfig() {
        modules.put("FPS Counter", new ModuleData("FPS Counter", true, 10, 10));
        modules.put("Fullbright", new ModuleData("Fullbright", false, 10, 50));
        // Add some dummy modules to demonstrate dragging & scrolling
        modules.put("Keystrokes", new ModuleData("Keystrokes", true, 10, 90));
        modules.put("Armor Status", new ModuleData("Armor Status", false, 10, 130));
        modules.put("Potion Effects", new ModuleData("Potion Effects", true, 10, 170));
        modules.put("CPS", new ModuleData("CPS", false, 10, 210));
        modules.put("Ping", new ModuleData("Ping", false, 10, 250));
    }

    public void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                HUDConfig loaded = GSON.fromJson(reader, HUDConfig.class);
                if (loaded != null && loaded.modules != null) {
                    for (Map.Entry<String, ModuleData> entry : loaded.modules.entrySet()) {
                        if (this.modules.containsKey(entry.getKey())) {
                            ModuleData defaultData = this.modules.get(entry.getKey());
                            ModuleData loadedData = entry.getValue();

                            // Restore transient variables and update values
                            loadedData.defaultX = defaultData.defaultX;
                            loadedData.defaultY = defaultData.defaultY;
                            loadedData.defaultMode = defaultData.defaultMode;
                            this.modules.put(entry.getKey(), loadedData);
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Failed to load HUD config: " + e.getMessage());
            }
        } else {
            save(); // Create default config if it doesn't exist
        }
    }

    public void save() {
        try {
            if (!CONFIG_FILE.getParentFile().exists()) {
                CONFIG_FILE.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save HUD config: " + e.getMessage());
        }
    }

    public Map<String, ModuleData> getModules() {
        return modules;
    }

    public ModuleData getModule(String name) {
        return modules.get(name);
    }

    public void resetDefaults() {
        for (ModuleData mod : modules.values()) {
            mod.x = mod.defaultX;
            mod.y = mod.defaultY;
            mod.mode = mod.defaultMode;
        }
        save();
    }

    public static class ModuleData {
        public String name;
        public boolean enabled;
        public int x;
        public int y;
        public int mode = 0; // 0 = Standard/Detailed, 1 = Minimal/Compact, etc.
        public boolean showPercentage = false; // Armor Status specific: show % or exact
        public transient int defaultX;
        public transient int defaultY;
        public transient int defaultMode = 0;

        public ModuleData(String name, boolean enabled, int x, int y) {
            this.name = name;
            this.enabled = enabled;
            this.x = x;
            this.y = y;
            this.defaultX = x;
            this.defaultY = y;
            this.mode = 0;
            this.defaultMode = 0;
        }
    }
}
