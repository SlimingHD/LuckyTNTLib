package luckytntlib.util.explosions.rules;

import com.google.gson.JsonObject;

import luckytntlib.LuckyTNTLib;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * An {@link ExplosionRule} that filters for blocks that meet a given condition related to distance determined by a {@link DistanceComparator}
 */
public class DistanceExplosionRule implements ExplosionRule {
	
	public static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(LuckyTNTLib.MODID, "distance");

	private final ExplosionRule rule;
	private final DistanceComparator comparator;
	
	public DistanceExplosionRule(ExplosionRule rule, DistanceComparator comparator) {
		this.rule = rule;
		this.comparator = comparator;
	}
	
	@Override
	public boolean shouldApply(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ) {
		return comparator.withinRange(offX, offY, offZ) && rule.shouldApply(level, state, center, offX, offY, offZ);
	}

	@Override
	public BlockState getState() {
		return rule.getState();
	}

	@Override
	public JsonObject encode(JsonObject root) {
		root.addProperty("type", RESOURCE_LOCATION.toString());
		
		root.add("rule", rule.encode(new JsonObject()));
		
		comparator.write(root);
		
		return root;
	}
	
	public static ExplosionRule decode(JsonObject root) {
		ExplosionRule rule = ExplosionRule.parse(root.get("rule").getAsJsonObject());
		
		DistanceComparator comparator = DistanceComparator.parse(root);
		
		return new DistanceExplosionRule(rule, comparator);
	}
	
	/**
	 * Used to determine when a {@link DistanceExplosionRule} should apply by evaluating the distance of a position to the center of an explosion
	 */
	public static class DistanceComparator {

		private final DistanceComparingStrategy strategy;
		private final int minDistance, maxDistance;
		
		/**
		 * Creates a new DistanceComparator
		 * @param strategy  the {@link DistanceComparingStrategy} that will be used for evaluation
		 * @param minDistance  the minimum distance a block can be from the explosions center and still be affected. Might not be used depending on {@code strategy}.
		 * @param maxDistance  the maximum distance a block can be from the explosions center and still be affected. Might not be used depending on {@code strategy}.
		 */
		public DistanceComparator(DistanceComparingStrategy strategy, int minDistance, int maxDistance) {
			if ((strategy.useMinDistance() && minDistance < 0) || (strategy.useMaxDistance() && maxDistance < 0)) {
				throw new IllegalArgumentException("Neither minDistance (" + minDistance + ") nor maxDistance (" + maxDistance + ") can be lower than 0");
			}
			if (strategy.useMinDistance() && strategy.useMaxDistance() && minDistance > maxDistance) {
				throw new IllegalArgumentException("minDistance (" + minDistance + ") cannot be bigger than maxDistance (" + maxDistance + ")");
			}
			this.strategy = strategy;
			this.minDistance = minDistance;
			this.maxDistance = maxDistance;
		}
		
		public boolean withinRange(int offX, int offY, int offZ) {
			return strategy.getDistanceEvaluator().withinRange(offX, offY, offZ, minDistance, maxDistance);
		}
		
		/**
		 * Encodes the DistanceComparator to a given {@link JsonObject}
		 * @param root  the {@link JsonObject} the DistanceComparator will be encoded to
		 */
		public void write(JsonObject root) {
			JsonObject encodedComparator = new JsonObject();
			encodedComparator.addProperty("strategy", strategy.getName());
			encodedComparator.addProperty("minDist", minDistance);
			encodedComparator.addProperty("maxDist", maxDistance);
			root.add("comparator", encodedComparator);
		}
		
		/**
		 * Parses a DistanceComparator from a given {@link JsonObject}
		 * @param root  the {@link JsonObject} containing an encoded DistanceComparator
		 * @return the parsed DistanceComparator
		 */
		public static DistanceComparator parse(JsonObject root) {
			JsonObject encodedComparator = root.get("comparator").getAsJsonObject();
			return new DistanceComparator(DistanceComparingStrategy.byName(encodedComparator.get("strategy").getAsString()), encodedComparator.get("minDist").getAsInt(), encodedComparator.get("maxDist").getAsInt());
		}
	}
	
	/**
	 * An enum holding all possible strategies when evaluating the distance of a block to an explosions center
	 */
	public static enum DistanceComparingStrategy {
		/**
		 * Block will be within range if its distance to the explosions center is bigger than or equal to a given minimum distance
		 */
		GREATER_THAN("greater", true, false, (x, y, z, min, max) -> x * x + y * y + z * z >= min * min),
		/**
		 * Block will be within range if its distance to the explosions center is smaller than or equal to a given maximum distance
		 */
		SMALLER_THAN("smaller", false, true, (x, y, z, min, max) -> x * x + y * y + z * z <= max * max),
		/**
		 * Block will be within range if its distance to the explosions center is in between a given minimum distance and a given maximum distance or equal to either
		 */
		BETWEEN("between", true, true, (x, y, z, min, max) -> {
			int distSquared = x * x + y * y + z * z;
			return distSquared >= min * min && distSquared <= max * max;
		});
		
		private final String name;
		private final boolean useMinDistance, useMaxDistance;
		private final DistanceEvaluator evaluator;
		
		private DistanceComparingStrategy(String name, boolean useMinDistance, boolean useMaxDistance, DistanceEvaluator evaluator) {
			this.name = name;
			this.useMinDistance = useMinDistance;
			this.useMaxDistance = useMaxDistance;
			this.evaluator = evaluator;
		}
		
		public String getName() {
			return name;
		}
		
		public boolean useMinDistance() {
			return useMinDistance;
		}
		
		public boolean useMaxDistance() {
			return useMaxDistance;
		}
		
		public DistanceEvaluator getDistanceEvaluator() {
			return evaluator;
		}
		
		/**
		 * Returns a DistanceComparingStrategy where the name matches a given {@link String} if there is one
		 * @param name  the name of a DistanceComparingStrategy
		 * @return a DistanceComparingStrategy where the name matches {@code name} if there is one, otherwise defaults to {@link #GREATER_THAN}
		 */
		public static DistanceComparingStrategy byName(String name) {
			for (DistanceComparingStrategy d : values()) {
				if (d.getName().equals(name)) {
					return d;
				}
			}
			return GREATER_THAN;
		}
	}
	
	/**
	 * {@link FunctionalInterface} used to evaluate whether a given offset is within a certain range
	 */
	@FunctionalInterface
	public static interface DistanceEvaluator {
		
		/**
		 * This method evaluates whether a given offset is within a certain range or not
		 * @param offX  the offset on the x axis
		 * @param offY  the offset on the y axis
		 * @param offZ  the offset on the z axis
		 * @param minDistance  the minimum distance the offset position must have
		 * @param maxDistance  the maximum distance the offset position can have
		 * @return {@code true} if the given offset is within a certain range, otherwise {@code false}
		 */
		boolean withinRange(int offX, int offY, int offZ, int minDistance, int maxDistance);
	}
}