package net.xansidev;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.Scoreboard;
import net.xansidev.network.LivesPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Reads each player's `lives` scoreboard score and pushes it to that player's client.
 * Polls every few ticks and only sends on change, so it also catches /trigger rmlife,
 * Book of Life use, the 7+ clamp, admin /scoreboard edits, etc.
 * Players without the mod are skipped (canSend), so vanilla clients are untouched :3
 */
public final class LivesSync {
	private LivesSync() {}

	private static final String OBJECTIVE = "lives";
	private static final int POLL_TICKS = 5;
	private static final Map<UUID, Integer> LAST_SENT = new HashMap<>();

	public static void init() {
		PayloadTypeRegistry.playS2C().register(LivesPayload.TYPE, LivesPayload.CODEC);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer p = handler.getPlayer();
			LAST_SENT.remove(p.getUUID());
			sync(p, server.getScoreboard());
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
			LAST_SENT.remove(handler.getPlayer().getUUID()));

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % POLL_TICKS != 0) return;
			Scoreboard sb = server.getScoreboard();
			for (ServerPlayer p : server.getPlayerList().getPlayers()) sync(p, sb);
		});
	}

	private static void sync(ServerPlayer p, Scoreboard sb) {
		if (!ServerPlayNetworking.canSend(p, LivesPayload.TYPE)) return;
		int value = read(p, sb);
		Integer prev = LAST_SENT.get(p.getUUID());
		if (prev != null && prev == value) return;
		ServerPlayNetworking.send(p, new LivesPayload(value));
		LAST_SENT.put(p.getUUID(), value);
	}

	private static int read(ServerPlayer p, Scoreboard sb) {
		Objective obj = sb.getObjective(OBJECTIVE);
		if (obj == null) return LivesPayload.NO_SCORE;
		ReadOnlyScoreInfo info = sb.getPlayerScoreInfo(p, obj);
		return info == null ? LivesPayload.NO_SCORE : info.value();
	}
}