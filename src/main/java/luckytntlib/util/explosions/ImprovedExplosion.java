package luckytntlib.util.explosions;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import org.joml.Vector3f;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import luckytntlib.config.LuckyTNTLibConfigValues;
import luckytntlib.network.ClientboundSetupExplosionPacket;
import luckytntlib.network.ClientboundUpdateChunkSectionPacket;
import luckytntlib.network.PacketHandler;
import luckytntlib.util.IExplosiveEntity;
import luckytntlib.util.explosions.rules.ExplosionRule;
import luckytntlib.util.explosions.rules.FireExplosionRule;
import luckytntlib.util.light.LightUpdateHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.EntityBasedExplosionDamageCalculator;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.PacketDistributor;

/**
 * ImprovedExplosion is an extension of Minecraft's {@link Explosion}.
 * It is needed because the explosion of minecraft is rather limited in functionality and size,
 * while an ImprovedExplosion has no limit in its size and offers multiple and dynamic ways to interact with and customize the explosion.
 */
public class ImprovedExplosion extends Explosion {

	public final ServerLevel level;
	public final double posX, posY, posZ;
	public final int size;
	public final ExplosionDamageCalculator damageCalculator;
	@Deprecated(forRemoval = true)
	List<Integer> affectedBlocks = new ArrayList<>();
	
	private static ImprovedExplosion dummyExplosion;
	
	/**
	 * Creates a new ImprovedExplosion
	 * @param level  the level
	 * @param position  the center position of the explosion
	 * @param size  the rough size of the explosion, which must not be greater than 511 in most cases
	 */
	public ImprovedExplosion(ServerLevel level, Vec3 position, int size) {
		this(level, null, null, position, size);
	}
	
	/**
	 * Creates a new ImprovedExplosion
	 * @param level  the level
	 * @param source  the DamageSource this explosion uses
	 * @param position  the center position of the explosion
	 * @param size  the rough size of the explosion, which must not be greater than 511 in most cases
	 */
	public ImprovedExplosion(ServerLevel level, @Nullable DamageSource source, Vec3 position, int size) {
		this(level, null, source, position, size);
	}
	
	/**
	 * Creates a new ImprovedExplosion
	 * @param level  the level
	 * @param entity  the entity not affected by this explosion. Should be the entity causing the explosion and also an IExplosiveEntity
	 * @param position  the center position of the explosion
	 * @param size  the rough size of the explosion, which must not be greater than 511 in most cases
	 */	
	public ImprovedExplosion(ServerLevel level, @Nullable Entity explodingEntity, Vec3 position, int size) {
		this(level, explodingEntity, null, position.x, position.y, position.z, size);
	}
	
	/**
	 * Creates a new ImprovedExplosion
	 * @param level  the level
	 * @param entity  the entity not affected by this explosion. Should be the entity causing the explosion and also an IExplosiveEntity
	 * @param source  the DamageSource this explosion uses
	 * @param position  the center position of the explosion
	 * @param size  the rough size of the explosion, which must not be greater than 511 in most cases
	 */	
	public ImprovedExplosion(ServerLevel level, @Nullable Entity explodingEntity, @Nullable DamageSource source, Vec3 position, int size) {
		this(level, explodingEntity, source, position.x, position.y, position.z, size);
	}
	
	/**
	 * Creates a new ImprovedExplosion
	 * @param level  the level
	 * @param entity  the entity not affected by this explosion. Should be the entity causing the explosion and also an IExplosiveEntity
	 * @param x  the x center position
	 * @param y  the y center position
	 * @param z  the z center position
	 * @param size  the rough size of the explosion, which must not be greater than 511 in most cases
	 */	
	public ImprovedExplosion(ServerLevel level, @Nullable Entity explodingEntity, double x, double y, double z, int size) {
		this(level, explodingEntity, null, x, y, z, size);
	}
	
	/**
	 * Creates a new ImprovedExplosion
	 * @param level  the level
	 * @param entity  the entity not affected by this explosion. Should be the entity causing the explosion and also an IExplosiveEntity
	 * @param source  the DamageSource this explosion uses
	 * @param x  the x center position
	 * @param y  the y center position
	 * @param z  the z center position
	 * @param size  the rough size of the explosion, which must not be greater than 511 in most cases
	 */	
	public ImprovedExplosion(ServerLevel level, @Nullable Entity explodingEntity, @Nullable DamageSource source, double x, double y, double z, int size) {
		super(level, explodingEntity, source, null, x, y, z, size, false, BlockInteraction.KEEP);
		this.level = level;
		this.posX = x;
		this.posY = y;
		this.posZ = z;
		this.size = size;
		damageCalculator = explodingEntity == null ? new ExplosionDamageCalculator() : new EntityBasedExplosionDamageCalculator(explodingEntity);
	}
	
	/**
	 * Executes a block explosion using either a single or multiple threads based on explosion size and user settings.
	 * This method exits if it is not executed on the server side, as the explosion wouldn't work.
	 * @param resistanceImpact  the relative impact that explosion resistance of blocks has on the penetration force of the explosion
	 * @param randomVecLength  the greater this value, the more distributed the length of the explosion vectors will be. Large explosions should have a value less than 1
	 * @param ignoreFluidResistance  whether or not fluids should be ignored in the explosion resistance calculation
	 * @param fire  whether or not the explosion should spawn fire afterwards
	 * @param random  random number generator
	 * @param rule  optional rule for causing effects other than just destruction. Leave as null for an efficient explosion that destroys blocks
	 */
	public void doImprovedBlockExplosion(float resistanceImpact, float randomVecLength, boolean ignoreFluidResistance, boolean fire, @Nullable ExplosionRule rule) {
		if (level.isClientSide()) {
			return;
		}
		if (LuckyTNTLibConfigValues.MULTITHREADED_EXPLOSIONS.get() && size >= 30) {
			doImprovedBlockExplosionMultithreaded(resistanceImpact, randomVecLength, ignoreFluidResistance, fire, rule);
		} else {
			doImprovedBlockExplosionSinglethreaded(resistanceImpact, randomVecLength, ignoreFluidResistance, fire, rule);
		}
	}
	
	/**
	 * Multithreads the improved block explosion, queueing the result to be applied in the next tick.
	 * Due to being multithreaded, the game and other explosions will run in the background while the explosion loads.
	 * The amount of parallel explosions is limited by the user settings, making many simultaneous explosions be queued.
	 */
	protected void doImprovedBlockExplosionMultithreaded(float resistanceImpact, float randomVecLength, boolean ignoreFluidResistance, boolean fire, @Nullable ExplosionRule rule) {			
		float randomVecLengthFac = 0.6f * randomVecLength;
		float resistanceFac = 0.675f * resistanceImpact;
		RandomSource random = level.getRandom();

		List<Vector3f> vectors = new ArrayList<Vector3f>((int)(4 * size * size * Math.PI + 10));
		
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
		
		ExplosionThread thread = new ExplosionThread(this, resistanceFac, randomVecLengthFac, ignoreFluidResistance, fire, rule, vectors);
		MultithreadExplosionHandler.enqueue(thread);
	}

	
	/**
	 * Raycasts onto the surface of a sphere determined by the size of this explosion.
	 * Each ray uses the explosion resistances of blocks to reduce their length.
	 * Rays are further lengthened or shortend using random generation.
	 * Blocks in a ray's remaining path are marked efficiently, making sure RAM and CPU are optimally utilized.
	 * Blocks are only marked by this method.
	 */
	protected void doImprovedBlockExplosionSinglethreaded(float resistanceImpact, float randomVecLength, boolean ignoreFluidResistance, boolean fire, @Nullable ExplosionRule rule) {			
		float randomVecLengthFac = 0.6f * randomVecLength;
		float resistanceFac = 0.3f * resistanceImpact * 2.25f;
		RandomSource random = level.getRandom();
		
		List<Vector3f> vectors = new ArrayList<Vector3f>((int)(4 * size * size * Math.PI + 10));
		
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
		/*
		 * We use BitSets to only take up 1 bit per block, also making sure a singular chunk section is utilizing cache locality.
		 * Sections that are empty are marked to be skipped.
		 * Sections that have every block marked are saved more efficiently.
		 */
		Long2ObjectMap<BitSet> editedSections = new Long2ObjectOpenHashMap<BitSet>();
		Set<Long> fullSections = new LongOpenHashSet();
		Set<Long> emptySections = new LongOpenHashSet();
		Long2ObjectMap<float[]> sectionResistances = new Long2ObjectOpenHashMap<float[]>();
		
		for (Vector3f v : vectors) {
			float vectorLength = v.length();
			float xStep = v.x / vectorLength * 0.3f;
			float yStep = v.y / vectorLength * 0.3f;
			float zStep = v.z / vectorLength * 0.3f;
			float blockX = (float)posX;
			float blockY = (float)posY;
			float blockZ = (float)posZ;
			int lastPosX = 0;
			int lastPosY = -10000;
			int lastPosZ = 0;
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			BlockPos.MutableBlockPos innerPos = new BlockPos.MutableBlockPos();
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
				pos.set((int)blockX, (int)blockY, (int)blockZ);
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
									innerPos.set((chunkX << 4) + x, (chunkY << 4) + y, (chunkZ << 4) + z);
									explosionResistances[x << 8 | y << 4 | z] = ignoreFluidResistance && !currentBlockState.getFluidState().isEmpty() ? 0f : damageCalculator.getBlockExplosionResistance(this, level, innerPos, currentBlockState, currentBlockState.getFluidState()).orElse(0f);
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
		
		finishImprovedExplosion(editedSections, fullSections, rule);
		if (fire) {
			float sizeReduction = (float)Math.sqrt(Math.sqrt(size));
			ImprovedExplosion fireExplosion = new ImprovedExplosion(level, getPosition(), Math.round(size / sizeReduction));
			fireExplosion.doImprovedBlockExplosion(1f, 1.2f * sizeReduction, false, false, new FireExplosionRule(1f / sizeReduction));
		}
	}
	
	/**
	 * Finishes an explosion, both multithreaded and singlethreaded, by applying an optional rule to all marked blocks or simply removing them if null is given as a rule.
	 * @param editedSections  the chunk sections which were only partially affected by the explosion
	 * @param fullSections  the chunk sections which are fully affected by the explosion
	 * @param rule  an optional rule for applying effects other than just destroying all marked blocks
	 */
	protected void finishImprovedExplosion(Map<Long, BitSet> editedSections, Set<Long> fullSections, @Nullable ExplosionRule rule) {
		if (rule == null) {
			finishImprovedExplosionWithoutRule(editedSections, fullSections);
		} else {
			finishImprovedExplosionWithRule(editedSections, fullSections, rule);
		}
	}
	
	/**
	 * Removes all blocks that are given, having a high level of performance.
	 * For this reason, block updates are omitted.
	 * Sky light is automatically updated, block light is not.
	 */
	private void finishImprovedExplosionWithoutRule(Map<Long, BitSet> editedSections, Set<Long> fullSections) {
		HashMap<LevelChunk, BitSet> chunks = new HashMap<>();
		
		for(long encodedPos : fullSections) {
			SectionPos pos = SectionPos.of(encodedPos);
			LevelChunk chunk = level.getChunk(pos.x(), pos.z());
			LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(pos.y()));
			PalettedContainer<BlockState> states = section.getStates();
			
			chunk.setLoaded(true);
			
			for(short s = 0; s < 4096; s++) {
				BlockState state = states.getAndSet((s >> 8) & 15, (s >> 4) & 15, s & 15, Blocks.AIR.defaultBlockState());
				state.getBlock().wasExploded(level, new BlockPos((pos.x() << 4) + ((s >> 8) & 15), (pos.y() << 4) + ((s >> 4) & 15), (pos.z() << 4) + (s & 15)), this);
			}
			
			section.recalcBlockCounts();
			
			PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new ClientboundUpdateChunkSectionPacket(SectionPos.of(pos.x(), level.getSectionIndexFromSectionY(pos.y()), pos.z()), new BitSet(0), true, false));
			
			if(!chunks.containsKey(chunk)) {
				chunks.put(chunk, new BitSet());
			}
			chunks.get(chunk).set(pos.y() - (level.getMinSection() - 1));
			
			chunk.setUnsaved(true);
		}
		
		for(Entry<Long, BitSet> entry : editedSections.entrySet()) {
			SectionPos pos = SectionPos.of(entry.getKey());
			BitSet removedBlocks = entry.getValue();
			LevelChunk chunk = level.getChunk(pos.x(), pos.z());
			LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(pos.y()));
			PalettedContainer<BlockState> states = section.getStates();
			
			chunk.setLoaded(true);
			
			for(short s = 0; s < 4096; s++) {
				if(removedBlocks.get(s)) {
					BlockState state = states.getAndSet((s >> 8) & 15, (s >> 4) & 15, s & 15, Blocks.AIR.defaultBlockState());
					state.getBlock().wasExploded(level, new BlockPos((pos.x() << 4) + ((s >> 8) & 15), (pos.y() << 4) + ((s >> 4) & 15), (pos.z() << 4) + (s & 15)), this);
				}
			}
			
			section.recalcBlockCounts();
			
			PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new ClientboundUpdateChunkSectionPacket(SectionPos.of(pos.x(), level.getSectionIndexFromSectionY(pos.y()), pos.z()), removedBlocks, false, false));
			
			if(!chunks.containsKey(chunk)) {
				chunks.put(chunk, new BitSet());
			}
			chunks.get(chunk).set(pos.y() - (level.getMinSection() - 1));
			
			chunk.setUnsaved(true);
		}
		
		LightUpdateHelper.updateDirectSkyLight((ServerLevel)level, chunks);
		LightUpdateHelper.updateIndirectSkyLight((ServerLevel)level, chunks);
	}
	
	/**
	 * Affects all blocks that are given using the provided explosion rule.
	 * Block updates are omitted to save performance.
	 * Sky light is automatically updated, block light is not.
	 */
	private void finishImprovedExplosionWithRule(Map<Long, BitSet> editedSections, Set<Long> fullSections, ExplosionRule rule) {
		HashMap<LevelChunk, BitSet> chunks = new HashMap<>();
		PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new ClientboundSetupExplosionPacket(rule, BlockPos.containing(posX, posY, posZ)));

		for(long encodedPos : fullSections) {
			SectionPos pos = SectionPos.of(encodedPos);
			LevelChunk chunk = level.getChunk(pos.x(), pos.z());
			LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(pos.y()));
			PalettedContainer<BlockState> states = section.getStates();
			
			chunk.setLoaded(true);
			
			for(short s = 0; s < 4096; s++) {
				int xl = (s >> 8) & 15;
				int yl = (s >> 4) & 15;
				int zl = s & 15;
				int x = (pos.x() << 4) + xl;
				int y = (pos.y() << 4) + yl;
				int z = (pos.z() << 4) + zl;
				BlockState state = states.get(xl, yl, zl);
				if (rule.shouldApply(level, state, getPosition(), x - Mth.floor(posX), y - Mth.floor(posY), z - Mth.floor(posZ))) {
					state.getBlock().wasExploded(level, new BlockPos(x, y, z), this);
					states.set(xl, yl, zl, rule.getState());
				}
			}
			
			section.recalcBlockCounts();
			
			PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new ClientboundUpdateChunkSectionPacket(SectionPos.of(pos.x(), level.getSectionIndexFromSectionY(pos.y()), pos.z()), new BitSet(0), true, false));
			
			if(!chunks.containsKey(chunk)) {
				chunks.put(chunk, new BitSet());
			}
			chunks.get(chunk).set(pos.y() - (level.getMinSection() - 1));
			
			chunk.setUnsaved(true);
		}
		
		for(Entry<Long, BitSet> entry : editedSections.entrySet()) {
			SectionPos pos = SectionPos.of(entry.getKey());
			BitSet affectedBlocks = entry.getValue();
			LevelChunk chunk = level.getChunk(pos.x(), pos.z());
			LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(pos.y()));
			PalettedContainer<BlockState> states = section.getStates();
			
			chunk.setLoaded(true);
			
			for(short s = 0; s < 4096; s++) {
				if(affectedBlocks.get(s)) {
					int xl = (s >> 8) & 15;
					int yl = (s >> 4) & 15;
					int zl = s & 15;
					int x = (pos.x() << 4) + xl;
					int y = (pos.y() << 4) + yl;
					int z = (pos.z() << 4) + zl;
					BlockState state = states.get(xl, yl, zl);
					if (rule.shouldApply(level, state, getPosition(), x - Mth.floor(posX), y - Mth.floor(posY), z - Mth.floor(posZ))) {
						state.getBlock().wasExploded(level, new BlockPos(x, y, z), this);
						states.set(xl, yl, zl, rule.getState());
					}
				}
			}
			
			section.recalcBlockCounts();
			
			PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new ClientboundUpdateChunkSectionPacket(SectionPos.of(pos.x(), level.getSectionIndexFromSectionY(pos.y()), pos.z()), affectedBlocks, false, false));
			
			if(!chunks.containsKey(chunk)) {
				chunks.put(chunk, new BitSet());
			}
			chunks.get(chunk).set(pos.y() - (level.getMinSection() - 1));
			
			chunk.setUnsaved(true);
		}
		
		LightUpdateHelper.updateDirectSkyLight((ServerLevel)level, chunks);
		LightUpdateHelper.updateIndirectSkyLight((ServerLevel)level, chunks);
	}
	
	/**
	 * Deprecated and will be removed in versions 1.21+. Please switch to {@link ImprovedExplosion#doImprovedBlockExplosion(float, float, boolean, boolean, RandomSource, ExplosionRule)}, which is magnitudes more efficient.
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
	@Deprecated(since = "47.2.32.1", forRemoval = true)
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
	 * Deprecated and will be removed in versions 1.21+. Please switch to {@link ImprovedExplosion#doImprovedBlockExplosion(float, float, boolean, boolean, RandomSource, ExplosionRule)}, which is magnitudes more efficient.
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
	@Deprecated(since = "47.2.32.1", forRemoval = true)
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
	 * Deprecated and will be removed in versions 1.21+. Please switch to {@link ImprovedExplosion#doImprovedBlockExplosion(float, float, boolean, boolean, RandomSource, ExplosionRule)}, which is magnitudes more efficient.
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
	@Deprecated(since = "47.2.32.1", forRemoval = true)
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
	 * Deprecated and will be removed in versions 1.21+. Please switch to {@link ImprovedExplosion#doImprovedBlockExplosion(float, float, boolean, boolean, RandomSource, ExplosionRule)}, which is magnitudes more efficient.
	 * 
	 * Executes {@link ImprovedExplosion#doBlockExplosion(float, float, float, float, boolean, boolean, blockEffect)} with default values.
	 * @param blockEffect  determines what should happen to the blocks gotten by this explosion
	 */
	@Deprecated(since = "47.2.32.1", forRemoval = true)
	public void doBlockExplosion(IForEachBlockExplosionEffect blockEffect) {
		doBlockExplosion(1f, 1f, 1f, 1f, false, blockEffect);
	}
	
	/**
	 * Deprecated and will be removed in versions 1.21+. Please switch to {@link ImprovedExplosion#doImprovedBlockExplosion(float, float, boolean, boolean, RandomSource, ExplosionRule)}, which is magnitudes more efficient.
	 * 
	 * Executes {@link ImprovedExplosion#doBlockExplosion(float, float, float, float, boolean, boolean, condition, blockEffect)} with default values.
	 * @param blockEffect  determines what should happen to the blocks gotten by this explosion
	 */
	@Deprecated(since = "47.2.32.1", forRemoval = true)
	public void doBlockExplosion(IBlockExplosionCondition condition, IForEachBlockExplosionEffect blockEffect) {
		doBlockExplosion(1f, 1f, 1f, 1f, false, condition, blockEffect);
	}
	
	/**
	 * Deprecated and will be removed in versions 1.21+. Please switch to {@link ImprovedExplosion#doImprovedBlockExplosion(float, float, boolean, boolean, RandomSource, ExplosionRule)}, which is magnitudes more efficient.
	 * 
	 * Executes {@link ImprovedExplosion#doBlockExplosion(float, float, float, float, boolean, boolean)} with default values.
	 */
	@Deprecated(since = "47.2.32.1", forRemoval = true)
	public void doBlockExplosion() {
		doBlockExplosion(1f, 1f, 1f, 1f, false, false);
	}
	
	/**
	 * Deprecated and will be removed in versions 1.21+. Please switch to {@link ImprovedExplosion#doImprovedBlockExplosion(float, float, boolean, boolean, RandomSource, ExplosionRule)}, which is magnitudes more efficient.
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
	@Deprecated(since = "47.2.32.1", forRemoval = true)
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
	@Deprecated(since = "47.2.32.1", forRemoval = true)
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
	@Deprecated(since = "47.2.32.1", forRemoval = true)
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
	 * Damages and throws back all entities affected by this explosion determined by the {@link ImprovedExplosion#size}.
	 * @param knockbackStrength  multiplier to the strength of the knockback
	 * @param damageEntities  whether or not entities should be damaged by this explosion
	 */
	public void doEntityExplosion(float knockbackStrength, boolean damageEntities) {
		List<Entity> entities = level.getEntities(getExploder(), new AABB(posX - size * 2, posY - size * 2, posZ - size * 2, posX + size * 2, posY + size * 2, posZ + size * 2));
		ForgeEventFactory.onExplosionDetonate(level, this, entities, size * 2);
		for(Entity entity : entities) {
			if(!entity.ignoreExplosion()) {
				double distance = Math.sqrt(entity.distanceToSqr(getPosition())) / (size * 2);
				if(distance <= 1f) {
					double offX = (entity.getX() - posX);
					double offY = (entity.getEyeY() - posY);
					double offZ = (entity.getZ() - posZ);
					double distance2 = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
					offX /= distance2;
					offY /= distance2;
					offZ /= distance2;
					double seenPercent = getSeenPercent(getPosition(), entity);
					float damage = (1f - (float)distance) * (float)seenPercent;
					if(damageEntities) {
						entity.hurt(getDamageSource(), (damage * damage + damage) / 2f * 7 * size + 1f);
					}
					double knockback = damage;
					if(entity instanceof LivingEntity lEnt) {
						knockback = ProtectionEnchantment.getExplosionKnockbackAfterDampener(lEnt, damage);
					}
					entity.setDeltaMovement(entity.getDeltaMovement().add(offX * knockback * knockbackStrength, offY * knockback * knockbackStrength, offZ * knockback * knockbackStrength));
					if(entity instanceof Player) {
						Player player = (Player)entity;
						player.hurtMarked = true;
						if(!player.isSpectator() && (!player.isCreative() || !player.getAbilities().flying)) {
							getHitPlayers().put(player, new Vec3(offX * damage, offY * damage, offZ * damage));
						}
					}
				}
			}
		}
	}
	
	/**
	 * Does whatever specified in the {@link IForEachBlockExplosionEffect} to all entities gotten by this explosion,
	 * which is determined by the {@link ImprovedExplosion#size}.
	 * @param entityEffect  determines what should be done to the entities gotten by this explosion
	 */
	public void doEntityExplosion(EntityExplosionEffect entityEffect) {
		List<Entity> entities = level.getEntities(getExploder(), new AABB(posX - size * 2d, posY - size * 2d, posZ - size * 2d, posX + size * 2d, posY + size * 2d, posZ + size * 2d));
		ForgeEventFactory.onExplosionDetonate(level, this, entities, size * 2d);
		for(Entity entity : entities) {
			if(!entity.ignoreExplosion()) {
				double distance = Math.sqrt(entity.distanceToSqr(posX, posY, posZ)) / size * 2d;
				if (distance <= 1f) {
					entityEffect.handleEntity(entity, distance);
				}
			}
		}
	}
	
	/**
	 * Spawns particles on the server side.
	 * Particle count, distribution, speed, and whether or not poof particles are spawned, is determined by the size of this explosion.
	 */
	public void spawnExplosionParticles() {
		level.sendParticles(ParticleTypes.EXPLOSION, posX, posY + 0.5d, posZ, Math.min(size * size / 4, 5000), Math.min(size / 4d, 6d), Math.min(size / 4d, 6d), Math.min(size / 4d, 6d), 0d);
		if (size > 2) {
			level.sendParticles(ParticleTypes.POOF, posX, posY, posZ, Math.min(size * size, 10000), 0d, 0d, 0d, Math.min(size / 8d, 1.5d));
		}
	}
	
	@Nullable
	@Override
	public LivingEntity getIndirectSourceEntity() {
		if(getExploder() instanceof IExplosiveEntity ent) {
			return ent.owner();
		}
		return super.getIndirectSourceEntity();
	}
	
	/** 
	 * @apiNote Use a dummy explosions in methods that need an explosion to be given.
	 * Do not use it to create an actual explosion, as it will do nothing.
	 * @return ImprovedExplosion with no strength and position at (0, 0, 0)
	 */
	public static ImprovedExplosion dummyExplosion(ServerLevel level) {
		return dummyExplosion == null ? dummyExplosion = new ImprovedExplosion(level, new Vec3(0, 0, 0), 0) : dummyExplosion;
	}

	@Override
	@Deprecated
	public void explode() {
	}
	
	@Override
	@Deprecated
	public void finalizeExplosion(boolean spawnParticles) {		
	}
	
	@Override
	@Deprecated
	public List<BlockPos> getToBlow(){
		List<BlockPos> blocks = new ArrayList<>();
		for(int intPos : affectedBlocks) {
			blocks.add(decodeBlockPos(intPos).offset(Mth.floor(posX), Mth.floor(posY), Mth.floor(posZ)));
		}
		return blocks;
	}
}
