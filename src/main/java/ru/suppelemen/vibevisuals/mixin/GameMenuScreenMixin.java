package ru.suppelemen.vibevisuals.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.suppelemen.vibevisuals.feature.screen.VibeVisualsMenuScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    private static final Set<String> FEEDBACK_KEYS = Set.of(
            "menu.sendFeedback",
            "menu.reportBugs",
            "menu.feedback",
            "menu.playerReporting"
    );

    @Inject(method = "init", at = @At("TAIL"))
    private void vibevisuals$replaceFeedbackButton(CallbackInfo ci) {
        List<ClickableWidget> widgets = new ArrayList<>();
        for (var child : children()) {
            if (child instanceof ClickableWidget widget) {
                widgets.add(widget);
            }
        }

        ClickableWidget target = null;
        for (ClickableWidget widget : widgets) {
            if (vibevisuals$matchesFeedback(widget.getMessage())) {
                target = widget;
                break;
            }
        }

        if (target != null) {
            int x = target.getX();
            int y = target.getY();
            int width = target.getWidth();
            int height = target.getHeight();
            remove(target);
            addDrawableChild(ButtonWidget.builder(Text.literal("VibeVisuals"),
                            button -> MinecraftClient.getInstance().setScreen(new VibeVisualsMenuScreen()))
                    .dimensions(x, y, width, height)
                    .build());
            return;
        }

        ClickableWidget anchor = vibevisuals$findAnchor(widgets);
        if (anchor != null) {
            addDrawableChild(ButtonWidget.builder(Text.literal("VibeVisuals"),
                            button -> MinecraftClient.getInstance().setScreen(new VibeVisualsMenuScreen()))
                    .dimensions(anchor.getX(), anchor.getY() + anchor.getHeight() + 4,
                            anchor.getWidth(), anchor.getHeight())
                    .build());
        }
    }

    private static boolean vibevisuals$matchesFeedback(Text message) {
        if (message == null) {
            return false;
        }
        TextContent content = message.getContent();
        if (content instanceof TranslatableTextContent translatable && FEEDBACK_KEYS.contains(translatable.getKey())) {
            return true;
        }
        String fallback = message.getString().toLowerCase(Locale.ROOT);
        return fallback.contains("feedback") || fallback.contains("report") || fallback.contains("bug")
                || fallback.contains("отзыв") || fallback.contains("ошибк") || fallback.contains("обратн");
    }

    private static ClickableWidget vibevisuals$findAnchor(List<ClickableWidget> widgets) {
        ClickableWidget last = null;
        for (ClickableWidget widget : widgets) {
            if (last == null || widget.getY() > last.getY()) {
                last = widget;
            }
        }
        return last;
    }
}
