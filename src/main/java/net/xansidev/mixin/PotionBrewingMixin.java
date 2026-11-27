package net.xansidev.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.xansidev.Afterlife;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Both brewing orders reach "Splash Potion of Weaving (8:00)":
//   Weaving -> Redstone -> Gunpowder
//   Weaving -> Gunpowder -> Redstone
// Splash 8:00 Weaving cannot be turned into lingering (or anything else).
// I am praying this works and is not bugged as fuck.
@Mixin(PotionBrewing.class)
public abstract class PotionBrewingMixin {

    // hasMix(input, ingredient)
    @Inject(method = "hasMix", at = @At("HEAD"), cancellable = true)
    private void afterlife$hasMix(ItemStack input, ItemStack ingredient, CallbackInfoReturnable<Boolean> cir) {
        boolean longWeaving = Afterlife.isLongWeaving(input);
        boolean baseWeaving = Afterlife.isWeavingPotion(input);

        if (!longWeaving && !baseWeaving) return; // not ours, let vanilla handle it

        boolean isBottle = input.is(Items.POTION);
        boolean isSplash = input.is(Items.SPLASH_POTION);

        // Redstone: base weaving -> 8:00 weaving (works on bottle OR splash)
        if (baseWeaving && ingredient.is(Items.REDSTONE) && (isBottle || isSplash)) {
            cir.setReturnValue(true);
            return;
        }

        // Gunpowder: bottle -> splash (works on base OR 8:00 weaving)
        if (isBottle && ingredient.is(Items.GUNPOWDER)) {
            cir.setReturnValue(true);
            return;
        }

        // Everything else is blocked (no lingering, no re-redstone, etc.)
        cir.setReturnValue(false);
    }

    // mix(ingredient, input) -- argument order is flipped vs hasMix
    @Inject(method = "mix", at = @At("HEAD"), cancellable = true)
    private void afterlife$mix(ItemStack ingredient, ItemStack input, CallbackInfoReturnable<ItemStack> cir) {
        boolean longWeaving = Afterlife.isLongWeaving(input);
        boolean baseWeaving = Afterlife.isWeavingPotion(input);

        if (!longWeaving && !baseWeaving) return;

        boolean isBottle = input.is(Items.POTION);
        boolean isSplash = input.is(Items.SPLASH_POTION);

        // Redstone: keep the container the potion is already in
        if (baseWeaving && ingredient.is(Items.REDSTONE) && (isBottle || isSplash)) {
            cir.setReturnValue(Afterlife.longWeavingPotion().transmuteCopy(input.getItem()));
            return;
        }

        // Gunpowder: swap container, keep all components (base or 8:00)
        if (isBottle && ingredient.is(Items.GUNPOWDER)) {
            cir.setReturnValue(input.transmuteCopy(Items.SPLASH_POTION));
            return;
        }

        // The stand calls mix() on every slot, so hand it back untouched
        cir.setReturnValue(input);
    }
}