package ru.suppelemen.vibevisuals.feature.keybind;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public class KeyStroke {
    public static final String TYPE_KEYSYM = "KEYSYM";
    public static final String TYPE_MOUSE = "MOUSE";
    public static final String TYPE_NONE = "NONE";

    public String type = TYPE_NONE;
    public int code = -1;
    public boolean ctrl = false;
    public boolean shift = false;
    public boolean alt = false;

    public KeyStroke() {
    }

    public KeyStroke(String type, int code, boolean ctrl, boolean shift, boolean alt) {
        this.type = type;
        this.code = code;
        this.ctrl = ctrl;
        this.shift = shift;
        this.alt = alt;
    }

    public static KeyStroke key(int code) {
        return new KeyStroke(TYPE_KEYSYM, code, false, false, false);
    }

    public static KeyStroke keyWithModifiers(int code, boolean ctrl, boolean shift, boolean alt) {
        return new KeyStroke(TYPE_KEYSYM, code, ctrl, shift, alt);
    }

    public static KeyStroke mouse(int button) {
        return new KeyStroke(TYPE_MOUSE, button, false, false, false);
    }

    public boolean isAssigned() {
        return type != null && !TYPE_NONE.equals(type) && code >= 0;
    }

    public boolean hasModifiers() {
        return ctrl || shift || alt;
    }

    public void normalize() {
        if (type == null) {
            type = TYPE_NONE;
        } else {
            type = type.toUpperCase(Locale.ROOT);
            if (!TYPE_KEYSYM.equals(type) && !TYPE_MOUSE.equals(type) && !TYPE_NONE.equals(type)) {
                type = TYPE_NONE;
            }
        }

        if (!isAssigned()) {
            code = -1;
            ctrl = false;
            shift = false;
            alt = false;
        }
    }

    public boolean isPressed(MinecraftClient client) {
        if (!isAssigned() || client == null) {
            return false;
        }

        Window window = client.getWindow();
        if (window == null) {
            return false;
        }

        if (TYPE_KEYSYM.equals(type)) {
            return InputUtil.isKeyPressed(window, code);
        }

        if (TYPE_MOUSE.equals(type)) {
            return GLFW.glfwGetMouseButton(window.getHandle(), code) == GLFW.GLFW_PRESS;
        }

        return false;
    }

    public String describe() {
        if (!isAssigned()) {
            return "—";
        }

        StringBuilder builder = new StringBuilder();
        if (ctrl) {
            builder.append("Ctrl+");
        }
        if (shift) {
            builder.append("Shift+");
        }
        if (alt) {
            builder.append("Alt+");
        }

        if (TYPE_MOUSE.equals(type)) {
            builder.append("Mouse").append(code);
        } else {
            builder.append(InputUtil.Type.KEYSYM.createFromCode(code).getLocalizedText().getString());
        }
        return builder.toString();
    }
}