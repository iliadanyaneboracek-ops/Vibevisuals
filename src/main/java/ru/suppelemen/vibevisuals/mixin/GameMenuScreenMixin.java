package ru.suppelemen.vibevisuals.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
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
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.feature.pvp.PvpCombatTracker;
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

    @Inject(method = "init", at = @At("TAIL"))
    private void vibevisuals$guardDisconnectButton(CallbackInfo ci) {
        ButtonWidget disconnect = null;
        for (var child : children()) {
            if (child instanceof ButtonWidget widget && vibevisuals$matchesDisconnect(widget.getMessage())) {
                disconnect = widget;
                break;
            }
        }

        if (disconnect == null) {
            return;
        }

        ButtonWidget.PressAction original = ((ButtonWidgetAccessor) disconnect).vibevisuals$getPressAction();
        int x = disconnect.getX();
        int y = disconnect.getY();
        int width = disconnect.getWidth();
        int height = disconnect.getHeight();
        Text message = disconnect.getMessage();
        GameMenuScreen self = (GameMenuScreen) (Object) this;

        remove(disconnect);
        addDrawableChild(ButtonWidget.builder(message, button -> {
            if (!vibevisuals$shouldConfirmQuit()) {
                original.onPress(button);
                return;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            client.setScreen(new ConfirmScreen(
                    confirmed -> {
                        if (confirmed) {
                            original.onPress(button);
                        } else {
                            client.setScreen(self);
                        }
                    },
                    Text.translatableWithFallback("screen.vibevisuals.quit_guard.title", "Вы точно хотите выйти?"),
                    Text.translatableWithFallback("screen.vibevisuals.quit_guard.message",
                            "Вы сейчас в PvP. Выход во время боя может быть засчитан как поражение."),
                    Text.translatableWithFallback("screen.vibevisuals.quit_guard.confirm", "Выйти"),
                    Text.translatableWithFallback("screen.vibevisuals.quit_guard.cancel", "Остаться")));
        }).dimensions(x, y, width, height).build());
    }

    private static boolean vibevisuals$shouldConfirmQuit() {
        return VibeVisualsConfigManager.get().pvpQuitGuard.enabled && PvpCombatTracker.isActive();
    }

    private static boolean vibevisuals$matchesDisconnect(Text message) {
        if (message == null) {
            return false;
        }
        TextContent content = message.getContent();
        return content instanceof TranslatableTextContent translatable
                && "menu.disconnect".equals(translatable.getKey());
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
