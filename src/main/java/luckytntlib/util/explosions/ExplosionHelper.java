package luckytntlib.util.explosions;

import java.util.BitSet;
import java.util.HashMap;

import javax.annotation.Nullable;

import org.joml.Vector3f;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import luckytntlib.config.LuckyTNTLibConfigValues;
import luckytntlib.network.ClientboundSetupExplosionPacket;
import luckytntlib.network.ClientboundUpdateChunkSectionPacket;
import luckytntlib.network.PacketHandler;
import luckytntlib.util.ExplosionProfiler;
import luckytntlib.util.ExplosionProfiler.Benchmark;
import luckytntlib.util.ExplosionProfiler.Counter;
import luckytntlib.util.explosions.rules.ExplosionRule;
import luckytntlib.util.light.LightUpdateHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/**
 * ExplosionHelper is a class that provides utility methods to create non-ray-traced explosions. <br>
 * The main method is {@link #createCrater(Level, Vec3, int, Vector3f, int, DistanceCalculator, ExplosionRule)},
 * but this class also provides multiple default implementations based on geometric shapes. 
 * <p>
 * It is important to mention that {@link #createCrater(Level, Vec3, int, Vector3f, int, DistanceCalculator, ExplosionRule)}
 * as well as all the implementations are built for performance, not perfection.
 * To ensure the enormous performance boost, block updates are omitted and updates are batched and synchronized to the client manually.
 * By omitting block updates, light updates stop working altogether.
 * To increase performance on that front, light updates aren't handled by Minecraft's light engine and instead by {@link LightUpdateHelper}. 
 * <p>
 * Any method in this class annotated with {@link Deprecated} have been so in favor of the new methods.
 * We highly encourage anyone to switch to the new system as future ports for this mod to newer versions of Minecraft won't contain these methods anymore.
 * As it is always with software development, we can't foresee all possible edge cases these methods might have been or will be used for.
 * Keeping that in mind, if your specific use case isn't covered by the new system, you'll have to make do yourself.
 */
public class ExplosionHelper {

	/**
	 * {@link DistanceCalculator} for calculating a explosion crater shaped like a spheroid
	 */
	public static final DistanceCalculator SPHEROID_CALCULATOR = (x, z, r, s) -> (int)((r * r - x * x / s.x - z * z / s.z) * s.y);
	/**
	 * {@link DistanceCalculator} for calculating a explosion crater shaped like a cuboid
	 */
	public static final DistanceCalculator CUBOID_CALCULATOR = (x, z, r, s) -> Math.abs(x) <= r * s.x && Math.abs(z) <= r * s.z ? (int)(r * r * s.y * s.y) : 0;
	
	
	private ExplosionHelper() {
	}
	
	/**
	 * Removes all the blocks in a sphere around the given center position. <br>
	 * If you want to have control over what happens to the blocks use {@link #createSphericalCrater(Level, Vec3, int, int, ExplosionRule)} instead.
	 * @param level  the current level
	 * @param position  the center position of the spherical explosion
	 * @param radius  the radius of the sphere
	 * @param maxResistance  blocks with an explosion resistance lower or equal to this value will be removed, all other blocks will be untouched
	 * 
	 * @see #createSphericalCrater(Level, Vec3, int, int, ExplosionRule)
	 * @see #createSpheroidCrater(Level, Vec3, int, Vector3f, int)
	 * @see #createSpheroidCrater(Level, Vec3, int, Vector3f, int, ExplosionRule)
	 */
	public static void createSphericalCrater(Level level, Vec3 position, int radius, float maxResistance) {
		if (radius < LuckyTNTLibConfigValues.BLOCK_UPDATE_THRESHOLD.get()) {
			legacySphericalExplosion(level, position, radius, maxResistance, null);
		} else {
			createSphericalCrater(level, position, radius, maxResistance, null);
		}
	}
	
	/**
	 * Edits all the blocks in a sphere around the given center position. <br>
	 * The given {@link ExplosionRule} determines what happens to the affected blocks.
	 * @param level  the current level
	 * @param position  the center position of the spherical explosion
	 * @param radius  the radius of the sphere
	 * @param maxResistance  blocks with an explosion resistance lower or equal to this value will be removed, all other blocks will be untouched
	 * @param rule  an optional {@link ExplosionRule} that determines how affected blocks will be edited. If it's {@code null}, blocks will simply be removed.
	 * 
	 * @see #createSphericalCrater(Level, Vec3, int, int)
	 * @see #createSpheroidCrater(Level, Vec3, int, Vector3f, int)
	 * @see #createSpheroidCrater(Level, Vec3, int, Vector3f, int, ExplosionRule)
	 */
	public static void createSphericalCrater(Level level, Vec3 position, int radius, float maxResistance, @Nullable ExplosionRule rule) {
		if (radius < LuckyTNTLibConfigValues.BLOCK_UPDATE_THRESHOLD.get()) {
			legacySphericalExplosion(level, position, radius, maxResistance, rule);
		} else {
			createSpheroidCrater(level, position, radius, new Vector3f(1), maxResistance, rule);
		}
	}
	
	/**
	 * Removes all the blocks in a spheroid around the given center position. <br>
	 * The spheroid can be scaled on all axes individually. <br>
	 * If you want to have control over what happens to the blocks use {@link #createSpheroidCrater(Level, Vec3, int, Vector3f, int, ExplosionRule)} instead.
	 * @param level  the current level
	 * @param position  the center position of the spheroid explosion
	 * @param radius  the radius of the unscaled sphere
	 * @param scaling  a {@link Vector3f} containing the scaling for all axes
	 * @param maxResistance  blocks with an explosion resistance lower or equal to this value will be removed, all other blocks will be untouched
	 * 
	 * @see #createSphericalCrater(Level, Vec3, int, int)
	 * @see #createSphericalCrater(Level, Vec3, int, int, ExplosionRule)
	 * @see #createSpheroidCrater(Level, Vec3, int, Vector3f, int, ExplosionRule)
	 */
	public static void createSpheroidCrater(Level level, Vec3 position, int radius, Vector3f scaling, float maxResistance) {
		if (radius < LuckyTNTLibConfigValues.BLOCK_UPDATE_THRESHOLD.get()) {
			legacySpheroidExplosion(level, position, radius, scaling, maxResistance, null);
		} else {
			createSpheroidCrater(level, position, radius, scaling, maxResistance, null);
		}
	}
	
	/**
	 * Edits all the blocks in a spheroid around the given center position. <br>
	 * The spheroid can be scaled on all axes individually. <br>
	 * The given {@link ExplosionRule} determines what happens to the affected blocks.
	 * @param level  the current level
	 * @param position  the center position of the spheroid explosion
	 * @param radius  the radius of the unscaled sphere
	 * @param scaling  a {@link Vector3f} containing the scaling for all axes
	 * @param maxResistance  blocks with an explosion resistance lower or equal to this value will be removed, all other blocks will be untouched
	 * @param rule  an optional {@link ExplosionRule} that determines how affected blocks will be edited. If it's {@code null}, blocks will simply be removed.
	 * 
	 * @see #createSphericalCrater(Level, Vec3, int, int)
	 * @see #createSphericalCrater(Level, Vec3, int, int, ExplosionRule)
	 * @see #createSpheroidCrater(Level, Vec3, int, Vector3f, int)
	 */
	public static void createSpheroidCrater(Level level, Vec3 position, int radius, Vector3f scaling, float maxResistance, @Nullable ExplosionRule rule) {
		if (radius < LuckyTNTLibConfigValues.BLOCK_UPDATE_THRESHOLD.get()) {
			legacySpheroidExplosion(level, position, radius, scaling, maxResistance, rule);
		} else {
			createCrater(level, position, radius, scaling, maxResistance, SPHEROID_CALCULATOR, rule);
		}
	}
	
	/**
	 * Removes all the blocks in a cube around the given center position. <br>
	 * If you want to have control over what happens to the blocks use {@link #createCubicalCrater(Level, Vec3, int, int, ExplosionRule)} instead.
	 * @param level  the current level
	 * @param position  the center position of the cubical explosion
	 * @param radius  the radius of the cube
	 * @param maxResistance  blocks with an explosion resistance lower or equal to this value will be removed, all other blocks will be untouched
	 * 
	 * @see #createCubicalCrater(Level, Vec3, int, int, ExplosionRule)
	 * @see #createCuboidCrater(Level, Vec3, int, Vector3f, int)
	 * @see #createCuboidCrater(Level, Vec3, int, Vector3f, int, ExplosionRule)
	 */
	public static void createCubicalCrater(Level level, Vec3 position, int radius, float maxResistance) {
		if (radius < LuckyTNTLibConfigValues.BLOCK_UPDATE_THRESHOLD.get()) {
			legacyCubicalExplosion(level, position, radius, maxResistance, null);
		} else {
			createCubicalCrater(level, position, radius, maxResistance, null);
		}
	}
	
	/**
	 * Edits all the blocks in a cube around the given center position. <br>
	 * The given {@link ExplosionRule} determines what happens to the affected blocks.
	 * @param level  the current level
	 * @param position  the center position of the cubical explosion
	 * @param radius  the radius of the cube
	 * @param maxResistance  blocks with an explosion resistance lower or equal to this value will be removed, all other blocks will be untouched
	 * @param rule  an optional {@link ExplosionRule} that determines how affected blocks will be edited. If it's {@code null}, blocks will simply be removed.
	 * 
	 * @see #createCubicalCrater(Level, Vec3, int, int)
	 * @see #createCuboidCrater(Level, Vec3, int, Vector3f, int)
	 * @see #createCuboidCrater(Level, Vec3, int, Vector3f, int, ExplosionRule)
	 */
	public static void createCubicalCrater(Level level, Vec3 position, int radius, float maxResistance, @Nullable ExplosionRule rule) {
		if (radius < LuckyTNTLibConfigValues.BLOCK_UPDATE_THRESHOLD.get()) {
			legacyCubicalExplosion(level, position, radius, maxResistance, rule);
		} else {
			createCuboidCrater(level, position, radius, new Vector3f(1), maxResistance, rule);
		}
	}
	
	/**
	 * Removes all the blocks in a cuboid around the given center position. <br>
	 * The cuboid can be scaled on all axes individually. <br>
	 * If you want to have control over what happens to the blocks use {@link #createCuboidCrater(Level, Vec3, int, Vector3f, int, ExplosionRule)} instead.
	 * @param level  the current level
	 * @param position  the center position of the cuboid explosion
	 * @param radius  the radius of the unscaled cube
	 * @param scaling  a {@link Vector3f} containing the scaling for all axes
	 * @param maxResistance  blocks with an explosion resistance lower or equal to this value will be removed, all other blocks will be untouched
	 * 
	 * @see #createCubicalCrater(Level, Vec3, int, int)
	 * @see #createCubicalCrater(Level, Vec3, int, int, ExplosionRule)
	 * @see #createCuboidCrater(Level, Vec3, int, Vector3f, int, ExplosionRule)
	 */
	public static void createCuboidCrater(Level level, Vec3 position, int radius, Vector3f scaling, float maxResistance) {
		if (radius < LuckyTNTLibConfigValues.BLOCK_UPDATE_THRESHOLD.get()) {
			legacyCuboidExplosion(level, position, radius, scaling, maxResistance, null);
		} else {
			createCuboidCrater(level, position, radius, scaling, maxResistance, null);
		}
	}
	
	/**
	 * Edits all the blocks in a cuboid around the given center position. <br>
	 * The cuboid can be scaled on all axes individually. <br>
	 * The given {@link ExplosionRule} determines what happens to the affected blocks.
	 * @param level  the current level
	 * @param position  the center position of the cuboid explosion
	 * @param radius  the radius of the unscaled cube
	 * @param scaling  a {@link Vector3f} containing the scaling for all axes
	 * @param maxResistance  blocks with an explosion resistance lower or equal to this value will be removed, all other blocks will be untouched
	 * @param rule  an optional {@link ExplosionRule} that determines how affected blocks will be edited. If it's {@code null}, blocks will simply be removed.
	 * 
	 * @see #createCubicalCrater(Level, Vec3, int, int)
	 * @see #createCubicalCrater(Level, Vec3, int, int, ExplosionRule)
	 * @see #createCuboidCrater(Level, Vec3, int, Vector3f, int)
	 */
	public static void createCuboidCrater(Level level, Vec3 position, int radius, Vector3f scaling, float maxResistance, @Nullable ExplosionRule rule) {
		if (radius < LuckyTNTLibConfigValues.BLOCK_UPDATE_THRESHOLD.get()) {
			legacyCuboidExplosion(level, position, radius, scaling, maxResistance, rule);
		} else {
			createCrater(level, position, radius, scaling, maxResistance, CUBOID_CALCULATOR, rule);
		}
	}
	
	/**
	 * Removes all the blocks in a cylinder around the given center position. <br>
	 * If you want to have control over what happens to the blocks use {@link #createCylindricalCrater(Level, Vec3, int, int, int, ExplosionRule)} instead.
	 * @param level  the current level
	 * @param position  the center position of the cylindrical explosion
	 * @param radiusXZ  the radius for the circle that represents the base area of the cylinder
	 * @param radiusY  half the height of the cylinder
	 * @param maxResistance  blocks with an explosion resistance lower or equal to this value will be removed, all other blocks will be untouched
	 * 
	 * @see #createCylindricalCrater(Level, Vec3, int, int, int, ExplosionRule)
	 * @see #createScaledCylindricalCrater(Level, Vec3, int, int, Vector3f, int)
	 * @see #createScaledCylindricalCrater(Level, Vec3, int, int, Vector3f, int, ExplosionRule)
	 */
	public static void createCylindricalCrater(Level level, Vec3 position, int radiusXZ, int radiusY, float maxResistance) {
		if (radiusXZ < LuckyTNTLibConfigValues.BLOCK_UPDATE_THRESHOLD.get() && radiusY < LuckyTNTLibConfigValues.BLOCK_UPDATE_THRESHOLD.get()) {
			legacyCylindricalExplosion(level, position, radiusXZ, radiusY, maxResistance, null);
		} else {
			createCylindricalCrater(level, position, radiusXZ, radiusY, maxResistance, null);
		}
	}
	
	/**
	 * Edits all the blocks in a cylinder around the given center position. <br>
	 * The given {@link ExplosionRule} determines what happens to the affected blocks.
	 * @param level  the current level
	 * @param position  the center position of the cylindrical explosion
	 * @param radiusXZ  the radius for the circle that represents the base area of the cylinder
	 * @param radiusY  half the height of the cylinder
	 * @param maxResistance  blocks with an explosion resistance lower or equal to this value will be removed, all other blocks will be untouched
	 * @param rule  an optional {@link ExplosionRule} that determines how affected blocks will be edited. If it's {@code null}, blocks will simply be removed.
	 * 
	 * @see #createCylindricalCrater(Level, Vec3, int, int, int)
	 * @see #createScaledCylindricalCrater(Level, Vec3, int, int, Vector3f, int)
	 * @see #createScaledCylindricalCrater(Level, Vec3, int, int, Vector3f, int, ExplosionRule)
	 */
	public static void createCylindricalCrater(Level level, Vec3 position, int radiusXZ, int radiusY, float maxResistance, @Nullable ExplosionRule rule) {
		if (radiusXZ < LuckyTNTLibConfigValues.BLOCK_UPDATE_THRESHOLD.get() && radiusY < LuckyTNTLibConfigValues.BLOCK_UPDATE_THRESHOLD.get()) {
			legacyCylindricalExplosion(level, position, radiusXZ, radiusY, maxResistance, rule);
		} else {
			createScaledCylindricalCrater(level, position, radiusXZ, radiusY, new Vector3f(1), maxResistance, rule);
		}
	}
	
	/**
	 * Removes all the blocks in a scaled cylinder around the given center position. <br>
	 * The cylinder can be scaled on all axes individually. <br>
	 * If you want to have control over what happens to the blocks use {@link #createScaledCylindricalCrater(Level, Vec3, int, int, Vector3f, int, ExplosionRule)} instead.
	 * @param level  the current level
	 * @param position  the center position of the cylindrical explosion
	 * @param radiusXZ  the radius for the circle that represents the base area of the unscaled cylinder
	 * @param radiusY  half the height of the unscaled cylinder
	 * @param scaling  a {@link Vector3f} containing the scaling for all axes
	 * @param maxResistance  blocks with an explosion resistance lower or equal to this value will be removed, all other blocks will be untouched
	 * 
	 * @see #createCylindricalCrater(Level, Vec3, int, int, int)
	 * @see #createCylindricalCrater(Level, Vec3, int, int, int, ExplosionRule)
	 * @see #createScaledCylindricalCrater(Level, Vec3, int, int, Vector3f, int, ExplosionRule)
	 */
	public static void createScaledCylindricalCrater(Level level, Vec3 position, int radiusXZ, int radiusY, Vector3f scaling, float maxResistance) {
		if (radiusXZ < LuckyTNTLibConfigValues.BLOCK_UPDATE_THRESHOLD.get() && radiusY < LuckyTNTLibConfigValues.BLOCK_UPDATE_THRESHOLD.get()) {
			legacyScaledCylindricalExplosion(level, position, radiusXZ, radiusY, scaling, maxResistance, null);
		} else {
			createScaledCylindricalCrater(level, position, radiusXZ, radiusY, scaling, maxResistance, null);
		}
	}
	
	/**
	 * Edits all the blocks in a scaled cylinder around the given center position. <br>
	 * The cylinder can be scaled on all axes individually. <br>
	 * The given {@link ExplosionRule} determines what happens to the affected blocks.
	 * @param level  the current level
	 * @param position  the center position of the cylindrical explosion
	 * @param radiusXZ  the radius for the circle that represents the base area of the unscaled cylinder
	 * @param radiusY  half the height of the unscaled cylinder
	 * @param scaling  a {@link Vector3f} containing the scaling for all axes
	 * @param maxResistance  blocks with an explosion resistance lower or equal to this value will be removed, all other blocks will be untouched
	 * @param rule  an optional {@link ExplosionRule} that determines how affected blocks will be edited. If it's {@code null}, blocks will simply be removed.
	 * 
	 * @see #createCylindricalCrater(Level, Vec3, int, int, int)
	 * @see #createCylindricalCrater(Level, Vec3, int, int, int, ExplosionRule)
	 * @see #createScaledCylindricalCrater(Level, Vec3, int, int, Vector3f, int)
	 */
	public static void createScaledCylindricalCrater(Level level, Vec3 position, int radiusXZ, int radiusY, Vector3f scaling, float maxResistance, @Nullable ExplosionRule rule) {
		if (radiusXZ < LuckyTNTLibConfigValues.BLOCK_UPDATE_THRESHOLD.get() && radiusY < LuckyTNTLibConfigValues.BLOCK_UPDATE_THRESHOLD.get()) {
			legacyScaledCylindricalExplosion(level, position, radiusXZ, radiusY, scaling, maxResistance, rule);
		} else {
			DistanceCalculator calc = (x, z, r, s) -> Math.sqrt(x * x / s.x + z * z / s.z) <= radiusXZ ? (int)(radiusY * radiusY * s.y * s.y) : 0;
			createCrater(level, position, radiusXZ, scaling, maxResistance, calc, rule);
		}
	}
	
	/**
	 * Edits all the blocks in a shape determined by the given {@link DistanceCalculator}.
	 * @param level  the current level
	 * @param position  the center position of the crater
	 * @param radius  the radius of the crater in blocks
	 * @param scaling  a {@link Vector3f} containing the scaling for all axes
	 * @param maxResistance  blocks with an explosion resistance lower or equal to this value will be removed, all other blocks will be untouched
	 * @param calculator  a {@link DistanceCalculator} that determines the shape of the crater as explained at {@link DistanceCalculator#getMaxYDistanceSqr(int, int, int, Vector3f)}
	 * @param rule  an optional {@link ExplosionRule} that determines how affected blocks will be edited. If it's {@code null}, blocks will simply be removed.
	 * 
	 * @see <i> Standard implementations for this method: </i>
	 * @see #createSphericalCrater(Level, Vec3, int, int)
	 * @see #createSphericalCrater(Level, Vec3, int, int, ExplosionRule)
	 * @see #createSpheroidCrater(Level, Vec3, int, Vector3f, int)
	 * @see #createSpheroidCrater(Level, Vec3, int, Vector3f, int, ExplosionRule)
	 * @see #createCubicalCrater(Level, Vec3, int, int)
	 * @see #createCubicalCrater(Level, Vec3, int, int, ExplosionRule)
	 * @see #createCuboidCrater(Level, Vec3, int, Vector3f, int)
	 * @see #createCuboidCrater(Level, Vec3, int, Vector3f, int, ExplosionRule)
	 * @see #createCylindricalCrater(Level, Vec3, int, int, int)
	 * @see #createCylindricalCrater(Level, Vec3, int, int, int, ExplosionRule)
	 * @see #createScaledCylindricalCrater(Level, Vec3, int, int, Vector3f, int)
	 * @see #createScaledCylindricalCrater(Level, Vec3, int, int, Vector3f, int, ExplosionRule)
	 * @see #createScaledCylindricalCrater(Level, Vec3, int, int, Vector3f, int, ExplosionRule) <br>
	 * @see <i> Related classes: </i>
	 * @see DistanceCalculator
	 * @see ExplosionRule
	 * @see LightUpdateHelper
	 */
	@SuppressWarnings("deprecation")
	public static void createCrater(Level level, Vec3 position, int radius, Vector3f scaling, float maxResistance, DistanceCalculator calculator, @Nullable ExplosionRule rule) {
		if (level instanceof ServerLevel server) {
			ExplosionProfiler profiler = startBenchmark(level, radius);
			int checkedBlocks = 0;
			int affectedBlocks = 0;
			
			HashMap<LevelChunk, BitSet> chunks = new HashMap<LevelChunk, BitSet>();
			Long2ObjectMap<LightUpdateHelper.LightDataHolder> dataLayerCache = new Long2ObjectOpenHashMap<>();
			long worldSeed = server.getSeed();
			ChunkPos pos = new ChunkPos(Mth.floor(position.x) >> 4, Mth.floor(position.z) >> 4);
			SingleThreadedRandomSource random = new SingleThreadedRandomSource(0);
			BlockPos center = BlockPos.containing(position);
			boolean useRule = rule != null;
			
			PacketHandler.CHANNEL.send(PacketDistributor.DIMENSION.with(() -> server.dimension()), new ClientboundSetupExplosionPacket(rule, position, worldSeed));
			
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
						if (section.hasOnlyAir() && !useRule) {
							height += 16;
							continue;
						}

						PalettedContainer<BlockState> states = section.getStates();
						BitSet changed = new BitSet(4096);
						boolean sectionChanged = false;

						for (int i = 0; i < 16; i++) {
							for (int k = 0; k < 16; k++) {
								int dx = chunkPos.getBlockX(i) - center.getX();
								int dz = chunkPos.getBlockZ(k) - center.getZ();
								int dyMax = calculator.getMaxYDistanceSqr(dx, dz, radius, scaling);
								for (int j = 0; j < 16; j++) {
									int dy = height + j - center.getY();
									if (dy * dy >= dyMax) {
										continue;
									}
									
									BlockState state = states.get(i, j, k);
									if ((!useRule && state.isAir()) || Math.max(state.getBlock().getExplosionResistance(), state.getFluidState().getExplosionResistance()) > maxResistance) {
										continue;
									}
									
									if (useRule) {
										random.setSeed(explosionSeed(worldSeed, center, dx, dy, dz));
									}
									BlockState newState = useRule ? rule.getState(level, state, position, dx, dy, dz, random) : Blocks.AIR.defaultBlockState();
									++checkedBlocks;
									if (newState == null) {
										continue;
									}
									
									states.set(i, j, k, newState);

									BlockPos blockpos = new BlockPos((chunkPos.x << 4) + i, height + j, (chunkPos.z << 4) + k);
									chunk.removeBlockEntity(blockpos);

									changed.set(encodeSectionPos(i, j, k));

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
						if (!changed.isEmpty()) {
							if (changed.cardinality() == 4096) {
								PacketHandler.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), new ClientboundUpdateChunkSectionPacket(SectionPos.of(chunkPos, height / 16 - chunk.getMinSection()), new BitSet(0), true, false));
								affectedBlocks += 4096;
							} else {
								PacketHandler.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), new ClientboundUpdateChunkSectionPacket(SectionPos.of(chunkPos, height / 16 - chunk.getMinSection()), changed, false, false));
								affectedBlocks += changed.cardinality();
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
			
			profiler.startBenchmark(Benchmark.BENCHMARK_3, "luckytntlib.benchmarking.light_update_time");
			LightUpdateHelper.updateDirectSkyLight(server, chunks, dataLayerCache);
			LightUpdateHelper.updateIndirectSkyLight(server, chunks, dataLayerCache);
			profiler.stopBenchmarkTime(Benchmark.BENCHMARK_3);
			
			server.save(null, false, false);

			stopBenchmark(profiler, checkedBlocks, affectedBlocks);
		}
	}
	
	/**
	 * Edits all the blocks in a sphere around the given position according to the given {@link ExplosionRule}.
	 * This method uses the legacy way of changing the world that issues all updates when changing a block while using the new {@link ExplosionRule}. <br>
	 * It is recommended to use {@link #createCrater(Level, Vec3, int, Vector3f, int, DistanceCalculator, ExplosionRule)} or one of its default implementations for bigger explosions.
	 * @param level  the current {@link Level}
	 * @param position  the center of the explosion
	 * @param radius  the radius of the sphere
	 * @param maxResistance  blocks with an explosion resistance lower or equal to this value will be removed, all other blocks will be untouched
	 * @param rule  an optional {@link ExplosionRule} that determines how affected blocks will be edited. If it's {@code null}, blocks will simply be removed.
	 */
	public static void legacySphericalExplosion(Level level, Vec3 position, int radius, float maxResistance, @Nullable ExplosionRule rule) {
		legacySpheroidExplosion(level, position, radius, new Vector3f(1f), maxResistance, rule);
	}
	
	/**
	 * Edits all the blocks in a spheroid around the given position according to the given {@link ExplosionRule}.
	 * This method uses the legacy way of changing the world that issues all updates when changing a block while using the new {@link ExplosionRule}. <br>
	 * It is recommended to use {@link #createCrater(Level, Vec3, int, Vector3f, int, DistanceCalculator, ExplosionRule)} or one of its default implementations for bigger explosions.
	 * @param level  the current {@link Level}
	 * @param position  the center of the explosion
	 * @param radius  the radius of the unscaled sphere
	 * @param scaling  a {@link Vector3f} containing the scaling for all axes
	 * @param maxResistance  blocks with an explosion resistance lower or equal to this value will be removed, all other blocks will be untouched
	 * @param rule  an optional {@link ExplosionRule} that determines how affected blocks will be edited. If it's {@code null}, blocks will simply be removed.
	 */
	@SuppressWarnings("deprecation")
	public static void legacySpheroidExplosion(Level level, Vec3 position, int radius, Vector3f scaling, float maxResistance, @Nullable ExplosionRule rule) {
		if (level instanceof ServerLevel server) {
			ExplosionProfiler profiler = startBenchmark(level, radius);
			int checkedBlocks = 0;
			int affectedBlocks = 0;
			
			int radiusSqr = radius * radius;
			BlockPos center = BlockPos.containing(position);
			ImprovedExplosion dummy = ImprovedExplosion.dummyExplosion(level);
			SingleThreadedRandomSource random = new SingleThreadedRandomSource(0);
			long worldSeed = server.getSeed();
			boolean useRule = rule != null;
			for (int offX = (int)(-radius * scaling.x); offX <= Mth.ceil(radius * scaling.x); offX++) {
				for (int offY = (int)(-radius * scaling.y); offY <= Mth.ceil(radius * scaling.y); offY++) {
					for (int offZ = (int)(-radius * scaling.z); offZ <= Mth.ceil(radius * scaling.z); offZ++) {
						float distSqr = offX * offX / scaling.x + offY * offY / scaling.y + offZ * offZ / scaling.z ;
						if (distSqr > radiusSqr) {
							continue;
						}
						
						BlockPos pos = center.offset(offX, offY, offZ);
						BlockState state = level.getBlockState(pos);
						if (Math.max(state.getBlock().getExplosionResistance(), state.getFluidState().getExplosionResistance()) > maxResistance) {
							continue;
						}
						
						if (useRule) {
							random.setSeed(explosionSeed(worldSeed, center, offX, offY, offZ));
						}
						BlockState newState = useRule ? rule.getState(level, state, position, offX, offY, offZ, random) : Blocks.AIR.defaultBlockState();
						++checkedBlocks;
						if (newState == null) {
							continue;
						}
						
						level.setBlockAndUpdate(pos, newState);
						state.getBlock().wasExploded(level, pos, dummy);
						++affectedBlocks;
					}
				}
			}
			stopBenchmark(profiler, checkedBlocks, affectedBlocks);
		}
	}
	
	/**
	 * Edits all the blocks in a cube around the given position according to the given {@link ExplosionRule}.
	 * This method uses the legacy way of changing the world that issues all updates when changing a block while using the new {@link ExplosionRule}. <br>
	 * It is recommended to use {@link #createCrater(Level, Vec3, int, Vector3f, int, DistanceCalculator, ExplosionRule)} or one of its default implementations for bigger explosions.
	 * @param level  the current {@link Level}
	 * @param position  the center of the explosion
	 * @param radius  the radius of the cube
	 * @param maxResistance  blocks with an explosion resistance lower or equal to this value will be removed, all other blocks will be untouched
	 * @param rule  an optional {@link ExplosionRule} that determines how affected blocks will be edited. If it's {@code null}, blocks will simply be removed.
	 */
	public static void legacyCubicalExplosion(Level level, Vec3 position, int radius, float maxResistance, @Nullable ExplosionRule rule) {
		legacyCuboidExplosion(level, position, radius, new Vector3f(1f), maxResistance, rule);
	}
	
	/**
	 * Edits all the blocks in a cuboid around the given position according to the given {@link ExplosionRule}.
	 * This method uses the legacy way of changing the world that issues all updates when changing a block while using the new {@link ExplosionRule}. <br>
	 * It is recommended to use {@link #createCrater(Level, Vec3, int, Vector3f, int, DistanceCalculator, ExplosionRule)} or one of its default implementations for bigger explosions.
	 * @param level  the current {@link Level}
	 * @param position  the center of the explosion
	 * @param radius  the radius of the unscaled cuboid
	 * @param scaling  a {@link Vector3f} containing the scaling for all axes
	 * @param maxResistance  blocks with an explosion resistance lower or equal to this value will be removed, all other blocks will be untouched
	 * @param rule  an optional {@link ExplosionRule} that determines how affected blocks will be edited. If it's {@code null}, blocks will simply be removed.
	 */
	@SuppressWarnings("deprecation")
	public static void legacyCuboidExplosion(Level level, Vec3 position, int radius, Vector3f scaling, float maxResistance, @Nullable ExplosionRule rule) {
		if (level instanceof ServerLevel server) {
			ExplosionProfiler profiler = startBenchmark(level, radius);
			int checkedBlocks = 0;
			int affectedBlocks = 0;
			
			BlockPos center = BlockPos.containing(position);
			ImprovedExplosion dummy = ImprovedExplosion.dummyExplosion(level);
			SingleThreadedRandomSource random = new SingleThreadedRandomSource(0);
			long worldSeed = server.getSeed();
			boolean useRule = rule != null;
			for (int offX = (int)(-radius * scaling.x); offX <= Mth.ceil(radius * scaling.x); offX++) {
				for (int offY = (int)(-radius * scaling.y); offY <= Mth.ceil(radius * scaling.y); offY++) {
					for (int offZ = (int)(-radius * scaling.z); offZ <= Mth.ceil(radius * scaling.z); offZ++) {
						BlockPos pos = center.offset(offX, offY, offZ);
						BlockState state = level.getBlockState(pos);
						if (Math.max(state.getBlock().getExplosionResistance(), state.getFluidState().getExplosionResistance()) > maxResistance) {
							continue;
						}
						
						if (useRule) {
							random.setSeed(explosionSeed(worldSeed, center, offX, offY, offZ));
						}
						BlockState newState = useRule ? rule.getState(level, state, position, offX, offY, offZ, random) : Blocks.AIR.defaultBlockState();
						++checkedBlocks;
						if (newState == null) {
							continue;
						}

						level.setBlockAndUpdate(pos, newState);
						state.getBlock().wasExploded(level, pos, dummy);
						++affectedBlocks;
					}
				}
			}
			stopBenchmark(profiler, checkedBlocks, affectedBlocks);
		}
	}
	
	/**
	 * Edits all the blocks in a cylinder around the given position according to the given {@link ExplosionRule}.
	 * This method uses the legacy way of changing the world that issues all updates when changing a block while using the new {@link ExplosionRule}. <br>
	 * It is recommended to use {@link #createCrater(Level, Vec3, int, Vector3f, int, DistanceCalculator, ExplosionRule)} or one of its default implementations for bigger explosions.
	 * @param level  the current {@link Level}
	 * @param position  the center of the explosion
	 * @param radius  the radius of the spherical base area of the cylinder
	 * @param radiusY  half the height of the cylinder
	 * @param maxResistance  blocks with an explosion resistance lower or equal to this value will be removed, all other blocks will be untouched
	 * @param rule  an optional {@link ExplosionRule} that determines how affected blocks will be edited. If it's {@code null}, blocks will simply be removed.
	 */
	public static void legacyCylindricalExplosion(Level level, Vec3 position, int radius, int radiusY, float maxResistance, @Nullable ExplosionRule rule) {
		legacyScaledCylindricalExplosion(level, position, radius, radiusY, new Vector3f(1f), maxResistance, rule);
	}
	
	/**
	 * Edits all the blocks in a scaled cylinder around the given position according to the given {@link ExplosionRule}.
	 * This method uses the legacy way of changing the world that issues all updates when changing a block while using the new {@link ExplosionRule}. <br>
	 * It is recommended to use {@link #createCrater(Level, Vec3, int, Vector3f, int, DistanceCalculator, ExplosionRule)} or one of its default implementations for bigger explosions.
	 * @param level  the current {@link Level}
	 * @param position  the center of the explosion
	 * @param radius  the radius of the unscaled spherical base area of the cylinder
	 * @param radiusY  half the height of the unscaled cylinder
	 * @param scaling  a {@link Vector3f} containing the scaling for all axes
	 * @param maxResistance  blocks with an explosion resistance lower or equal to this value will be removed, all other blocks will be untouched
	 * @param rule  an optional {@link ExplosionRule} that determines how affected blocks will be edited. If it's {@code null}, blocks will simply be removed.
	 */
	@SuppressWarnings("deprecation")
	public static void legacyScaledCylindricalExplosion(Level level, Vec3 position, int radius, int radiusY, Vector3f scaling, float maxResistance, @Nullable ExplosionRule rule) {
		if (level instanceof ServerLevel server) {
			ExplosionProfiler profiler = startBenchmark(level, radius);
			int checkedBlocks = 0;
			int affectedBlocks = 0;
			
			int radiusSqr = radius * radius;
			BlockPos center = BlockPos.containing(position);
			ImprovedExplosion dummy = ImprovedExplosion.dummyExplosion(level);
			SingleThreadedRandomSource random = new SingleThreadedRandomSource(0);
			long worldSeed = server.getSeed();
			boolean useRule = rule != null;
			for (int offX = (int)(-radius * scaling.x); offX <= Mth.ceil(radius * scaling.x); offX++) {
				for (int offY = (int)(-radiusY * scaling.y); offY <= Mth.ceil(radiusY * scaling.y); offY++) {
					for (int offZ = (int)(-radius * scaling.z); offZ <= Mth.ceil(radius * scaling.z); offZ++) {
						float distSqr = offX * offX / scaling.x + offZ * offZ / scaling.z;
						if (distSqr > radiusSqr) {
							continue;
						}
						
						BlockPos pos = center.offset(offX, offY, offZ);
						BlockState state = level.getBlockState(pos);
						if (Math.max(state.getBlock().getExplosionResistance(), state.getFluidState().getExplosionResistance()) > maxResistance) {
							continue;
						}
						
						if (useRule) {
							random.setSeed(explosionSeed(worldSeed, center, offX, offY, offZ));
						}
						BlockState newState = useRule ? rule.getState(level, state, position, offX, offY, offZ, random) : Blocks.AIR.defaultBlockState();
						++checkedBlocks;
						if (newState == null) {
							continue;
						}

						level.setBlockAndUpdate(pos, newState);
						state.getBlock().wasExploded(level, pos, dummy);
						++affectedBlocks;
					}
				}
			}
			stopBenchmark(profiler, checkedBlocks, affectedBlocks);
		}
	}
	
	/**
	 * Edits all the blocks in a cylinder around the given position that are considered the top most surface block according to given {@link ExplosionRule}.
	 * A block is considered a surface if it has a non-collidable block above it is collidable itself. 
	 * This method uses the legacy way of changing the world that issues all updates when changing a block while using the new {@link ExplosionRule}. <br>
	 * It is recommended to use {@link #createCrater(Level, Vec3, int, Vector3f, int, DistanceCalculator, ExplosionRule)} or one of its default implementations for bigger explosions.
	 * @param level  the current {@link Level}
	 * @param position  the center of the explosion
	 * @param radius  the radius of the spherical base area of the cylinder and half the height of the cylinder
	 * @param maxResistance  blocks with an explosion resistance lower or equal to this value will be removed, all other blocks will be untouched
	 * @param rule  an optional {@link ExplosionRule} that determines how affected blocks will be edited. If it's {@code null}, blocks will simply be removed.
	 */
	@SuppressWarnings("deprecation")
	public static void legacySurfaceExplosion(Level level, Vec3 position, int radius, float maxResistance, @Nullable ExplosionRule rule) {
		if (level instanceof ServerLevel server) {
			ExplosionProfiler profiler = startBenchmark(level, radius);
			int checkedBlocks = 0;
			int affectedBlocks = 0;
			
			int radiusSqr = radius * radius;
			BlockPos center = BlockPos.containing(position);
			ImprovedExplosion dummy = ImprovedExplosion.dummyExplosion(level);
			SingleThreadedRandomSource random = new SingleThreadedRandomSource(0);
			long worldSeed = server.getSeed();
			boolean useRule = rule != null;
			for (int offX = -radius; offX <= radius; offX++) {
				for (int offZ = -radius; offZ <= radius; offZ++) {
					int distSqr = offX * offX + offZ * offZ;
					if (distSqr > radiusSqr) {
						continue;
					}
					
					for (int offY = radius; offY >= -radius; offY--) {
						BlockPos pos = center.offset(offX, offY, offZ);
						BlockState state = level.getBlockState(pos);
						if (Math.max(state.getBlock().getExplosionResistance(), state.getFluidState().getExplosionResistance()) > maxResistance) {
							continue;
						}

						BlockPos above = pos.above();
						if (state.getCollisionShape(level, pos).isEmpty() || !level.getBlockState(above).getCollisionShape(level, above).isEmpty()) {
							continue;
						}
						
						
						if (useRule) {
							random.setSeed(explosionSeed(worldSeed, center, offX, offY, offZ));
						}
						BlockState newState = useRule ? rule.getState(level, state, position, offX, offY, offZ, random) : Blocks.AIR.defaultBlockState();
						++checkedBlocks;
						if (newState == null) {
							continue;
						}

						level.setBlockAndUpdate(pos, newState);
						state.getBlock().wasExploded(level, pos, dummy);
						++affectedBlocks;
						break;
					}
				}
			}
			stopBenchmark(profiler, checkedBlocks, affectedBlocks);
		}
	}
	
	/**
	 * Edits all the blocks in a sphere around the given position according to the given {@link BlockExplosionEffect} giving you full control.
	 * This method can be used for any case where using an {@link ExplosionRule} doesn't work. <br>
	 * It is recommended to use {@link #createCrater(Level, Vec3, int, Vector3f, int, DistanceCalculator, ExplosionRule)} or one of its default implementations for bigger explosions.
	 * @param level  the current {@link Level}
	 * @param position  the center of the explosion
	 * @param radius  the radius of the sphere
	 * @param effect  a {@link BlockExplosionEffect} where you can fully customize what happens to each block
	 */
	public static void customSphericalExplosion(Level level, Vec3 position, int radius, BlockExplosionEffect effect) {
		customSpheroidExplosion(level, position, radius, new Vector3f(1f), effect);
	}
	
	/**
	 * Edits all the blocks in a spheroid around the given position according to the given {@link BlockExplosionEffect} giving you full control.
	 * This method can be used for any case where using an {@link ExplosionRule} doesn't work. <br>
	 * It is recommended to use {@link #createCrater(Level, Vec3, int, Vector3f, int, DistanceCalculator, ExplosionRule)} or one of its default implementations for bigger explosions.
	 * @param level  the current {@link Level}
	 * @param position  the center of the explosion
	 * @param radius  the radius of the unscaled spheroid
	 * @param scaling  a {@link Vector3f} containing the scaling for all axes
	 * @param effect  a {@link BlockExplosionEffect} where you can fully customize what happens to each block
	 */
	public static void customSpheroidExplosion(Level level, Vec3 position, int radius, Vector3f scaling, BlockExplosionEffect effect) {
		ExplosionProfiler profiler = startBenchmark(level, radius);
		int affectedBlocks = 0;
		
		int radiusSqr = radius * radius;
		BlockPos center = BlockPos.containing(position);
		for (int offX = (int)(-radius * scaling.x); offX <= Mth.ceil(radius * scaling.x); offX++) {
			for (int offY = (int)(-radius * scaling.y); offY <= Mth.ceil(radius * scaling.y); offY++) {
				for (int offZ = (int)(-radius * scaling.z); offZ <= Mth.ceil(radius * scaling.z); offZ++) {
					float distSqr = offX * offX / scaling.x + offY * offY / scaling.y + offZ * offZ / scaling.z;
					if (distSqr <= radiusSqr) {
						BlockPos pos = center.offset(offX, offY, offZ);
						BlockState state = level.getBlockState(pos);
						effect.handleBlock(level, position, pos, state);
						++affectedBlocks;
					}
				}
			}
		}
		stopBenchmark(profiler, affectedBlocks, affectedBlocks);
	}
	
	/**
	 * Edits all the blocks in a cube around the given position according to the given {@link BlockExplosionEffect} giving you full control.
	 * This method can be used for any case where using an {@link ExplosionRule} doesn't work. <br>
	 * It is recommended to use {@link #createCrater(Level, Vec3, int, Vector3f, int, DistanceCalculator, ExplosionRule)} or one of its default implementations for bigger explosions.
	 * @param level  the current {@link Level}
	 * @param position  the center of the explosion
	 * @param radius  the radius of the cube
	 * @param effect  a {@link BlockExplosionEffect} where you can fully customize what happens to each block
	 */
	public static void customCubicalExplosion(Level level, Vec3 position, int radius, BlockExplosionEffect effect) {
		customCuboidExplosion(level, position, radius, new Vector3f(1f), effect);
	}
	
	/**
	 * Edits all the blocks in a cuboid around the given position according to the given {@link BlockExplosionEffect} giving you full control.
	 * This method can be used for any case where using an {@link ExplosionRule} doesn't work. <br>
	 * It is recommended to use {@link #createCrater(Level, Vec3, int, Vector3f, int, DistanceCalculator, ExplosionRule)} or one of its default implementations for bigger explosions.
	 * @param level  the current {@link Level}
	 * @param position  the center of the explosion
	 * @param radius  the radius of the unscaled cuboid
	 * @param scaling  a {@link Vector3f} containing the scaling for all axes
	 * @param effect  a {@link BlockExplosionEffect} where you can fully customize what happens to each block
	 */
	public static void customCuboidExplosion(Level level, Vec3 position, int radius, Vector3f scaling, BlockExplosionEffect effect) {
		ExplosionProfiler profiler = startBenchmark(level, radius);
		int affectedBlocks = 0;
		
		BlockPos center = BlockPos.containing(position);
		for (int offX = (int)(-radius * scaling.x); offX <= Mth.ceil(radius * scaling.x); offX++) {
			for (int offY = (int)(-radius * scaling.y); offY <= Mth.ceil(radius * scaling.y); offY++) {
				for (int offZ = (int)(-radius * scaling.z); offZ <= Mth.ceil(radius * scaling.z); offZ++) {
					BlockPos pos = center.offset(offX, offY, offZ);
					BlockState state = level.getBlockState(pos);
					effect.handleBlock(level, position, pos, state);
					++affectedBlocks;
				}
			}
		}
		stopBenchmark(profiler, affectedBlocks, affectedBlocks);
	}
	
	/**
	 * Edits all the blocks in a cylinder around the given position according to the given {@link BlockExplosionEffect} giving you full control.
	 * This method can be used for any case where using an {@link ExplosionRule} doesn't work. <br>
	 * It is recommended to use {@link #createCrater(Level, Vec3, int, Vector3f, int, DistanceCalculator, ExplosionRule)} or one of its default implementations for bigger explosions.
	 * @param level  the current {@link Level}
	 * @param position  the center of the explosion
	 * @param radius  the radius of the cylinders spherical base area
	 * @param radiusY  half the height of the cylinder
	 * @param effect  a {@link BlockExplosionEffect} where you can fully customize what happens to each block
	 */
	public static void customCylindricalExplosion(Level level, Vec3 position, int radius, int radiusY, BlockExplosionEffect effect) {
		customScaledCylindricalExplosion(level, position, radius, radiusY, new Vector3f(1f), effect);
	}
	
	/**
	 * Edits all the blocks in a scaled cylinder around the given position according to the given {@link BlockExplosionEffect} giving you full control.
	 * This method can be used for any case where using an {@link ExplosionRule} doesn't work. <br>
	 * It is recommended to use {@link #createCrater(Level, Vec3, int, Vector3f, int, DistanceCalculator, ExplosionRule)} or one of its default implementations for bigger explosions.
	 * @param level  the current {@link Level}
	 * @param position  the center of the explosion
	 * @param radius  the radius of the unscaled cylinders spherical base area
	 * @param radiusY  half the height of the unscaled cylinder
	 * @param scaling  a {@link Vector3f} containing the scaling for all axes
	 * @param effect  a {@link BlockExplosionEffect} where you can fully customize what happens to each block
	 */
	public static void customScaledCylindricalExplosion(Level level, Vec3 position, int radius, int radiusY, Vector3f scaling, BlockExplosionEffect effect) {
		ExplosionProfiler profiler = startBenchmark(level, radius);
		int affectedBlocks = 0;
		
		int radiusSqr = radius * radius;
		BlockPos center = BlockPos.containing(position);
		for (int offX = (int)(-radius * scaling.x); offX <= Mth.ceil(radius * scaling.x); offX++) {
			for (int offY = (int)(-radiusY * scaling.y); offY <= Mth.ceil(radiusY * scaling.y); offY++) {
				for (int offZ = (int)(-radius * scaling.z); offZ <= Mth.ceil(radius * scaling.z); offZ++) {
					float distSqr = offX * offX / scaling.x + offZ * offZ / scaling.z;
					if (distSqr <= radiusSqr) {
						BlockPos pos = center.offset(offX, offY, offZ);
						BlockState state = level.getBlockState(pos);
						effect.handleBlock(level, position, pos, state);
						++affectedBlocks;
					}
				}
			}
		}
		stopBenchmark(profiler, affectedBlocks, affectedBlocks);
	}
	
	/**
	 * Edits all the blocks in a cylinder around the given position that are considered the top most surface block according to given {@link BlockExplosionEffect} giving you full control.
	 * A block is considered a surface if it has a non-collidable block above it is collidable itself. 
	 * This method can be used for any case where using an {@link ExplosionRule} doesn't work. <br>
	 * It is recommended to use {@link #createCrater(Level, Vec3, int, Vector3f, int, DistanceCalculator, ExplosionRule)} or one of its default implementations for bigger explosions.
	 * @param level  the current {@link Level}
	 * @param position  the center of the explosion
	 * @param radius  the radius of the spherical base area of the cylinder and half the height of the cylinder
	 * @param effect  a {@link BlockExplosionEffect} where you can fully customize what happens to each block
	 */
	public static void customSurfaceExplosion(Level level, Vec3 position, int radius, BlockExplosionEffect effect) {
		ExplosionProfiler profiler = startBenchmark(level, radius);
		int affectedBlocks = 0;
		
		int radiusSqr = radius * radius;
		BlockPos center = BlockPos.containing(position);
		for (int offX = -radius; offX <= radius; offX++) {
			for (int offZ = -radius; offZ <= radius; offZ++) {
				int distSqr = offX * offX + offZ * offZ;
				if (distSqr <= radiusSqr) {
					for (int offY = radius; offY >= -radius; offY--) {
						BlockPos pos = center.offset(offX, offY, offZ);
						BlockPos above = pos.above();
						BlockState state = level.getBlockState(pos);
						if (!state.getCollisionShape(level, pos).isEmpty() && level.getBlockState(above).getCollisionShape(level, above).isEmpty()) {
							effect.handleBlock(level, position, pos, state);
							++affectedBlocks;
							break;
						}
					}
				}
			}
		}
		stopBenchmark(profiler, affectedBlocks, affectedBlocks);
	}
	
	/**
	 * Creates a seed that is based on the supplied block position as well as a base seed. <br>
	 * Used to synchronize random number generators without explicitly sending anything but {@code baseSeed}.
	 */
	public static long explosionSeed(long baseSeed, BlockPos center, int offX, int offY, int offZ) {
		long seed = baseSeed ^
				    ((long)center.getX() * -7046029288634856825l) ^
				    ((long)center.getY() * -4417276706812531889l) ^
				    ((long)center.getZ() * 1609587929392839161l) ^
				    ((long)offX * 1609587929392839161l) ^
				    ((long)offY * -8796714831421723037l) ^
				    ((long)offZ * 2870177450012600261l);
		return RandomSupport.mixStafford13(seed);
	}
	
	/**
	 * Encodes 3 {@code int}s between 0 and 15 into a single {@code int}
	 * @param x  the x value to be encoded
	 * @param y  the y value to be encoded
	 * @param z  the z value to be encoded
	 * @return a {@code int} that has all three values encoded into it
	 * 
	 * @see #decodeSectionPos(int)
	 */
	public static int encodeSectionPos(int x, int y, int z) {
		return ((x & 15) << 8) | ((y & 15) << 4) | (z & 15);
	}
	
	/**
	 * Quick helper method to create a new {@link ExplosionProfiler} and start default benchmarks for all non-deprecated methods in this class
	 */
	private static ExplosionProfiler startBenchmark(Level level, int radius) {
		ExplosionProfiler profiler = new ExplosionProfiler(level);
		
		profiler.startBenchmark(Benchmark.BENCHMARK_0, "luckytntlib.benchmarking.total_explosion_time");
		profiler.startBenchmark(Benchmark.BENCHMARK_1, "luckytntlib.benchmarking.total_checked_blocks");
		profiler.addCounterTo(Benchmark.BENCHMARK_1, Counter.COUNTER_0, 0);
		profiler.startBenchmark(Benchmark.BENCHMARK_2, "luckytntlib.benchmarking.total_affected_blocks");
		profiler.addCounterTo(Benchmark.BENCHMARK_2, Counter.COUNTER_0, 0);
		
		if (radius < 50) {
			profiler.disable();
		}
		
		return profiler;
	}
	
	/**
	 * Quick helper method to finish default benchmarks for all non-deprecated methods in this class
	 */
	private static void stopBenchmark(ExplosionProfiler profiler, int checkedBlocks, int affectedBlocks) {
		profiler.countUp(Benchmark.BENCHMARK_1, Counter.COUNTER_0, checkedBlocks);
		profiler.countUp(Benchmark.BENCHMARK_2, Counter.COUNTER_0, affectedBlocks);
		profiler.stopBenchmarkTime(Benchmark.BENCHMARK_0);
		profiler.printResults();
	}
	
	/**
	 * <strong><i><font color="#E00000"> This method has been deprecated and will be removed in versions for Minecraft 1.21+ </font></i></strong>
	 * <br> <br>
	 * Gets all blocks in a specified sphere and returns them in a HashMap consisting of {@link BlockPos} and {@link BlockState}
	 * @param level  the current level
	 * @param position  the center position of the sphere
	 * @param radius  the radius of the sphere
	 * @return a {@link HashMap} of {@link BlockPos} and {@link BlockState}
	 */
	@Deprecated(since = "47.2.32.2", forRemoval = true)
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
	 * <strong><i><font color="#E00000"> This method has been deprecated and will be removed in versions for Minecraft 1.21+ </font></i></strong>
	 * <br> <br>
	 * Gets all blocks in a specified cuboid and returns them in a HashMap consisting of {@link BlockPos} and {@link BlockState}
	 * @param level  the current level
	 * @param position  the center position of the cuboid
	 * @param radii  the radii for the x, y and z directions
	 * @return a {@link HashMap} of {@link BlockPos} and {@link BlockState}
	 */
	@Deprecated(since = "47.2.32.2", forRemoval = true)
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
	 * <strong><i><font color="#E00000"> This method has been deprecated and will be removed in versions for Minecraft 1.21+ </font></i></strong>
	 * <br> <br>
	 * Gets all blocks in a specified cylinder and returns them in a HashMap consisting of {@link BlockPos} and {@link BlockState}
	 * @param level  the current level
	 * @param position  the center position of the cylinder
	 * @param radius  the radius of the cylinder
	 * @return a {@link HashMap} of {@link BlockPos} and {@link BlockState}
	 */
	@Deprecated(since = "47.2.32.2", forRemoval = true)
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
	 * <strong><i><font color="#E00000"> This method has been deprecated in favor of {@link #createSphericalCrater(Level, Vec3, int, int, ExplosionRule)} and will be removed in versions for Minecraft 1.21+ </font></i></strong>
	 * <br> <br>
	 * Gets blocks contained in a specified sphere around a center position and edits them according to the given {@link IForEachBlockExplosionEffect}
	 * @param level  the current level
	 * @param position  the center position of the spherical explosion
	 * @param radius  the radius of the sphere
	 * @param blockEffect  determines what should happen to the blocks gotten by this function
	 */
	@Deprecated(since = "47.2.32.2", forRemoval = true)
	public static void doSphericalExplosion(Level level, Vec3 position, int radius, IForEachBlockExplosionEffect blockEffect) {
		doModifiedSphericalExplosion(level, position, radius, new Vec3(1, 1, 1), blockEffect);
	}

	/**
	 * <strong><i><font color="#E00000"> This method has been deprecated in favor of {@link #createSpheroidCrater(Level, Vec3, int, Vector3f, int, ExplosionRule)} and will be removed in versions for Minecraft 1.21+ </font></i></strong>
	 * <br> <br>
	 * Gets blocks contained in a specified sphere around a center position and edits them according to the given {@link IForEachBlockExplosionEffect}. <br>
	 * The sphere can be scaled in all the 3 directions individually.
	 * @param level  the current level
	 * @param position  the center position of the spherical explosion
	 * @param radius  the radius of the sphere
	 * @param scaling  the scaling of the sphere
	 * @param blockEffect  determines what should happen to the blocks gotten by this function
	 */
	@Deprecated(since = "47.2.32.2", forRemoval = true)
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
	 * <strong><i><font color="#E00000"> This method has been deprecated in favor of {@link #createCubicalCrater(Level, Vec3, int, int, ExplosionRule)} and will be removed in versions for Minecraft 1.21+ </font></i></strong>
	 * <br> <br>
	 * Gets blocks contained in a specified cube around a center position and edits them according to the given {@link IForEachBlockExplosionEffect}
	 * @param level  the current level
	 * @param position  the center position of the cubical explosion
	 * @param radius  the radius of the cube
	 * @param blockEffect  determines what should happen to the blocks gotten by this function
	 */
	@Deprecated(since = "47.2.32.2", forRemoval = true)
	public static void doCubicalExplosion(Level level, Vec3 position, int radius, IForEachBlockExplosionEffect blockEffect) {
		doCuboidExplosion(level, position, new Vec3(radius, radius, radius), blockEffect);
	}
	
	/**
	 * <strong><i><font color="#E00000"> This method has been deprecated in favor of {@link #createCuboidCrater(Level, Vec3, int, Vector3f, int, ExplosionRule)} and will be removed in versions for Minecraft 1.21+ </font></i></strong>
	 * <br> <br>
	 * Gets blocks contained in a specified cuboid around a center position and edits them according to the given {@link IForEachBlockExplosionEffect}
	 * @param level  the current level
	 * @param position  the center position of the cubical explosion
	 * @param radii  a {@link Vec3} containing the radii for the x, y and z directions
	 * @param blockEffect  determines what should happen to the blocks gotten by this function
	 */
	@Deprecated(since = "47.2.32.2", forRemoval = true)
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
	 * <strong><i><font color="#E00000"> This method has been deprecated in favor of {@link #createCylindricalCrater(Level, Vec3, int, int, int, ExplosionRule)} and will be removed in versions for Minecraft 1.21+ </font></i></strong>
	 * <br> <br>
	 * Gets blocks contained in a specified cylinder around a center position and edits them according to the given {@link IForEachBlockExplosionEffect}
	 * @param level  the current level
	 * @param position  the center position of the cubical explosion
	 * @param radius  the radius of the x and z dimensions of the cylinder
	 * @param radiusY  the radius of the y dimension of the cylinder
	 * @param blockEffect  determines what should happen to the blocks gotten by this function
	 */
	@Deprecated(since = "47.2.32.2", forRemoval = true)
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
	 * <strong><i><font color="#E00000"> This method has been deprecated and will be removed in versions for Minecraft 1.21+ </font></i></strong>
	 * <br> <br>
	 * Gets only the top most blocks in a sphere and edits them according to the given {@link IForEachBlockExplosionEffect}. <br>
	 * The function goes from top to bottom and the first block that is air or not solid and followed by a solid block below is considered the top most block.
	 * @param level  the current level
	 * @param position  the center position of the top block explosion
	 * @param radius  the radius of the sphere
	 * @param blockEffect  determines what should happen to the blocks gotten by this function
	 */
	@Deprecated(since = "47.2.32.2", forRemoval = true)
	public static void doTopBlockExplosion(Level level, Vec3 position, int radius, IForEachBlockExplosionEffect blockEffect) {
		for(int offX = -radius; offX <= radius; offX++) {
			for(int offZ = -radius; offZ <= radius; offZ++) {
				topToBottom: for(int offY = radius; offY >= -radius; offY--) {
					double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
					if(distance <= radius) {
						BlockPos pos = new BlockPos((int)position.x, (int)position.y, (int)position.z).offset(offX, offY, offZ);
						BlockState state = level.getBlockState(pos);
						if((level.getBlockState(pos.below()).isCollisionShapeFullBlock(level, pos.below()) || level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) && (state.isAir() || state.canBeReplaced(new DirectionalPlaceContext(level, pos, Direction.DOWN, ItemStack.EMPTY, Direction.UP))  || (!state.isCollisionShapeFullBlock(level, pos) && state.getExplosionResistance(level, pos, ImprovedExplosion.dummyExplosion((ServerLevel)level)) == 0) || state.is(BlockTags.FLOWERS))) {
							blockEffect.doBlockExplosion(level, pos, state, distance);
							break topToBottom;
						}
					}
				}
			}
		}
	}
	
	/**
	 * <strong><i><font color="#E00000"> This method has been deprecated and will be removed in versions for Minecraft 1.21+ </font></i></strong>
	 * <br> <br>
	 * Gets only the top most blocks in a sphere and edits them according to the given {@link IForEachBlockExplosionEffect}. <br>
	 * The function goes from top to bottom and the block above the first block that is not air is considered the top most block. <br>
	 * If the condition is not met it will continue to search for another top block further down
	 * @param level  the current level
	 * @param position  the center position of the top block explosion
	 * @param radius  the radius of the sphere
	 * @param condition  the condition for the top block to be considered, otherwise a new block further down will be searched for
	 * @param blockEffect  determines what should happen to the blocks gotten by this function
	 */
	@Deprecated(since = "47.2.32.2", forRemoval = true)
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
	@Deprecated(since = "47.2.32.2", forRemoval = true)
	public static void doTopBlockExplosionForAll(Level level, Vec3 position, int radius, IForEachBlockExplosionEffect blockEffect) {
		for(int offX = -radius; offX <= radius; offX++) {
			for(int offZ = -radius; offZ <= radius; offZ++) {
				for(int offY = radius; offY >= -radius; offY--) {
					double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
					if(distance <= radius) {
						BlockPos pos = new BlockPos((int)position.x, (int)position.y, (int)position.z).offset(offX, offY, offZ);
						BlockState state = level.getBlockState(pos);
						if((level.getBlockState(pos.below()).isCollisionShapeFullBlock(level, pos.below()) || level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) && (state.isAir() || state.canBeReplaced(new DirectionalPlaceContext(level, pos, Direction.DOWN, ItemStack.EMPTY, Direction.UP)) || (!state.isCollisionShapeFullBlock(level, pos) && state.getExplosionResistance(level, pos, ImprovedExplosion.dummyExplosion((ServerLevel)level)) == 0) || state.is(BlockTags.FLOWERS))) {
							blockEffect.doBlockExplosion(level, pos, state, distance);
						}
					}
				}
			}
		}
	}
}