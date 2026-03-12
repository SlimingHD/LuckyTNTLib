package luckytntlib.util.explosions.rules;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;

import luckytntlib.LuckyTNTLib;
import luckytntlib.registry.ExplosionRuleRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class DistanceExplosionRule implements ExplosionRule {
	
	public static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(LuckyTNTLib.MODID, "distance");

	private final ExplosionRule rule;
	private final DistanceComparator comparator;
	
	public DistanceExplosionRule(ExplosionRule rule, DistanceComparator comparator) {
		this.rule = rule;
		this.comparator = comparator;
	}
	
	@Override
	public BlockState getState() {
		return rule.getState();
	}

	@Override
	public boolean shouldApply(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ) {
		return comparator.shouldApply(center, offX, offY, offZ) && rule.shouldApply(level, state, center, offX, offY, offZ);
	}

	@Override
	public JsonObject encode(JsonObject root) {
		root.addProperty("type", RESOURCE_LOCATION.toString());
		
		root.add("rule", rule.encode(new JsonObject()));
		
		root.add("comparator", comparator.encode());
		
		return root;
	}
	
	public static ExplosionRule decode(JsonObject root) {
		JsonObject encodedRule = root.get("rule").getAsJsonObject();
		ExplosionRule rule = ExplosionRule.byName(encodedRule.get("type").getAsString(), encodedRule);
		
		JsonObject jsonComparator = root.get("comparator").getAsJsonObject();
		DistanceComparator comparator = DistanceComparator.byName(jsonComparator.get("type").getAsString(), jsonComparator);
		
		return new DistanceExplosionRule(rule, comparator);
	}

	public static interface DistanceComparator {
		
		boolean shouldApply(Vec3 center, int offX, int offY, int offZ);
		
		JsonObject encode();
		
		@Nullable
		public static DistanceComparator byName(String name, JsonObject root) {
			return ExplosionRuleRegistry.DISTANCE_COMPARATORS.get().getValue(new ResourceLocation(name)).apply(root);
		}
	}
	
	public static class GreaterThanDistanceComparator implements DistanceComparator {
		
		public static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(LuckyTNTLib.MODID, "greater_than");
		
		private final int minDistance;
		
		public GreaterThanDistanceComparator(int minDistance) {
			if(minDistance < 0) {
				throw new IllegalArgumentException("minDistance (" + minDistance + ") cannot be lower than 0");
			}
			this.minDistance = minDistance;
		}

		@Override
		public boolean shouldApply(Vec3 center, int offX, int offY, int offZ) {
			return Math.sqrt(offX * offX + offY * offY + offZ * offZ) >= minDistance;
		}
		
		@Override
		public JsonObject encode() {
			JsonObject root = new JsonObject();
			root.addProperty("type", RESOURCE_LOCATION.toString());
			root.addProperty("minDistance", minDistance);
			return root;
		}
		
		public static DistanceComparator decode(JsonObject root) {
			Number number = root.get("minDistance").getAsNumber();
			return new GreaterThanDistanceComparator(number.intValue());
		}
	}
	
	public static class SmallerThanDistanceComparator implements DistanceComparator {

		public static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(LuckyTNTLib.MODID, "smaller_than");

		private final int maxDistance;
		
		public SmallerThanDistanceComparator(int maxDistance) {
			if(maxDistance < 0) {
				throw new IllegalArgumentException("maxDistance (" + maxDistance + ") cannot be lower than 0");
			}
			this.maxDistance = maxDistance;
		}

		@Override
		public boolean shouldApply(Vec3 center, int offX, int offY, int offZ) {
			return Math.sqrt(offX * offX + offY * offY + offZ * offZ) <= maxDistance;
		}

		@Override
		public JsonObject encode() {
			JsonObject root = new JsonObject();
			root.addProperty("type", RESOURCE_LOCATION.toString());
			root.addProperty("maxDistance", maxDistance);
			return root;
		}
		
		public static DistanceComparator decode(JsonObject root) {
			Number number = root.get("maxDistance").getAsNumber();
			return new SmallerThanDistanceComparator(number.intValue());
		}
	}
	
	public static class SmallerAndGreaterThanDistanceComparator implements DistanceComparator {

		public static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(LuckyTNTLib.MODID, "smaller_and_greater_than");

		private final int minDistance, maxDistance;
		
		public SmallerAndGreaterThanDistanceComparator(int minDistance, int maxDistance) {
			if(minDistance < 0 || maxDistance < 0) {
				throw new IllegalArgumentException("Neither minDistance (" + minDistance + ") nor maxDistance (" + maxDistance + ") can be lower than 0");
			}
			if(minDistance > maxDistance) {
				throw new IllegalArgumentException("Value of minDistance (" + minDistance + ") is bigger than value of maxDistance (" + maxDistance + ")");
			}
			this.minDistance = minDistance;
			this.maxDistance = maxDistance;
		}

		@Override
		public boolean shouldApply(Vec3 center, int offX, int offY, int offZ) {
			double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
			return distance >= minDistance && distance <= maxDistance;
		}

		@Override
		public JsonObject encode() {
			JsonObject root = new JsonObject();
			root.addProperty("type", RESOURCE_LOCATION.toString());
			root.addProperty("maxDistance", maxDistance);
			root.addProperty("minDistance", minDistance);
			return root;
		}
		
		public static DistanceComparator decode(JsonObject root) {
			Number maxNumber = root.get("maxDistance").getAsNumber();
			Number minNumber = root.get("minDistance").getAsNumber();
			return new SmallerAndGreaterThanDistanceComparator(minNumber.intValue(), maxNumber.intValue());
		}
	}
}