package luckytntlib.item;

import java.util.Random;

import javax.annotation.Nullable;

import luckytntlib.entity.LExplosiveProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

public class LDynamiteItem extends Item{
	
	@Nullable
	protected RegistryObject<EntityType<LExplosiveProjectile>> dynamite;
	protected Random random = new Random();
	
	public LDynamiteItem(Item.Properties properties, @Nullable RegistryObject<EntityType<LExplosiveProjectile>> dynamite) {
		super(properties);
		this.dynamite = dynamite;
	}
	
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand){
		onUsingTick(player.getItemInHand(hand), player, player.getItemInHand(hand).getCount());
		return new InteractionResultHolder<ItemStack>(InteractionResult.SUCCESS, player.getItemInHand(hand));
	}
	
	@Override
	public void onUsingTick(ItemStack stack, LivingEntity player, int count) {
		Level level = player.level;
		if(player instanceof ServerPlayer sPlayer && dynamite != null) {
			shoot(level, player.getX(), player.getY() + player.getEyeHeight(), player.getZ(), player.getViewVector(1), 2, player);		
			if(!sPlayer.isCreative()) {
				stack.shrink(1);
			}
		}
	}
	
	@Nullable
	public LExplosiveProjectile shoot(Level level, double x, double y, double z, Vec3 direction, float power, @Nullable LivingEntity thrower) throws NullPointerException {
		if(dynamite != null) {
			LExplosiveProjectile dyn = dynamite.get().create(level);
			dyn.setPos(x, y, z);
			dyn.shoot(direction.x, direction.y, direction.z, power, 0);
			dyn.setOwner(thrower);
			level.addFreshEntity(dyn);
			level.playSound(null, new BlockPos(x, y, z), SoundEvents.SNOWBALL_THROW, SoundSource.MASTER, 1, 0.5f);
			return dyn;
		}
		throw new NullPointerException("No dynamite entity present. Make sure it is registered before the item is registered");
	}
}
