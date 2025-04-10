package luckytntlib.registry;

import luckytntlib.LuckyTNTLib;
import luckytntlib.block.LTNTBlock;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.RegistryObject;

@EventBusSubscriber(bus = Bus.MOD)
public class BlockRegistry {
	
	public static final RegistryObject<LTNTBlock> TNT = LuckyTNTLib.RH.registerTNTBlock(EntityRegistry.TNT, new TNTBlockRegistryData.Builder("tnt").build());
}
