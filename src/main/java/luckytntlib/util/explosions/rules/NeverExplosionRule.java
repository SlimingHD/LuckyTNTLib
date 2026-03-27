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
 * An {@link ExplosionRule} that never applies. Used to break out of conditional paths.
 */
public class NeverExplosionRule implements ExplosionRule {
	
	public static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(LuckyTNTLib.MODID, "never");

	public NeverExplosionRule() {
	}

	@Override
	@Nullable
	public BlockState getState(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ, RandomSource random) {
		return null;
	}

	@Override
	public JsonObject encode(JsonObject root) {
		root.addProperty("type", RESOURCE_LOCATION.toString());
		return root;
	}

	public static ExplosionRule decode(JsonObject root) {
		return new NeverExplosionRule();
	}
}
