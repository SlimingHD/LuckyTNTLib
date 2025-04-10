package luckytntlib.util.explosions;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

import org.joml.Vector3f;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;

public class MasterExplosionThread extends Thread {

	private static final int minVectorsPerThread = 50000;
	private static final ForkJoinPool pool = new ForkJoinPool();
	
	private final ImprovedExplosion explosion;
	private final float resistanceFac;
	private final boolean ignoreFluids;
	public final List<Vector3f> vectors;
	public final ConcurrentHashMap<Long, Long> emptySections = new ConcurrentHashMap<Long, Long>();
	public final ConcurrentHashMap<Long, Long> sectionsToRemove = new ConcurrentHashMap<Long, Long>();
	public final ConcurrentHashMap<Long, float[]> sectionResistances = new ConcurrentHashMap<Long, float[]>();
	
	public MasterExplosionThread(ImprovedExplosion explosion, float resistanceFac, boolean ignoreFluids, List<Vector3f> vectors) {
		this.explosion = explosion;
		this.resistanceFac = resistanceFac;
		this.ignoreFluids = ignoreFluids;
		this.vectors = vectors;
	}
	
	@Override
	public void run() {
		HashMap<SectionPos, BitSet> editedSections = new HashMap<SectionPos, BitSet>();
		HashMap<Long, SectionPos> sectionsToRemoveMap = new HashMap<Long, SectionPos>();
		
		long time = System.currentTimeMillis();
		ExplosionTask task = new ExplosionTask(this, explosion, resistanceFac, ignoreFluids, Math.max(minVectorsPerThread, vectors.size() / (Runtime.getRuntime().availableProcessors())), vectors);
		Long2ObjectMap<BitSet> result = pool.invoke(task);
		System.out.println("Time for explosion block gathering: " + (System.currentTimeMillis() - time) + " task count: " + pool.getStealCount());
		
		for (long key : result.keySet()) {
			editedSections.put(SectionPos.of(key), result.get(key));
		}
		for (long key : sectionsToRemove.keySet()) {
			sectionsToRemoveMap.put(key, SectionPos.of(key));
		}
		if(explosion.level instanceof ServerLevel server) {
			ExplosionsTick.explosions.add(Map.entry(Map.entry(explosion, server), Map.entry(editedSections, sectionsToRemoveMap)));
		}
	}
}
