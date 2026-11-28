package net.xansidev.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.xansidev.DevTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
	@Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
	private void afterlife$devTag(CallbackInfoReturnable<Component> cir) {
		if ((Object) this instanceof Player p && DevTag.hasTag(p)) {
			cir.setReturnValue(DevTag.decorate(p, cir.getReturnValue()));
		}
	}
}