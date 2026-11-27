package net.xansidev.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.xansidev.Afterlife;

/** Server -> client: the receiving player's own `lives` score. */
public record LivesPayload(int lives) implements CustomPacketPayload {
	/** Sent when the player has no `lives` score (yet), so the client hides the heart. */
	public static final int NO_SCORE = Integer.MIN_VALUE;

	public static final CustomPacketPayload.Type<LivesPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Afterlife.MOD_ID, "lives"));

	public static final StreamCodec<RegistryFriendlyByteBuf, LivesPayload> CODEC =
		StreamCodec.composite(ByteBufCodecs.VAR_INT, LivesPayload::lives, LivesPayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}