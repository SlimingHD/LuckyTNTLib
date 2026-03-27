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
 * {@link ExplosionRule} that applies to all blocks randomly with a set probability.
 */
public class FilterRandomExplosionRule implements ExplosionRule {

	public static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(LuckyTNTLib.MODID, "filter_random");
	
	private final float probability;
	private final ExplosionRule rule;

	public FilterRandomExplosionRule(float probability,ExplosionRule rule) {
		this.rule = rule;
		this.probability = probability;
	}

	@Override
	@Nullable
	public BlockState getState(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ, RandomSource random) {
		if (random.nextFloat() < probability) {
			return rule.getState(level, state, center, offX, offY, offZ, random);
		}
		return null;
	}

	@Override
	public JsonObject encode(JsonObject root) {
		root.addProperty("type", RESOURCE_LOCATION.toString());
		root.addProperty("probability", probability);
		root.add("rule", rule.encode(new JsonObject()));
		return root;
	}

	public static ExplosionRule decode(JsonObject root) {
		return new FilterRandomExplosionRule(root.get("probability").getAsFloat(), ExplosionRule.parse(root.get("rule").getAsJsonObject()));
	}
}
