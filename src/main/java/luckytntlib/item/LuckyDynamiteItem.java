package luckytntlib.item;

import java.util.List;

import luckytntlib.entity.LExplosiveProjectile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

public class LuckyDynamiteItem extends LDynamiteItem{

	public List<RegistryObject<LDynamiteItem>> dynamites;
	
	public LuckyDynamiteItem(Item.Properties properties, List<RegistryObject<LDynamiteItem>> dynamites) {
		super(properties, null);
		this.dynamites = dynamites;
	}
	
	@Override
	public void onUsingTick(ItemStack stack, LivingEntity player, int count) {
		Level level = player.level;
		if(player instanceof ServerPlayer sPlayer) {
			shoot(level, player.getX(), player.getY() + player.getEyeHeight(), player.getZ(), player.getViewVector(1), 2, player);		
			if(!sPlayer.isCreative()) {
				stack.shrink(1);
			}
		}
	}
	
	@Override
	public LExplosiveProjectile shoot(Level level, double x, double y, double z, Vec3 direction, float power, LivingEntity thrower) throws NullPointerException{
		int rand = random.nextInt(dynamites.size());
		if(!(dynamites.get(rand).get() instanceof LuckyDynamiteItem)) {
			return dynamites.get(rand).get().shoot(level, x, y, z, direction, power, thrower);
		}
		throw new NullPointerException();
	}
}
