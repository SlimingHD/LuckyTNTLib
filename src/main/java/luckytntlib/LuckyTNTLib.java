package luckytntlib;

import com.mojang.datafixers.util.Pair;

import luckytntlib.block.LTNTBlock;
import luckytntlib.registry.TNTLists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


@Mod(LuckyTNTLib.MODID)
public class LuckyTNTLib
{
    public static final String MODID = "luckytntlib";
	public static final DeferredRegister<Block> TNT_BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, LuckyTNTLib.MODID);
	public static final DeferredRegister<Item> TNT_ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LuckyTNTLib.MODID);
	public static final DeferredRegister<EntityType<?>> TNT_ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, LuckyTNTLib.MODID);
	public static final DeferredRegister<Item> DYNAMITE_ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LuckyTNTLib.MODID);
	public static final DeferredRegister<EntityType<?>> DYNAMITE_ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, LuckyTNTLib.MODID);
	public static final DeferredRegister<EntityType<?>> MINECART_ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, LuckyTNTLib.MODID);
	
    public LuckyTNTLib() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::commonSetup);
        TNT_ITEMS.register(bus);
        TNT_BLOCKS.register(bus);
        DYNAMITE_ITEMS.register(bus);
        TNT_ENTITIES.register(bus);
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    private void commonSetup(final FMLCommonSetupEvent event) {
    	
    	for(Pair<RegistryObject<LTNTBlock>, RegistryObject<Item>> pair : TNTLists.TNT_DISPENSER_REGISTRY_LIST) {
    		LTNTBlock block = pair.getFirst().get();
    		Item item = pair.getSecond().get();
			DispenseItemBehavior behaviour = new DispenseItemBehavior() {
				@Override
				public ItemStack dispense(BlockSource source, ItemStack stack) {
					Level level = source.getLevel();
					BlockPos pos = new BlockPos(DispenserBlock.getDispensePosition(source));
					block.explode(level, false, pos.getX(), pos.getY(), pos.getZ(), null);
					stack.shrink(1);
					return stack;
				}
			};
			DispenserBlock.registerBehavior(item, behaviour);
    	}
    }
}
