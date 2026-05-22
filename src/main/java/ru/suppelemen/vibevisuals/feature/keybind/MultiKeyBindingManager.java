package ru.suppelemen.vibevisuals.feature.keybind;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

import java.util.HashSet;
import java.util.Set;

public final class MultiKeyBindingManager {
    private static final Set<String> PRESSED = new HashSet<>();
    private static PendingChord pendingChord;

    private MultiKeyBindingManager() {
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.getWindow() == null) {
            return;
        }

        if (client.currentScreen != null) {
            PRESSED.clear();
            pendingChord = null;
            return;
        }

        VibeVisualsConfig config = VibeVisualsConfigManager.get();
        if (config.multiKeyBindings == null || !config.multiKeyBindings.enabled) {
            PRESSED.clear();
            pendingChord = null;
            return;
        }

        Window window = client.getWindow();
        boolean ctrl = isPressedRaw(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || isPressedRaw(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
        boolean shift = isPressedRaw(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || isPressedRaw(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
        boolean alt = isPressedRaw(window, GLFW.GLFW_KEY_LEFT_ALT)
                || isPressedRaw(window, GLFW.GLFW_KEY_RIGHT_ALT);

        if (pendingChord != null && System.currentTimeMillis() > pendingChord.deadlineMs) {
            pendingChord = null;
        }

        for (MultiKeyBinding binding : config.multiKeyBindings.bindings) {
            if (!binding.enabled || !binding.primary.isAssigned() || binding.actions.isEmpty()) {
                continue;
            }

            if (binding.hasChord() && pendingChord != null && pendingChord.binding == binding) {
                String chordKey = strokeKey(binding.chord);
                boolean nowPressed = binding.chord.isPressed(client);
                boolean wasPressed = PRESSED.contains(chordKey);

                if (nowPressed && !wasPressed) {
                    PRESSED.add(chordKey);
                    pendingChord = null;
                    fire(client, binding);
                    continue;
                }
                if (!nowPressed) {
                    PRESSED.remove(chordKey);
                }
            }

            String primaryKey = strokeKey(binding.primary);
            boolean nowPressed = binding.primary.isPressed(client);
            boolean wasPressed = PRESSED.contains(primaryKey);

            if (nowPressed && !wasPressed) {
                PRESSED.add(primaryKey);
                if (!modifiersMatch(binding.primary, ctrl, shift, alt)) {
                    continue;
                }

                if (binding.hasChord()) {
                    pendingChord = new PendingChord(binding,
                            System.currentTimeMillis() + binding.chordTimeoutMs);
                } else {
                    pendingChord = null;
                    fire(client, binding);
                }
            } else if (!nowPressed) {
                PRESSED.remove(primaryKey);
            }
        }
    }

    private static boolean modifiersMatch(KeyStroke stroke, boolean ctrl, boolean shift, boolean alt) {
        if (KeyStroke.TYPE_KEYSYM.equals(stroke.type) && isModifierKey(stroke.code)) {
            return true;
        }
        return stroke.ctrl == ctrl && stroke.shift == shift && stroke.alt == alt;
    }

    private static boolean isModifierKey(int code) {
        return code == GLFW.GLFW_KEY_LEFT_CONTROL || code == GLFW.GLFW_KEY_RIGHT_CONTROL
                || code == GLFW.GLFW_KEY_LEFT_SHIFT || code == GLFW.GLFW_KEY_RIGHT_SHIFT
                || code == GLFW.GLFW_KEY_LEFT_ALT || code == GLFW.GLFW_KEY_RIGHT_ALT;
    }

    private static boolean isPressedRaw(Window window, int keyCode) {
        return window != null && InputUtil.isKeyPressed(window, keyCode);
    }

    private static String strokeKey(KeyStroke stroke) {
        return stroke.type + ":" + stroke.code;
    }

    private static void fire(MinecraftClient client, MultiKeyBinding binding) {
        for (String actionId : binding.actions) {
            ModAction action = ModAction.fromId(actionId);
            if (action != null) {
                action.execute(client);
            }
        }
    }

    public static void resetState() {
        PRESSED.clear();
        pendingChord = null;
    }

    private static final class PendingChord {
        final MultiKeyBinding binding;
        final long deadlineMs;

        PendingChord(MultiKeyBinding binding, long deadlineMs) {
            this.binding = binding;
            this.deadlineMs = deadlineMs;
        }
    }
}