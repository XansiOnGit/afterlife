package net.xansidev.mixin.client;

import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.xansidev.client.AfterlifeClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Adds a "Ghost Bell: ON/OFF" toggle to Options > Accessibility Settings.
@Mixin(AccessibilityOptionsScreen.class)
public abstract class AccessibilityOptionsScreenMixin extends OptionsSubScreen {

	// required by the superclass; never actually called
	protected AccessibilityOptionsScreenMixin(Screen lastScreen, Options options, Component title) {
		super(lastScreen, options, title);
	}

	@Inject(method = "addOptions", at = @At("RETURN"))
	private void afterlife$addGhostBellToggle(CallbackInfo ci) {
		this.list.addBig(AfterlifeClient.ghostBellOption());
	}
}
