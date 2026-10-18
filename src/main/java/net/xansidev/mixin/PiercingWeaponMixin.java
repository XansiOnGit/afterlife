package net.xansidev.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.enchantment.Enchantments;
import net.xansidev.Afterlife;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Spear jabs don't go through Player.attack, so Lunge cooldown lives here.
// NOTE: if PiercingWeapon#attack returns boolean in your mappings, switch to CallbackInfoReturnable<Boolean>.
@Mixin(PiercingWeapon.class)
public abstract class PiercingWeaponMixin {

	@Inject(method = "attack", at = @At("HEAD"), cancellable = true)
	private void afterlife$lunge(LivingEntity user, EquipmentSlot slot, CallbackInfo ci) {
		if (!(user instanceof ServerPlayer p)) return;
		ItemStack stack = p.getItemBySlot(slot);
		if (!Afterlife.hasEnchant(stack, Enchantments.LUNGE)) return;

		if (p.getCooldowns().isOnCooldown(stack)) {
			ci.cancel();
			return;
		}
		// Only cooldown the lunge if you were already in combat before this jab
		if (Afterlife.inCombat(p)) {
			p.getCooldowns().addCooldown(stack, Afterlife.LUNGE_COOLDOWN);
		}
	}
}
