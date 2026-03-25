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
	private final ExplosionRule conditionRule1;
	@Nullable
	private final ExplosionRule conditionRule2;
	private final ExplosionRule rule;
	
	protected LogicExplosionRule(LogicOperator operator, ExplosionRule conditionRule1, @Nullable ExplosionRule conditionRule2, ExplosionRule rule) {
		this.operator = operator;
		this.conditionRule1 = conditionRule1;
		this.conditionRule2 = conditionRule2;
		this.rule = rule;
	}
	
	@Override
	public void setupClientData(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ) {
		rule.setupClientData(level, state, center, offX, offY, offZ);
	}
	
	@Override
	public boolean shouldApply(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ) {
		if (operator.apply(conditionRule1.shouldApply(level, state, center, offX, offY, offZ), conditionRule2 == null ? false : conditionRule2.shouldApply(level, state, center, offX, offY, offZ))) {
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
		return new LogicExplosionRule(LogicOperator.NOT, null, null, ExplosionRule.parse(root.get("rule").getAsJsonObject()));
	}
	
	/**
	 * Creates a new {@code LogicExplosionRule} where the condition of {@code conditionRule} has to be false.
	 * If that applies, {@code rule} will determine whether to change the {@link BlockState} and provide the {@link BlockState} in that case.
	 */
	public static LogicExplosionRule not(ExplosionRule conditionRule, ExplosionRule rule) {
		return new LogicExplosionRule(LogicOperator.NOT, conditionRule, null, rule);
	}
	
	/**
	 * Creates a new {@code LogicExplosionRule} where the conditions of both {@code conditionRule1} and {@code conditionRule2} have to be met.
	 * If that applies, {@code rule} will determine whether to change the {@link BlockState} and provide the {@link BlockState} in that case.
	 */
	public static LogicExplosionRule and(ExplosionRule conditionRule1, ExplosionRule conditionRule2, ExplosionRule rule) {
		return new LogicExplosionRule(LogicOperator.AND, conditionRule1, conditionRule2, rule);
	}
	
	/**
	 * Creates a new {@code LogicExplosionRule} where the condition of at least one of {@code conditionRule1} and {@code conditionRule2} has to be met.
	 * If that applies, {@code rule} will determine whether to change the {@link BlockState} and provide the {@link BlockState} in that case.
	 */
	public static LogicExplosionRule or(ExplosionRule conditionRule1, ExplosionRule conditionRule2, ExplosionRule rule) {
		return new LogicExplosionRule(LogicOperator.OR, conditionRule1, conditionRule2, rule);
	}
	
	/**
	 * Creates a new {@code LogicExplosionRule} where the conditions of both {@code conditionRule1} and {@code conditionRule2} have to return the same value.
	 * If that applies, {@code rule} will determine whether to change the {@link BlockState} and provide the {@link BlockState} in that case.
	 */
	public static LogicExplosionRule equal(ExplosionRule conditionRule1, ExplosionRule conditionRule2, ExplosionRule rule) {
		return new LogicExplosionRule(LogicOperator.EQUAL, conditionRule1, conditionRule2, rule);
	}
	
	/**
	 * Creates a new {@code LogicExplosionRule} where the conditions of both {@code conditionRule1} and {@code conditionRule2} have to return different values.
	 * If that applies, {@code rule} will determine whether to change the {@link BlockState} and provide the {@link BlockState} in that case.
	 */
	public static LogicExplosionRule xor(ExplosionRule conditionRule1, ExplosionRule conditionRule2, ExplosionRule rule) {
		return new LogicExplosionRule(LogicOperator.XOR, conditionRule1, conditionRule2, rule);
	}

	/**
	 * Simple utility enum that represents boolean operators
	 */
	public static enum LogicOperator {
		NOT("not", (b1, b2) -> !b1),
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
