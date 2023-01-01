package luckytntlib.util.explosions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ExplosionHelper {

	public static void doSphericalExplosion(Level level, Vec3 position, int radius, IForEachBlockExplosionEffect blockEffect) {
		for(int offX = -radius; offX <= radius; offX++) {
			for(int offY = -radius; offY <= radius; offY++) {
				for(int offZ = -radius; offZ <= radius; offZ++) {
					double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
					if(distance <= radius) {
						BlockPos pos = new BlockPos(position).offset(offX, offY, offZ);
						BlockState state = level.getBlockState(pos);
						blockEffect.doBlockExplosion(level, pos, state, distance);
					}
				}
			}
		}
	}

	public static void doCubicalExplosion(Level level, Vec3 position, int radius, IForEachBlockExplosionEffect blockEffect) {
		for(int offX = -radius; offX <= radius; offX++) {
			for(int offY = -radius; offY <= radius; offY++) {
				for(int offZ = -radius; offZ <= radius; offZ++) {
					double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
					BlockPos pos = new BlockPos(position).offset(offX, offY, offZ);
					BlockState state = level.getBlockState(pos);
					blockEffect.doBlockExplosion(level, pos, state, distance);
				}
			}
		}
	}
	
	public static void doCylindricalExplosion(Level level, Vec3 position, int radius, int radiusY, IForEachBlockExplosionEffect blockEffect) {
		for(int offX = -radius; offX <= radius; offX++) {
			for(int offY = -radiusY; offY <= radiusY; offY++) {
				for(int offZ = -radius; offZ <= radius; offZ++) {
					double distance = Math.sqrt(offX * offX + offZ * offZ);
					if(distance <= radius) {
						BlockPos pos = new BlockPos(position).offset(offX, offY, offZ);
						BlockState state = level.getBlockState(pos);
						blockEffect.doBlockExplosion(level, pos, state, distance);
					}
				}
			}
		}
	}
	
	public static void doTopBlockExplosion(Level level, Vec3 position, int radius, IForEachBlockExplosionEffect blockEffect) {
		for(int offX = -radius; offX <= radius; offX++) {
			for(int offZ = -radius; offZ <= radius; offZ++) {
				topToBottom: for(int offY = radius; offY >= -radius; offY--) {
					double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
					if(distance <= radius) {
						BlockPos pos = new BlockPos(position).offset(offX, offY, offZ);
						if(level.getBlockState(pos.below()).isCollisionShapeFullBlock(level, pos.below()) && (level.getBlockState(pos).isAir() || level.getBlockState(pos).canBeReplaced(new DirectionalPlaceContext(level, pos, Direction.DOWN, ItemStack.EMPTY, Direction.UP)))) {
							BlockState state = level.getBlockState(pos);
							blockEffect.doBlockExplosion(level, pos, state, distance);
							break topToBottom;
						}
					}
				}
			}
		}
	}
	
	public static void doTopBlockExplosion(Level level, Vec3 position, int radius, IBlockExplosionCondition condition, IForEachBlockExplosionEffect blockEffect) {
		for(int offX = -radius; offX <= radius; offX++) {
			for(int offZ = -radius; offZ <= radius; offZ++) {
				topToBottom: for(int offY = radius; offY >= -radius; offY--) {
					double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
					if(distance <= radius) {
						BlockPos pos = new BlockPos(position).offset(offX, offY, offZ);
						if(level.getBlockState(pos.below()).isCollisionShapeFullBlock(level, pos.below()) && (level.getBlockState(pos).isAir() || level.getBlockState(pos).canBeReplaced(new DirectionalPlaceContext(level, pos, Direction.DOWN, ItemStack.EMPTY, Direction.UP)))) {
							BlockState state = level.getBlockState(pos);
							if(condition.conditionMet(level, pos.below(), level.getBlockState(pos.below()), Math.sqrt(offX * offX + (offY-1) * (offY-1) + offZ * offZ))) {
								blockEffect.doBlockExplosion(level, pos, state, distance);
								break topToBottom;
							}
						}
					}
				}
			}
		}
	}
	
	public static void doTopBlockExplosionForAll(Level level, Vec3 position, int radius, IForEachBlockExplosionEffect blockEffect) {
		for(int offX = -radius; offX <= radius; offX++) {
			for(int offZ = -radius; offZ <= radius; offZ++) {
				for(int offY = radius; offY >= -radius; offY--) {
					double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
					if(distance <= radius) {
						BlockPos pos = new BlockPos(position).offset(offX, offY, offZ);
						if(level.getBlockState(pos.below()).isCollisionShapeFullBlock(level, pos.below()) && (level.getBlockState(pos).isAir() || level.getBlockState(pos).canBeReplaced(new DirectionalPlaceContext(level, pos, Direction.DOWN, ItemStack.EMPTY, Direction.UP)))) {
							BlockState state = level.getBlockState(pos);
							blockEffect.doBlockExplosion(level, pos, state, distance);
						}
					}
				}
			}
		}
	}
}
