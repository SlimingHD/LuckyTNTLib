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
	@Nullable
	public BlockState getState(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ, RandomSource random) {
		if (Math.max(state.getBlock().getExplosionResistance(), state.getFluidState().getExplosionResistance()) <= maxResistance) {
			return rule.getState(level, state, center, offX, offY, offZ, random);
		}
		return null;
	}

	@Override
	public JsonObject encode(JsonObject root) {
		root.addProperty("type", RESOURCE_LOCATION.toString());
		root.addProperty("resistance", maxResistance);
		root.add("rule", rule.encode(new JsonObject()));
		return root;
	}

	public static ExplosionRule decode(JsonObject root) {
		return new FilterBlastResistanceExplosionRule(root.get("resistance").getAsFloat(), ExplosionRule.parse(root.get("rule").getAsJsonObject()));
	}
}
