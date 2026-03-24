package luckytntlib.util.explosions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public interface BlockExplosionEffect {

	public void handleBlock(Level level, Vec3 center, BlockPos pos, BlockState state);
}
