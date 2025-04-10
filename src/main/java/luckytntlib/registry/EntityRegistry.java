package luckytntlib.registry;

import luckytntlib.LuckyTNTLib;
import luckytntlib.entity.PrimedLTNT;
import luckytntlib.util.tnteffects.TNTXStrengthEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.RegistryObject;

@EventBusSubscriber(bus = Bus.MOD)
public class EntityRegistry {
	
	public static final TNTXStrengthEffect.Builder TNT_X10000_EFFECT = new TNTXStrengthEffect.Builder().strength(300).resistanceImpact(0.167f).randomVecLength(0.05f).knockbackStrength(30f).isStrongExplosion(true);
		
	public static final RegistryObject<EntityType<PrimedLTNT>> TNT = LuckyTNTLib.RH.registerTNTEntity("tnt", TNT_X10000_EFFECT.fuse(40).buildTNT(() -> BlockRegistry.TNT));
}
