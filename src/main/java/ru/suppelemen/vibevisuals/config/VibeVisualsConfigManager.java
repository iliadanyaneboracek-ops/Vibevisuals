package ru.suppelemen.vibevisuals.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import ru.suppelemen.vibevisuals.SupsVisualsClient;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class VibeVisualsConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve(SupsVisualsClient.MOD_ID + ".json");

    private static VibeVisualsConfig config = new VibeVisualsConfig();

    private VibeVisualsConfigManager() {
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                VibeVisualsConfig loaded = GSON.fromJson(reader, VibeVisualsConfig.class);
                config = loaded != null ? loaded : new VibeVisualsConfig();
            } catch (IOException | RuntimeException exception) {
                System.err.println("[vibevisuals] Failed to load config, using defaults");
                exception.printStackTrace();
                config = new VibeVisualsConfig();
            }
        }

        config.validate();
        save();
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
}
