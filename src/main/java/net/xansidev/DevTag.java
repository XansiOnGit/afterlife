package net.xansidev;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class DevTag {
	private DevTag() {}

	private static final FontDescription ICON_FONT =
		new FontDescription.Resource(Identifier.fromNamespaceAndPath(Afterlife.MOD_ID, "dev"));
	private static final String ICON_GLYPH = "\uE000";

	public static String raw(Player p) {
		return AfterlifeConfig.get().devTags.get(p.getUUID().toString());
	}

	public static boolean hasTag(Player p) {
		return raw(p) != null;
	}

	public static Component decorate(Player p, Component name) {
		return Component.empty().append(parse(raw(p))).append(" ").append(name);
	}

	public static void set(MinecraftServer server, ServerPlayer p, String text) {
		AfterlifeConfig.get().devTags.put(p.getUUID().toString(), text);
		AfterlifeConfig.save();
		refreshTab(server, p);
	}

	public static void clear(MinecraftServer server, ServerPlayer p) {
		AfterlifeConfig.get().devTags.remove(p.getUUID().toString());
		AfterlifeConfig.save();
		refreshTab(server, p);
	}

	/** Re-sends this player's tab list name to everyone so the change shows immediately. */
	private static void refreshTab(MinecraftServer server, ServerPlayer p) {
		server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(
			ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, p));
	}

	/** "&6" style color codes and {icon}. Default color is gold. */
	public static MutableComponent parse(String raw) {
		MutableComponent out = Component.empty();
		Style style = Style.EMPTY.withColor(ChatFormatting.GOLD);
		StringBuilder buf = new StringBuilder();

		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);

			if (c == '&' && i + 1 < raw.length()) {
				ChatFormatting f = ChatFormatting.getByCode(raw.charAt(i + 1));
				if (f != null) {
					flush(out, buf, style);
					style = f == ChatFormatting.RESET ? Style.EMPTY : style.applyFormat(f);
					i++;
					continue;
				}
			}
			if (raw.startsWith("{icon}", i)) {
				flush(out, buf, style);
				out.append(Component.literal(ICON_GLYPH)
					.withStyle(Style.EMPTY.withFont(ICON_FONT).withColor(ChatFormatting.WHITE)));
				i += 5;
				continue;
			}
			buf.append(c);
		}
		flush(out, buf, style);
		return out;
	}

	private static void flush(MutableComponent out, StringBuilder buf, Style style) {
		if (buf.length() == 0) return;
		out.append(Component.literal(buf.toString()).withStyle(style));
		buf.setLength(0);
	}
}