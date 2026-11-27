package net.xansidev.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.xansidev.Afterlife;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// the only point to this is to make sure that hoppers don't suck up the god mace so people can get around it.

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {

	@Inject(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/item/ItemEntity;)Z",
			at = @At("HEAD"), cancellable = true)
	private static void afterlife$noHopperPickup(Container container, ItemEntity entity,
	                                             CallbackInfoReturnable<Boolean> cir) {
		if (Afterlife.isGodMace(entity.getItem())) {
			cir.setReturnValue(false);
		}
	}
}