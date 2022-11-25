package luckytntlib.registry;

import com.mojang.datafixers.util.Pair;

import luckytntlib.LuckyTNTLib;
import luckytntlib.entity.LivingPrimedLTNT;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = LuckyTNTLib.MODID, bus = Bus.MOD)
public class EntityAttributesRegistry {
	
	@SubscribeEvent
	public static void registerAttributes(EntityAttributeCreationEvent event) {
		for(Pair<RegistryObject<EntityType<LivingPrimedLTNT>>, float[]> pair : TNTLists.attributeRegistries) {
			event.put(pair.getFirst().get(), Mob.createMobAttributes()
					.add(Attributes.ATTACK_DAMAGE, pair.getSecond()[0])
					.add(Attributes.MAX_HEALTH, pair.getSecond()[1])
					.add(Attributes.MOVEMENT_SPEED, pair.getSecond()[2])
					.build());
		}
	}
}
