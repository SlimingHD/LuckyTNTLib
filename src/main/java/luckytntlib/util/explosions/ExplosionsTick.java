package luckytntlib.util.explosions;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ExplosionsTick {

	public static final ConcurrentLinkedQueue<Map.Entry<Map.Entry<ImprovedExplosion, ServerLevel>, Map.Entry<HashMap<SectionPos, BitSet>, HashMap<Long, SectionPos>>>> explosions = new ConcurrentLinkedQueue<Map.Entry<Map.Entry<ImprovedExplosion, ServerLevel>, Map.Entry<HashMap<SectionPos,BitSet>,HashMap<Long,SectionPos>>>>();
	
	@SubscribeEvent
	public static void tick(ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			if (!explosions.isEmpty()) {
				long time = System.currentTimeMillis();
				Map.Entry<Map.Entry<ImprovedExplosion, ServerLevel>, Map.Entry<HashMap<SectionPos, BitSet>, HashMap<Long, SectionPos>>> explosionData = explosions.poll();
				ImprovedExplosion explosion = explosionData.getKey().getKey();
				HashMap<SectionPos, BitSet> editedSections = explosionData.getValue().getKey();
				HashMap<Long, SectionPos> sectionsToRemove = explosionData.getValue().getValue();
				explosion.finishImprovedExplosion(explosionData.getKey().getValue(), editedSections, sectionsToRemove);
				System.out.println("Time for explosion: " + (System.currentTimeMillis() - time));
			}
		}
	}
}
