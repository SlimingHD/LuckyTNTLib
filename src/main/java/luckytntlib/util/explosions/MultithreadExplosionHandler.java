package luckytntlib.util.explosions;

import java.util.LinkedList;
import java.util.Queue;

import luckytntlib.config.LuckyTNTLibConfigValues;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles multiple multithreaded explosions, as they are forced to be non-blocking.
 * This class handles queueing of explosions to make sure not too many explosions are run in parallel.
 * The maximum number of simultaneous explosions is decided by the user in their config.
 * Finalization of an explosion needs to be singlethreaded, as it needs to write into chunks.
 * Only one explosion is finalized per tick.
 * Extra explosion threads are queued and only started once space has been made in the explosion queue.
 * This is done to save on CPU and RAM usage and make sure the game doesn't just crash.
 */
@Mod.EventBusSubscriber
public class MultithreadExplosionHandler {

	private static final Queue<ExplosionThread> queuedExplosionThreads = new LinkedList<ExplosionThread>();
	private static final Queue<ExplosionThread> explosionThreads = new LinkedList<ExplosionThread>();
	
	@SubscribeEvent
	public static void tick(ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			if (!explosionThreads.isEmpty()) {
				ExplosionThread thread = explosionThreads.peek();
				if (!thread.isAlive()) {
					explosionThreads.poll();
					long time = System.currentTimeMillis();
					thread.getExplosion().finishImprovedExplosion(thread.getServerLevel(), thread.getEditedSections(), thread.getFullSections(), thread.getExplosionRule());
					if (thread.shouldPlaceFire()) {
						thread.getExplosion().placeFire(thread.getRandomVecLengthFac(), thread.getServerLevel().getRandom());
					}
					System.out.println("Time for finishing explosion: " + (System.currentTimeMillis() - time));
					if (!queuedExplosionThreads.isEmpty()) {
						ExplosionThread newThread = queuedExplosionThreads.poll();
						newThread.start();
						explosionThreads.add(newThread);
					}
				}
			}
		}
	}
	
	public static void enqueue(ExplosionThread explosionThread) {
		int maxSize = LuckyTNTLibConfigValues.MAX_EXPLOSION_THREADS.get();
		if (explosionThreads.size() >= maxSize) {
			queuedExplosionThreads.add(explosionThread);
		} else {
			explosionThread.start();
			explosionThreads.add(explosionThread);
		}
	}
}
