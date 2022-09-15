package luckytntlib.item;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.annotation.Nullable;

import luckytntlib.entity.LTNTMinecart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gameevent.GameEvent;

public class LTNTMinecartItem extends MinecartItem{

	Class<? extends LTNTMinecart> minecart;
	
	public LTNTMinecartItem(Item.Properties properties, Class<? extends LTNTMinecart> minecart, boolean addDispenseBehaviour) {
		super(AbstractMinecart.Type.TNT, properties);
		this.minecart = minecart;
		if(addDispenseBehaviour) {
			DefaultDispenseItemBehavior DEFAULT_DISPENSE = new DefaultDispenseItemBehavior();
			DispenseItemBehavior behaviour = new DispenseItemBehavior() {
				@SuppressWarnings("deprecation")
				@Override
				public ItemStack dispense(BlockSource source, ItemStack stack) {
			         Direction direction = source.getBlockState().getValue(DispenserBlock.FACING);
			         Level level = source.getLevel();
			         double x = source.x() + (double)direction.getStepX() * 1.125D;
			         double y = Math.floor(source.y()) + (double)direction.getStepY();
			         double z = source.z() + (double)direction.getStepZ() * 1.125D;
			         BlockPos pos = source.getPos().relative(direction);
			         BlockState state = level.getBlockState(pos);
			         RailShape rail = state.getBlock() instanceof BaseRailBlock ? ((BaseRailBlock)state.getBlock()).getRailDirection(state, level, pos, null) : RailShape.NORTH_SOUTH;
			         double railHeight;
			         if (state.is(BlockTags.RAILS)) {
			            if (rail.isAscending()) {
			               railHeight = 0.6D;
			            } else {
			               railHeight = 0.1D;
			            }
			         } else {
			            if (!state.isAir() || !level.getBlockState(pos.below()).is(BlockTags.RAILS)) {
			               return DEFAULT_DISPENSE.dispense(source, stack);
			            }

			            BlockState stateDown = level.getBlockState(pos.below());
			            RailShape railDown = stateDown.getBlock() instanceof BaseRailBlock ? stateDown.getValue(((BaseRailBlock)stateDown.getBlock()).getShapeProperty()) : RailShape.NORTH_SOUTH;
			            if (direction != Direction.DOWN && railDown.isAscending()) {
			               railHeight = -0.4D;
			            } else {
			               railHeight = -0.9D;
			            }
			         }

			         AbstractMinecart abstractminecart = createMinecart(level, x, y + railHeight, z, null);
			         if (stack.hasCustomHoverName()) {
			            abstractminecart.setCustomName(stack.getHoverName());
			         }
					stack.shrink(1);
					return stack;
				}
			};
			DispenserBlock.registerBehavior(this, behaviour);
		}
	}
	
	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = level.getBlockState(pos);
		if(!state.is(BlockTags.RAILS)) {
			return InteractionResult.FAIL;
		}
		ItemStack stack = context.getItemInHand();
		double railHeight = 0;
		if(!level.isClientSide) {
            RailShape rail = state.getBlock() instanceof BaseRailBlock ? ((BaseRailBlock)state.getBlock()).getRailDirection(state, level, pos, null) : RailShape.NORTH_SOUTH;
            if (rail.isAscending()) {
               railHeight = 0.5D;
            }
		}
		LTNTMinecart minecart = createMinecart(level, pos.getX() + 0.5f, pos.getY() + 0.0625f + railHeight, pos.getZ() + 0.5f, context.getPlayer());
        if (stack.hasCustomHoverName()) {
            minecart.setCustomName(stack.getHoverName());
        }
        level.addFreshEntity(minecart);
        level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, pos);
		stack.shrink(1);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}
	
	@Nullable
	@SuppressWarnings("rawtypes")
	public LTNTMinecart createMinecart(Level level, double x, double y, double z, @Nullable LivingEntity placer){
		try {
			Class[] parameters = new Class[] {Level.class, double.class, double.class, double.class, LivingEntity.class};
			Constructor<? extends LTNTMinecart> minecart_constructor = minecart.getDeclaredConstructor(parameters);
			minecart_constructor.setAccessible(true);
			LTNTMinecart minecart = minecart_constructor.newInstance(level, x, y, z, placer);
			level.addFreshEntity(minecart);
		}
		catch(InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException | NullPointerException e) {
			e.printStackTrace();
		}
		throw new NullPointerException("Could not instantiate Minecart");
	}
}
