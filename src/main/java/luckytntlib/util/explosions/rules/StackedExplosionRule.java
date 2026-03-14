package luckytntlib.util.explosions.rules;

import java.util.Collection;
import java.util.LinkedList;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import luckytntlib.LuckyTNTLib;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class StackedExplosionRule implements ExplosionRule {

	public static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(LuckyTNTLib.MODID, "stacked");

	private final ExplosionRule[] rules;
	private int applyingRuleIndex = 0;
	
	public StackedExplosionRule(Collection<ExplosionRule> rules) {
		this.rules = rules.toArray(new ExplosionRule[0]);
	}
	
	public StackedExplosionRule(ExplosionRule... rules) {
		this.rules = rules;
	}
	
	@Override
	public BlockState getState() {
		return rules[applyingRuleIndex].getState();
	}

	@Override
	public boolean shouldApply(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ) {
		for(int i = 0; i < rules.length; i++) {
			if(rules[i].shouldApply(level, state, center, offX, offY, offZ)) {
				applyingRuleIndex = i;
				return true;
			}
		}
		return false;
	}

	@Override
	public JsonObject encode(JsonObject root) {
		root.addProperty("type", RESOURCE_LOCATION.toString());
		
		JsonArray jsonRules = new JsonArray();
		for(ExplosionRule rule : rules) {
			jsonRules.add(rule.encode(new JsonObject()));
		}
		root.add("rules", jsonRules);
		
		return root;
	}
	
	public static ExplosionRule decode(JsonObject root) {
		LinkedList<ExplosionRule> rules = new LinkedList<>();
		JsonArray jsonRules = root.get("rules").getAsJsonArray();
		for(int i = 0; i < jsonRules.size(); i++) {
			JsonObject ruleRoot = jsonRules.get(i).getAsJsonObject();
			rules.add(ExplosionRule.byName(ruleRoot.get("type").getAsString(), ruleRoot));
		}
		
		return new StackedExplosionRule(rules);
	}
}