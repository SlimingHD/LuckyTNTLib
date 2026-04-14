package luckytntlib.util.explosions;

import java.util.LinkedList;
import java.util.Queue;

import luckytntlib.config.LuckyTNTLibConfigValues;
import luckytntlib.util.explosions.rules.FireExplosionRule;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles multiple multi-threaded explosions, as they are by nature forced to be non-blocking.
 * Finalization of an explosion needs to be single-threaded, as it needs to write into chunks.
 * Excess explosions are queued, not discarded.
 * <p>
 * Queuing behavior:<br>
 * Only one explosion is finalized per tick to not overload the game.
 * Extra explosion threads are queued and only started once space has been made in the explosion queue.
 * The maximum number of simultaneous explosions is decided by the user in their config.
 * This is done to save on CPU and RAM usage, as handling too many explosions simultaneously will just crash the game.
 */
@Mod.EventBusSubscriber
public final class MultithreadedExplosionHandler {

	private static final Queue<ExplosionThread> queuedExplosionThreads = new LinkedList<ExplosionThread>();
	private static final Queue<ExplosionThread> explosionThreads = new LinkedList<ExplosionThread>();
	
	@SubscribeEvent
	public static void tick(ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			if (!explosionThreads.isEmpty()) {
				ExplosionThread thread = explosionThreads.peek();
				if (!thread.isAlive()) {
					explosionThreads.poll();
					thread.getExplosion().finishImprovedExplosion(thread.getEditedSections(), thread.getFullSections(), thread.getExplosionRule());
					if (thread.shouldPlaceFire()) {
						float sizeReduction = (float)Math.sqrt(Math.sqrt(thread.getExplosion().size));
						ImprovedExplosion fireExplosion = new ImprovedExplosion(thread.getExplosion().level, thread.getExplosion().getPosition(), Math.round(thread.getExplosion().size / sizeReduction));
						fireExplosion.doImprovedBlockExplosion(1f, 1.2f * sizeReduction, false, false, new FireExplosionRule(1f / sizeReduction));
					}
					if (!queuedExplosionThreads.isEmpty()) {
						ExplosionThread newThread = queuedExplosionThreads.poll();
						newThread.start();
						explosionThreads.add(newThread);
					}
				}
			}
		}
	}
	
	static void enqueue(ExplosionThread explosionThread) {
		int maxSize = LuckyTNTLibConfigValues.MAX_EXPLOSION_THREADS.get();
		if (explosionThreads.size() >= maxSize) {
			queuedExplosionThreads.add(explosionThread);
		} else {
			explosionThread.start();
			explosionThreads.add(explosionThread);
		}
	}
}
