package luckytntlib.util.explosions.rules;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;

import luckytntlib.LuckyTNTLib;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * {@link ExplosionRule} that filters out any blocks affected by an explosion that are air
 */
public class FilterAirExplosionRule implements ExplosionRule {

	public static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(LuckyTNTLib.MODID, "filter_air");
	
	private final ExplosionRule rule;
	
	public FilterAirExplosionRule(ExplosionRule rule) {
		this.rule = rule;
	}
	
	@Override
	@Nullable
	public BlockState getState(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ, RandomSource random) {
		if (state.isAir()) {
			return null;
		}
		return rule.getState(level, state, center, offX, offY, offZ, random);
	}

	@Override
	public JsonObject encode(JsonObject root) {
		root.addProperty("type", RESOURCE_LOCATION.toString());
		root.add("rule", rule.encode(new JsonObject()));
		return root;
	}
	
	public static ExplosionRule decode(JsonObject root) {
		return new FilterAirExplosionRule(ExplosionRule.parse(root.get("rule").getAsJsonObject()));
	}
}