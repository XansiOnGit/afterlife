package net.xansidev.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import net.xansidev.Afterlife;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TridentItem.class)
public abstract class TridentItemMixin {

	// Vanilla already refuses to start using an item on cooldown, so we only need to
	// START the cooldown after a successful riptide release.
	@Inject(method = "releaseUsing", at = @At("RETURN"))
	private void afterlife$riptide(ItemStack stack, Level level, LivingEntity entity, int timeLeft,
	                               CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValue() && entity instanceof ServerPlayer p) {
			Afterlife.riptideUsed(p, stack);
		}
	}
}
