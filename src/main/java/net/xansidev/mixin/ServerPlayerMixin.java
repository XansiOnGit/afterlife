package net.xansidev.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.xansidev.DevTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// The tab list uses its own name, which is null by default (the client builds it from the team).
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
	@Inject(method = "getTabListDisplayName", at = @At("RETURN"), cancellable = true)
	private void afterlife$devTab(CallbackInfoReturnable<Component> cir) {
		ServerPlayer self = (ServerPlayer) (Object) this;
		if (!DevTag.hasTag(self)) return;
		Component base = cir.getReturnValue();
		cir.setReturnValue(base != null ? DevTag.decorate(self, base) : self.getDisplayName());
	}
}