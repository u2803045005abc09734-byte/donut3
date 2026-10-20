package com.donutclient.module;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class SpawnerFinder extends FinderModule {
	private final Target monster = new Target("Monster Spawners", "Highlight regular monster spawners.", true, 0xFF4D4D);
	private final Target trial = new Target("Trial Spawners", "Highlight trial spawners.", true, 0x5EEAD4);

	public SpawnerFinder() {
		super("SpawnerFinder", "Monster & trial spawners", "Spawners", Icon.GRID);
	}

	@Override
	protected Target classify(Block block, BlockState state) {
		if (block == Blocks.SPAWNER) return monster;
		if (block == Blocks.TRIAL_SPAWNER) return trial;
		return null;
	}
}
