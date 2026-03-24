package luckytntlib.util.explosions;

import net.minecraft.world.entity.Entity;

@FunctionalInterface
public interface EntityExplosionEffect {
	
	/**
	 * Handles a single entity gotten from an {@link ImprovedExplosion}.
	 * @param entity the {@link Entity} being affected
	 * @param distance the distance of the affected entity to the explosion center, being normalized to 0-1
	 */
	public void handleEntity(Entity entity, double distance);
}
