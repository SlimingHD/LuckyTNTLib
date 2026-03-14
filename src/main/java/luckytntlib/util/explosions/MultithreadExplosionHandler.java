package luckytntlib.util.explosions;

import java.util.LinkedList;
import java.util.Queue;

import luckytntlib.config.LuckyTNTLibConfigValues;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
					thread.getExplosion().finishImprovedExplosion(thread.getServerLevel(), thread.getEditedSections(), thread.getSectionsToRemove(), thread.getExplosionRule());
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
