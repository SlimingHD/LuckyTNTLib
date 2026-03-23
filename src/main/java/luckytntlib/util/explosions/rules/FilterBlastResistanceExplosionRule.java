package luckytntlib.util.explosions.rules;

import com.google.gson.JsonObject;

import luckytntlib.LuckyTNTLib;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * {@link ExplosionRule} that filters out any blocks affected by an explosion that exceed a certain blast resistance
 */
public class FilterBlastResistanceExplosionRule implements ExplosionRule {
	
	public static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(LuckyTNTLib.MODID, "filter_blast_resistance");

	private final float maxResistance;
	private final ExplosionRule rule;
	
	public FilterBlastResistanceExplosionRule(float maxResistance, ExplosionRule rule) {
		this.maxResistance = maxResistance;
		this.rule = rule;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean shouldApply(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ) {
		if (state.getBlock().getExplosionResistance() <= maxResistance) {
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

		root.addProperty("maxResistance", maxResistance);
		
		return root;
	}

	public static ExplosionRule decode(JsonObject root) {
		return new FilterBlastResistanceExplosionRule(root.get("maxResistance").getAsFloat(), ExplosionRule.parse(root.get("rule").getAsJsonObject()));
	}
}
