package luckytntlib.util.explosions.rules;

import com.google.gson.JsonObject;

import luckytntlib.LuckyTNTLib;
import net.minecraft.resources.ResourceLocation;
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
	
	public FilterRandomDistanceExplosionRule(int radius, ProbabilityCalculator probabilityCalculator, ExplosionRule rule) {
		if (radius <= 0) {
			throw new IllegalArgumentException("Provided radius must not be 0 or negative.");
		}
		this.radius = radius;
		this.probabilityCalculator = probabilityCalculator;
		this.rule = rule;
	}
	
	/**
	 * Client only constructor with defunct data, as it is not needed. Do not use on the server.
	 */
	private FilterRandomDistanceExplosionRule(ExplosionRule rule) {
		this.radius = 0;
		this.probabilityCalculator = null;
		this.rule = rule;
	}
	
	public static FilterRandomDistanceExplosionRule linearDecrease(int radius, ExplosionRule rule) {
		return new FilterRandomDistanceExplosionRule(radius, (offX, offY, offZ, r) -> {
			double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
			return 1f - (float)(distance / r);
		}, rule);
	}
	
	public static FilterRandomDistanceExplosionRule quadraticDecrease(int radius, ExplosionRule rule) {
		int radiusSqr = radius * radius;
		return new FilterRandomDistanceExplosionRule(radius, (offX, offY, offZ, r) -> {
			int distanceSqr = offX * offX + offY * offY + offZ * offZ;
			return 1f - distanceSqr / (float)radiusSqr;
		}, rule);
	}
	
	@Override
	public boolean shouldApply(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ) {
		if (level.isClientSide()) {
			return rule.shouldApply(level, state, center, offX, offY, offZ);
		}
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
		return new FilterRandomDistanceExplosionRule(ExplosionRule.parse(root.get("rule").getAsJsonObject()));
	}
	
	public static interface ProbabilityCalculator {
		public float calculate(int offX, int offY, int offZ, int radius);
	}
}
