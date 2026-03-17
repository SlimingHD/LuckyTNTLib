package luckytntlib.util.explosions;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

import javax.annotation.Nullable;

import org.joml.Vector3f;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import luckytntlib.util.explosions.rules.ExplosionRule;

/**
 * Handles the execution of a multithreaded explosion.
 * This is done in its own thread, as blocking the main thread will lead to a deadlock.
 * The threading itself is done using a {@link ForkJoinPool}, with the number of tasks being chosen dynamically based on the available processors and a minimum size.
 * This thread is started and its explosion is finalized in {@link MultithreadExplosionHandler}.
 * Only the gathering of blocks to affect is handled in multiple threads. Both the collection of vectors and the finalization are running in a single thread.
 */
public class ExplosionThread extends Thread {

	private static final int minVectorsPerThread = 25000;
	private static final ForkJoinPool pool = new ForkJoinPool();
	
	private final ImprovedExplosion explosion;
	private final float resistanceFac;
	private final float randomVecLengthFac;
	private final boolean ignoreFluids;
	private final boolean placeFire;
	@Nullable
	private final ExplosionRule rule;
	private final List<Vector3f> vectors;
	
	public final Set<Long> emptySections = ConcurrentHashMap.newKeySet();
	public final Set<Long> fullSections = ConcurrentHashMap.newKeySet();
	public final ConcurrentHashMap<Long, float[]> sectionResistances = new ConcurrentHashMap<Long, float[]>();
	
	private Map<Long, BitSet> editedSections = new Long2ObjectOpenHashMap<BitSet>();
	
	public ExplosionThread(ImprovedExplosion explosion, float resistanceFac, float randomVecLengthFac, boolean ignoreFluids, boolean placeFire, @Nullable ExplosionRule rule, List<Vector3f> vectors) {
		this.explosion = explosion;
		this.resistanceFac = resistanceFac;
		this.randomVecLengthFac = randomVecLengthFac;
		this.ignoreFluids = ignoreFluids;
		this.placeFire = placeFire;
		this.rule = rule;
		this.vectors = vectors;
	}
	
	@Override
	public void run() {	
		long time = System.currentTimeMillis();
		ExplosionTask task = new ExplosionTask(this, explosion, resistanceFac, ignoreFluids, Math.max(minVectorsPerThread, vectors.size() / (Runtime.getRuntime().availableProcessors() * 4)), vectors);
		editedSections = pool.invoke(task);
		System.out.println("Time for explosion block gathering: " + (System.currentTimeMillis() - time) + " task count: " + pool.getStealCount());
	}
	
	public Map<Long, BitSet> getEditedSections() {
		return editedSections;
	}
	
	public Set<Long> getFullSections() {
		return fullSections;
	}
	
	public ImprovedExplosion getExplosion() {
		return explosion;
	}
	
	public float getRandomVecLengthFac() {
		return randomVecLengthFac;
	}
	
	public boolean shouldPlaceFire() {
		return placeFire;
	}
	
	@Nullable
	public ExplosionRule getExplosionRule() {
		return rule;
	}
}
