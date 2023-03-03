package luckytntlib.util.explosions;

import net.minecraft.world.entity.Entity;

/**
 * 
 * An IForEachEntityExplosionEffect is used to affect individual entities gotten by an {@link ImprovedExplosion} in different ways.
 * It is usually used as a parameter of a function.
 */
@FunctionalInterface
public interface IForEachEntityExplosionEffect {
	
	/**
	 * @param entity  the entity
	 * @param distance  the distance of the entity to the explosion origin
	 */
	public void doEntityExplosion(Entity entity, double distance);
	
}
