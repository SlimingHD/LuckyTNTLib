package luckytntlib.util;

import java.util.ArrayList;
import java.util.List;

import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ExplosionHelper {
	
	public static ExplosionHelper INSTANCE = new ExplosionHelper();
	
	public static List<BlockState> getBlockStatesWithin(int size, Level level, BlockPos center) {
		List<BlockState> states = new ArrayList<>();
		for(int x = -size; x <= size; x++) {
			for(int y = -size; y <= size; y++) {
				for(int z = -size; z <= size; z++) {
					BlockPos pos = new BlockPos(center.getX() + x, center.getY() + y, center.getZ() + z);
					states.add(level.getBlockState(pos));
				}
			}
		}
		return states;
	}
	
	public static List<Pair<BlockState, BlockPos>> getBlocksWithin(int size, Level level, BlockPos center) {
		List<Pair<BlockState, BlockPos>> states = new ArrayList<>();
		for(int x = -size; x <= size; x++) {
			for(int y = -size; y <= size; y++) {
				for(int z = -size; z <= size; z++) {
					BlockPos pos = new BlockPos(center.getX() + x, center.getY() + y, center.getZ() + z);
					states.add(new Pair<BlockState, BlockPos>(level.getBlockState(pos), pos));
				}
			}
		}
		return states;
	}
	
	public static List<? extends Entity> getEntitiesOfClassWithin(Class<? extends Entity> entity, int size, Level level, Vec3 pos) {
		return level.getEntitiesOfClass(entity, new AABB(pos.x - size, pos.y - size, pos.z - size, pos.x + size, pos.y + size, pos.z + size));
	}
}
