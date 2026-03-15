package luckytntlib.util.explosions.rules;

import com.google.gson.JsonObject;

import luckytntlib.LuckyTNTLib;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * {@link ExplosionRule} that negates the condition of another given {@link ExplosionRule}
 */
public class NotExplosionRule implements ExplosionRule {
	
	public static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(LuckyTNTLib.MODID, "not");

	private final ExplosionRule rule;
	
	public NotExplosionRule(ExplosionRule rule) {
		this.rule = rule;
	}
	
	@Override
	public boolean shouldApply(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ) {
		return !rule.shouldApply(level, state, center, offX, offY, offZ);
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
		return new NotExplosionRule(ExplosionRule.parse(root.get("rule").getAsJsonObject()));
	}
}
