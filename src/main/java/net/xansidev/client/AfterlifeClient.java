package net.xansidev.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.OptionInstance;
import net.xansidev.AfterlifeConfig;
import net.xansidev.network.LivesPayload;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import java.util.function.Consumer;

/** Client entrypoint + client-only helpers. Never touched by server code. */
public final class AfterlifeClient implements ClientModInitializer {
	private static int lives = LivesPayload.NO_SCORE;

	/** The local player's lives as last reported by the server, or NO_SCORE. */
	public static int lives() {
		return lives;
	}

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(LivesPayload.TYPE,
			(payload, context) -> lives = payload.lives());

		// don't show a stale heart after leaving, or on a server without the mod
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> lives = LivesPayload.NO_SCORE);

		HeartHud.register();
	}

	public static OptionInstance<Boolean> ghostBellOption() {
		return OptionInstance.createBoolean(
			"options.afterlife.ghost_bell",
			AfterlifeConfig.get().ghostBellEnabled,
			enabled -> {
				AfterlifeConfig.get().ghostBellEnabled = enabled;
				AfterlifeConfig.save();
			});
	}

	public static OptionInstance<Boolean> heartHudOption() {
		return OptionInstance.createBoolean(
			"options.afterlife.heart_hud",
			AfterlifeConfig.get().heartHudEnabled,
			enabled -> {
				AfterlifeConfig.get().heartHudEnabled = enabled;
				AfterlifeConfig.save();
			});
	}

	private static OptionInstance<Integer> slider(String key, int min, int max, int current, Consumer<Integer> set) {
    return new OptionInstance<Integer>(
        key,
        OptionInstance.noTooltip(),
        (caption, value) -> Options.genericValueLabel(caption, Component.literal(value + "px")),
        new OptionInstance.IntRange(min, max),
        Mth.clamp(current, min, max),
        v -> {
            set.accept(v);
            AfterlifeConfig.save();
        });
		}

	public static OptionInstance<Integer> heartSizeOption() {
			return slider("options.afterlife.heart_size", 4, 48,
				AfterlifeConfig.get().heartHudSize, v -> AfterlifeConfig.get().heartHudSize = v);
		}

	public static OptionInstance<Integer> heartXOption() {
			return slider("options.afterlife.heart_x", -150, 150,
				AfterlifeConfig.get().heartHudOffsetX, v -> AfterlifeConfig.get().heartHudOffsetX = v);
		}

	public static OptionInstance<Integer> heartYOption() {
			return slider("options.afterlife.heart_y", -150, 150,
				AfterlifeConfig.get().heartHudOffsetY, v -> AfterlifeConfig.get().heartHudOffsetY = v);
		}
}