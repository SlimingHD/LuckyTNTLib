package luckytntlib.util.explosions.rules;

import com.google.gson.JsonObject;

import luckytntlib.LuckyTNTLib;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * An {@link ExplosionRule} that filters according to a supplied fully customizable filter
 */
public class FilterCustomExplosionRule implements ExplosionRule {
	
	public static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(LuckyTNTLib.MODID, "filter_custom");
	
	private final CustomFilter filter;
	private final ExplosionRule rule;
	
	public FilterCustomExplosionRule(CustomFilter filter, ExplosionRule rule) {
		this.filter = filter;
		this.rule = rule;
	}

	@Override
	public void setupClientData(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ) {
		rule.setupClientData(level, state, center, offX, offY, offZ);
	}
	
	@Override
	public boolean shouldApply(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ) {
		return filter.shouldApply(level, state, center, offX, offY, offZ) && rule.shouldApply(level, state, center, offX, offY, offZ);
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
		return new FilterCustomExplosionRule(null, ExplosionRule.parse(root.get("rule").getAsJsonObject()));
	}
	
	/**
	 * An interface used to supply a {@link FilterCustomExplosionRule} with a filter for what blocks should and shouldn't be affected
	 */
	@FunctionalInterface
	public static interface CustomFilter {
		
		/**
		 * For each block in the vicinity of an explosion a {@link FilterCustomExplosionRule} will check whether the block should be affected or not with this method
		 */
		boolean shouldApply(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ);
	}
}
