package luckytntlib.util.explosions.rules;

import java.util.function.BiFunction;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;

import luckytntlib.LuckyTNTLib;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * An {@link ExplosionRule} that evaluates the conditions of one or two wrapped {@link ExplosionRule}s based on a {@link LogicOperator} to determine whether it should apply
 */
public class LogicExplosionRule implements ExplosionRule {

	public static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(LuckyTNTLib.MODID, "logic");
	
	private final LogicOperator operator;
	@Nullable
	private final ExplosionRule conditionRule;
	private final ExplosionRule rule;
	
	protected LogicExplosionRule(LogicOperator operator, @Nullable ExplosionRule conditionRule, ExplosionRule rule) {
		this.operator = operator;
		this.conditionRule = conditionRule;
		this.rule = rule;
	}
	
	@Override
	public boolean shouldApply(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ) {
		return operator.apply(conditionRule == null ? false : conditionRule.shouldApply(level, state, center, offX, offY, offZ), rule.shouldApply(level, state, center, offX, offY, offZ));
	}

	@Override
	public BlockState getState() {
		return rule.getState();
	}

	@Override
	public JsonObject encode(JsonObject root) {
		root.addProperty("type", RESOURCE_LOCATION.toString());
		
		root.add("conditionRule", conditionRule.encode(new JsonObject()));
		
		root.add("rule", rule.encode(new JsonObject()));
		
		root.addProperty("operator", operator.getName());
		
		return root;
	}
	
	public static ExplosionRule decode(JsonObject root) {
		return new LogicExplosionRule(LogicOperator.byName(root.get("operator").getAsString()), ExplosionRule.parse(root.get("conditionRule").getAsJsonObject()), ExplosionRule.parse(root.get("conditionRule").getAsJsonObject()));
	}
	
	/**
	 * Creates a new {@code LogicExplosionRule} where the condition of the wrapped {@link ExplosionRule} is negated
	 */
	public static LogicExplosionRule not(ExplosionRule rule) {
		return new LogicExplosionRule(LogicOperator.NOT, null, rule);
	}
	
	/**
	 * Creates a new {@code LogicExplosionRule} where the condition of both wrapped {@link ExplosionRule}s have to be met.
	 * If that applies, the {@link BlockState} will be determined by {@code rule}.
	 * {@code conditionRule} will only be used to check whether the rule should apply.
	 */
	public static LogicExplosionRule and(ExplosionRule conditionRule, ExplosionRule rule) {
		return new LogicExplosionRule(LogicOperator.AND, conditionRule, rule);
	}
	
	/**
	 * Creates a new {@code LogicExplosionRule} where the condition of at least one of the wrapped {@link ExplosionRule}s has to be met.
	 * If that applies, the {@link BlockState} will be determined by {@code rule}.
	 * {@code conditionRule} will only be used to check whether the rule should apply.
	 */
	public static LogicExplosionRule or(ExplosionRule conditionRule, ExplosionRule rule) {
		return new LogicExplosionRule(LogicOperator.OR, conditionRule, rule);
	}
	
	/**
	 * Creates a new {@code LogicExplosionRule} where either both wrapped {@link ExplosionRule}s must apply, or neither of them.
	 * If that condition id met, the {@link BlockState} will be determined by {@code rule}.
	 * {@code conditionRule} will only be used to check whether the rule should apply.
	 */
	public static LogicExplosionRule equal(ExplosionRule conditionRule, ExplosionRule rule) {
		return new LogicExplosionRule(LogicOperator.EQUAL, conditionRule, rule);
	}
	
	/**
	 * Creates a new {@code LogicExplosionRule} where the condition of exactly one of the wrapped {@link ExplosionRule}s has to be met.
	 * If that applies, the {@link BlockState} will be determined by {@code rule}.
	 * {@code conditionRule} will only be used to check whether the rule should apply.
	 */
	public static LogicExplosionRule xor(ExplosionRule conditionRule, ExplosionRule rule) {
		return new LogicExplosionRule(LogicOperator.XOR, conditionRule, rule);
	}

	/**
	 * Simple utility enum that represents boolean operators
	 */
	public static enum LogicOperator {
		NOT("not", (b1, b2) -> !b2),
		AND("and", (b1, b2) -> b1 && b2),
		OR("or", (b1, b2) -> b1 || b2),
		EQUAL("equal", (b1, b2) -> b1 == b2),
		XOR("xor", (b1, b2) -> b1 != b2);
		
		private final String name;
		private final BiFunction<Boolean, Boolean, Boolean> comparator;
		
		private LogicOperator(String name, BiFunction<Boolean, Boolean, Boolean> comparator) {
			this.name = name;
			this.comparator = comparator;
		}
		
		/**
		 * Evaluates two given {@code boolean} values
		 * @param b1  the first {@code boolean} value
		 * @param b2  the second {@code boolean} value
		 * @return {@code true} if the given {@code boolean} values model the operator this method has been performed on, otherwise {@code false}
		 */
		public boolean apply(boolean b1, boolean b2) {
			return comparator.apply(b1, b2);
		}
		
		public String getName() {
			return name;
		}
		
		public BiFunction<Boolean, Boolean, Boolean> getComparator() {
			return comparator;
		}
		
		/**
		 * Returns a {@code LogicOperator} whose name matches the given {@link String} if there is one
		 * @param name  the name of a {@code LogicOperator}
		 * @return a {@code LogicOperator} whose name matches {@code name} if there is one, otherwise defaults to {@link #NOT}
		 */
		public static LogicOperator byName(String name) {
			for (LogicOperator operator : LogicOperator.values()) {
				if (operator.getName().equals(name)) {
					return operator;
				}
			}
			return NOT;
		}
	}
}
