package luckytntlib.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.mojang.datafixers.util.Pair;

import luckytntlib.block.LTNTBlock;
import luckytntlib.entity.LivingPrimedLTNT;
import luckytntlib.item.LDynamiteItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

public class TNTLists {
	public static HashMap<String, List<RegistryObject<LTNTBlock>>> TNTLists = new HashMap<>();
	public static HashMap<String, List<RegistryObject<LDynamiteItem>>> DynamiteLists = new HashMap<>();
	
	public static List<Pair<RegistryObject<LTNTBlock>, RegistryObject<Item>>> TNT_DISPENSER_REGISTRY_LIST = new ArrayList<>();
	
	public static final List<Pair<RegistryObject<EntityType<LivingPrimedLTNT>>, float[]>> attributeRegistries = new ArrayList<Pair<RegistryObject<EntityType<LivingPrimedLTNT>>, float[]>>();
}
