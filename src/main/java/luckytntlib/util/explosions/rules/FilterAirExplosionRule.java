package luckytntlib.util.explosions.rules;

import com.google.gson.JsonObject;

import luckytntlib.LuckyTNTLib;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class FilterAirExplosionRule implements ExplosionRule {

	public static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(LuckyTNTLib.MODID, "filter_air");
	
	private final ExplosionRule rule;
	
	public FilterAirExplosionRule(ExplosionRule rule) {
		this.rule = rule;
	}
	
	@Override
	public BlockState getState() {
		return rule.getState();
	}

	@Override
	public boolean shouldApply(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ) {
		if(state.isAir()) {
			return false;
		}
		return rule.shouldApply(level, state, center, offX, offY, offZ);
	}

	@Override
	public JsonObject encode(JsonObject root) {
		root.addProperty("type", RESOURCE_LOCATION.toString());
		
		root.add("rule", rule.encode(new JsonObject()));
		
		return root;
	}
	
	public static ExplosionRule decode(JsonObject root) {
		JsonObject ruleRoot = root.get("rule").getAsJsonObject();
		return new FilterAirExplosionRule(ExplosionRule.byName(ruleRoot.get("type").getAsString(), ruleRoot));
	}
}