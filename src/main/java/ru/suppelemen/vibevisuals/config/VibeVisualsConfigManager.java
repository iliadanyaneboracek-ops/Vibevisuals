package ru.suppelemen.vibevisuals.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import ru.suppelemen.vibevisuals.VibeVisualsClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

public final class VibeVisualsConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve(VibeVisualsClient.MOD_ID + ".json");

    private static VibeVisualsConfig config = new VibeVisualsConfig();

    private VibeVisualsConfigManager() {
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                VibeVisualsConfig loaded = new VibeVisualsConfig();
                if (root != null) {
                    applyJsonObject(loaded, root, "config");
                }
                config = loaded;
            } catch (IOException | RuntimeException exception) {
                System.err.println("[vibevisuals] Failed to parse config, using runtime defaults and leaving file unchanged");
                exception.printStackTrace();
                config = new VibeVisualsConfig();
                config.validate();
                return;
            }
        } else {
            // First-run baseline: load the shipped default-config.json so new
            // installs get the curated VibeVisuals look out of the box instead
            // of stock Java-field defaults.
            try (InputStream in = VibeVisualsConfigManager.class.getResourceAsStream(
                    "/assets/" + VibeVisualsClient.MOD_ID + "/default-config.json")) {
                if (in != null) {
                    try (Reader reader = new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)) {
                        JsonObject root = GSON.fromJson(reader, JsonObject.class);
                        VibeVisualsConfig loaded = new VibeVisualsConfig();
                        if (root != null) {
                            applyJsonObject(loaded, root, "config");
                        }
                        config = loaded;
                        System.out.println("[vibevisuals] First run — seeded config from default-config.json");
                    }
                }
            } catch (Exception ex) {
                System.err.println("[vibevisuals] Could not load shipped default-config.json — using code defaults");
            }
        }

        config.validate();
        save();
    }

    private static void applyJsonObject(Object target, JsonObject json, String path) {
        for (Field field : target.getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers()) || !json.has(field.getName())) {
                continue;
            }

            JsonElement element = json.get(field.getName());
            if (element == null || element.isJsonNull()) {
                continue;
            }

            try {
                Object currentValue = field.get(target);
                if (currentValue != null && element.isJsonObject() && isConfigSection(field.getType())) {
                    applyJsonObject(currentValue, element.getAsJsonObject(), path + "." + field.getName());
                } else {
                    field.set(target, GSON.fromJson(element, field.getGenericType()));
                }
            } catch (IllegalAccessException | RuntimeException exception) {
                System.err.println("[vibevisuals] Invalid config value ignored: " + path + "." + field.getName());
                exception.printStackTrace();
            }
        }
    }

    private static boolean isConfigSection(Class<?> type) {
        return type.getName().startsWith(VibeVisualsConfig.class.getName() + "$");
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException exception) {
            System.err.println("[vibevisuals] Failed to save config");
            exception.printStackTrace();
        }
    }

    public static VibeVisualsConfig get() {
        return config;
    }

    public static Path getConfigPath() {
        return CONFIG_PATH;
    }
}
