package net.xansidev;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/** Saved to config/afterlife.json. 0 seconds = that cooldown is disabled. */
public final class AfterlifeConfig {
	public int maceCooldownSeconds = 30;
	public int lungeCooldownSeconds = 3;
	public int riptideCooldownSeconds = 60;
	public int bellRadius = 100;
	/** Client-side only: toggled from Options > Accessibility. */
	public boolean ghostBellEnabled = true;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static AfterlifeConfig instance = new AfterlifeConfig();

	public static AfterlifeConfig get() {
		return instance;
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("afterlife.json");
	}

	public static void load() {
		Path f = file();
		if (Files.exists(f)) {
			try (Reader r = Files.newBufferedReader(f)) {
				AfterlifeConfig loaded = GSON.fromJson(r, AfterlifeConfig.class);
				if (loaded != null) instance = loaded;
			} catch (IOException | RuntimeException e) {
				Afterlife.LOGGER.warn("Couldn't read afterlife.json, using defaults", e);
			}
		}
		save(); // writes defaults / fills in any missing fields
	}

	public static void save() {
		try {
			Files.writeString(file(), GSON.toJson(instance));
		} catch (IOException e) {
			Afterlife.LOGGER.warn("Couldn't save afterlife.json", e);
		}
	}
}
