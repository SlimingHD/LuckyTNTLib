package luckytntlib.util.explosions.rules;

import java.util.Optional;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import luckytntlib.LuckyTNTLib;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * This {@link ExplosionRule} applies only if the supplied {@link BlockState} can survive on the position.
 */
public class CanSurviveExplosionRule implements ExplosionRule {

	public static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(LuckyTNTLib.MODID, "can_survive");

	private final BlockState stateToPlace;
	
	public CanSurviveExplosionRule(BlockState stateToPlace) {
		this.stateToPlace = stateToPlace;
	}
	
	@Override
	public boolean shouldApply(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ) {
		return stateToPlace.canSurvive(level, BlockPos.containing(center).offset(offX, offY, offZ));
	}

	@Override
	public BlockState getState() {
		return stateToPlace;
	}

	@Override
	public JsonObject encode(JsonObject root) {
		root.addProperty("type", RESOURCE_LOCATION.toString());
		root.add("state", BlockState.CODEC.encodeStart(JsonOps.COMPRESSED, stateToPlace).getOrThrow(false, s -> {}));
		return root;
	}
	
	public static ExplosionRule decode(JsonObject root) {
		BlockState state = Blocks.AIR.defaultBlockState();
		Optional<BlockState> optional = BlockState.CODEC.parse(JsonOps.COMPRESSED, root.get("state").getAsJsonObject()).result();
		if (optional.isPresent()) {
			state = optional.get();
		}
		return new CanSurviveExplosionRule(state);
	}
}
