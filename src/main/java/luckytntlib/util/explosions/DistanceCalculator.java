package luckytntlib.util.explosions;

import org.joml.Vector3f;

/**
 * The DistanceCalculator is a {@link FunctionalInterface} used to calculate the maximum squared y distance for a pair of x and z coordinates in the context of explosions.
 * It is mainly used in the {@link ExplosionHelper}.
 */
@FunctionalInterface
public interface DistanceCalculator {
	
	/**
	 * Calculates the absolute squared maximum y offset from the y-plane the explosion's center is in for a specific pair of x and z offsets
	 * @param x  the x offset from the center of the explosion
	 * @param z  the z offset from the center of the explosion
	 * @param radius  the radius of the explosion
	 * @param scaling  a {@link Vector3f} that contains the scalings for all axes
	 * @return the maximum squared y distance from the y-plane the explosion it's used for took place in
	 * 
	 * @see <i> Implementation examples: </i>
	 * @see ExplosionHelper#SPHEROID_CALCULATOR
	 * @see ExplosionHelper#CUBOID_CALCULATOR
	 * @see ExplosionHelper#createScaledCylindricalCrater(net.minecraft.world.level.Level, net.minecraft.world.phys.Vec3, int, int, Vector3f, int, luckytntlib.util.explosions.rules.ExplosionRule)
	 */
	public int getMaxYDistanceSqr(int x, int z, int radius, Vector3f scaling);
}
