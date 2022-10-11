package luckytntlib.util.explosions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface IForEachBlockExplosionEffect {

	public void doBlockExplosion(Level level, BlockPos pos, BlockState state, double distance);
	
}
