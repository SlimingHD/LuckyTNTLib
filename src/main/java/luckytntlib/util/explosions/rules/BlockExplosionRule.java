package luckytntlib.util.explosions.rules;

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
public class BlockExplosionRule implements ExplosionRule {

	public static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(LuckyTNTLib.MODID, "block");
	
	private final BlockState stateToPlace;
	
	public BlockExplosionRule(BlockState stateToPlace) {
		this.stateToPlace = stateToPlace;
	}
	
	@Override
	public BlockState getState(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ, RandomSource random) {
		return stateToPlace;
	}

	@Override
	public JsonObject encode(JsonObject root) {
		root.addProperty("type", RESOURCE_LOCATION.toString());
		root.add("state", BlockState.CODEC.encodeStart(JsonOps.INSTANCE, stateToPlace).getOrThrow(false, s -> {}));
		return root;
	}

	public static ExplosionRule decode(JsonObject root) {
		BlockState state = BlockState.CODEC.parse(JsonOps.INSTANCE, root.get("state")).get().left().orElseGet(() -> null);
		if (state == null) {
			state = Blocks.AIR.defaultBlockState();
		}
		
		return new BlockExplosionRule(state);
	}
}