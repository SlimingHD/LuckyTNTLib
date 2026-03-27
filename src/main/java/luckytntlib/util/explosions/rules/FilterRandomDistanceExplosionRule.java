package luckytntlib.util.explosions.rules;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;

import luckytntlib.LuckyTNTLib;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * {@link ExplosionRule} that applies to all blocks randomly with a probability based on the block's distance to the center of the explosion.
 * The probability function can be manually provided or given by a default setting.
 */
public class FilterRandomDistanceExplosionRule implements ExplosionRule {

	public static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(LuckyTNTLib.MODID, "filter_random_distance");
	
	private final int radius, startRadius;
	private final ProbabilityFunction probabilityFunction;
	private final ExplosionRule rule;
	
	protected FilterRandomDistanceExplosionRule(int radius, int startRadius, ProbabilityFunction probabilityFunction, ExplosionRule rule) {
		this.radius = radius;
		this.startRadius = startRadius;
		if (radius <= 0) {
			throw new IllegalArgumentException("Provided radius must not be 0 or negative.");
		}
		this.probabilityFunction = probabilityFunction;
		this.rule = rule;
	}
	
	public static FilterRandomDistanceExplosionRule linearDecrease(int startRadius, int endRadius, ExplosionRule rule) {
		return new FilterRandomDistanceExplosionRule(endRadius - startRadius, startRadius, ProbabilityFunction.LINEAR, rule);
	}
	
	/**
	 * Generally faster than {@link #linearDecrease(int, int, ExplosionRule)}, as it omits {@link Math#sqrt(double)}, but probability fall off curve looks different. 
	 */
	public static FilterRandomDistanceExplosionRule quadraticDecrease(int startRadius, int endRadius, ExplosionRule rule) {
		int startRadiusSqr = startRadius * startRadius;
		int endRadiusSqr = endRadius * endRadius;
		return new FilterRandomDistanceExplosionRule(endRadiusSqr - startRadiusSqr, startRadiusSqr, ProbabilityFunction.QUADRATIC, rule);
	}
	
	@Override
	@Nullable
	public BlockState getState(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ, RandomSource random) {
		float probability = probabilityFunction.calculateProbability(offX, offY, offZ, radius, startRadius);
		if (probability > 0 && random.nextFloat() < probability) {
			return rule.getState(level, state, center, offX, offY, offZ, random);
		}
		return null;
	}

	@Override
	public JsonObject encode(JsonObject root) {
		root.addProperty("type", RESOURCE_LOCATION.toString());
		root.addProperty("radius", radius);
		root.addProperty("startRadius", startRadius);
		root.addProperty("function", probabilityFunction.getName());
		root.add("rule", rule.encode(new JsonObject()));
		return root;
	}

	public static ExplosionRule decode(JsonObject root) {
		return new FilterRandomDistanceExplosionRule(root.get("radius").getAsInt(), root.get("startRadius").getAsInt(), ProbabilityFunction.byName(root.get("function").getAsString()), ExplosionRule.parse(root.get("rule").getAsJsonObject()));
	}
	
	public static enum ProbabilityFunction {
		LINEAR("linear", (offX, offY, offZ, r, start) -> {
			float distance = Mth.sqrt(offX * offX + offY * offY + offZ * offZ);
			return 1f - (distance - start) / r;
		}),
		QUADRATIC("quadratic", (offX, offY, offZ, r, start) -> {
			int distanceSqr = offX * offX + offY * offY + offZ * offZ;
			return 1f - (distanceSqr - start) / r;
		});
		
		private final String name;
		private final ProbabilityCalculator probabilityCalculator;
		
		private ProbabilityFunction(String name, ProbabilityCalculator probabilityCalculator) {
			this.name = name;
			this.probabilityCalculator = probabilityCalculator;
		}
		
		public float calculateProbability(int offX, int offY, int offZ, int radius, int startRadius) {
			return probabilityCalculator.calculate(offX, offY, offZ, radius, startRadius);
		}
		
		public String getName() {
			return name;
		}
		
		public ProbabilityCalculator getCalculator() {
			return probabilityCalculator;
		}
		
		public static ProbabilityFunction byName(String name) {
			if (name.equals(QUADRATIC.getName())) {
				return QUADRATIC;
			}
			return LINEAR;
		}
	}
	
	@FunctionalInterface
	public static interface ProbabilityCalculator {
		public float calculate(int offX, int offY, int offZ, int radius, int startRadius);
	}
}
