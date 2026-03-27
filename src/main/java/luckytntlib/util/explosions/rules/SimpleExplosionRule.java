package luckytntlib.util.explosions.rules;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import luckytntlib.LuckyTNTLib;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * {@link ExplosionRule} that provides a given {@link BlockState} for placement
 */
public class SimpleExplosionRule implements ExplosionRule {

	public static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(LuckyTNTLib.MODID, "simple");
	
	private final BlockState state;
	
	public SimpleExplosionRule(BlockState state) {
		this.state = state;
	}
	
	@Override
	public BlockState getState(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ, RandomSource random) {
		return state;
	}

	@Override
	public JsonObject encode(JsonObject root) {
		root.addProperty("type", RESOURCE_LOCATION.toString());
		
		JsonElement encodedBlockState = BlockState.CODEC.encodeStart(JsonOps.COMPRESSED, state).getOrThrow(false, s -> {});
		root.add("state", encodedBlockState);
		
		return root;
	}

	public static ExplosionRule decode(JsonObject root) {
		BlockState state = BlockState.CODEC.parse(JsonOps.COMPRESSED, root.get("state")).get().left().orElseGet(() -> null);
		if (state == null) {
			state = Blocks.AIR.defaultBlockState();
		}
		
		return new SimpleExplosionRule(state);
	}
}