package net.xansidev.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.xansidev.Afterlife;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// The god mace may only ever sit in a player's own inventory fuck you stashers :3
@Mixin(Slot.class)
public abstract class SlotMixin {

	@Shadow @Final public Container container;

	@Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
	private void afterlife$noStorage(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (!(this.container instanceof Inventory) && Afterlife.isGodMace(stack)) {
			cir.setReturnValue(false);
		}
	}
}