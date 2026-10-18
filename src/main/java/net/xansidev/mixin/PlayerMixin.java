package net.xansidev.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.xansidev.Afterlife;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {

	// Was this swing a real smash attack? Must be sampled BEFORE the attack,
	// because a smash resets fall distance.
	@Unique private boolean afterlife$smash;

	@Inject(method = "attack", at = @At("HEAD"), cancellable = true)
	private void afterlife$maceHead(Entity target, CallbackInfo ci) {
		Player self = (Player) (Object) this;
		ItemStack stack = self.getMainHandItem();
		afterlife$smash = stack.is(Items.MACE) && MaceItem.canSmashAttack(self);

		// Smash attacks are blocked while on cooldown; normal sword-style hits still work.
		if (afterlife$smash && self.getCooldowns().isOnCooldown(stack)) {
			afterlife$smash = false;
			ci.cancel();
		}
	}

	@Inject(method = "attack", at = @At("RETURN"))
	private void afterlife$maceCooldown(Entity target, CallbackInfo ci) {
		boolean smash = afterlife$smash;
		afterlife$smash = false;
		if (!smash || !((Object) this instanceof ServerPlayer sp)) return;
		ItemStack stack = sp.getMainHandItem();
		if (stack.is(Items.MACE) && target instanceof LivingEntity && target.isAttackable()) {
			sp.getCooldowns().addCooldown(stack, Afterlife.MACE_COOLDOWN);
		}
	}
}
