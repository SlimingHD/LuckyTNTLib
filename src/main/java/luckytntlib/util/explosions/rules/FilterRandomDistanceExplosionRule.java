package luckytntlib.util.explosions.rules;

import com.google.gson.JsonObject;

import luckytntlib.LuckyTNTLib;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * {@link ExplosionRule} that applies to all blocks randomly with a probability based on the block's distance to the center of the explosion.
 * The probability function can be manually provided or given by a default setting.
 */
public class FilterRandomDistanceExplosionRule implements ExplosionRule {

	public static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(LuckyTNTLib.MODID, "filter_random_distance");
	
	private final int radius;
	private final ProbabilityCalculator probabilityCalculator;
	private final ExplosionRule rule;
	
	public FilterRandomDistanceExplosionRule(int startRadius, int endRadius, ProbabilityCalculator probabilityCalculator, ExplosionRule rule) {
		this.radius = endRadius - startRadius;
		if (radius <= 0) {
			throw new IllegalArgumentException("Provided radius must not be 0 or negative.");
		}
		this.probabilityCalculator = probabilityCalculator;
		this.rule = rule;
	}
	
	public static FilterRandomDistanceExplosionRule linearDecrease(int startRadius, int endRadius, ExplosionRule rule) {
		return new FilterRandomDistanceExplosionRule(startRadius, endRadius, (offX, offY, offZ, r) -> {
			float distance = Mth.sqrt(offX * offX + offY * offY + offZ * offZ);
			return 1f - (distance - startRadius) / r;
		}, rule);
	}
	
	/**
	 * Generally faster than {@link #linearDecrease(int, int, ExplosionRule)}, as it omits {@link Math#sqrt(double)}, but probability fall off curve looks different. 
	 */
	public static FilterRandomDistanceExplosionRule quadraticDecrease(int startRadius, int endRadius, ExplosionRule rule) {
		int startRadiusSqr = startRadius * startRadius;
		int endRadiusSqr = endRadius * endRadius;
		float radiusSqr = endRadiusSqr - startRadiusSqr;
		return new FilterRandomDistanceExplosionRule(startRadius, endRadius, (offX, offY, offZ, r) -> {
			int distanceSqr = offX * offX + offY * offY + offZ * offZ;
			return 1f - (distanceSqr - startRadiusSqr) / radiusSqr;
		}, rule);
	}
	
	@Override
	public void setupClientData(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ) {
		rule.setupClientData(level, state, center, offX, offY, offZ);
	}
	
	@Override
	public boolean shouldApply(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ) {
		float probability = probabilityCalculator.calculate(offX, offY, offZ, radius);
		if (probability > 0 && level.getRandom().nextFloat() < probability) {
			return rule.shouldApply(level, state, center, offX, offY, offZ);
		}
		return false;
	}
	
	@Override
	public BlockState getState() {
		return rule.getState();
	}

	@Override
	public JsonObject encode(JsonObject root) {
		root.addProperty("type", RESOURCE_LOCATION.toString());
		root.add("rule", rule.encode(new JsonObject()));
		return root;
	}

	public static ExplosionRule decode(JsonObject root) {
		return new FilterRandomDistanceExplosionRule(0, 1, (offX, offY, offZ, r) -> 0f, ExplosionRule.parse(root.get("rule").getAsJsonObject()));
	}
	
	@FunctionalInterface
	public static interface ProbabilityCalculator {
		public float calculate(int offX, int offY, int offZ, int radius);
	}
}
