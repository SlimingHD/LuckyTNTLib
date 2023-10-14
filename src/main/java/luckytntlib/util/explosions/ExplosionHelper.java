package luckytntlib.util.explosions;

import java.util.HashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 
 * The ExplosionHelper offers many basic functions that help you get or edit large ares of blocks in the current {@link Level}.
 * This includes simple spherical functions, but also more complex methods like getting the top most block in a sphere.
 * On top of that it also allows for easy customization.
 */
public class ExplosionHelper {

	/**
	 * Gets all blocks in a specified sphere and returns them in a HashMap consisting of {@link BlockPos} and {@link BlockState}
	 * @param level  the current level
	 * @param position  the center position of the sphere
	 * @param radius  the radius of the sphere
	 * @return a {@link HashMap} of {@link BlockPos} and {@link BlockState}
	 */
	public static HashMap<BlockPos, BlockState> getBlocksInSphere(Level level, Vec3 position, int radius) {
		HashMap<BlockPos, BlockState> blocks = new HashMap<>();
		for(int offX = -radius; offX <= radius; offX++) {
			for(int offY = radius; offY >= -radius; offY--) {
				for(int offZ = -radius; offZ <= radius; offZ++) {
					double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
					if(distance <= radius) {
						BlockPos pos = new BlockPos((int)position.x, (int)position.y, (int)position.z).offset(offX, offY, offZ);
						BlockState state = level.getBlockState(pos);
						blocks.put(pos, state);
					}
				}
			}
		}
		return blocks;
	}
	
	/**
	 * Gets all blocks in a specified cuboid and returns them in a HashMap consisting of {@link BlockPos} and {@link BlockState}
	 * @param level  the current level
	 * @param position  the center position of the cuboid
	 * @param radii  the radii for the x, y and z directions
	 * @return a {@link HashMap} of {@link BlockPos} and {@link BlockState}
	 */
	public static HashMap<BlockPos, BlockState> getBlocksInCuboid(Level level, Vec3 position, Vec3 radii) {
		HashMap<BlockPos, BlockState> blocks = new HashMap<>();
		for(int offX = (int)-radii.x; offX <= (int)radii.x; offX++) {
			for(int offY = (int)radii.y; offY >= (int)-radii.y; offY--) {
				for(int offZ = (int)-radii.z; offZ <= (int)radii.z; offZ++) {
					BlockPos pos = new BlockPos((int)position.x, (int)position.y, (int)position.z).offset(offX, offY, offZ);
					BlockState state = level.getBlockState(pos);
					blocks.put(pos, state);
				}
			}
		}
		return blocks;
	}
	
	/**
	 * Gets all blocks in a specified cylinder and returns them in a HashMap consisting of {@link BlockPos} and {@link BlockState}
	 * @param level  the current level
	 * @param position  the center position of the cylinder
	 * @param radius  the radius of the cylinder
	 * @return a {@link HashMap} of {@link BlockPos} and {@link BlockState}
	 */
	public static HashMap<BlockPos, BlockState> getBlocksInCylinder(Level level, Vec3 position, int radius, int radiusY) {
		HashMap<BlockPos, BlockState> blocks = new HashMap<>();
		for(int offX = -radius; offX <= radius; offX++) {
			for(int offY = radiusY; offY >= -radiusY; offY--) {
				for(int offZ = -radius; offZ <= radius; offZ++) {
					double distance = Math.sqrt(offX * offX + offZ * offZ);
					if(distance <= radius) {
						BlockPos pos = new BlockPos((int)position.x, (int)position.y, (int)position.z).offset(offX, offY, offZ);
						BlockState state = level.getBlockState(pos);
						blocks.put(pos, state);
					}
				}
			}
		}
		return blocks;
	}
	
	/**
	 * Gets blocks contained in a specified sphere around a center position and edits them according to the given blockEffect.
	 * @param level  the current level
	 * @param position  the center position of the spherical explosion
	 * @param radius  the radius of the sphere
	 * @param blockEffect  determines what should happen to the blocks gotten by this function
	 */
	public static void doSphericalExplosion(Level level, Vec3 position, int radius, IForEachBlockExplosionEffect blockEffect) {
		for(int offX = -radius; offX <= radius; offX++) {
			for(int offY = radius; offY >= -radius; offY--) {
				for(int offZ = -radius; offZ <= radius; offZ++) {
					double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
					if(distance <= radius) {
						BlockPos pos = new BlockPos((int)position.x, (int)position.y, (int)position.z).offset(offX, offY, offZ);
						BlockState state = level.getBlockState(pos);
						blockEffect.doBlockExplosion(level, pos, state, distance);
					}
				}
			}
		}
	}

	/**
	 * Gets blocks contained in a specified sphere around a center position and edits them according to the given blockEffect.
	 * The sphere can be scaled in all the 3 directions individually.
	 * @param level  the current level
	 * @param position  the center position of the spherical explosion
	 * @param radius  the radius of the sphere
	 * @param scaling  the scaling of the sphere
	 * @param blockEffect  determines what should happen to the blocks gotten by this function
	 */
	public static void doModifiedSphericalExplosion(Level level, Vec3 position, int radius, Vec3 scaling, IForEachBlockExplosionEffect blockEffect) {
		for(double offX = -radius * scaling.x; offX <= radius * scaling.x; offX++) {
			for(double offY = radius * scaling.y; offY >= -radius * scaling.y; offY--) {
				for(double offZ = -radius * scaling.z; offZ <= radius * scaling.z; offZ++) {
					double distance = Math.sqrt(offX * offX / scaling.x + offY * offY / scaling.y + offZ * offZ / scaling.z);
					if(distance <= radius) {
						BlockPos pos = new BlockPos((int)position.x, (int)position.y, (int)position.z).offset((int)offX, (int)offY, (int)offZ);
						BlockState state = level.getBlockState(pos);
						blockEffect.doBlockExplosion(level, pos, state, distance);
					}
				}
			}
		}
	}

	/**
	 * Gets blocks contained in a specified cube around a center position and edits them according to the given blockEffect.
	 * @param level  the current level
	 * @param position  the center position of the cubical explosion
	 * @param radius  the radius of the cube
	 * @param blockEffect  determines what should happen to the blocks gotten by this function
	 */
	public static void doCubicalExplosion(Level level, Vec3 position, int radius, IForEachBlockExplosionEffect blockEffect) {
		for(int offX = -radius; offX <= radius; offX++) {
			for(int offY = -radius; offY <= radius; offY++) {
				for(int offZ = -radius; offZ <= radius; offZ++) {
					double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
					BlockPos pos = new BlockPos((int)position.x, (int)position.y, (int)position.z).offset(offX, offY, offZ);
					BlockState state = level.getBlockState(pos);
					blockEffect.doBlockExplosion(level, pos, state, distance);
				}
			}
		}
	}
	
	/**
	 * Gets blocks contained in a specified cuboid around a center position and edits them according to the given blockEffect.
	 * @param level  the current level
	 * @param position  the center position of the cubical explosion
	 * @param radii  the radii for the x, y and z directions
	 * @param blockEffect  determines what should happen to the blocks gotten by this function
	 */
	public static void doCuboidExplosion(Level level, Vec3 position, Vec3 radii, IForEachBlockExplosionEffect blockEffect) {
		for(int offX = (int)-radii.x; offX <= (int)radii.x; offX++) {
			for(int offY = (int)-radii.y; offY <= (int)radii.y; offY++) {
				for(int offZ = (int)-radii.z; offZ <= (int)radii.z; offZ++) {
					double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
					BlockPos pos = new BlockPos((int)position.x, (int)position.y, (int)position.z).offset(offX, offY, offZ);
					BlockState state = level.getBlockState(pos);
					blockEffect.doBlockExplosion(level, pos, state, distance);
				}
			}
		}
	}
	
	/**
	 * Gets blocks contained in a specified cylinder around a center position and edits them according to the given blockEffect.
	 * @param level  the current level
	 * @param position  the center position of the cubical explosion
	 * @param radius  the radius of the x and z dimensions of the cylinder
	 * @param radiusY  the radius of the y dimension of the cylinder
	 * @param blockEffect  determines what should happen to the blocks gotten by this function
	 */
	public static void doCylindricalExplosion(Level level, Vec3 position, int radius, int radiusY, IForEachBlockExplosionEffect blockEffect) {
		for(int offX = -radius; offX <= radius; offX++) {
			for(int offY = -radiusY; offY <= radiusY; offY++) {
				for(int offZ = -radius; offZ <= radius; offZ++) {
					double distance = Math.sqrt(offX * offX + offZ * offZ);
					if(distance <= radius) {
						BlockPos pos = new BlockPos((int)position.x, (int)position.y, (int)position.z).offset(offX, offY, offZ);
						BlockState state = level.getBlockState(pos);
						blockEffect.doBlockExplosion(level, pos, state, distance);
					}
				}
			}
		}
	}
	
	/**
	 * Gets only the top most blocks in a sphere and edits them according to the given blockEffect.
	 * The function goes from top to bottom and the first block that is air or not solid and followed by a solid block below is considered the top most block.
	 * @param level  the current level
	 * @param position  the center position of the top block explosion
	 * @param radius  the radius of the sphere
	 * @param blockEffect  determines what should happen to the blocks gotten by this function
	 */
	public static void doTopBlockExplosion(Level level, Vec3 position, int radius, IForEachBlockExplosionEffect blockEffect) {
		for(int offX = -radius; offX <= radius; offX++) {
			for(int offZ = -radius; offZ <= radius; offZ++) {
				topToBottom: for(int offY = radius; offY >= -radius; offY--) {
					double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
					if(distance <= radius) {
						BlockPos pos = new BlockPos((int)position.x, (int)position.y, (int)position.z).offset(offX, offY, offZ);
						BlockState state = level.getBlockState(pos);
						if((level.getBlockState(pos.below()).isCollisionShapeFullBlock(level, pos.below()) || level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) && (state.isAir() || state.canBeReplaced(new DirectionalPlaceContext(level, pos, Direction.DOWN, ItemStack.EMPTY, Direction.UP))  || (!state.isCollisionShapeFullBlock(level, pos) && state.getExplosionResistance(level, pos, ImprovedExplosion.dummyExplosion(level)) == 0) || state.is(BlockTags.FLOWERS))) {
							blockEffect.doBlockExplosion(level, pos, state, distance);
							break topToBottom;
						}
					}
				}
			}
		}
	}
	
	/**
	 * Gets only the top most blocks in a sphere and edits them according to the given blockEffect.
	 * The function goes from top to bottom and the block above the first block that is not air is considered the top most block.
	 * If the condition is not met it will continue to search for another top block further down
	 * @param level  the current level
	 * @param position  the center position of the top block explosion
	 * @param radius  the radius of the sphere
	 * @param condition  the condition for the top block to be considered, otherwise a new block further down will be searched for
	 * @param blockEffect  determines what should happen to the blocks gotten by this function
	 */
	public static void doTopBlockExplosion(Level level, Vec3 position, int radius, IBlockExplosionCondition condition, IForEachBlockExplosionEffect blockEffect) {
		for(int offX = -radius; offX <= radius; offX++) {
			for(int offZ = -radius; offZ <= radius; offZ++) {
				topToBottom: for(int offY = radius; offY >= -radius; offY--) {
					double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
					if(distance <= radius) {
						BlockPos pos = new BlockPos((int)position.x, (int)position.y, (int)position.z).offset(offX, offY, offZ);
						BlockState state = level.getBlockState(pos);
						if(!level.getBlockState(pos.below()).isAir()) {
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
	
	/**
	 * Gets all the top blocks in a sphere and edits them according to the given blockEffect.
	 * A top block is any air or non-solid block followed by a solid block below.
	 * @param level  the current level
	 * @param position  the center position of the top block explosion
	 * @param radius  the radius of the sphere
	 * @param blockEffect  determines what should happen to the blocks gotten by this function
	 */
	public static void doTopBlockExplosionForAll(Level level, Vec3 position, int radius, IForEachBlockExplosionEffect blockEffect) {
		for(int offX = -radius; offX <= radius; offX++) {
			for(int offZ = -radius; offZ <= radius; offZ++) {
				for(int offY = radius; offY >= -radius; offY--) {
					double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
					if(distance <= radius) {
						BlockPos pos = new BlockPos((int)position.x, (int)position.y, (int)position.z).offset(offX, offY, offZ);
						BlockState state = level.getBlockState(pos);
						if((level.getBlockState(pos.below()).isCollisionShapeFullBlock(level, pos.below()) || level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) && (state.isAir() || state.canBeReplaced(new DirectionalPlaceContext(level, pos, Direction.DOWN, ItemStack.EMPTY, Direction.UP)) || (!state.isCollisionShapeFullBlock(level, pos) && state.getExplosionResistance(level, pos, ImprovedExplosion.dummyExplosion(level)) == 0) || state.is(BlockTags.FLOWERS))) {
							blockEffect.doBlockExplosion(level, pos, state, distance);
						}
					}
				}
			}
		}
	}
}
