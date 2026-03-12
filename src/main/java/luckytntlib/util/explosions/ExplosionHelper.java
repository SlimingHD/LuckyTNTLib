package luckytntlib.util.explosions;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

import org.joml.Vector3f;

import luckytntlib.network.ClientboundSetupExplosionPacket;
import luckytntlib.network.ClientboundUpdateChunkSectionPacket;
import luckytntlib.network.PacketHandler;
import luckytntlib.util.explosions.rules.CraterExplosionRule;
import luckytntlib.util.explosions.rules.ExplosionRule;
import luckytntlib.util.light.LightUpdateHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/**
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
	 * Gets blocks contained in a specified sphere around a center position and edits them according to the given {@link IForEachBlockExplosionEffect}. <br>
	 * Removing blocks in a spherical shape will be faster with {@link #createSphericalCrater(Level, Vec3, int, int)}. <br>
	 * If you want to change blocks instead of removing them using this method is required.
	 * @param level  the current level
	 * @param position  the center position of the spherical explosion
	 * @param radius  the radius of the sphere
	 * @param blockEffect  determines what should happen to the blocks gotten by this function
	 * 
	 * @see #createSphericalCrater(Level, Vec3, int, int)
	 */
	public static void doSphericalExplosion(Level level, Vec3 position, int radius, IForEachBlockExplosionEffect blockEffect) {
		doModifiedSphericalExplosion(level, position, radius, new Vec3(1, 1, 1), blockEffect);
	}

	/**
	 * Gets blocks contained in a specified sphere around a center position and edits them according to the given {@link IForEachBlockExplosionEffect}. <br>
	 * The sphere can be scaled in all the 3 directions individually. <br>
	 * Removing blocks in a modified spherical shape will be faster with {@link #createModifiedSphericalCrater(Level, Vec3, int, Vector3f, int)}. <br>
	 * If you want to change blocks instead of removing them using this method is required.
	 * @param level  the current level
	 * @param position  the center position of the spherical explosion
	 * @param radius  the radius of the sphere
	 * @param scaling  the scaling of the sphere
	 * @param blockEffect  determines what should happen to the blocks gotten by this function
	 * 
	 * @see #createModifiedSphericalCrater(Level, Vec3, int, Vector3f, int)
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
	 * Gets blocks contained in a specified cube around a center position and edits them according to the given {@link IForEachBlockExplosionEffect}. <br>
	 * Removing blocks in a cubical shape will be faster with {@link #createCubicalCrater(Level, Vec3, int, int)}. <br>
	 * If you want to change blocks instead of removing them using this method is required.
	 * @param level  the current level
	 * @param position  the center position of the cubical explosion
	 * @param radius  the radius of the cube
	 * @param blockEffect  determines what should happen to the blocks gotten by this function
	 * 
	 * @see #createCubicalCrater(Level, Vec3, int, int)
	 */
	public static void doCubicalExplosion(Level level, Vec3 position, int radius, IForEachBlockExplosionEffect blockEffect) {
		doCuboidExplosion(level, position, new Vec3(radius, radius, radius), blockEffect);
	}
	
	/**
	 * Gets blocks contained in a specified cuboid around a center position and edits them according to the given {@link IForEachBlockExplosionEffect}. <br>
	 * Removing blocks in a cuboid shape will be faster with {@link #createCuboidCrater(Level, Vec3, int, Vector3f, int)}. <br>
	 * If you want to change blocks instead of removing them using this method is required.
	 * @param level  the current level
	 * @param position  the center position of the cubical explosion
	 * @param radii  a {@link Vec3} containing the radii for the x, y and z directions
	 * @param blockEffect  determines what should happen to the blocks gotten by this function
	 * 
	 * @see #createCuboidCrater(Level, Vec3, int, Vector3f, int)
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
	 * Gets blocks contained in a specified cylinder around a center position and edits them according to the given {@link IForEachBlockExplosionEffect}. <br>
	 * Removing blocks in a cylindrical shape will be faster with {@link #createCylindricalCrater(Level, Vec3, int, int, int)}. <br>
	 * If you want to change blocks instead of removing them using this method is required.
	 * @param level  the current level
	 * @param position  the center position of the cubical explosion
	 * @param radius  the radius of the x and z dimensions of the cylinder
	 * @param radiusY  the radius of the y dimension of the cylinder
	 * @param blockEffect  determines what should happen to the blocks gotten by this function
	 * 
	 * @see #createCylindricalCrater(Level, Vec3, int, int, int)
	 * @see #createModifiedCylindricalCrater(Level, Vec3, int, int, Vector3f, int)
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
	 * Gets only the top most blocks in a sphere and edits them according to the given {@link IForEachBlockExplosionEffect}. <br>
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
	 * Gets only the top most blocks in a sphere and edits them according to the given {@link IForEachBlockExplosionEffect}. <br>
	 * The function goes from top to bottom and the block above the first block that is not air is considered the top most block. <br>
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
	 * Gets all the top blocks in a sphere and edits them according to the given {@link IForEachBlockExplosionEffect}. <br>
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
	
	/**
	 * Removes all the blocks in a sphere around the given center position. <br>
	 * If you want to have control over what happens to the blocks use {@link #doSphericalExplosion(Level, Vec3, int, IForEachBlockExplosionEffect)} instead.
	 * @param level  the current level
	 * @param position  the center position of the spherical explosion
	 * @param radius  the radius of the sphere
	 * @param maxResistance  blocks with an explosion resistance lower or equal to this value will be removed, all other blocks will be untouched
	 * 
	 * @see #doSphericalExplosion(Level, Vec3, int, IForEachBlockExplosionEffect)
	 */
	public static void createSphericalCrater(Level level, Vec3 position, int radius, int maxResistance) {
		createModifiedSphericalCrater(level, position, radius, new Vector3f(1, 1, 1), maxResistance);
	}
	
	/**
	 * Removes all the blocks in a sphere around the given center position. <br>
	 * The sphere can be scaled on all axes individually. <br>
	 * If you want to have control over what happens to the blocks use {@link #doModifiedSphericalExplosion(Level, Vec3, int, Vec3, IForEachBlockExplosionEffect)} instead.
	 * @param level  the current level
	 * @param position  the center position of the spherical explosion
	 * @param radius  the radius of the sphere
	 * @param scaling  a {@link Vector3f} containing the scaling for all axes
	 * @param maxResistance  blocks with an explosion resistance lower or equal to this value will be removed, all other blocks will be untouched
	 * 
	 * @see #doModifiedSphericalExplosion(Level, Vec3, int, Vec3, IForEachBlockExplosionEffect)
	 */
	public static void createModifiedSphericalCrater(Level level, Vec3 position, int radius, Vector3f scaling, int maxResistance) {
		DistanceCalculator calc = (x, z, r, s) -> (int)(Math.sqrt(r * r - x * x / s.x - z * z / s.z) * Math.sqrt(s.y));
		createCrater(level, position, radius, scaling, maxResistance, calc, new CraterExplosionRule());
	}
	
	/**
	 * Removes all the blocks in a cube around the given center position. <br>
	 * If you want to have control over what happens to the blocks use {@link #doCubicalExplosion(Level, Vec3, int, IForEachBlockExplosionEffect)} instead.
	 * @param level  the current level
	 * @param position  the center position of the cubical explosion
	 * @param radius  the radius of the sphere
	 * @param maxResistance  blocks with an explosion resistance lower or equal to this value will be removed, all other blocks will be untouched
	 * 
	 * @see #doCubicalExplosion(Level, Vec3, int, IForEachBlockExplosionEffect)
	 */
	public static void createCubicalCrater(Level level, Vec3 position, int radius, int maxResistance) {
		createCuboidCrater(level, position, radius, new Vector3f(1, 1, 1), maxResistance);
	}
	
	/**
	 * Removes all the blocks in a cuboid around the given center position. <br>
	 * The cuboid can be scaled on all axes individually. <br>
	 * If you want to have control over what happens to the blocks use {@link #doCuboidExplosion(Level, Vec3, Vec3, IForEachBlockExplosionEffect)} instead.
	 * @param level  the current level
	 * @param position  the center position of the cuboid explosion
	 * @param radius  the radius of the sphere
	 * @param scaling  a {@link Vector3f} containing the scaling for all axes
	 * @param maxResistance  blocks with an explosion resistance lower or equal to this value will be removed, all other blocks will be untouched
	 * 
	 * @see #doCuboidExplosion(Level, Vec3, Vec3, IForEachBlockExplosionEffect)
	 */
	public static void createCuboidCrater(Level level, Vec3 position, int radius, Vector3f scaling, int maxResistance) {
		DistanceCalculator calc = (x, z, r, s) -> Math.abs(x) <= r * s.x && Math.abs(z) <= r * s.z ? (int)(r * s.y) : 0;
		createCrater(level, position, radius, scaling, maxResistance, calc, new CraterExplosionRule());
	}
	
	/**
	 * Removes all the blocks in a cylinder around the given center position. <br>
	 * If you want to have control over what happens to the blocks use {@link #doCylindricalExplosion(Level, Vec3, int, int, IForEachBlockExplosionEffect)} instead.
	 * @param level  the current level
	 * @param position  the center position of the cylindrical explosion
	 * @param radiusXZ  the radius for the circle that represents the base area of the cylinder
	 * @param radiusY  half the height of the cylinder
	 * @param maxResistance  blocks with an explosion resistance lower or equal to this value will be removed, all other blocks will be untouched
	 * 
	 * @see #doCylindricalExplosion(Level, Vec3, int, int, IForEachBlockExplosionEffect)
	 */
	public static void createCylindricalCrater(Level level, Vec3 position, int radiusXZ, int radiusY, int maxResistance) {
		createModifiedCylindricalCrater(level, position, radiusXZ, radiusY, new Vector3f(1, 1, 1), maxResistance);
	}
	
	/**
	 * Removes all the blocks in a modified cylinder around the given center position. <br>
	 * If you want to have control over what happens to the blocks use {@link #doCylindricalExplosion(Level, Vec3, int, int, IForEachBlockExplosionEffect)} instead. <br>
	 * Be aware that {@link #doCylindricalExplosion(Level, Vec3, int, int, IForEachBlockExplosionEffect)} doesn't allow for scaling.
	 * @param level  the current level
	 * @param position  the center position of the cylindrical explosion
	 * @param radiusXZ  the radius for the circle that represents the base area of the cylinder
	 * @param radiusY  half the height of the cylinder
	 * @param scaling  a {@link Vector3f} containing the scaling for all axes
	 * @param maxResistance  blocks with an explosion resistance lower or equal to this value will be removed, all other blocks will be untouched
	 * 
	 * <br><br> 
	 * 
	 * <strong><i><font color="#B50000"> THE METHOD LISTED BELOW DOES NOT SUPPORT SCALING </font></i></strong>
	 * @see #doCylindricalExplosion(Level, Vec3, int, int, IForEachBlockExplosionEffect)
	 */
	public static void createModifiedCylindricalCrater(Level level, Vec3 position, int radiusXZ, int radiusY, Vector3f scaling, int maxResistance) {
		DistanceCalculator calc = (x, z, r, s) -> Math.sqrt(x * x / s.x + z * z / s.z) <= radiusXZ ? (int)(radiusY * s.y) : 0;
		createCrater(level, position, radiusXZ, scaling, maxResistance, calc, new CraterExplosionRule());
	}
	
	/**
	 * Removes all the blocks in a shape determined by the given {@link DistanceCalculator}.
	 * @param level  the current level
	 * @param position  the center position of the crater
	 * @param radius  the radius of the crater in blocks
	 * @param scaling  a {@link Vector3f} containing the scaling for all axes
	 * @param maxResistance  blocks with an explosion resistance lower or equal to this value will be removed, all other blocks will be untouched
	 * @param calculator  a {@link DistanceCalculator} that determines the shape of the crater as explained at {@link DistanceCalculator#getMaxYDistance(int, int, int, Vector3f)}
	 * @param rule  a {@link ExplosionRule} that determines the {@link BlockState} that will replace any position affected by the explosion
	 * 
	 * <br> <br>
	 * 
	 * @see <i> Standard implementations for this method: </i>
	 * @see #createSphericalCrater(Level, Vec3, int, int)
	 * @see #createModifiedSphericalCrater(Level, Vec3, int, Vector3f, int)
	 * @see #createCubicalCrater(Level, Vec3, int, int)
	 * @see #createCuboidCrater(Level, Vec3, int, Vector3f, int)
	 * @see #createCylindricalCrater(Level, Vec3, int, int, int)
	 * @see #createModifiedCylindricalCrater(Level, Vec3, int, int, Vector3f, int)
	 * <br>
	 * @see DistanceCalculator
	 */
	@SuppressWarnings("deprecation")
	public static void createCrater(Level level, Vec3 position, int radius, Vector3f scaling, int maxResistance, DistanceCalculator calculator, ExplosionRule rule) {
		long time = System.currentTimeMillis();
		if (level instanceof ServerLevel server) {
			ImprovedExplosion dummyExplosion = ImprovedExplosion.dummyExplosion(server);
			HashMap<LevelChunk, BitSet> chunks = new HashMap<LevelChunk, BitSet>();
			ChunkPos pos = new ChunkPos(Mth.floor(position.x) >> 4, Mth.floor(position.z) >> 4);
			BlockPos center = new BlockPos(Mth.floor(position.x), Mth.floor(position.y), Mth.floor(position.z));
			
			PacketHandler.CHANNEL.send(PacketDistributor.DIMENSION.with(() -> server.dimension()), new ClientboundSetupExplosionPacket(rule, center));
			
			float scale = Math.max(scaling.x, scaling.z);
			int chunkRadius = (int)Math.ceil((float)(radius * scale) / 16f) + 1;
			for (int x = -chunkRadius; x <= chunkRadius; x++) {
				for (int z = -chunkRadius; z <= chunkRadius; z++) {
					ChunkPos chunkPos = new ChunkPos(pos.x + x, pos.z + z);
					LevelChunk chunk = server.getChunk(chunkPos.x, chunkPos.z);
					
					chunk.setLoaded(true);

					boolean chunkEdited = false;
					int height = server.getMinBuildHeight();

					for (LevelChunkSection section : chunk.getSections()) {
						if (section.hasOnlyAir()) {
							height += 16;
							continue;
						}

						PalettedContainer<BlockState> states = section.getStates();
						List<Short> changed = new ArrayList<>();
						boolean sectionChanged = false;

						for (byte i = 0; i < 16; i++) {
							for (byte k = 0; k < 16; k++) {
								int dx = chunkPos.getBlockX(i) - center.getX();
								int dz = chunkPos.getBlockZ(k) - center.getZ();
								int dyMax = calculator.getMaxYDistance(dx, dz, radius, scaling);
								for (byte j = 0; j < 16; j++) {
									int dy = height + j - center.getY();
									if (-dyMax < dy && dy < dyMax) {
										BlockState state = states.get(i, j, k);
										if (state.getBlock().getExplosionResistance() <= maxResistance && rule.shouldApply(level, state, position, dx, dy, dz)) {
											states.set(i, j, k, rule.getState());

											BlockPos blockpos = new BlockPos((chunkPos.x << 4) + i, height + j, (chunkPos.z << 4) + k);
											state.getBlock().wasExploded(server, blockpos, dummyExplosion);
											chunk.removeBlockEntity(blockpos);

											changed.add(encodeSectionPos(i, j, k));

											if (!chunkEdited) {
												chunkEdited = true;
												chunks.put(chunk, new BitSet());
											}
											if (!sectionChanged) {
												sectionChanged = true;
												chunks.get(chunk).set(height / 16 - (server.getMinSection() - 1));
											}
										}
									}
								}
							}
						}
						if (!changed.isEmpty()) {
							if(changed.size() == 4096) {
								PacketHandler.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), new ClientboundUpdateChunkSectionPacket(SectionPos.of(chunkPos, height / 16 - chunk.getMinSection()), new ArrayList<>(0), true, false));
							} else {
								PacketHandler.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), new ClientboundUpdateChunkSectionPacket(SectionPos.of(chunkPos, height / 16 - chunk.getMinSection()), changed, false, false));
							}
						}

						if (sectionChanged) {
							section.recalcBlockCounts();
						}

						height += 16;
					}
					chunk.setUnsaved(true);
				}
			}
			
			LightUpdateHelper.updateDirectSkyLight(server, chunks);
			LightUpdateHelper.updateIndirectSkyLight(server, chunks);
			
			server.save(null, false, false);
			
			System.out.println("total time: " + (System.currentTimeMillis() - time));
		}
	}
	
	/**
	 * Encodes 3 {@code shorts} between 0 and 15 into a single short.
	 * @param x  the x value to be encoded
	 * @param y  the y value to be encoded
	 * @param z  the z value to be encoded
	 * @return a {@code short} that has all three values encoded into it
	 * 
	 * @see #decodeSectionPos(short)
	 */
	public static short encodeSectionPos(short x, short y, short z) {
		return (short)((x << 8) | (y << 4) | z);
	}
	
	/**
	 * Decodes a given {@code short} encoded by {@link #encodeSectionPos(short, short, short)} into 3 {@code shorts} contained in a {@link Vec3i}.
	 * @param decode  the {@code short} to be decoded
	 * @return a {@link Vec3i} that contains all 3 decoded values
	 * 
	 * @see #encodeSectionPos(short, short, short)
	 */
	public static Vec3i decodeSectionPos(short decode) {
		return new Vec3i((decode & 3840) >> 8, (decode & 240) >> 4, (decode & 15));
	}
}
