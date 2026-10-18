package net.xansidev.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.xansidev.Afterlife;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Potion of Weaving + Redstone -> 8:00 Weaving, without registering a custom Potion
// (custom registry entries would break vanilla clients).
@Mixin(PotionBrewing.class)
public abstract class PotionBrewingMixin {

	@Inject(method = "hasMix", at = @At("HEAD"), cancellable = true)
	private void afterlife$hasMix(ItemStack a, ItemStack b, CallbackInfoReturnable<Boolean> cir) {
		if (Afterlife.isWeavingBrew(a, b)) cir.setReturnValue(true);
	}

	@Inject(method = "mix", at = @At("HEAD"), cancellable = true)
	private void afterlife$mix(ItemStack a, ItemStack b, CallbackInfoReturnable<ItemStack> cir) {
		if (Afterlife.isWeavingBrew(a, b)) cir.setReturnValue(Afterlife.longWeavingPotion());
	}
}
