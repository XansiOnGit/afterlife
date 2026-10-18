package net.xansidev.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.xansidev.Afterlife;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Potion of Weaving + Redstone -> 8:00 Weaving (custom effects, no custom registry entry),
// and the 8:00 potion can't be turned into splash/lingering.
@Mixin(PotionBrewing.class)
public abstract class PotionBrewingMixin {

	@Inject(method = "hasMix", at = @At("HEAD"), cancellable = true)
	private void afterlife$hasMix(ItemStack a, ItemStack b, CallbackInfoReturnable<Boolean> cir) {
		if (Afterlife.isLongWeaving(a) || Afterlife.isLongWeaving(b)) {
			cir.setReturnValue(false);
		} else if (Afterlife.isWeavingBrew(a, b)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mix", at = @At("HEAD"), cancellable = true)
	private void afterlife$mix(ItemStack a, ItemStack b, CallbackInfoReturnable<ItemStack> cir) {
		// Brewing stands call mix() on every slot, so hand the long potion back untouched
		if (Afterlife.isLongWeaving(a)) cir.setReturnValue(a);
		else if (Afterlife.isLongWeaving(b)) cir.setReturnValue(b);
		else if (Afterlife.isWeavingBrew(a, b)) cir.setReturnValue(Afterlife.longWeavingPotion());
	}
}
