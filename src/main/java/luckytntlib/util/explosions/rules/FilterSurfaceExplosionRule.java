package luckytntlib.util.explosions.rules;

import com.google.gson.JsonObject;

import luckytntlib.LuckyTNTLib;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * {@link ExplosionRule} that only applies if a block is considered to be a surface (have a non-collidable block above it and have a full top face itself).
 */
public class FilterSurfaceExplosionRule implements ExplosionRule {
	
	public static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(LuckyTNTLib.MODID, "filter_surface");

	/**
	 * Whether the target of this rule should be the surface block ({@code true}) or the non-collidable block above it ({@code false})
	 */
	private final boolean targetSurface;
	private final ExplosionRule rule;
	
	/**
	 * @param targetSurface Whether the target of this rule should be the surface block ({@code true}) or the non-collidable block above it ({@code false})
	 * @param rule Another rule to wrap this filter around
	 */
	public FilterSurfaceExplosionRule(boolean targetSurface, ExplosionRule rule) {
		this.targetSurface = targetSurface;
		this.rule = rule;
	}
	
	@Override
	public boolean shouldApply(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ) {
		BlockPos pos = BlockPos.containing(center).offset(offX, offY, offZ);
		if (targetSurface) {
			BlockPos above = pos.above();
			if (Block.isFaceFull(state.getCollisionShape(level, pos), Direction.UP) && level.getBlockState(above).getCollisionShape(level, above).isEmpty()) {
				return rule.shouldApply(level, state, center, offX, offY, offZ);
			}
		} else {
			BlockPos below = pos.below();
			if (state.getCollisionShape(level, pos).isEmpty() && Block.isFaceFull(level.getBlockState(below).getCollisionShape(level, below), Direction.UP)) {
				return rule.shouldApply(level, state, center, offX, offY, offZ);
			}
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
		root.addProperty("target_surface", targetSurface);
		root.add("rule", rule.encode(new JsonObject()));
		return root;
	}

	public static ExplosionRule decode(JsonObject root) {
		return new FilterSurfaceExplosionRule(root.get("target_surface").getAsBoolean(), ExplosionRule.parse(root.get("rule").getAsJsonObject()));
	}
}
