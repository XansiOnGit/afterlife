package net.xansidev;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.EnchantmentEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.tags.ItemTags;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class Afterlife implements ModInitializer {
	public static final String MOD_ID = "afterlife";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// Cooldown lengths live in config/afterlife.json and are changed with /afterlife cooldown
	public static final int COMBAT_WINDOW = 10 * 20;
	public static final int WEAVING_DURATION = 8 * 60 * 20;

	/** Played by ID only (never registered) so vanilla clients are unaffected. */
	public static final Identifier BELL_ID = Identifier.fromNamespaceAndPath(MOD_ID, "ghost_bell");
	private static final Holder<SoundEvent> GHOST_BELL = Holder.direct(SoundEvent.createVariableRangeEvent(BELL_ID));

	private static final Map<UUID, Long> LAST_COMBAT = new HashMap<>();

	@Override
	public void onInitialize() {
		AfterlifeConfig.load();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			AfterlifeCommands.register(dispatcher));

		// Combat tracking: any damage dealt or taken by a player
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDmg, dmg, blocked) -> {
			if (entity instanceof ServerPlayer p) markCombat(p);
			if (source.getEntity() instanceof ServerPlayer attacker) markCombat(attacker);
		});

		// Ghost bell: everyone within range (same dimension) hears a toll when a player dies
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer p) ghostBell(p);
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
			LAST_COMBAT.remove(handler.getPlayer().getUUID()));

		// Enchant rules (table + anvil): no Breach/Density on maces, no Unbreaking on shields,
		// and the secret: Breach is allowed on axes. - Edit; they caught me :<
		EnchantmentEvents.ALLOW_ENCHANTING.register((enchantment, target, context) -> {
			if (target.is(Items.MACE) && (enchantment.is(Enchantments.BREACH) || enchantment.is(Enchantments.DENSITY)))
				return TriState.FALSE;
			if (target.is(Items.SHIELD) && enchantment.is(Enchantments.UNBREAKING))
				return TriState.FALSE;
			return TriState.DEFAULT;
		});

		// God mace can't be handed to item frames, armor stands, chest boats/minecarts, allays fucken. anything that i can think of.
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (isGodMace(player.getItemInHand(hand))
				&& (entity instanceof ItemFrame || entity instanceof ArmorStand
					|| entity instanceof ContainerEntity || entity instanceof Allay)) {
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});

		// or put into decorated pots had to look this fucker up to figure it out.
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (isGodMace(player.getItemInHand(hand))
				&& level.getBlockState(hit.getBlockPos()).is(Blocks.DECORATED_POT)) {
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});

		// main tick loop this miiiighhhttt cause a little lag here and there at max players.

			ServerTickEvents.END_SERVER_TICK.register(server -> {
		if (server.getTickCount() % 10 != 0) return;
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			var inv = p.getInventory();
			for (int i = 0; i < inv.getContainerSize(); i++) strip(inv.getItem(i), server.registryAccess());
			strip(p.containerMenu.getCarried(), server.registryAccess());
			}
		});

		LOGGER.info("Afterlife initialize!");
	}

	// ---------- Ghost bell ----------
	private static void ghostBell(ServerPlayer dead) {
		ServerLevel level = (ServerLevel) dead.level();
		double radius = Math.max(1, AfterlifeConfig.get().bellRadius);

		for (ServerPlayer other : level.players()) {
			if (other == dead) continue;
			double dx = dead.getX() - other.getX();
			double dy = dead.getY() - other.getY();
			double dz = dead.getZ() - other.getZ();
			double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (dist > radius) continue;

			// quieter the farther away; nudged 3 blocks toward the death for a faint direction hint
			float volume = (float) Math.max(0.15, 1.0 - (dist / radius) * 0.85);
			double nx = dist < 0.001 ? 0 : dx / dist;
			double ny = dist < 0.001 ? 0 : dy / dist;
			double nz = dist < 0.001 ? 0 : dz / dist;

			other.connection.send(new ClientboundSoundPacket(
				GHOST_BELL, SoundSource.AMBIENT,
				other.getX() + nx * 3, other.getY() + ny * 3, other.getZ() + nz * 3,
				volume, 1.0f, level.getRandom().nextLong()));
		}
	}

	// ---------- Cooldowns (config-driven; 0 = off) ----------
	public static int maceTicks() { return AfterlifeConfig.get().maceCooldownSeconds * 20; }
	public static int lungeTicks() { return AfterlifeConfig.get().lungeCooldownSeconds * 20; }
	public static int riptideTicks() { return AfterlifeConfig.get().riptideCooldownSeconds * 20; }

	public static void applyCooldown(ServerPlayer p, ItemStack stack, int ticks) {
		if (ticks > 0) p.getCooldowns().addCooldown(stack, ticks);
	}

	// ---------- Enchant stripping ----------

	private static void strip(ItemStack stack, RegistryAccess registries) {
	if (stack.isEmpty()) return;
	if (stack.is(Items.MACE)) {
		if (isGodMace(stack)) { // the real one: keep it topped up (grindstone etc.)
			applyGodMace(stack, registries);
			return;
		}
		if (!godTag(stack).isEmpty()) {
			clearGod(stack); // stale ex-god mace
			stack.remove(DataComponents.USE_COOLDOWN);
		}
		if (hasEnchant(stack, Enchantments.BREACH) || hasEnchant(stack, Enchantments.DENSITY)) {
			EnchantmentHelper.updateEnchantments(stack, e ->
				e.removeIf(h -> h.is(Enchantments.BREACH) || h.is(Enchantments.DENSITY)));
		}
	} else if (stack.is(Items.SHIELD) && hasEnchant(stack, Enchantments.UNBREAKING)) {
		EnchantmentHelper.updateEnchantments(stack, e -> e.removeIf(h -> h.is(Enchantments.UNBREAKING)));
	}
}

		public static boolean hasEnchant(ItemStack stack, ResourceKey<Enchantment> key) {
		return stack.getEnchantments().keySet().stream().anyMatch(h -> h.is(key));
	}

	// ---------- God mace (just components on a vanilla mace, so server side only) ----------
	private static final String GOD_KEY = "afterlife_godmace";
	private static final String GOD_MODEL_KEY = "god_mace";

	private static boolean hasGodModel(ItemStack stack) {
		CustomModelData m = stack.get(DataComponents.CUSTOM_MODEL_DATA);
			return m != null && m.strings().contains(GOD_MODEL_KEY);
}

	private static String godTag(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data == null ? "" : data.copyTag().getString(GOD_KEY).orElse("");
	}

	public static boolean isGodMace(ItemStack stack) {
		if (!stack.is(Items.MACE)) return false;
		String id = AfterlifeConfig.get().godMaceId;
		return !id.isEmpty() && id.equals(godTag(stack));
	}

	/** Makes this stack THE god mace. Every previous one becomes stale automatically. */
	public static void makeGodMace(ItemStack stack, RegistryAccess registries) {
		String id = UUID.randomUUID().toString();
		AfterlifeConfig.get().godMaceId = id;
		AfterlifeConfig.save();
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(GOD_KEY, id));
		applyGodMace(stack, registries);
	}

	/** Invalidates the current god mace without making a new one. */
	public static void clearGodMaceId() {
		AfterlifeConfig.get().godMaceId = "";
		AfterlifeConfig.save();
	}
	private static final Identifier GOD_COOLDOWN_GROUP = Identifier.fromNamespaceAndPath(MOD_ID, "god_mace");
	private static final int GOD_GOLD = 0xFFD700; // just a cool color so the wielder can see it ig. idk looks cool :p
	private static final Style GOD_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(GOD_GOLD)).withItalic(false);

	private static void applyGodMace(ItemStack stack, RegistryAccess registries) {
	Component custom = stack.get(DataComponents.CUSTOM_NAME);
	if (custom != null) {
		if (!GOD_STYLE.equals(custom.getStyle())) {
			stack.set(DataComponents.CUSTOM_NAME, Component.literal(custom.getString()).withStyle(GOD_STYLE));
		}
	} else {
		Component item = stack.get(DataComponents.ITEM_NAME);
		if (item == null || !GOD_STYLE.equals(item.getStyle())) {
			stack.set(DataComponents.ITEM_NAME, Component.translatable("item.minecraft.mace").withStyle(GOD_STYLE));
		}
	}

	// own cooldown group so normal mace cooldowns don't show on the god mace i've come to like calling it the god mace
	if (stack.get(DataComponents.USE_COOLDOWN) == null) {
		stack.set(DataComponents.USE_COOLDOWN, new UseCooldown(0.05f, Optional.of(GOD_COOLDOWN_GROUP)));
	}

	// leftover from the old custom sprite attempt may change in the future but amber doesn't want a sprite they just figure it out the hard way.
	stack.remove(DataComponents.CUSTOM_MODEL_DATA);

	stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

	Holder<Enchantment> density = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.DENSITY);
	if (stack.getEnchantments().getLevel(density) < 1) {
		EnchantmentHelper.updateEnchantments(stack, e -> e.set(density, 1));
		}
	}

	private static void clearGod(ItemStack stack) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(GOD_KEY));
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data != null && data.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);

		// back to a normal mace: plain name, keep any custom name the player gave it
		stack.set(DataComponents.ITEM_NAME, Component.translatable("item.minecraft.mace"));
		Component custom = stack.get(DataComponents.CUSTOM_NAME);
		if (custom != null) {
			stack.set(DataComponents.CUSTOM_NAME, Component.literal(custom.getString()));
		}
		stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
	}

	// ---------- Combat tracking ----------
	public static void markCombat(ServerPlayer p) {
		LAST_COMBAT.put(p.getUUID(), p.level().getGameTime());
	}

	public static boolean inCombat(ServerPlayer p) {
		Long last = LAST_COMBAT.get(p.getUUID());
		return last != null && p.level().getGameTime() - last < COMBAT_WINDOW;
	}

	// ---------- Riptide hook (called from TridentItemMixin after a successful release) ----------
	public static void riptideUsed(ServerPlayer p, ItemStack stack) {
		if (EnchantmentHelper.getTridentSpinAttackStrength(stack, p) > 0 && inCombat(p)) {
			applyCooldown(p, stack, riptideTicks());
		}
	}

	// ---------- Weaving brew (vanilla-client safe: no custom registry entries) ----------
	public static boolean isWeavingBrew(ItemStack a, ItemStack b) {
		return weavingPair(a, b) || weavingPair(b, a);
	}

	private static boolean weavingPair(ItemStack potion, ItemStack ingredient) {
		if (!potion.is(Items.POTION) || !ingredient.is(Items.REDSTONE)) return false;
		PotionContents c = potion.get(DataComponents.POTION_CONTENTS);
		return c != null && c.potion().filter(h -> h.is(Potions.WEAVING)).isPresent();
	}

	/** Base (non-8:00) Potion of Weaving, in a bottle or a splash bottle. */
	// finally it can work with both :DDDD
	public static boolean isWeavingPotion(ItemStack stack) {
		if (!stack.is(Items.POTION) && !stack.is(Items.SPLASH_POTION)) return false;
		PotionContents c = stack.get(DataComponents.POTION_CONTENTS);
		return c != null && c.potion().filter(h -> h.is(Potions.WEAVING)).isPresent();
	}

	// Our 8:00 potion: no base potion, custom Weaving effect with the long duration.
	// Works for both bottle and splash.
	public static boolean isLongWeaving(ItemStack stack) {
		if (!stack.is(Items.POTION) && !stack.is(Items.SPLASH_POTION)) return false;
		PotionContents c = stack.get(DataComponents.POTION_CONTENTS);
		if (c == null || c.potion().isPresent()) return false;
		for (MobEffectInstance e : c.customEffects()) {
			if (e.getEffect().is(MobEffects.WEAVING) && e.getDuration() == WEAVING_DURATION) return true;
		}
		return false;
}

	public static ItemStack longWeavingPotion() {
		ItemStack s = new ItemStack(Items.POTION);
		s.set(DataComponents.POTION_CONTENTS, new PotionContents(
			Optional.empty(), Optional.empty(),
			List.of(new MobEffectInstance(MobEffects.WEAVING, WEAVING_DURATION)),
			Optional.of("weaving")));
		return s;
	}
}
