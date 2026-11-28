package net.xansidev;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

public final class AfterlifeCommands {
	private AfterlifeCommands() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("afterlife")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.literal("cooldown")
				.executes(ctx -> show(ctx.getSource()))
				.then(setter("mace"))
				.then(setter("lunge"))
				.then(setter("riptide"))
				.then(Commands.literal("get")
					.then(Commands.argument("player", EntityArgument.player())
						.executes(ctx -> get(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
				.then(Commands.literal("reset")
					.then(Commands.argument("player", EntityArgument.player())
						.executes(ctx -> reset(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))))
			.then(Commands.literal("tag")
				.then(Commands.literal("godmace")
					.executes(ctx -> tagGodMace(ctx.getSource(), ctx.getSource().getPlayerOrException()))
					.then(Commands.argument("player", EntityArgument.player())
						.executes(ctx -> tagGodMace(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
				.then(Commands.literal("clear")
					.executes(ctx -> {
						Afterlife.clearGodMaceId();
						ctx.getSource().sendSuccess(() -> Component.literal("God mace invalidated"), true);
						return 1;
					}))
				.then(Commands.literal("devtag")
					.then(Commands.argument("player", EntityArgument.player())
						.then(Commands.literal("clear")
							.executes(ctx -> {
								ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
								DevTag.clear(ctx.getSource().getServer(), p);
								ctx.getSource().sendSuccess(() -> Component.literal("Cleared tag for " + p.getGameProfile().name()), true);
								return 1;
							}))
						.then(Commands.argument("text", StringArgumentType.greedyString())
							.executes(ctx -> {
								ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
								String text = StringArgumentType.getString(ctx, "text");
								DevTag.set(ctx.getSource().getServer(), p, text);
								ctx.getSource().sendSuccess(() -> Component.literal("Tag for " + p.getGameProfile().name() + ": ").append(DevTag.parse(text)), true);
								return 1;
							}))))));
	}

	private static int tagGodMace(CommandSourceStack src, ServerPlayer p) {
		ItemStack held = p.getMainHandItem();
		if (!held.is(Items.MACE)) {
			src.sendFailure(Component.literal(p.getGameProfile().name() + " isn't holding a mace"));
			return 0;
		}
		Afterlife.makeGodMace(held, src.registryAccess());
		src.sendSuccess(() -> Component.literal("That mace is now the one and only god mace"), true);
		return 1;
	}

	private static LiteralArgumentBuilder<CommandSourceStack> setter(String which) {
		return Commands.literal(which)
			.then(Commands.argument("seconds", IntegerArgumentType.integer(0, 3600))
				.executes(ctx -> set(ctx.getSource(), which, IntegerArgumentType.getInteger(ctx, "seconds"))));
	}

	private static int show(CommandSourceStack src) {
		AfterlifeConfig c = AfterlifeConfig.get();
		src.sendSuccess(() -> Component.literal("Afterlife cooldowns (0 = off): mace " + c.maceCooldownSeconds
			+ "s, lunge " + c.lungeCooldownSeconds + "s, riptide " + c.riptideCooldownSeconds + "s"), false);
		return 1;
	}

	private static int set(CommandSourceStack src, String which, int seconds) {
		AfterlifeConfig c = AfterlifeConfig.get();
		switch (which) {
			case "mace" -> c.maceCooldownSeconds = seconds;
			case "lunge" -> c.lungeCooldownSeconds = seconds;
			case "riptide" -> c.riptideCooldownSeconds = seconds;
		}
		AfterlifeConfig.save();
		src.sendSuccess(() -> Component.literal("Set " + which + " cooldown to " + seconds + "s"
			+ (seconds == 0 ? " (disabled)" : "")), true);
		return 1;
	}

	// Minecraft only exposes cooldowns as a percentage, so remaining time is an estimate
	// based on the currently configured length.
	private static int get(CommandSourceStack src, ServerPlayer p) {
		AfterlifeConfig c = AfterlifeConfig.get();
		ItemCooldowns cd = p.getCooldowns();

		String mace = remaining(cd, new ItemStack(Items.MACE), c.maceCooldownSeconds);
		String trident = remaining(cd, new ItemStack(Items.TRIDENT), c.riptideCooldownSeconds);

		String lunge = "no lunge item";
		var inv = p.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (!s.isEmpty() && Afterlife.hasEnchant(s, Enchantments.LUNGE)) {
				lunge = remaining(cd, s, c.lungeCooldownSeconds);
				break;
			}
		}

		String line = p.getGameProfile().name() + " -> mace: " + mace + ", riptide: " + trident + ", lunge: " + lunge;
		src.sendSuccess(() -> Component.literal(line), false);
		return 1;
	}

	private static String remaining(ItemCooldowns cd, ItemStack stack, int totalSeconds) {
		float pct = cd.getCooldownPercent(stack, 0.0f);
		if (pct <= 0.0f) return "ready";
		return "~" + String.format("%.1f", pct * totalSeconds) + "s left";
	}

	private static int reset(CommandSourceStack src, ServerPlayer p) {
		ItemCooldowns cd = p.getCooldowns();
		cd.removeCooldown(BuiltInRegistries.ITEM.getKey(Items.MACE));
		cd.removeCooldown(BuiltInRegistries.ITEM.getKey(Items.TRIDENT));

		var inv = p.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (!s.isEmpty() && Afterlife.hasEnchant(s, Enchantments.LUNGE)) {
				cd.removeCooldown(BuiltInRegistries.ITEM.getKey(s.getItem()));
			}
		}
		src.sendSuccess(() -> Component.literal("Cleared mace, riptide and lunge cooldowns for "
			+ p.getGameProfile().name()), true);
		return 1;
	}
}