package luckytntlib.item;

import java.util.List;

import luckytntlib.entity.LExplosiveProjectile;
import luckytntlib.registry.RegistryHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

/**
 * 
 * The LuckyDynamiteItem is an extension of the {@link LDynamiteItem} and serves the simple purpose of spawning a random
 * {@link LExplosiveProjectile} of a {@link LDynamiteItem} contained in a {@link List}.
 * The list could for instance be set to one of the many lists of {@link LDynamiteItem} found in the {@link RegistryHelper}.
 */
public class LuckyDynamiteItem extends LDynamiteItem{

	public List<RegistryObject<LDynamiteItem>> dynamites;
	
	public LuckyDynamiteItem(Item.Properties properties, List<RegistryObject<LDynamiteItem>> dynamites) {
		super(properties, null);
		this.dynamites = dynamites;
	}
	
	@Override
	public void onUseTick(Level level, LivingEntity player, ItemStack stack, int count) {
		if(player instanceof ServerPlayer sPlayer) {
			shoot(level, player.getX(), player.getY() + player.getEyeHeight(), player.getZ(), player.getViewVector(1), 2, player);		
			if(!sPlayer.isCreative()) {
				stack.shrink(1);
			}
		}
	}
	
	/**
	 * Gets a random {@link LDynamiteItem} from the list held by this item and calls its shoot method.
	 * Can not shoot another {@link LuckyDynamiteItem}.
	 * @param level  the current level
	 * @param x  the x position
	 * @param y  the y position (eye level needs to be added manually!)
	 * @param z  the z position
	 * @param direction  the direction the projectile will be thrown in
	 * @param power  the power with which the projectile is thrown
	 * @param thrower  the owner for the spawned projectile (used primarely for the {@link DamageSource})
	 * @return {@link LExplosiveProjectile} or null
	 * @throws UnsupportedOperationException
	 */
	@Override
	public LExplosiveProjectile shoot(Level level, double x, double y, double z, Vec3 direction, float power, LivingEntity thrower) throws UnsupportedOperationException{
		int rand = random.nextInt(dynamites.size());
		if(!(dynamites.get(rand).get() instanceof LuckyDynamiteItem)) {
			return dynamites.get(rand).get().shoot(level, x, y, z, direction, power, thrower);
		}
		throw new UnsupportedOperationException("Dynamite item is a Lucky Dynamite itself");
	}
}
