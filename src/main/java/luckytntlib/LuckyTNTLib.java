package luckytntlib;

import java.util.function.BiFunction;

import com.mojang.datafixers.util.Pair;

import luckytntlib.block.LTNTBlock;
import luckytntlib.client.gui.ConfigScreen;
import luckytntlib.config.LuckyTNTLibConfigs;
import luckytntlib.entity.LTNTMinecart;
import luckytntlib.item.LDynamiteItem;
import luckytntlib.item.LTNTMinecartItem;
import luckytntlib.registry.RegistryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegistryObject;

@Mod(LuckyTNTLib.MODID)
public class LuckyTNTLib
{
    public static final String MODID = "luckytntlib";	
    public LuckyTNTLib() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        LuckyTNTLibConfigs.register();
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory(new BiFunction<Minecraft, Screen, Screen>() {		
			@Override
			public Screen apply(Minecraft mc, Screen screen) {
				return new ConfigScreen();
			}
		}));
    }
    
    private void commonSetup(final FMLCommonSetupEvent event) {
    	for(Pair<RegistryObject<LTNTBlock>, RegistryObject<Item>> pair : RegistryHelper.TNT_DISPENSER_REGISTRY_LIST) {
    		LTNTBlock block = pair.getFirst().get();
    		Item item = pair.getSecond().get();
			DispenseItemBehavior behaviour = new DispenseItemBehavior() {
				@Override
				public ItemStack dispense(BlockSource source, ItemStack stack) {
					Level level = source.getLevel();
					Position p = DispenserBlock.getDispensePosition(source);
					BlockPos pos = new BlockPos((int)p.x(), (int)p.y(), (int)p.z());
					block.explode(level, false, pos.getX(), pos.getY(), pos.getZ(), null);
					stack.shrink(1);
					return stack;
				}
			};
			DispenserBlock.registerBehavior(item, behaviour);
    	}
    	for(RegistryObject<LDynamiteItem> dynamite : RegistryHelper.DYNAMITE_DISPENSER_REGISTRY_LIST) {
    		LDynamiteItem item = dynamite.get();
    		DispenseItemBehavior behaviour = new DispenseItemBehavior() {
				
				@Override
				public ItemStack dispense(BlockSource source, ItemStack stack) {
					Level level = source.getLevel();
					Vec3 dispenserPos = new Vec3(source.getPos().getX() + 0.5f, source.getPos().getY() + 0.5f, source.getPos().getZ() + 0.5f);
					Position pos = DispenserBlock.getDispensePosition(source);
					item.shoot(level, pos.x(), pos.y(), pos.z(), new Vec3(pos.x(), pos.y(), pos.z()).add(-dispenserPos.x(), -dispenserPos.y(), -dispenserPos.z()), 2, null);
					stack.shrink(1);
					return stack;
				}
			};
			DispenserBlock.registerBehavior(item, behaviour);
    	}
    	for(RegistryObject<LTNTMinecartItem> minecart : RegistryHelper.MINECART_DISPENSER_REGISTRY_LIST) {
    		LTNTMinecartItem item = minecart.get();
			DispenseItemBehavior behaviour = new DispenseItemBehavior() {
				@Override
				public ItemStack dispense(BlockSource source, ItemStack stack) {
					Direction direction = source.getBlockState().getValue(DispenserBlock.FACING);
					Level level = source.getLevel();
					double x = source.x() + (double) direction.getStepX() * 1.125D;
					double y = Math.floor(source.y()) + (double) direction.getStepY();
					double z = source.z() + (double) direction.getStepZ() * 1.125D;
					BlockPos pos = source.getPos().relative(direction);
					BlockState state = level.getBlockState(pos);
					RailShape rail = state.getBlock() instanceof BaseRailBlock ? ((BaseRailBlock) state.getBlock()).getRailDirection(state, level, pos, null) : RailShape.NORTH_SOUTH;
					double railHeight;
					if (state.is(BlockTags.RAILS)) {
						if (rail.isAscending()) {
							railHeight = 0.6D;
						} else {
							railHeight = 0.1D;
						}
					} else {
						if (!state.isAir() || !level.getBlockState(pos.below()).is(BlockTags.RAILS)) {
							return new DefaultDispenseItemBehavior().dispense(source, stack);
						}

						BlockState stateDown = level.getBlockState(pos.below());
						@SuppressWarnings("deprecation")
						RailShape railDown = stateDown.getBlock() instanceof BaseRailBlock ? stateDown.getValue(((BaseRailBlock) stateDown.getBlock()).getShapeProperty()) : RailShape.NORTH_SOUTH;
						if (direction != Direction.DOWN && railDown.isAscending()) {
							railHeight = -0.4D;
						} else {
							railHeight = -0.9D;
						}
					}

					LTNTMinecart cart = item.createMinecart(level, x, y + railHeight, z, null);
					if (stack.hasCustomHoverName()) {
						cart.setCustomName(stack.getHoverName());
					}
					stack.shrink(1);
					return stack;
				}
			};
			DispenserBlock.registerBehavior(item, behaviour);
    	}
    }
}
