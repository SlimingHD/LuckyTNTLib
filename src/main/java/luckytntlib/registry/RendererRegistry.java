package luckytntlib.registry;

import luckytntlib.client.renderer.LTNTRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(bus = Bus.MOD, value = Dist.CLIENT)
public class RendererRegistry {

	@SubscribeEvent
	public static void register(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(EntityRegistry.TNT.get(), LTNTRenderer::new);
	}
}
