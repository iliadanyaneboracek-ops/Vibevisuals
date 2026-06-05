package ru.suppelemen.vibevisuals.config;

import net.fabricmc.loader.api.FabricLoader;
import ru.suppelemen.vibevisuals.VibeVisualsClient;
import ru.suppelemen.vibevisuals.core.hud.HudManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Profile sharing on top of {@link VibeVisualsConfigManager}.
 *
 * <p>On-disk layout (under {@code .minecraft/config/}):
 * <pre>
 *   vibevisuals.json                ← the active config (as before; unchanged)
 *   vibevisuals-profiles/
 *     default.json                  ← seeded from vibevisuals.json on first run
 *     pvp.json
 *     imported-{8-char-hash}.json   ← profiles brought in via share code
 *   vibevisuals-active.txt          ← name of the profile currently mirrored
 *                                     into vibevisuals.json
 * </pre>
 *
 * <p>Share codes are {@code VV1:} + base64(gzip(json)). Pasting a code creates
 * a new profile next to your existing ones and makes it active; the previous
 * profile stays on disk so you can switch back without losing it.
 */
public final class ConfigShareManager {

    private static final String SHARE_PREFIX = "VV1:";
    private static final String DEFAULT_NAME = "default";
    private static final int MAX_NAME = 48;

    private static final Path BASE_DIR = FabricLoader.getInstance().getConfigDir();
    private static final Path PROFILES_DIR = BASE_DIR.resolve(VibeVisualsClient.MOD_ID + "-profiles");
    private static final Path ACTIVE_FILE = BASE_DIR.resolve(VibeVisualsClient.MOD_ID + "-active.txt");
    private static final Path LIVE_FILE = VibeVisualsConfigManager.getConfigPath();

    private ConfigShareManager() {}

    /** Seed the profiles directory on first run + ensure {@code active.txt} exists. */
    public static synchronized void init() {
        try {
            Files.createDirectories(PROFILES_DIR);

            boolean hasAnyProfile;
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(PROFILES_DIR, "*.json")) {
                hasAnyProfile = ds.iterator().hasNext();
            }

            if (!hasAnyProfile) {
                // Copy current live config as the first ("default") profile, or
                // bootstrap an empty default if no config file exists yet.
                Path defaultProfile = profilePath(DEFAULT_NAME);
                if (Files.exists(LIVE_FILE)) {
                    Files.copy(LIVE_FILE, defaultProfile, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    // Will be filled when VibeVisualsConfigManager.save() next runs.
                    Files.writeString(defaultProfile, "{}");
                }
                writeActive(DEFAULT_NAME);
            } else if (!Files.exists(ACTIVE_FILE)) {
                // Profiles exist but no active marker — pick first alphabetically.
                List<String> names = listProfiles();
                writeActive(names.isEmpty() ? DEFAULT_NAME : names.get(0));
            }
        } catch (IOException e) {
            System.err.println("[vibevisuals] ConfigShareManager init failed");
            e.printStackTrace();
        }
    }

    // ---------- Export / import ----------

    /** Snapshot the currently-active config as a copy-pasteable share code. */
    public static String exportCurrent() {
        try {
            if (!Files.exists(LIVE_FILE)) return SHARE_PREFIX;
            byte[] raw = Files.readAllBytes(LIVE_FILE);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (GZIPOutputStream gz = new GZIPOutputStream(bos)) {
                gz.write(raw);
            }
            return SHARE_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bos.toByteArray());
        } catch (IOException e) {
            System.err.println("[vibevisuals] exportCurrent failed");
            e.printStackTrace();
            return SHARE_PREFIX;
        }
    }

    /**
     * Decode a share code, save it as a new profile next to the existing ones,
     * make it active and reload the live config.
     *
     * @return generated profile name on success, {@code null} on failure.
     */
    public static String importCode(String code) {
        if (code == null) return null;
        String trimmed = code.trim();
        if (!trimmed.startsWith(SHARE_PREFIX)) return null;
        String body = trimmed.substring(SHARE_PREFIX.length()).trim();
        if (body.isEmpty()) return null;

        try {
            byte[] gz = Base64.getUrlDecoder().decode(body);
            byte[] json;
            try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(gz));
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                gis.transferTo(out);
                json = out.toByteArray();
            }

            // Quick sanity check: must look like a JSON object.
            String jsonStr = new String(json, StandardCharsets.UTF_8).trim();
            if (!jsonStr.startsWith("{")) return null;

            String name = "imported-" + shortHash(body);
            Path target = profilePath(name);
            Files.createDirectories(PROFILES_DIR);
            Files.write(target, json);

            switchToProfile(name);
            return name;
        } catch (IllegalArgumentException | IOException e) {
            System.err.println("[vibevisuals] importCode rejected: " + e.getMessage());
            return null;
        }
    }

    // ---------- Profile management ----------

    public static synchronized List<String> listProfiles() {
        try {
            if (!Files.exists(PROFILES_DIR)) return Collections.emptyList();
            List<String> names = new ArrayList<>();
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(PROFILES_DIR, "*.json")) {
                for (Path p : ds) {
                    String fn = p.getFileName().toString();
                    names.add(fn.substring(0, fn.length() - ".json".length()));
                }
            }
            Collections.sort(names);
            return names;
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public static String activeProfile() {
        try {
            if (!Files.exists(ACTIVE_FILE)) return null;
            return Files.readString(ACTIVE_FILE).trim();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Persist the current live config back into its profile file, then load
     * {@code name}'s JSON into the live config and reload everything that
     * depends on it.
     */
    public static synchronized boolean switchToProfile(String name) {
        if (name == null || name.isEmpty()) return false;
        try {
            // Save outgoing changes into the previously-active profile so the
            // user doesn't lose unsaved tweaks on the current one.
            String prev = activeProfile();
            if (prev != null && !prev.isEmpty() && Files.exists(LIVE_FILE)) {
                Files.copy(LIVE_FILE, profilePath(prev), StandardCopyOption.REPLACE_EXISTING);
            }

            Path src = profilePath(name);
            if (!Files.exists(src)) return false;

            Files.copy(src, LIVE_FILE, StandardCopyOption.REPLACE_EXISTING);
            writeActive(name);

            // Live-reload via the normal manager so all dependent systems pick
            // up the new values.
            VibeVisualsConfigManager.load();
            HudManager.reload();
            return true;
        } catch (IOException e) {
            System.err.println("[vibevisuals] switchToProfile failed");
            e.printStackTrace();
            return false;
        }
    }

    /** Save the current live config as a new profile with the given name. */
    public static synchronized boolean saveCurrentAs(String name) {
        String sanitised = sanitise(name);
        if (sanitised.isEmpty()) return false;
        try {
            VibeVisualsConfigManager.save();
            Files.createDirectories(PROFILES_DIR);
            Files.copy(LIVE_FILE, profilePath(sanitised), StandardCopyOption.REPLACE_EXISTING);
            writeActive(sanitised);
            return true;
        } catch (IOException e) {
            System.err.println("[vibevisuals] saveCurrentAs failed");
            return false;
        }
    }

    public static synchronized boolean deleteProfile(String name) {
        if (name == null || name.isEmpty()) return false;
        // Never let the user delete the active profile out from under themselves.
        if (name.equals(activeProfile())) return false;
        try {
            Files.deleteIfExists(profilePath(name));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // ---------- Helpers ----------

    private static Path profilePath(String name) {
        return PROFILES_DIR.resolve(sanitise(name) + ".json");
    }

    private static void writeActive(String name) throws IOException {
        Files.writeString(ACTIVE_FILE, sanitise(name));
    }

    /** Strip anything that isn't safe in a filename; clamp length. */
    public static String sanitise(String name) {
        if (name == null) return "";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < name.length() && b.length() < MAX_NAME; i++) {
            char c = name.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
                b.append(Character.toLowerCase(c));
            } else if (c == ' ' || c == '.') {
                b.append('-');
            }
        }
        return b.toString();
    }

    private static String shortHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] h = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < 4; i++) s.append(String.format(Locale.ROOT, "%02x", h[i]));
            return s.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    /** For UI display — share code can be long; abbreviate the middle. */
    public static String abbreviate(String code, int maxLen) {
        if (code == null || code.length() <= maxLen) return code;
        int keep = (maxLen - 1) / 2;
        return code.substring(0, keep) + "…" + code.substring(code.length() - keep);
    }
}
