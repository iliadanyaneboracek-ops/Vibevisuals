package ru.suppelemen.vibevisuals.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.suppelemen.vibevisuals.feature.screen.HudEditorScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void vibevisuals$replaceReportBugsButton(CallbackInfo ci) {
        List<ClickableWidget> widgets = new ArrayList<>();
        for (var child : children()) {
            if (child instanceof ClickableWidget widget) {
                widgets.add(widget);
            }
        }

        for (ClickableWidget widget : widgets) {
            String message = widget.getMessage().getString().toLowerCase(Locale.ROOT);
            if (message.contains("report") || message.contains("bug") || message.contains("ошиб")) {
                int x = widget.getX();
                int y = widget.getY();
                int width = widget.getWidth();
                int height = widget.getHeight();
                remove(widget);
                addDrawableChild(ButtonWidget.builder(Text.literal("HUD"), button -> MinecraftClient.getInstance().setScreen(new HudEditorScreen()))
                        .dimensions(x, y, width, height)
                        .build());
                return;
            }
        }
    }
}
