package luckytntlib.util.explosions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

@FunctionalInterface
public interface BlockExplosionEffect {

	/**
	 * Handles a single block gotten from an {@link ImprovedExplosion} or {@link ExplosionHelper}.
	 * @param level the {@link Level} in which the explosion occurred
	 * @param center the origin position of the explosion
	 * @param pos the position of the block currently being affected
	 * @param state the {@link BlockState} of the block currently being affected
	 */
	public void handleBlock(Level level, Vec3 center, BlockPos pos, BlockState state);
}
