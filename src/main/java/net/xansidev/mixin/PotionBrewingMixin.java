package net.xansidev.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.xansidev.Afterlife;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Potion of Weaving + Redstone -> 8:00 Weaving (custom effects, no custom registry entry),
// and the 8:00 potion can be turned into a splash potion with gunpowder (not lingering).
@Mixin(PotionBrewing.class)
public abstract class PotionBrewingMixin {

	// hasMix(input, ingredient)
	@Inject(method = "hasMix", at = @At("HEAD"), cancellable = true)
	private void afterlife$hasMix(ItemStack input, ItemStack ingredient, CallbackInfoReturnable<Boolean> cir) {
		if (Afterlife.isLongWeaving(input)) {
			// Only a normal bottle + gunpowder is allowed. Everything else is blocked.
			cir.setReturnValue(input.is(Items.POTION) && ingredient.is(Items.GUNPOWDER));
		} else if (Afterlife.isWeavingBrew(input, ingredient)) {
			cir.setReturnValue(true);
		}
	}

	// mix(ingredient, input)  <-- order is flipped vs hasMix
	@Inject(method = "mix", at = @At("HEAD"), cancellable = true)
	private void afterlife$mix(ItemStack ingredient, ItemStack input, CallbackInfoReturnable<ItemStack> cir) {
		if (Afterlife.isLongWeaving(input)) {
			if (input.is(Items.POTION) && ingredient.is(Items.GUNPOWDER)) {
				cir.setReturnValue(input.transmuteCopy(Items.SPLASH_POTION));
			} else {
				// stand calls mix() on every slot, so hand it back untouched
				cir.setReturnValue(input);
			}
		} else if (Afterlife.isWeavingBrew(ingredient, input)) {
			cir.setReturnValue(Afterlife.longWeavingPotion());
		}
	}
}
