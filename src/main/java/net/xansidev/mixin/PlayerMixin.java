package net.xansidev.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.xansidev.Afterlife;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {

	// Block mace hits while on cooldown
	@Inject(method = "attack", at = @At("HEAD"), cancellable = true)
	private void afterlife$maceGate(Entity target, CallbackInfo ci) {
		Player self = (Player) (Object) this;
		ItemStack stack = self.getMainHandItem();
		if (stack.is(Items.MACE) && self.getCooldowns().isOnCooldown(stack)) ci.cancel();
	}

	// Start the cooldown after a mace hit lands on a living target (server only)
	@Inject(method = "attack", at = @At("RETURN"))
	private void afterlife$maceCooldown(Entity target, CallbackInfo ci) {
		if (!((Object) this instanceof ServerPlayer sp)) return;
		ItemStack stack = sp.getMainHandItem();
		if (stack.is(Items.MACE) && target instanceof LivingEntity && target.isAttackable()) {
			sp.getCooldowns().addCooldown(stack, Afterlife.MACE_COOLDOWN);
		}
	}
}
