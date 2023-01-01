package luckytntlib.util.explosions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface IBlockExplosionCondition {

	public boolean conditionMet(Level level, BlockPos pos, BlockState state, double distance);
	
}
