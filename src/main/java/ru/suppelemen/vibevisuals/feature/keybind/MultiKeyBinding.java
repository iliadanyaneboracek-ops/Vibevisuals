package ru.suppelemen.vibevisuals.feature.keybind;

import java.util.ArrayList;
import java.util.List;

public class MultiKeyBinding {
    public String id = "";
    public String displayName = "";
    public boolean enabled = true;
    public KeyStroke primary = new KeyStroke();
    public KeyStroke chord = null;
    public int chordTimeoutMs = 1000;
    public List<String> actions = new ArrayList<>();

    public MultiKeyBinding() {
    }

    public MultiKeyBinding(String id, String displayName, KeyStroke primary, List<String> actions) {
        this.id = id;
        this.displayName = displayName;
        this.primary = primary;
        this.actions = new ArrayList<>(actions);
    }

    public boolean hasChord() {
        return chord != null && chord.isAssigned();
    }

    public void validate() {
        if (id == null) {
            id = "";
        }
        if (displayName == null) {
            displayName = "";
        }
        if (primary == null) {
            primary = new KeyStroke();
        }
        primary.normalize();

        if (chord != null) {
            chord.normalize();
            if (!chord.isAssigned()) {
                chord = null;
            }
        }

        if (chordTimeoutMs < 100) {
            chordTimeoutMs = 100;
        } else if (chordTimeoutMs > 5000) {
            chordTimeoutMs = 5000;
        }

        if (actions == null) {
            actions = new ArrayList<>();
        } else {
            actions.removeIf(action -> action == null || ModAction.fromId(action) == null);
        }
    }
}