package net.xansidev.mixin.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.xansidev.Afterlife;
import net.xansidev.AfterlifeConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// If the player turned the Ghost Bell off, drop that one sound packet.
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

	@Inject(method = "handleSoundEvent", at = @At("HEAD"), cancellable = true)
	private void afterlife$muteGhostBell(ClientboundSoundPacket packet, CallbackInfo ci) {
		if (!AfterlifeConfig.get().ghostBellEnabled
			&& packet.getSound().value().location().equals(Afterlife.BELL_ID)) {
			ci.cancel();
		}
	}
}
