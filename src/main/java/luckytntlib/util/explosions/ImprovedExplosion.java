package luckytntlib.util.explosions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.joml.Vector3f;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import luckytntlib.config.LuckyTNTLibConfigValues;
import luckytntlib.network.ClientboundSetupExplosionPacket;
import luckytntlib.network.ClientboundUpdateChunkSectionPacket;
import luckytntlib.network.PacketHandler;
import luckytntlib.util.ExplosionProfiler;
import luckytntlib.util.ExplosionProfiler.Benchmark;
import luckytntlib.util.ExplosionProfiler.Counter;
import luckytntlib.util.IExplosiveEntity;
import luckytntlib.util.explosions.rules.ExplosionRule;
import luckytntlib.util.explosions.rules.FireExplosionRule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EntityBasedExplosionDamageCalculator;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.PacketDistributor;

/**
 * Minecraft's explosions are limited by size, performance, and versatility.
 * This extension of {@link Explosion} tackles all of those problems by providing a multitude of functions for executing and customizing explosions.
 * <p>
 * Any method in this class annotated with {@link Deprecated} have been so in favor of the new methods.
 * We highly encourage anyone to switch to the new system as future ports for this mod to newer versions of Minecraft won't contain these methods anymore.
 * As it is always with software development, we can't foresee all possible edge cases these methods might have been or will be used for.
 * Keeping that in mind, if your specific use case isn't covered by the new system, you'll have to make do yourself.
 */
public class ImprovedExplosion extends Explosion {

	public final Level level;
	public final double posX, posY, posZ;
	public final int size;
	public final ExplosionDamageCalculator damageCalculator;
	public final boolean performsBlockUpdates;
	
	@Nullable
	private BlockExplosionEffect customExplosionEffect;
	@Nullable
	private Consumer<ImprovedExplosion> onExplosionFinish;
	
	final ExplosionProfiler PROFILER;
	
	@Deprecated(forRemoval = true)
	List<Integer> affectedBlocks = new ArrayList<>();
	
	/**
	 * Creates a new ImprovedExplosion
	 * @param level  the level
	 * @param position  the center position of the explosion
	 * @param size  the radius of a sphere that is being used for ray-tracing. It influences strength and reach of the explosion
	 */
	public ImprovedExplosion(Level level, Vec3 position, int size) {
		this(level, null, null, position, size);
	}
	
	/**
	 * Creates a new ImprovedExplosion
	 * @param level  the level
	 * @param source  the {@link DamageSource} this explosion uses
	 * @param position  the center position of the explosion
	 * @param size  the radius of a sphere that is being used for ray-tracing. It influences strength and reach of the explosion
	 */
	public ImprovedExplosion(Level level, @Nullable DamageSource source, Vec3 position, int size) {
		this(level, null, source, position, size);
	}
	
	/**
	 * Creates a new ImprovedExplosion
	 * @param level  the level
	 * @param entity  the entity not affected by this explosion. Should be the entity causing the explosion and also an IExplosiveEntity
	 * @param position  the center position of the explosion
	 * @param size  the radius of a sphere that is being used for ray-tracing. It influences strength and reach of the explosion
	 */	
	public ImprovedExplosion(Level level, @Nullable Entity explodingEntity, Vec3 position, int size) {
		this(level, explodingEntity, null, position.x, position.y, position.z, size);
	}
	
	/**
	 * Creates a new ImprovedExplosion
	 * @param level  the level
	 * @param entity  the entity not affected by this explosion. Should be the entity causing the explosion and also an IExplosiveEntity
	 * @param source  the {@link DamageSource} this explosion uses
	 * @param position  the center position of the explosion
	 * @param size  the radius of a sphere that is being used for ray-tracing. It influences strength and reach of the explosion
	 */	
	public ImprovedExplosion(Level level, @Nullable Entity explodingEntity, @Nullable DamageSource source, Vec3 position, int size) {
		this(level, explodingEntity, source, position.x, position.y, position.z, size);
	}
	
	/**
	 * Creates a new ImprovedExplosion
	 * @param level  the level
	 * @param entity  the entity not affected by this explosion. Should be the entity causing the explosion and also an IExplosiveEntity
	 * @param x  the x center position
	 * @param y  the y center position
	 * @param z  the z center position
	 * @param size  the radius of a sphere that is being used for ray-tracing. It influences strength and reach of the explosion
	 */	
	public ImprovedExplosion(Level level, @Nullable Entity explodingEntity, double x, double y, double z, int size) {
		this(level, explodingEntity, null, x, y, z, size);
	}
	
	/**
	 * Creates a new ImprovedExplosion
	 * @param level  the level
	 * @param entity  the entity not affected by this explosion. Should be the entity causing the explosion and also an IExplosiveEntity
	 * @param source  the {@link DamageSource} this explosion uses
	 * @param x  the x center position
	 * @param y  the y center position
	 * @param z  the z center position
	 * @param size  the radius of a sphere that is being used for ray-tracing. It influences strength and reach of the explosion
	 */	
	public ImprovedExplosion(Level level, @Nullable Entity explodingEntity, @Nullable DamageSource source, double x, double y, double z, int size) {
		super(level, explodingEntity, source, null, x, y, z, size, false, BlockInteraction.DESTROY);
		this.level = level;
		this.posX = x;
		this.posY = y;
		this.posZ = z;
		this.size = size;
		damageCalculator = explodingEntity == null ? new ExplosionDamageCalculator() : new EntityBasedExplosionDamageCalculator(explodingEntity);
		this.performsBlockUpdates = size < LuckyTNTLibConfigValues.BLOCK_UPDATE_THRESHOLD.get();
		PROFILER = new ExplosionProfiler(level);
		if (size < 50) {
			PROFILER.disable();
		}
	}
	
	public void setProfilerTitle(String translationKey) {
		PROFILER.setTitle(translationKey);
	}
	
	public void disableProfiler() {
		PROFILER.disable();
	}
	
	/**
	 * Sets a consumer of this explosion that will execute after {@link #finishImprovedExplosion(Map, Set, ExplosionRule)}.
	 * <p>
	 * Due to the possibility of explosions being mutli-threaded if their size is larger than or equal to the user defined threshold (minimum {@code 60}),
	 * having a way to perform an action after the explosion has finished editing the world is crucial, as the main thread will not be blocked.
	 * @param onExplosionFinish  the action to perform once the explosion has been finished.
	 * @return this explosion
	 */
	public ImprovedExplosion setExplosionFinishWork(@Nullable Consumer<ImprovedExplosion> onExplosionFinish) {
		this.onExplosionFinish = onExplosionFinish;
		return this;
	}
	
	/**
	 * Sets a custom effect to be applied to all blocks gathered by {@link #doImprovedBlockExplosion(float, float, boolean, boolean, ExplosionRule)}.
	 * This effectively redirects the finishing of the explosion to the given {@link BlockExplosionEffect}.
	 * If set to {@code null}, the explosion will finish normally.
	 * @param effect  the effect to redirect the explosion to.
	 * @return this explosion
	 */
	public ImprovedExplosion setCustomExplosionEffect(@Nullable BlockExplosionEffect effect) {
		customExplosionEffect = effect;
		return this;
	}
	
	/**
	 * Executes a block explosion using either a single or multiple threads based on explosion size and user settings.
	 * Only works on the server sided level.
	 * The explosion is ray-casted onto the sphere with the radius determined by the size of this explosion.
	 * @param resistanceImpact  the relative impact that explosion resistance of blocks has on the penetration force of the explosion
	 * @param randomVecLength  the greater this value, the more distributed the length of the explosion vectors will be. Large explosions should have a value less than 1
	 * @param ignoreFluidResistance  whether or not fluids should be ignored in the explosion resistance calculation
	 * @param fire  whether or not the explosion should spawn fire afterwards. Fire placement is run as its own explosion. Fire placement may not work correctly in rare special cases.
	 * @param random  random number generator
	 * @param rule  optional rule for causing effects other than just destruction. Leave as null for an efficient explosion that destroys blocks
	 */
	public void doImprovedBlockExplosion(float resistanceImpact, float randomVecLength, boolean ignoreFluidResistance, boolean fire, @Nullable ExplosionRule rule) {
		if (level.isClientSide()) {
			return;
		}
		// Total explosion time
		PROFILER.startBenchmark(Benchmark.BENCHMARK_0, "luckytntlib.benchmarking.total_explosion_time");
		// Total blocks checked by explosion
		PROFILER.startBenchmark(Benchmark.BENCHMARK_1, "luckytntlib.benchmarking.total_checked_blocks");
		PROFILER.addCounterTo(Benchmark.BENCHMARK_1, Counter.COUNTER_0, 0);
		// Total blocks actually changed by explosion
		PROFILER.startBenchmark(Benchmark.BENCHMARK_2, "luckytntlib.benchmarking.total_affected_blocks");
		PROFILER.addCounterTo(Benchmark.BENCHMARK_2, Counter.COUNTER_0, 0);
		if (LuckyTNTLibConfigValues.MULTITHREADED_EXPLOSIONS.get() && size >= LuckyTNTLibConfigValues.MULTITHREADING_THRESHOLD.get()) {
			doImprovedBlockExplosionMultithreaded(resistanceImpact, randomVecLength, ignoreFluidResistance, fire, rule);
		} else {
			doImprovedBlockExplosionSinglethreaded(resistanceImpact, randomVecLength, ignoreFluidResistance, fire, rule);
		}
	}
	
	/**
	 * Multi-threads the improved block explosion, queuing the result to be applied in the next tick.
	 * Due to being multi-threaded, the game and other explosions will run in the background while the explosion loads.
	 * The amount of parallel explosions is limited by the user settings, making many simultaneous explosions be queued.
	 * Functionally equivalent to the single-threaded version.
	 */
	private void doImprovedBlockExplosionMultithreaded(float resistanceImpact, float randomVecLength, boolean ignoreFluidResistance, boolean fire, @Nullable ExplosionRule rule) {			
		ServerLevel serverLevel = (ServerLevel)level;
		float randomVecLengthFac = 0.6f * randomVecLength;
		float resistanceFac = 0.675f * resistanceImpact;
		long worldSeed = serverLevel.getSeed();
		SingleThreadedRandomSource random = new SingleThreadedRandomSource(ExplosionHelper.explosionSeed(worldSeed, BlockPos.containing(getPosition()), 0, 0, 0));

		List<Vector3f> vectors = new ArrayList<Vector3f>((int)(4 * size * size * Math.PI + 10));

		// Time and count of vector creation
		PROFILER.startBenchmark(Benchmark.BENCHMARK_3, "luckytntlib.benchmarking.vectors_gathered");
		PROFILER.addCounterTo(Benchmark.BENCHMARK_3, Counter.COUNTER_0, 0);
		for (int offX = -size; offX <= size; offX++) {
			for (int offY = -size; offY <= size; offY++) {
				for (int offZ = -size; offZ <= size; offZ++) {
					int distanceSqr = offX * offX + offY * offY + offZ * offZ;
					if (distanceSqr >= size * size && distanceSqr < (size + 1) * (size + 1)) {
						vectors.add(new Vector3f(offX, offY, offZ).mul(0.7f + random.nextFloat() * randomVecLengthFac));
					}
				}
			}
		}
		PROFILER.countUp(Benchmark.BENCHMARK_3, Counter.COUNTER_0, vectors.size());
		PROFILER.stopBenchmarkTime(Benchmark.BENCHMARK_3);
		
		ExplosionThread thread = new ExplosionThread(this, resistanceFac, randomVecLengthFac, ignoreFluidResistance, fire, rule, vectors);
		MultithreadedExplosionHandler.enqueue(thread);
	}

	
	/**
	 * Ray-casts onto the surface of the sphere determined by the size of this explosion.
	 * Ray length is also determined by the size and a bit of random addition.
	 * Ray length of any ray is dynamically reduced by the explosion resistances in its path.
	 * Blocks in a ray's remaining path are marked, not directly affected.
	 */
	@SuppressWarnings("deprecation")
	private void doImprovedBlockExplosionSinglethreaded(float resistanceImpact, float randomVecLength, boolean ignoreFluidResistance, boolean fire, @Nullable ExplosionRule rule) {			
		float randomVecLengthFac = 0.6f * randomVecLength;
		float resistanceFac = 0.3f * resistanceImpact * 2.25f;
		ServerLevel serverLevel = (ServerLevel)level;
		long worldSeed = serverLevel.getSeed();
		SingleThreadedRandomSource random = new SingleThreadedRandomSource(ExplosionHelper.explosionSeed(worldSeed, BlockPos.containing(getPosition()), 0, 0, 0));
		
		List<Vector3f> vectors = new ArrayList<Vector3f>((int)(4 * size * size * Math.PI + 10));

		// Time and count of vector creation
		PROFILER.startBenchmark(Benchmark.BENCHMARK_3, "luckytntlib.benchmarking.vectors_gathered");
		PROFILER.addCounterTo(Benchmark.BENCHMARK_3, Counter.COUNTER_0, 0);
		for (int offX = -size; offX <= size; offX++) {
			for (int offY = -size; offY <= size; offY++) {
				for (int offZ = -size; offZ <= size; offZ++) {
					int distanceSqr = offX * offX + offY * offY + offZ * offZ;
					if (distanceSqr >= size * size && distanceSqr < (size + 1) * (size + 1)) {
						vectors.add(new Vector3f(offX, offY, offZ).mul(0.7f + random.nextFloat() * randomVecLengthFac));
					}
				}
			}
		}
		PROFILER.countUp(Benchmark.BENCHMARK_3, Counter.COUNTER_0, vectors.size());
		PROFILER.stopBenchmarkTime(Benchmark.BENCHMARK_3);
		/*
		 * We use BitSets to only take up 1 bit per block, also making sure a singular chunk section is utilizing cache locality.
		 * Sections that are empty are marked to be skipped.
		 * Sections that have every block marked are saved more efficiently.
		 */
		Long2ObjectMap<BitSet> editedSections = new Long2ObjectOpenHashMap<BitSet>();
		Set<Long> fullSections = new LongOpenHashSet();
		Set<Long> emptySections = new LongOpenHashSet();
		Long2ObjectMap<float[]> sectionResistances = new Long2ObjectOpenHashMap<float[]>();

		PROFILER.startBenchmark(Benchmark.BENCHMARK_4, "luckytntlib.benchmarking.blocks_gathered");
		for (Vector3f v : vectors) {
			float vectorLength = v.length();
			float xStep = v.x / vectorLength * 0.3f;
			float yStep = v.y / vectorLength * 0.3f;
			float zStep = v.z / vectorLength * 0.3f;
			double blockX = posX;
			double blockY = posY;
			double blockZ = posZ;
			int lastPosX = 0;
			int lastPosY = -10000;
			int lastPosZ = 0;
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			long sectionPos;
			long lastSectionPos = Long.MAX_VALUE;
			boolean sectionEmpty = false;
			boolean sectionFull = false;
			float[] currentExplosionResistances = new float[0];
			BitSet bitSet = new BitSet(0);
			for (float step = 0f; step < vectorLength; step += 0.225f) {
				blockX += xStep;
				blockY += yStep;
				blockZ += zStep;
				pos.set(Mth.floor(blockX), Mth.floor(blockY), Mth.floor(blockZ));
				if (!level.isInWorldBounds(pos)) {
					break;
				}
				if (pos.getX() == lastPosX && pos.getY() == lastPosY && pos.getZ() == lastPosZ) {
					continue;
				}
				lastPosX = pos.getX();
				lastPosY = pos.getY();
				lastPosZ = pos.getZ();
				sectionPos = SectionPos.asLong(pos);
				if ((sectionPos == lastSectionPos && sectionEmpty) || emptySections.contains(sectionPos)) {
					sectionEmpty = true;
					vectorLength -= 0.3f * resistanceFac;
					continue;
				}
				sectionEmpty = false;
				if (sectionPos != lastSectionPos) {
					currentExplosionResistances = sectionResistances.get(sectionPos);
					if (currentExplosionResistances == null) {
						int chunkX = pos.getX() >> 4;
						int chunkY = pos.getY() >> 4;
						int chunkZ = pos.getZ() >> 4;
						LevelChunkSection section = level.getChunk(chunkX, chunkZ).getSection(chunkY - level.getMinSection());
						if (section.hasOnlyAir()) {
							emptySections.add(sectionPos);
							vectorLength -= 0.3f * resistanceFac;
							continue;
						}
						float[] explosionResistances = new float[4096];
						sectionResistances.put(sectionPos, explosionResistances);
						BlockState currentBlockState;
						for (int x = 0; x < 16; x++) {
							for (int y = 0; y < 16; y++) {
								for (int z = 0; z < 16; z++) {
									currentBlockState = section.getBlockState(x, y, z);
									explosionResistances[x << 8 | y << 4 | z] = ignoreFluidResistance && !currentBlockState.getFluidState().isEmpty() ? 0f : Math.max(currentBlockState.getBlock().getExplosionResistance(), currentBlockState.getFluidState().getExplosionResistance());
								}
							}
						}
						currentExplosionResistances = explosionResistances;
					}
					bitSet = editedSections.get(sectionPos);
					if (bitSet == null) {
						editedSections.put(sectionPos, bitSet = new BitSet(4096));
					}
				}
				int blockIndex = ((pos.getX() & 15) << 8) | ((pos.getY() & 15) << 4) | (pos.getZ() & 15);
				vectorLength -= (currentExplosionResistances[blockIndex] + 0.3f) * resistanceFac;
				if (vectorLength <= 0f) {
					break;
				}
				if ((sectionPos == lastSectionPos && sectionFull) || fullSections.contains(sectionPos)) {
					sectionFull = true;
					continue;
				}
				sectionFull = false;
				lastSectionPos = sectionPos;
				if (!bitSet.get(blockIndex)) {
					bitSet.set(blockIndex);
					if (bitSet.nextClearBit(0) >= 4096) {
						editedSections.remove(sectionPos);
						fullSections.add(sectionPos);
					}
				}
			}
		}
		PROFILER.stopBenchmarkTime(Benchmark.BENCHMARK_4);
		editedSections.forEach((pos, bitset) -> {
			PROFILER.countUp(Benchmark.BENCHMARK_1, Counter.COUNTER_0, bitset);
		});
		fullSections.forEach(pos -> {
			PROFILER.countUp(Benchmark.BENCHMARK_1, Counter.COUNTER_0, 4096);
		});
		
		finishImprovedExplosion(editedSections, fullSections, rule);
		if (fire) {
			float sizeReduction = (float)Math.sqrt(Math.sqrt(size));
			ImprovedExplosion fireExplosion = new ImprovedExplosion(level, getPosition(), Math.round(size / sizeReduction));
			fireExplosion.setProfilerTitle("luckytntlib.benchmarking.fire_explosion_title");
			fireExplosion.doImprovedBlockExplosion(1f, 1.2f * sizeReduction, false, false, new FireExplosionRule(1f / sizeReduction));
		}
	}
	
	/**
	 * Finalizes an explosion by removing / altering the blocks retrieved by raytracing.
	 * If no rule is given, blocks will simply be removed.
	 * @param editedSections  the chunk sections which were only partially affected by the explosion
	 * @param fullSections  the chunk sections which are fully affected by the explosion
	 * @param rule  an optional rule for applying effects other than destroying all marked blocks
	 * @param PROFILER  the profiler used to benchmark this explosion
	 */
	void finishImprovedExplosion(Map<Long, BitSet> editedSections, Set<Long> fullSections, @Nullable ExplosionRule rule) {
		PROFILER.startBenchmark(Benchmark.BENCHMARK_5, "luckytntlib.benchmarking.explosion_finish_time");
		
		if (ExplosionHelper.getChunkHolder != null) {
			Set<ChunkPos> chunks = new HashSet<>();
			for (long section : fullSections) {
				SectionPos pos = SectionPos.of(section);
				chunks.add(new ChunkPos(pos.getX(), pos.getZ()));
			}
			for (long section : editedSections.keySet()) {
				SectionPos pos = SectionPos.of(section);
				chunks.add(new ChunkPos(pos.getX(), pos.getZ()));
			}
			for (ChunkPos pos : chunks) {
				try {
					LevelChunk chunk = level.getChunk(pos.x, pos.z);
					ChunkHolder holder = (ChunkHolder)ExplosionHelper.getChunkHolder.invoke(((ServerLevel)level).getChunkSource(), new Object[]{pos.toLong()});
					holder.broadcastChanges(chunk);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
		
		if (customExplosionEffect != null) {
			finishCustomExplosion(editedSections, fullSections);
		} else if (performsBlockUpdates) {
			if (rule == null) {
				finishUpdatingExplosionWithoutRule(editedSections, fullSections);
			} else {
				finishUpdatingExplosionWithRule(editedSections, fullSections, rule);
			}
		} else if (rule == null) {
			finishImprovedExplosionWithoutRule(editedSections, fullSections);
		} else {
			finishImprovedExplosionWithRule(editedSections, fullSections, rule);
		}
		PROFILER.stopBenchmarkTime(Benchmark.BENCHMARK_0);
		PROFILER.printResults();
		if (onExplosionFinish != null) {
			onExplosionFinish.accept(this);
		}
	}
	
	/**
	 * Efficient finalization of an explosion that only removes blocks.
	 * This will not prompt block updates for performance reasons.
	 * Block and sky light will be updated correctly and efficiently.
	 */
	private void finishImprovedExplosionWithoutRule(Map<Long, BitSet> editedSections, Set<Long> fullSections) {
		HashMap<LevelChunk, BitSet> chunks = new HashMap<>();
		Long2ObjectMap<LightUpdateHelper.LightDataHolder> dataLayerCache = new Long2ObjectOpenHashMap<>();
		PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new ClientboundSetupExplosionPacket(null, null, 0));
		
		for (long encodedPos : fullSections) {
			SectionPos pos = SectionPos.of(encodedPos);
			LevelChunk chunk = level.getChunk(pos.x(), pos.z());
			LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(pos.y()));
			PalettedContainer<BlockState> states = section.getStates();
			
			chunk.setLoaded(true);
			
			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					for (int y = 0; y < 16; y++) {
						states.set(x, y, z, Blocks.AIR.defaultBlockState());
					}
				}
			}
			
			section.recalcBlockCounts();
			
			PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new ClientboundUpdateChunkSectionPacket(SectionPos.of(pos.x(), level.getSectionIndexFromSectionY(pos.y()), pos.z()), new BitSet(0), true, false));
			
			if (!chunks.containsKey(chunk)) {
				chunks.put(chunk, new BitSet());
			}
			chunks.get(chunk).set(pos.y() - (level.getMinSection() - 1));
			
			chunk.setUnsaved(true);
		}
		
		for (Entry<Long, BitSet> entry : editedSections.entrySet()) {
			SectionPos pos = SectionPos.of(entry.getKey());
			BitSet removedBlocks = entry.getValue();
			LevelChunk chunk = level.getChunk(pos.x(), pos.z());
			LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(pos.y()));
			PalettedContainer<BlockState> states = section.getStates();
			
			chunk.setLoaded(true);
			
			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					for (int y = 0; y < 16; y++) {
						if (removedBlocks.get(ExplosionHelper.encodeSectionPos(x, y, z))) {
							states.set(x, y, z, Blocks.AIR.defaultBlockState());
						}
					}
				}
			}
			
			section.recalcBlockCounts();
			
			PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new ClientboundUpdateChunkSectionPacket(SectionPos.of(pos.x(), level.getSectionIndexFromSectionY(pos.y()), pos.z()), removedBlocks, false, false));
			
			if (!chunks.containsKey(chunk)) {
				chunks.put(chunk, new BitSet());
			}
			chunks.get(chunk).set(pos.y() - (level.getMinSection() - 1));
			
			chunk.setUnsaved(true);
		}

		PROFILER.countUp(Benchmark.BENCHMARK_2, Counter.COUNTER_0, PROFILER.getCount(Benchmark.BENCHMARK_1, Counter.COUNTER_0));
		
		HeightmapUpdateHelper.updateHeightmaps((ServerLevel)level, chunks.keySet());
		
		PROFILER.stopBenchmarkTime(Benchmark.BENCHMARK_5);

		PROFILER.startBenchmark(Benchmark.BENCHMARK_6, "luckytntlib.benchmarking.light_update_time");
		LightUpdateHelper.updateDirectSkyLight((ServerLevel)level, chunks, dataLayerCache);
		LightUpdateHelper.updateIndirectSkyLight((ServerLevel)level, chunks, dataLayerCache);
		PROFILER.stopBenchmarkTime(Benchmark.BENCHMARK_6);
		
		((ServerLevel)level).save(null, false, false);
	}
	
	/**
	 * Efficient finalization of an explosion that affects blocks using a rule.
	 * This will not prompt block updates for performance reasons. Such updates need to be manually queued in the given rule.
	 * Block and sky light will be updated correctly and efficiently.
	 */
	private void finishImprovedExplosionWithRule(Map<Long, BitSet> editedSections, Set<Long> fullSections, ExplosionRule rule) {		
		long worldSeed = ((ServerLevel)level).getSeed();
		SingleThreadedRandomSource random = new SingleThreadedRandomSource(0);
		BlockPos center = BlockPos.containing(getPosition());
		
		HashMap<LevelChunk, BitSet> chunks = new HashMap<>();
		Long2ObjectMap<LightUpdateHelper.LightDataHolder> dataLayerCache = new Long2ObjectOpenHashMap<>();
		PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new ClientboundSetupExplosionPacket(rule, new Vec3(posX, posY, posZ), worldSeed));

		for (long encodedPos : fullSections) {
			SectionPos pos = SectionPos.of(encodedPos);
			LevelChunk chunk = level.getChunk(pos.x(), pos.z());
			LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(pos.y()));
			PalettedContainer<BlockState> states = section.getStates();
			
			chunk.setLoaded(true);
			
			BitSet changed = null;
			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					for (int y = 0; y < 16; y++) {
						int xg = (pos.x() << 4) + x;
						int yg = (pos.y() << 4) + y;
						int zg = (pos.z() << 4) + z;
						int offX = xg - center.getX();
						int offY = yg - center.getY();
						int offZ = zg - center.getZ();
						
						BlockState state = states.get(x, y, z);
						
						random.setSeed(ExplosionHelper.explosionSeed(worldSeed, center, offX, offY, offZ));
						BlockState newState = rule.getState(level, state, getPosition(), offX, offY, offZ, random);
						
						if (newState != null) {
							BlockPos blockpos = new BlockPos(xg, yg, zg);
							states.set(x, y, z, newState);
							chunk.removeBlockEntity(blockpos);
						} else {
							if (changed == null) {
								changed = new BitSet(4096);
								changed.set(0, 4096);
							}
							changed.set(ExplosionHelper.encodeSectionPos(x, y, z), false);
						}
					}
				}
			}
			
			section.recalcBlockCounts();
			
			if (changed != null) {
				PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new ClientboundUpdateChunkSectionPacket(SectionPos.of(pos.x(), level.getSectionIndexFromSectionY(pos.y()), pos.z()), changed, false, false));
				PROFILER.countUp(Benchmark.BENCHMARK_2, Counter.COUNTER_0, changed);
			} else {
				PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new ClientboundUpdateChunkSectionPacket(SectionPos.of(pos.x(), level.getSectionIndexFromSectionY(pos.y()), pos.z()), new BitSet(0), true, false));
				PROFILER.countUp(Benchmark.BENCHMARK_2, Counter.COUNTER_0, 4096);
			}
			
			if (!chunks.containsKey(chunk)) {
				chunks.put(chunk, new BitSet());
			}
			chunks.get(chunk).set(pos.y() - (level.getMinSection() - 1));
			
			chunk.setUnsaved(true);
		}
		
		for (Entry<Long, BitSet> entry : editedSections.entrySet()) {
			SectionPos pos = SectionPos.of(entry.getKey());
			BitSet affectedBlocks = entry.getValue();
			LevelChunk chunk = level.getChunk(pos.x(), pos.z());
			LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(pos.y()));
			PalettedContainer<BlockState> states = section.getStates();
			
			chunk.setLoaded(true);
			
			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					for (int y = 0; y < 16; y++) {
						int index = ExplosionHelper.encodeSectionPos(x, y, z);
						if (affectedBlocks.get(index)) {
							int xg = (pos.x() << 4) + x;
							int yg = (pos.y() << 4) + y;
							int zg = (pos.z() << 4) + z;
							int offX = xg - center.getX();
							int offY = yg - center.getY();
							int offZ = zg - center.getZ();
					
							BlockState state = states.get(x, y, z);
							
							random.setSeed(ExplosionHelper.explosionSeed(worldSeed, center, offX, offY, offZ));
							BlockState newState = rule.getState(level, state, getPosition(), offX, offY, offZ, random);
							
							if (newState != null) {
								BlockPos blockpos = new BlockPos(xg, yg, zg);
								states.set(x, y, z, newState);
								chunk.removeBlockEntity(blockpos);
							} else {
								affectedBlocks.set(index, false);
							}
						}
					}
				}
			}
			
			section.recalcBlockCounts();

			PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new ClientboundUpdateChunkSectionPacket(SectionPos.of(pos.x(), level.getSectionIndexFromSectionY(pos.y()), pos.z()), affectedBlocks, false, false));
			PROFILER.countUp(Benchmark.BENCHMARK_2, Counter.COUNTER_0, affectedBlocks);
			
			if (!chunks.containsKey(chunk)) {
				chunks.put(chunk, new BitSet());
			}
			chunks.get(chunk).set(pos.y() - (level.getMinSection() - 1));
			
			chunk.setUnsaved(true);
		}

		HeightmapUpdateHelper.updateHeightmaps((ServerLevel)level, chunks.keySet());
		
		PROFILER.stopBenchmarkTime(Benchmark.BENCHMARK_5);
		
		PROFILER.startBenchmark(Benchmark.BENCHMARK_6, "luckytntlib.benchmarking.light_update_time");
		LightUpdateHelper.updateDirectSkyLight((ServerLevel)level, chunks, dataLayerCache);
		LightUpdateHelper.updateIndirectSkyLight((ServerLevel)level, chunks, dataLayerCache);
		PROFILER.stopBenchmarkTime(Benchmark.BENCHMARK_6);
		
		((ServerLevel)level).save(null, false, false);
	}
	
	/**
	 * Finalization of an explosion without a rule that automatically updates all affected blocks, having a large negative impact on performance.
	 * Best used for small explosions.
	 */
	private void finishUpdatingExplosionWithoutRule(Map<Long, BitSet> editedSections, Set<Long> fullSections) {
		for (long encodedPos : fullSections) {
			SectionPos sectionPos = SectionPos.of(encodedPos);
			int x = sectionPos.getX() << 4;
			int y = sectionPos.getY() << 4;
			int z = sectionPos.getZ() << 4;
			for (int i = 0; i < 4096; i++) {
				BlockPos pos = new BlockPos(x + ((i >> 8) & 15), y + ((i >> 4) & 15), z + (i & 15));
				BlockState state = level.getBlockState(pos);
				level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
				state.getBlock().wasExploded(level, pos, this);
			}
		}
		
		for (Entry<Long, BitSet> entry : editedSections.entrySet()) {
			SectionPos sectionPos = SectionPos.of(entry.getKey());
			BitSet affectedBlocks = entry.getValue();
			int x = sectionPos.getX() << 4;
			int y = sectionPos.getY() << 4;
			int z = sectionPos.getZ() << 4;
			for (int i = 0; i < 4096; i++) {
				if (affectedBlocks.get(i)) {
					BlockPos pos = new BlockPos(x + ((i >> 8) & 15), y + ((i >> 4) & 15), z + (i & 15));
					BlockState state = level.getBlockState(pos);
					level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
					state.getBlock().wasExploded(level, pos, this);
				}
			}
		}
		PROFILER.countUp(Benchmark.BENCHMARK_2, Counter.COUNTER_0, PROFILER.getCount(Benchmark.BENCHMARK_1, Counter.COUNTER_0));
		
		PROFILER.stopBenchmarkTime(Benchmark.BENCHMARK_5);
	}
	
	/**
	 * Finalization of an explosion with a rule that automatically updates all affected blocks, having a large negative impact on performance.
	 * Best used for small explosions.
	 */
	private void finishUpdatingExplosionWithRule(Map<Long, BitSet> editedSections, Set<Long> fullSections, ExplosionRule rule) {
		long worldSeed = ((ServerLevel)level).getSeed();
		SingleThreadedRandomSource random = new SingleThreadedRandomSource(0);
		BlockPos center = BlockPos.containing(getPosition());
		
		for (long encodedPos : fullSections) {
			int blocks = 0;
			SectionPos sectionPos = SectionPos.of(encodedPos);
			LevelChunk chunk = level.getChunk(sectionPos.x(), sectionPos.z());
			LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(sectionPos.y()));
			PalettedContainer<BlockState> states = section.getStates();
			int x = sectionPos.getX() << 4;
			int y = sectionPos.getY() << 4;
			int z = sectionPos.getZ() << 4;
			for (int i = 0; i < 4096; i++) {
				int lx = (i >> 8) & 15;
				int ly = (i >> 4) & 15;
				int lz = i & 15;
				int offX = x + lx - center.getX();
				int offY = y + ly - center.getY();
				int offZ = z + lz - center.getZ();
				
				BlockState state = states.get(lx, ly, lz);
				
				random.setSeed(ExplosionHelper.explosionSeed(worldSeed, center, offX, offY, offZ));
				BlockState newState = rule.getState(level, state, getPosition(), offX, offY, offZ, random);
				
				if (newState != null) {
					BlockPos pos = new BlockPos(x + lx, y + ly, z + lz);
					level.setBlockAndUpdate(pos, newState);
					state.getBlock().wasExploded(level, pos, this);
					++blocks;
				}
			}
			PROFILER.countUp(Benchmark.BENCHMARK_2, Counter.COUNTER_0, blocks);
		}
		
		for (Entry<Long, BitSet> entry : editedSections.entrySet()) {
			int blocks = 0;
			SectionPos sectionPos = SectionPos.of(entry.getKey());
			LevelChunk chunk = level.getChunk(sectionPos.x(), sectionPos.z());
			LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(sectionPos.y()));
			PalettedContainer<BlockState> states = section.getStates();
			BitSet affectedBlocks = entry.getValue();
			int x = sectionPos.getX() << 4;
			int y = sectionPos.getY() << 4;
			int z = sectionPos.getZ() << 4;
			for (int i = 0; i < 4096; i++) {
				if (affectedBlocks.get(i)) {
					int lx = (i >> 8) & 15;
					int ly = (i >> 4) & 15;
					int lz = i & 15;
					int offX = x + lx - center.getX();
					int offY = y + ly - center.getY();
					int offZ = z + lz - center.getZ();
					
					BlockState state = states.get(lx, ly, lz);
					
					random.setSeed(ExplosionHelper.explosionSeed(worldSeed, center, offX, offY, offZ));
					BlockState newState = rule.getState(level, state, getPosition(), offX, offY, offZ, random);
					
					if (newState != null) {
						BlockPos pos = new BlockPos(x + lx, y + ly, z + lz);
						level.setBlockAndUpdate(pos, newState);
						state.getBlock().wasExploded(level, pos, this);
						++blocks;
					}
				}
			}
			PROFILER.countUp(Benchmark.BENCHMARK_2, Counter.COUNTER_0, blocks);
		}
		
		PROFILER.stopBenchmarkTime(Benchmark.BENCHMARK_5);
	}
	
	/**
	 * Finalization of an explosion with a custom effect that is applied to each affected block.
	 * The effect must be set in the constructor and if not set to null, will automatically lead to this function.
	 * Due to the nature of the code being entirely injected, this method is generally the most powerful when it comes to possibilities,
	 * but requires great care in implementation or it will suffer immensely in performance.
	 */
	private void finishCustomExplosion(Map<Long, BitSet> editedSections, Set<Long> fullSections) {
		for (long encodedPos : fullSections) {
			SectionPos sectionPos = SectionPos.of(encodedPos);
			LevelChunk chunk = level.getChunk(sectionPos.x(), sectionPos.z());
			LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(sectionPos.y()));
			PalettedContainer<BlockState> states = section.getStates();
			int x = sectionPos.getX() << 4;
			int y = sectionPos.getY() << 4;
			int z = sectionPos.getZ() << 4;
			for (int i = 0; i < 4096; i++) {
				int lx = (i >> 8) & 15;
				int ly = (i >> 4) & 15;
				int lz = i & 15;
				BlockState state = states.get(lx, ly, lz);
				BlockPos pos = new BlockPos(x + ((i >> 8) & 15), y + ((i >> 4) & 15), z + (i & 15));
				customExplosionEffect.handleBlock(level, getPosition(), pos, state);
			}
			PROFILER.countUp(Benchmark.BENCHMARK_2, Counter.COUNTER_0, 4096);
		}
		for (Entry<Long, BitSet> entry : editedSections.entrySet()) {
			SectionPos sectionPos = SectionPos.of(entry.getKey());
			LevelChunk chunk = level.getChunk(sectionPos.x(), sectionPos.z());
			LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(sectionPos.y()));
			PalettedContainer<BlockState> states = section.getStates();
			BitSet affectedBlocks = entry.getValue();
			int x = sectionPos.getX() << 4;
			int y = sectionPos.getY() << 4;
			int z = sectionPos.getZ() << 4;
			for (int i = 0; i < 4096; i++) {
				if (affectedBlocks.get(i)) {
					int lx = (i >> 8) & 15;
					int ly = (i >> 4) & 15;
					int lz = i & 15;
					BlockState state = states.get(lx, ly, lz);
					BlockPos pos = new BlockPos(x + lx, y + ly, z + lz);
					customExplosionEffect.handleBlock(level, getPosition(), pos, state);
				}
			}
			PROFILER.countUp(Benchmark.BENCHMARK_2, Counter.COUNTER_0, affectedBlocks);
		}
		
		PROFILER.stopBenchmarkTime(Benchmark.BENCHMARK_5);
	}
	
	/**
	 * Deprecated and will be removed in versions 1.21+. Please switch to {@link #doImprovedBlockExplosion(float, float, boolean, boolean, RandomSource, ExplosionRule)}, which is magnitudes more efficient.
	 *  
	 * Gets all blocks in an area calculated by shooting vectors to the borders of a cube determined by the {@link ImprovedExplosion#size} and destroys them.
	 * If any of the relative coordinates of the affected block exceed 511 they will be clamped to that value.
	 * Encodes block positions into a singular int, increasing performance.
	 * The shape the vectors orient to can either be a sphere or a cube, depending on the players config.
	 * @param xzStrength  a multiplier to the x and z vector addition, which makes the explosion more powerful. It should not be set higher than 1.2, otherwise blocks might be skipped
	 * @param yStrength  a multiplier to the y vector addition, which makes the explosion more powerful. It should not be set to high, otherwise blocks might be skipped
	 * @param resistanceImpact  the relative impact that explosion resistance of blocks has on the penetration force of explosion
	 * @param randomVecLength  the greater this value, the more distributed the length of the explosion vectors will be. Large explosions should have a value less than 1
	 * @param fire  whether or not the explosion should spawn fire afterwards
	 * @param isStrongExplosion  whether or not fluids should be ignored in the explosion resistance calculation. Very useful for large explosions
	 */
	@Deprecated(since = "47.2.32.2", forRemoval = true)
	public void doBlockExplosion(float xzStrength, float yStrength, float resistanceImpact, float randomVecLength, boolean fire, boolean isStrongExplosion) {			
		long time = System.currentTimeMillis();
		BlockPos posTNT = new BlockPos(Mth.floor(posX), Mth.floor(posY), Mth.floor(posZ));
		Set<Integer> blocks = new HashSet<>();		
		for (int offX = -size; offX <= size; offX++) {
			for (int offY = -size; offY <= size; offY++) {
				for (int offZ = -size; offZ <= size; offZ++) {
					double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
					if (((int) distance == size && LuckyTNTLibConfigValues.PERFORMANT_EXPLOSION.get()) || (!LuckyTNTLibConfigValues.PERFORMANT_EXPLOSION.get() && (offX == -size || offX == size || offY == -size || offY == size || offZ == -size || offZ == size))) {
						double xStep = offX / distance;
						double yStep = offY / distance;
						double zStep = offZ / distance;
						float vecLength = size * (0.7f + (float) Math.random() * 0.6f * randomVecLength);
						double blockX = posX;
						double blockY = posY;
						double blockZ = posZ;
						for (float vecStep = 0; vecStep < vecLength; vecStep += LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * 1.5f - 0.225f) {
							blockX += xStep * LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * xzStrength;
							blockY += yStep * LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * yStrength;
							blockZ += zStep * LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * xzStrength;
							BlockPos pos = new BlockPos((int)blockX, (int)blockY, (int)blockZ);
							if (!level.isInWorldBounds(pos)) {
								break;
							}
							BlockState blockState = level.getBlockState(pos);
							FluidState fluidState = level.getFluidState(pos);
							if (!(isStrongExplosion && !fluidState.isEmpty())) {
								Optional<Float> explosionResistance = damageCalculator.getBlockExplosionResistance(this, level, pos, blockState, fluidState);
								if (explosionResistance.isPresent()) {
									vecLength -= (explosionResistance.get() + 0.3f) * 0.3f * resistanceImpact;
								}
								if (vecLength > 0 && damageCalculator.shouldBlockExplode(this, level, pos, blockState, vecLength) && !blockState.isAir()) {
									blocks.add(encodeBlockPos(pos.subtract(posTNT).getX(), pos.subtract(posTNT).getY(), pos.subtract(posTNT).getZ()));
								}
							} else {
								blocks.add(encodeBlockPos(pos.subtract(posTNT).getX(), pos.subtract(posTNT).getY(), pos.subtract(posTNT).getZ()));
							}
						}
					}
				}
			}
		}
		System.out.println(System.currentTimeMillis() - time);
		affectedBlocks.addAll(blocks);
		for(int intPos : blocks) {
			BlockPos pos = decodeBlockPos(intPos).offset(posTNT);
			level.getBlockState(pos).getBlock().onBlockExploded(level.getBlockState(pos), level, pos, this);
		}
		if(fire) {
			for(int intPos : blocks) {
				BlockPos pos = decodeBlockPos(intPos).offset(posTNT);
				if(Math.random() > 0.75f && level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolidRender(level, pos)) {
					level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
				}
			}
		}
		System.out.println(System.currentTimeMillis() - time);
	}
	
	/**
	 * Deprecated and will be removed in versions 1.21+. Please switch to {@link #doImprovedBlockExplosion(float, float, boolean, boolean, RandomSource, ExplosionRule)}, which is magnitudes more efficient.
	 * 
	 * Gets all blocks in an area calculated by shooting vectors to the borders of a cube determined by the {@link ImprovedExplosion#size} 
	 * and does to them whatever specified in the {@link IForEachBlockExplosionEffect}. 
	 * If any of the relative coordinates of the affected block exceed 511 they will be clamped to that value.
	 * Encodes block positions into a singular int, increasing performance.
	 * The shape the vectors orient to can either be a sphere or a cube, depending on the players config.
	 * @param xzStrength  a multiplier to the x and z vector addition, which makes the explosion more powerful. It should not be set to high, otherwise blocks might be skipped
	 * @param yStrength  a multiplier to the y vector addition, which makes the explosion more powerful. It should not be set to high, otherwise blocks might be skipped
	 * @param resistanceImpact  the relative impact that explosion resistance of blocks has on the penetration force of explosion
	 * @param randomVecLength  the greater this value, the more distributed the length of the explosion vectors will be. Large explosions should have a value less than 1
	 * @param fire  whether or not the explosion should spawn fire afterwards
	 * @param isStrongExplosion  whether or not fluids should be ignored in the explosion resistance calculation. Very useful for large explosions
	 * @param blockEffect  determines what should happen to the blocks gotten by this explosion
	 */
	@Deprecated(since = "47.2.32.2", forRemoval = true)
	public void doBlockExplosion(float xzStrength, float yStrength, float resistanceImpact, float randomVecLength, boolean isStrongExplosion, IForEachBlockExplosionEffect blockEffect) {
		BlockPos posTNT = new BlockPos(Mth.floor(posX), Mth.floor(posY), Mth.floor(posZ));
		Set<Integer> blocks = new HashSet<>();
		for(int offX = -size; offX <= size; offX++) {
			for(int offY = -size; offY <= size; offY++) {
				for(int offZ = -size; offZ <= size; offZ++) {
					double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
					if(((int)distance == size && LuckyTNTLibConfigValues.PERFORMANT_EXPLOSION.get()) || (!LuckyTNTLibConfigValues.PERFORMANT_EXPLOSION.get() && (offX == -size || offX == size || offY == -size || offY == size || offZ == -size || offZ == size))) {
						double xStep = offX / distance;
						double yStep = offY / distance;
						double zStep = offZ / distance;
						float vecLength = size * (0.7f + (float)Math.random() * 0.6f * randomVecLength);
						double blockX = posX;
						double blockY = posY;
						double blockZ = posZ;
						for(float vecStep = 0; vecStep < vecLength; vecStep += LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * 1.5f - 0.225f) {
							blockX += xStep * LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * xzStrength;
							blockY += yStep * LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * yStrength;
							blockZ += zStep * LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * xzStrength;
							BlockPos pos = new BlockPos((int)blockX, (int)blockY, (int)blockZ);
							if(!level.isInWorldBounds(pos)) {
								break;
							}
							BlockState blockState = level.getBlockState(pos);
							FluidState fluidState = level.getFluidState(pos);
							if(!(isStrongExplosion && !fluidState.isEmpty())) {
								Optional<Float> explosionResistance = damageCalculator.getBlockExplosionResistance(this, level, pos, blockState, fluidState);
								if(explosionResistance.isPresent()) {
									vecLength -= (explosionResistance.get() + 0.3f) * 0.3f * resistanceImpact;
								}
								if(vecLength > 0 && damageCalculator.shouldBlockExplode(this, level, pos, blockState, vecLength) && !blockState.isAir()) {
									blocks.add(encodeBlockPos(pos.subtract(posTNT).getX(), pos.subtract(posTNT).getY(), pos.subtract(posTNT).getZ()));
								}
							}
							else {
								blocks.add(encodeBlockPos(pos.subtract(posTNT).getX(), pos.subtract(posTNT).getY(), pos.subtract(posTNT).getZ()));
							}
						}
					}
				}
			}
		}
		affectedBlocks.addAll(blocks);
		for(int intPos : blocks) {
			BlockPos pos = decodeBlockPos(intPos).offset(posTNT);
			double distance = Math.sqrt(pos.distToLowCornerSqr(posX, posY, posZ));
			blockEffect.doBlockExplosion(level, pos, level.getBlockState(pos), distance);
		}
	}
	
	/**
	 * Deprecated and will be removed in versions 1.21+. Please switch to {@link #doImprovedBlockExplosion(float, float, boolean, boolean, RandomSource, ExplosionRule)}, which is magnitudes more efficient.
	 * 
	 * Gets blocks in an area calculated by shooting vectors to the borders of a cube determined by the {@link ImprovedExplosion#size} if the {@link IBlockExplosionCondition} is met 
	 * and does to them whatever specified in the blockEffect.
	 * If any of the relative coordinates of the affected block exceed 511 they will be clamped to that value.
	 * Encodes block positions into a singular int, increasing performance.
	 * The shape the vectors orient to can either be a sphere or a cube, depending on the players config.
	 * @param xzStrength  a multiplier to the x and z vector addition, which makes the explosion more powerful. It should not be set to high, otherwise blocks might be skipped
	 * @param yStrength  a multiplier to the y vector addition, which makes the explosion more powerful. It should not be set to high, otherwise blocks might be skipped
	 * @param resistanceImpact  the relative impact that explosion resistance of blocks has on the penetration force of explosion
	 * @param randomVecLength  the greater this value, the more distributed the length of the explosion vectors will be. Large explosions should have a value less than 1
	 * @param fire  whether or not the explosion should spawn fire afterwards
	 * @param isStrongExplosion  whether or not fluids should be ignored in the explosion resistance calculation. Very useful for large explosions
	 * @param condition  the condition on which a block is added to the {@link Set} of blocks
	 * @param blockEffect  determines what should happen to the blocks gotten by this explosion
	 */
	@Deprecated(since = "47.2.32.2", forRemoval = true)
	public void doBlockExplosion(float xzStrength, float yStrength, float resistanceImpact, float randomVecLength, boolean isStrongExplosion, IBlockExplosionCondition condition, IForEachBlockExplosionEffect blockEffect) {
		BlockPos posTNT = new BlockPos(Mth.floor(posX), Mth.floor(posY), Mth.floor(posZ));
		Set<Integer> blocks = new HashSet<>();
		for(int offX = -size; offX <= size; offX++) {
			for(int offY = -size; offY <= size; offY++) {
				for(int offZ = -size; offZ <= size; offZ++) {
					double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
					if(((int)distance == size && LuckyTNTLibConfigValues.PERFORMANT_EXPLOSION.get()) || (!LuckyTNTLibConfigValues.PERFORMANT_EXPLOSION.get() && (offX == -size || offX == size || offY == -size || offY == size || offZ == -size || offZ == size))) {
						double xStep = offX / distance;
						double yStep = offY / distance;
						double zStep = offZ / distance;
						float vecLength = size * (0.7f + (float)Math.random() * 0.6f * randomVecLength);
						double blockX = posX;
						double blockY = posY;
						double blockZ = posZ;
						for(float vecStep = 0; vecStep < vecLength; vecStep += LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * 1.5f - 0.225f) {
							blockX += xStep * LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * xzStrength;
							blockY += yStep * LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * yStrength;
							blockZ += zStep * LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * xzStrength;
							BlockPos pos = new BlockPos((int)blockX, (int)blockY, (int)blockZ);
							if(!level.isInWorldBounds(pos)) {
								break;
							}
							BlockState blockState = level.getBlockState(pos);
							FluidState fluidState = level.getFluidState(pos);
							if(!(isStrongExplosion && !fluidState.isEmpty())) {
								Optional<Float> explosionResistance = damageCalculator.getBlockExplosionResistance(this, level, pos, blockState, fluidState);
								if(explosionResistance.isPresent()) {
									vecLength -= (explosionResistance.get() + 0.3f) * 0.3f * resistanceImpact;
								}
								if(vecLength > 0 && damageCalculator.shouldBlockExplode(this, level, pos, blockState, vecLength) && !blockState.isAir()) {
									if(condition.conditionMet(level, pos, blockState, distance)) {
										blocks.add(encodeBlockPos(pos.subtract(posTNT).getX(), pos.subtract(posTNT).getY(), pos.subtract(posTNT).getZ()));
									}
								}
							}
							else {
								if(condition.conditionMet(level, pos, blockState, distance)) {
									blocks.add(encodeBlockPos(pos.subtract(posTNT).getX(), pos.subtract(posTNT).getY(), pos.subtract(posTNT).getZ()));
								}
							}
						}
					}
				}
			}
		}
		affectedBlocks.addAll(blocks);
		for(int intPos : blocks) {
			BlockPos pos = decodeBlockPos(intPos).offset(posTNT);
			double distance = Math.sqrt(pos.distToLowCornerSqr(posX, posY, posZ));
			blockEffect.doBlockExplosion(level, pos, level.getBlockState(pos), distance);
		}
	}
	
	/**
	 * Deprecated and will be removed in versions 1.21+. Please switch to {@link #doImprovedBlockExplosion(float, float, boolean, boolean, RandomSource, ExplosionRule)}, which is magnitudes more efficient.
	 * 
	 * Executes {@link ImprovedExplosion#doBlockExplosion(float, float, float, float, boolean, boolean, blockEffect)} with default values.
	 * @param blockEffect  determines what should happen to the blocks gotten by this explosion
	 */
	@Deprecated(since = "47.2.32.2", forRemoval = true)
	public void doBlockExplosion(IForEachBlockExplosionEffect blockEffect) {
		doBlockExplosion(1f, 1f, 1f, 1f, false, blockEffect);
	}
	
	/**
	 * Deprecated and will be removed in versions 1.21+. Please switch to {@link #doImprovedBlockExplosion(float, float, boolean, boolean, RandomSource, ExplosionRule)}, which is magnitudes more efficient.
	 * 
	 * Executes {@link ImprovedExplosion#doBlockExplosion(float, float, float, float, boolean, boolean, condition, blockEffect)} with default values.
	 * @param blockEffect  determines what should happen to the blocks gotten by this explosion
	 */
	@Deprecated(since = "47.2.32.2", forRemoval = true)
	public void doBlockExplosion(IBlockExplosionCondition condition, IForEachBlockExplosionEffect blockEffect) {
		doBlockExplosion(1f, 1f, 1f, 1f, false, condition, blockEffect);
	}
	
	/**
	 * Deprecated and will be removed in versions 1.21+. Please switch to {@link #doImprovedBlockExplosion(float, float, boolean, boolean, RandomSource, ExplosionRule)}, which is magnitudes more efficient.
	 * 
	 * Executes {@link ImprovedExplosion#doBlockExplosion(float, float, float, float, boolean, boolean)} with default values.
	 */
	@Deprecated(since = "47.2.32.2", forRemoval = true)
	public void doBlockExplosion() {
		doBlockExplosion(1f, 1f, 1f, 1f, false, false);
	}
	
	/**
	 * Deprecated and will be removed in versions 1.21+. Please switch to {@link #doImprovedBlockExplosion(float, float, boolean, boolean, RandomSource, ExplosionRule)}, which is magnitudes more efficient.
	 * 
	 * Gets all blocks in an area calculated by shooting vectors to the borders of a cube determined by the {@link ImprovedExplosion#size} and destroys them.
	 * @param xzStrength  a multiplier to the x and z vector addition, which makes the explosion more powerful. It should not be set to high, otherwise blocks might be skipped
	 * @param yStrength  a multiplier to the y vector addition, which makes the explosion more powerful. It should not be set to high, otherwise blocks might be skipped
	 * @param resistanceImpact  the relative impact that explosion resistance of blocks has on the penetration force of explosion
	 * @param randomVecLength  the greater this value, the more distributed the length of the explosion vectors will be. Large explosions should have a value less than 1
	 * @param fire  whether or not the explosion should spawn fire afterwards
	 * @param isStrongExplosion  whether or not fluids should be ignored in the explosion resistance calculation. Very useful for large explosions
	 * @param saveBlockPos  whether or not affected blocks should be saved to be used externally
	 */
	@Deprecated(since = "47.2.32.2", forRemoval = true)
	public void doOldBlockExplosion(float xzStrength, float yStrength, float resistanceImpact, float randomVecLength, boolean fire, boolean isStrongExplosion, boolean saveBlockPos) {
		Set<BlockPos> blocks = new HashSet<>();
		for(int offX = -size; offX <= size; offX++) {
			for(int offY = -size; offY <= size; offY++) {
				for(int offZ = -size; offZ <= size; offZ++) {
					double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
					if(((int)distance == size && LuckyTNTLibConfigValues.PERFORMANT_EXPLOSION.get()) || (!LuckyTNTLibConfigValues.PERFORMANT_EXPLOSION.get() && (offX == -size || offX == size || offY == -size || offY == size || offZ == -size || offZ == size))) {
						double xStep = offX / distance;
						double yStep = offY / distance;
						double zStep = offZ / distance;
						float vecLength = size * (0.7f + (float)Math.random() * 0.6f * randomVecLength);
						double blockX = posX;
						double blockY = posY;
						double blockZ = posZ;
						for(float vecStep = 0; vecStep < vecLength; vecStep += LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * 1.5f - 0.225f) {
							blockX += xStep * LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * xzStrength;
							blockY += yStep * LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * yStrength;
							blockZ += zStep * LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * xzStrength;
							BlockPos pos = new BlockPos((int)blockX, (int)blockY, (int)blockZ);
							if(!level.isInWorldBounds(pos)) {
								break;
							}
							BlockState blockState = level.getBlockState(pos);
							FluidState fluidState = level.getFluidState(pos);
							if(!(isStrongExplosion && !fluidState.isEmpty())) {
								Optional<Float> explosionResistance = damageCalculator.getBlockExplosionResistance(this, level, pos, blockState, fluidState);
								if(explosionResistance.isPresent()) {
									vecLength -= (explosionResistance.get() + 0.3f) * 0.3f * resistanceImpact;
								}
								if(vecLength > 0 && damageCalculator.shouldBlockExplode(this, level, pos, blockState, vecLength) && !blockState.isAir()) {
									blocks.add(pos);
								}
							}
							else {
								blocks.add(pos);
							}
						}
					}
				}
			}
		}
		if(saveBlockPos) {
			BlockPos posTNT = new BlockPos(Mth.floor(posX), Mth.floor(posY), Mth.floor(posZ));
			for(BlockPos pos : blocks) {
				affectedBlocks.add(encodeBlockPos(pos.subtract(posTNT).getX(), pos.subtract(posTNT).getY(), pos.subtract(posTNT).getZ()));
			}
		}
		for(BlockPos pos : blocks) {
			level.getBlockState(pos).getBlock().onBlockExploded(level.getBlockState(pos), level, pos, this);
		}
		if(fire) {
			for(BlockPos pos : blocks) {
				if(Math.random() > 0.75f && level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolidRender(level, pos)) {
					level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
				}
			}
		}
	}
	
	/**
	 * Deprecated and will be removed in versions 1.21+. The new system makes this obsolete (it was obsolete before anyway, but well).
	 * 
	 * Encodes 3 coordinates into a singular int value. 
	 * Coordinates greater than the absolute value of 511 will be clamped to 511.
	 * @implNote coordinates given must be realtive coordinates to the center of the explosion
	 * @param x  the x position of the block
	 * @param y  the y position of the block
	 * @param z  the z position of the block
	 * @return encoded int containing information about x, y and z positions, all of which can have values between -511 and 511
	 */
	@Deprecated(since = "47.2.32.2", forRemoval = true)
	protected int encodeBlockPos(int x, int y, int z) {
		int x0 = Integer.signum(x);
		x = Math.abs(x) > 511 ? 511 : Math.abs(x);
		x0 = x0 == -1 ? 0b1000000000 : 0;		
		x += x0;
		
		x = x << 20;
		
		int y0 = Integer.signum(y);
		y = Math.abs(y) > 511 ? 511 : Math.abs(y);
		y0 = y0 == -1 ? 0b1000000000 : 0;
		y += y0;

		y = y << 10;
		
		int z0 = Integer.signum(z);
		z = Math.abs(z) > 511 ? 511 : Math.abs(z);
		z0 = z0 == -1 ? 0b1000000000 : 0;
		z += z0;
		
		return (x + y + z);
	}
	
	/**
	 * Deprecated and will be removed in versions 1.21+. The new system makes this obsolete (it was obsolete before anyway, but well).
	 * 
	 * Decodes an encoded value generated by {@link ImprovedExplosion#encodeBlockPos(int, int, int)} into a {@link BlockPos}.
	 * @param encodedVal  the position encoded by {@link ImprovedExplosion#encodeBlockPos(int, int, int)}
	 * @return BlockPos with the relative x, y and z coordinates decoded again with an absolute max value of 511
	 */
	@Deprecated(since = "47.2.32.2", forRemoval = true)
	protected BlockPos decodeBlockPos(int encodedVal) {
		int zRaw = (encodedVal & 0b00000000000000000000000111111111);
		int zNeg = (encodedVal & 0b00000000000000000000001000000000) >> 9;
		int yRaw = (encodedVal & 0b00000000000001111111110000000000) >> 10;
		int yNeg = (encodedVal & 0b00000000000010000000000000000000) >> 19;
		int xRaw = (encodedVal & 0b00011111111100000000000000000000) >> 20;
		int xNeg = (encodedVal & 0b00100000000000000000000000000000) >> 29;
		int xVal = xNeg == 1 ? -xRaw : xRaw;
		int yVal = yNeg == 1 ? -yRaw : yRaw;
		int zVal = zNeg == 1 ? -zRaw : zRaw;
		return new BlockPos(xVal, yVal, zVal);
	}
	
	/**
	 * Damages and knocks back all entities affected by this explosion.
	 * @param knockbackStrength  multiplier to the strength of the knockback
	 * @param damageEntities  whether or not entities should be damaged by this explosion
	 */
	public void doEntityExplosion(float knockbackStrength, boolean damageEntities) {
		doEntityExplosion((Entity entity, double distance) -> {
			double offX = (entity.getX() - posX);
			double offY = (entity.getEyeY() - posY);
			double offZ = (entity.getZ() - posZ);
			double distance2 = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
			offX /= distance2;
			offY /= distance2;
			offZ /= distance2;
			double seenPercent = getSeenPercent(getPosition(), entity);
			float damage = (1f - (float)distance) * (float)seenPercent;
			if (damageEntities) {
				entity.hurt(getDamageSource(), (damage * damage + damage) / 2f * 7f * size + 1f);
			}
			double knockback = damage;
			if (entity instanceof LivingEntity lEnt) {
				knockback = ProtectionEnchantment.getExplosionKnockbackAfterDampener(lEnt, damage);
			}
			entity.setDeltaMovement(entity.getDeltaMovement().add(offX * knockback * knockbackStrength, offY * knockback * knockbackStrength, offZ * knockback * knockbackStrength));
			if (entity instanceof Player) {
				Player player = (Player)entity;
				player.hurtMarked = true;
				if (!player.isSpectator() && (!player.isCreative() || !player.getAbilities().flying)) {
					getHitPlayers().put(player, new Vec3(offX * damage, offY * damage, offZ * damage));
				}
			}
		});
	}
	
	/**
	 * Affects all entities in the reach of this explosion.
	 * Each entity is handled individually by the given {@link EntityExplosionEffect}.
	 * @param entityEffect  determines what should be done to each entity inside the radius
	 */
	public void doEntityExplosion(EntityExplosionEffect entityEffect) {
		List<Entity> entities = level.getEntities(getExploder(), new AABB(posX - size * 2d, posY - size * 2d, posZ - size * 2d, posX + size * 2d, posY + size * 2d, posZ + size * 2d));
		ForgeEventFactory.onExplosionDetonate(level, this, entities, size * 2d);
		for (Entity entity : entities) {
			if (!entity.ignoreExplosion()) {
				double distance = Math.sqrt(entity.distanceToSqr(posX, posY, posZ)) / (size * 2d);
				if (distance <= 1f) {
					entityEffect.handleEntity(entity, distance);
				}
			}
		}
	}
	
	/**
	 * Spawns explosion particles in an area around the center of this explosion.
	 * Only works on the server sided level.
	 * Particle count, distribution, speed, and whether or not poof particles are spawned, is determined by the size of this explosion.
	 */
	public void spawnExplosionParticles() {
		if (level instanceof ServerLevel serverLevel) {
			if (size >= 50) {
				for (ServerPlayer player : serverLevel.players()) {
					serverLevel.sendParticles(player, ParticleTypes.EXPLOSION, true, posX, posY + 0.5d, posZ, Math.min(size * size / 4, 5000), Math.min(size / 4d, 6d), Math.min(size / 4d, 6d), Math.min(size / 4d, 6d), 0d);
					serverLevel.sendParticles(player, ParticleTypes.POOF, true, posX, posY, posZ, Math.min(size * size, 10000), 0d, 0d, 0d, Math.min(size / 8d, 1.5d));
				}
			} else {
				serverLevel.sendParticles(ParticleTypes.EXPLOSION, posX, posY + 0.5d, posZ, Math.min(size * size / 4, 5000), Math.min(size / 4d, 6d), Math.min(size / 4d, 6d), Math.min(size / 4d, 6d), 0d);
				if (size > 2) {
					serverLevel.sendParticles(ParticleTypes.POOF, posX, posY, posZ, Math.min(size * size, 10000), 0d, 0d, 0d, Math.min(size / 8d, 1.5d));
				}
			}
		}
	}
	
	/**
	 * Attempts to retrieve the owning entity of this explosion, if one was passed along.
	 * @return the owning entity of this explosion, if it exists, otherwise {@code null}
	 */
	@Nullable
	@Override
	public LivingEntity getIndirectSourceEntity() {
		if (getExploder() instanceof IExplosiveEntity ent) {
			return ent.owner();
		}
		return super.getIndirectSourceEntity();
	}
	
	/**
	 * Use a dummy explosions in methods that require a non-null explosion, but the explosion itself remains unused.
	 * @param level  the level that needs to be given
	 * @return ImprovedExplosion with no strength and position at (0, 0, 0)
	 */
	public static ImprovedExplosion dummyExplosion(Level level) {
		return new ImprovedExplosion(level, new Vec3(0, 0, 0), 0);
	}

	/**
	 * Use {@link #doImprovedBlockExplosion(float, float, boolean, boolean, ExplosionRule)}, {@link #doEntityExplosion(float, boolean)}, or {@link #doEntityExplosion(EntityExplosionEffect)}.
	 */
	@Override
	@Deprecated
	public void explode() {
	}
	
	/**
	 * Performs a standard full explosion and optionally spawns particles.
	 * It is advised to not use this in favor of {@link #doImprovedBlockExplosion(float, float, boolean, boolean, ExplosionRule)}, {@link #doEntityExplosion(float, boolean)},
	 * {@link #doEntityExplosion(EntityExplosionEffect)}, and {@link #spawnExplosionParticles()}.
	 */
	@Override
	@Deprecated
	public void finalizeExplosion(boolean spawnParticles) {
		if (level.isClientSide()) {
			return;
		}
		if (spawnParticles) {
			spawnExplosionParticles();
		}
		doEntityExplosion(size / 10f, true);
		doImprovedBlockExplosion(1f, 1f, false, false, null);
	}
	
	/**
	 * Due to performance reasons, affected blocks are no longer saved and can therefore no longer be retrieved.
	 * This method will therefore always return an empty list.
	 * @return An empty list.
	 */
	@Override
	@Deprecated
	public List<BlockPos> getToBlow() {
		return List.of();
	}
}
