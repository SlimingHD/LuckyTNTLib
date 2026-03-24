package luckytntlib.util.explosions.rules;

import java.util.Random;

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
	
	private static final Random RANDOM = new Random();
	
	private final float probability;
	private final long seed;
	private final RandomSource random;
	private final ExplosionRule rule;
	
	public FilterRandomExplosionRule(float probability, ExplosionRule rule) {
		this(probability, RANDOM.nextLong(), rule);
	}

	public FilterRandomExplosionRule(float probability, long seed, ExplosionRule rule) {
		this.rule = rule;
		this.probability = probability;
		this.seed = seed;
		this.random = RandomSource.create(seed);
	}
	
	@Override
	public boolean shouldApply(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ) {
		if (random.nextFloat() < probability) {
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
		root.addProperty("probability", probability);
		root.addProperty("seed", seed);
		root.add("rule", rule.encode(new JsonObject()));
		return root;
	}

	public static ExplosionRule decode(JsonObject root) {
		return new FilterRandomExplosionRule(root.get("probability").getAsFloat(), root.get("seed").getAsLong(), ExplosionRule.parse(root.get("rule").getAsJsonObject()));
	}
}
