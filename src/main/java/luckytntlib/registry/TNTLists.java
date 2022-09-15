package luckytntlib.registry;

import java.util.ArrayList;
import java.util.List;

import com.mojang.datafixers.util.Pair;

import luckytntlib.block.LTNTBlock;
import luckytntlib.item.LDynamiteItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

public class TNTLists {
	public static List<RegistryObject<LTNTBlock>> NORMAL_TNT = new ArrayList<>();
	public static List<RegistryObject<LTNTBlock>> GOD_TNT = new ArrayList<>();
	public static List<RegistryObject<LTNTBlock>> DOOMSDAY_TNT = new ArrayList<>();
	public static List<RegistryObject<LDynamiteItem>> DYNAMITE = new ArrayList<>();
	
	public static List<Pair<RegistryObject<LTNTBlock>, RegistryObject<Item>>> TNT_DISPENSER_REGISTRY_LIST = new ArrayList<>();
}
