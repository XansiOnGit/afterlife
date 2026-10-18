package net.xansidev;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.fabric.api.item.v1.EnchantmentEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
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

	// ---- tweak these (ticks, 20 = 1s) ----
	public static final int MACE_COOLDOWN = 30 * 20;
	public static final int LUNGE_COOLDOWN = 3 * 20;
	public static final int RIPTIDE_COOLDOWN = 60 * 20;
	public static final int COMBAT_WINDOW = 10 * 20;
	public static final int WEAVING_DURATION = 8 * 60 * 20;
	public static final int END_CRYSTAL_STACK = 4;

	private static final Map<UUID, Long> LAST_COMBAT = new HashMap<>();

	@Override
	public void onInitialize() {
		// End crystals stack to 4 (server-side; vanilla clients may show minor desync)
		DefaultItemComponentEvents.MODIFY.register(ctx ->
			ctx.modify(Items.END_CRYSTAL, b -> b.set(DataComponents.MAX_STACK_SIZE, END_CRYSTAL_STACK)));

		// Combat tracking: any damage dealt or taken by a player
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDmg, dmg, blocked) -> {
			if (entity instanceof ServerPlayer p) markCombat(p);
			if (source.getEntity() instanceof ServerPlayer attacker) markCombat(attacker);
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
			LAST_COMBAT.remove(handler.getPlayer().getUUID()));

		// Enchant rules (table + anvil): no Breach/Density on maces, no Unbreaking on shields,
		// and the secret: Breach is allowed on axes.
		EnchantmentEvents.ALLOW_ENCHANTING.register((enchantment, target, context) -> {
			if (target.is(Items.MACE) && (enchantment.is(Enchantments.BREACH) || enchantment.is(Enchantments.DENSITY)))
				return TriState.FALSE;
			if (target.is(Items.SHIELD) && enchantment.is(Enchantments.UNBREAKING))
				return TriState.FALSE;
			if (target.is(ItemTags.AXES) && enchantment.is(Enchantments.BREACH))
				return TriState.TRUE;
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
			p.getCooldowns().addCooldown(stack, RIPTIDE_COOLDOWN);
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

	public static ItemStack longWeavingPotion() {
		ItemStack s = new ItemStack(Items.POTION);
		s.set(DataComponents.POTION_CONTENTS, new PotionContents(
			Optional.empty(), Optional.empty(),
			List.of(new MobEffectInstance(MobEffects.WEAVING, WEAVING_DURATION)),
			Optional.of("weaving")));
		return s;
	}
}
