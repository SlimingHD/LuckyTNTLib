package luckytntlib.util.explosions.rules;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;

import luckytntlib.LuckyTNTLib;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * {@link ExplosionRule} that places fire wherever eligible. Eligibility is also randomly influenced by a set probability.
 * Fire blocks are automatically updated.
 */
public class FireExplosionRule implements ExplosionRule {

	public static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(LuckyTNTLib.MODID, "fire");
	
	private final float probability;
	
	public FireExplosionRule(float probability) {
		this.probability = probability;
	}
	
	@Override
	@Nullable
	public BlockState getState(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ, RandomSource random) {
		if (!state.isAir()) {
			return null;
		}
		if (!Mth.equal(probability, 1f) && random.nextFloat() > probability) {
			return null;
		}
		BlockPos pos = BlockPos.containing(center).offset(offX, offY, offZ);
		if (!BaseFireBlock.canBePlacedAt(level, pos, Direction.DOWN)) {
			return null;
		}
		level.scheduleTick(pos, Blocks.FIRE, 1);
		return BaseFireBlock.getState(level, pos);
	}
	
	@Override
	public JsonObject encode(JsonObject root) {
		root.addProperty("type", RESOURCE_LOCATION.toString());
		root.addProperty("probability", probability);
		return root;
	}
	
	public static ExplosionRule decode(JsonObject root) {
		return new FireExplosionRule(root.get("probability").getAsFloat());
	}
}
