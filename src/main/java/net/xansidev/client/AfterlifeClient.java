package net.xansidev.client;

import net.minecraft.client.OptionInstance;
import net.xansidev.AfterlifeConfig;

/** Client-only helpers. Never touched by server code. */
public final class AfterlifeClient {
	private AfterlifeClient() {}

	public static OptionInstance<Boolean> ghostBellOption() {
		return OptionInstance.createBoolean(
			"options.afterlife.ghost_bell",
			AfterlifeConfig.get().ghostBellEnabled,
			enabled -> {
				AfterlifeConfig.get().ghostBellEnabled = enabled;
				AfterlifeConfig.save();
			});
	}
}
