package com.donutclient.module;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;

public class StorageFinder extends FinderModule {
	private final Target chests = new Target("Chests", "Highlight normal chests (double chests as one box).", true, 0xF5A623);
	private final Target trapped = new Target("Trapped Chests", "Highlight trapped chests.", true, 0xFF5A5A);
	private final Target shulkers = new Target("Shulkers", "Highlight shulker boxes of every color.", true, 0xC77DFF);
	private final Target droppers = new Target("Droppers", "Highlight droppers.", true, 0x38BDF8);
	private final Target barrels = new Target("Barrels", "Highlight barrels.", false, 0xB8845A);
	private final Target enderChests = new Target("Ender Chests", "Highlight ender chests.", false, 0x2DD4BF);
	private final Target dispensers = new Target("Dispensers", "Highlight dispensers.", false, 0x60A5FA);
	private final Target hoppers = new Target("Hoppers", "Highlight hoppers.", false, 0x9CA3AF);

	public StorageFinder() {
		super("StorageFinder", "Chests, shulkers & droppers", "Storage", Icon.CHEST);
	}

	@Override
	protected Target classify(Block block, BlockState state) {
		if (block == Blocks.TRAPPED_CHEST) return trapped;
		if (block == Blocks.CHEST) return chests;
		if (block instanceof ShulkerBoxBlock) return shulkers;
		if (block == Blocks.DROPPER) return droppers;
		if (block == Blocks.BARREL) return barrels;
		if (block == Blocks.ENDER_CHEST) return enderChests;
		if (block == Blocks.DISPENSER) return dispensers;
		if (block == Blocks.HOPPER) return hoppers;
		return null;
	}
}
