package luckytntlib.util.explosions;

import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import org.joml.Vector3f;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * A singular task of a multi-threaded explosion used in a {@link ForkJoinPool}, either splitting itself further if its workload is too large,
 * merging the data of its sub-tasks once they finished computing,
 * or computing its result and merging it with its parent task.
 * <p>
 * Each task that does not split itself further will compute a sublist of the list of vectors given by an {@link ImprovedExplosion}, marking all blocks in its vectors' paths using ray-tracing.
 * Data is cached and stored in such a way that threads will block each other as little as possible while still saving as much memory as possible.
 */
public class ExplosionTask extends RecursiveTask<Long2ObjectMap<BitSet>> {
	
	private static final float[] EMPTY_SECTION = new float[0];
	
	private final ExplosionThread explosionThread;
	private final ImprovedExplosion explosion;
	private final Level level;
	private final float x, y, z;
	private final float resistanceFac;
	private final boolean ignoreFluids;
	private final int allowedSize;
	private final List<Vector3f> vectors;
	
	ExplosionTask(ExplosionThread explosionThread, ImprovedExplosion explosion, float resistanceFac, boolean ignoreFluids, int allowedSize, List<Vector3f> vectors) {
		this.explosionThread = explosionThread;
		this.explosion = explosion;
		this.level = explosion.level;
		this.x = (float)explosion.posX;
		this.y = (float)explosion.posY;
		this.z = (float)explosion.posZ;
		this.resistanceFac = resistanceFac;
		this.ignoreFluids = ignoreFluids;
		this.allowedSize = allowedSize;
		this.vectors = vectors;
	}
	
	/**
	 * Splits itself into two smaller tasks if too many vectors have been supplied.
	 * Amount of vectors per task is determined to be either 50000 or the total amount of vectors divided by the available processor count.
	 */
	@Override
	protected Long2ObjectMap<BitSet> compute() {
		if (vectors.size() > allowedSize) {
			ExplosionTask task1 = new ExplosionTask(explosionThread, explosion, resistanceFac, ignoreFluids, allowedSize, vectors.subList(0, vectors.size() / 2));
			ExplosionTask task2 = new ExplosionTask(explosionThread, explosion, resistanceFac, ignoreFluids, allowedSize, vectors.subList(vectors.size() / 2, vectors.size()));
			task1.fork();
			task2.fork();
			Long2ObjectMap<BitSet> toMergeBitSets = task1.join();
			Long2ObjectMap<BitSet> resultBitSets = task2.join();
			for (long key : toMergeBitSets.keySet()) {
				if (resultBitSets.containsKey(key)) {
					resultBitSets.get(key).or(toMergeBitSets.get(key));
					if (resultBitSets.get(key).nextClearBit(0) >= 4096) {
						resultBitSets.remove(key);
						explosionThread.fullSections.add(key);
					}
				} else {
					resultBitSets.put(key, toMergeBitSets.get(key));
				}
			}
			return resultBitSets;
		}
		return calculate();
	}

	/**
	 * Functionally equivalent to the single-threaded method.
	 */
	@SuppressWarnings("deprecation")
	private Long2ObjectMap<BitSet> calculate() {
		Long2ObjectMap<BitSet> editedSections = new Long2ObjectOpenHashMap<BitSet>();

		for (Vector3f v : vectors) {
			float vectorLength = v.length();
			float xStep = v.x / vectorLength * 0.3f;
			float yStep = v.y / vectorLength * 0.3f;
			float zStep = v.z / vectorLength * 0.3f;
			float blockX = x;
			float blockY = y;
			float blockZ = z;
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
				if ((sectionPos == lastSectionPos && sectionEmpty) || explosionThread.emptySections.contains(sectionPos)) {
					sectionEmpty = true;
					vectorLength -= 0.3f * resistanceFac;
					continue;
				}
				sectionEmpty = false;
				if (sectionPos != lastSectionPos) {
					int chunkX = pos.getX() >> 4;
					int chunkY = pos.getY() >> 4;
					int chunkZ = pos.getZ() >> 4;
					currentExplosionResistances = explosionThread.sectionResistances.computeIfAbsent(sectionPos, sp -> {
						LevelChunkSection section = level.getChunk(chunkX, chunkZ).getSection(chunkY - level.getMinSection());
						if (section.hasOnlyAir()) {
							return EMPTY_SECTION;
						}
						float[] explosionResistances = new float[4096];
						BlockState currentBlockState;
						for (int x = 0; x < 16; x++) {
							for (int y = 0; y < 16; y++) {
								for (int z = 0; z < 16; z++) {
									currentBlockState = section.getBlockState(x, y, z);
									explosionResistances[x << 8 | y << 4 | z] = ignoreFluids && !currentBlockState.getFluidState().isEmpty() ? 0f : Math.max(currentBlockState.getBlock().getExplosionResistance(), currentBlockState.getFluidState().getExplosionResistance());								}
							}
						}
						return explosionResistances;
					});
					if (currentExplosionResistances == EMPTY_SECTION) {
						explosionThread.emptySections.add(sectionPos);
						vectorLength -= 0.3f * resistanceFac;
						continue;
					}
					bitSet = editedSections.computeIfAbsent(sectionPos, sp -> new BitSet(4096));
				}
				int blockIndex = ((pos.getX() & 15) << 8) | ((pos.getY() & 15) << 4) | (pos.getZ() & 15);
				vectorLength -= (currentExplosionResistances[blockIndex] + 0.3f) * resistanceFac;
				if (vectorLength <= 0f) {
					break;
				}
				if ((sectionPos == lastSectionPos && sectionFull) || explosionThread.fullSections.contains(sectionPos)) {
					sectionFull = true;
					continue;
				}
				sectionFull = false;
				lastSectionPos = sectionPos;
				if (!bitSet.get(blockIndex)) {
					bitSet.set(blockIndex);
					if (bitSet.nextClearBit(0) >= 4096) {
						editedSections.remove(sectionPos);
						explosionThread.fullSections.add(sectionPos);
					}
				}
			}
		}
		return editedSections;
	}
}
