package luckytntlib.util.explosions.rules;

import com.google.gson.JsonObject;

import luckytntlib.LuckyTNTLib;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * An {@link ExplosionRule} that always applies. Used as an end to conditional paths.
 */
public class AlwaysExplosionRule implements ExplosionRule {

	public static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(LuckyTNTLib.MODID, "always");

	public AlwaysExplosionRule() {
	}

	@Override
	public BlockState getState(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ, RandomSource random) {
		return Blocks.AIR.defaultBlockState();
	}

	@Override
	public JsonObject encode(JsonObject root) {
		root.addProperty("type", RESOURCE_LOCATION.toString());
		return root;
	}

	public static ExplosionRule decode(JsonObject root) {
		return new AlwaysExplosionRule();
	}
}
