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
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
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

		// Safety net: rip banned enchants off anything that slipped through (loot, commands, creative)
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 10 != 0) return;
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				var inv = p.getInventory();
				for (int i = 0; i < inv.getContainerSize(); i++) strip(inv.getItem(i));
				strip(p.containerMenu.getCarried());
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
	private static void strip(ItemStack stack) {
		if (stack.isEmpty()) return;
		if (stack.is(Items.MACE) && (hasEnchant(stack, Enchantments.BREACH) || hasEnchant(stack, Enchantments.DENSITY))) {
			EnchantmentHelper.updateEnchantments(stack, e ->
				e.removeIf(h -> h.is(Enchantments.BREACH) || h.is(Enchantments.DENSITY)));
		} else if (stack.is(Items.SHIELD) && hasEnchant(stack, Enchantments.UNBREAKING)) {
			EnchantmentHelper.updateEnchantments(stack, e -> e.removeIf(h -> h.is(Enchantments.UNBREAKING)));
		}
	}

	public static boolean hasEnchant(ItemStack stack, ResourceKey<Enchantment> key) {
		return stack.getEnchantments().keySet().stream().anyMatch(h -> h.is(key));
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

	// Our 8:00 potion: no base potion, custom Weaving effect with the long duration
	public static boolean isLongWeaving(ItemStack stack) {
		if (!stack.is(Items.POTION)) return false;
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
