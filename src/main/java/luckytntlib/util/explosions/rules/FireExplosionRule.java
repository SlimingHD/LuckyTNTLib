package luckytntlib.util.explosions.rules;

import com.google.gson.JsonObject;

import luckytntlib.LuckyTNTLib;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class FireExplosionRule implements ExplosionRule {

	public static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(LuckyTNTLib.MODID, "fire");
	
	private final RandomSource random = RandomSource.create();
	private final float probability;
	
	private Level currentLevel;
	private BlockPos currentPos;
	
	public FireExplosionRule(float probability) {
		this.probability = probability;
	}
	
	@Override
	public boolean shouldApply(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ) {
		currentPos = BlockPos.containing(center).offset(offX, offY, offZ);
		currentLevel = level;
		if (!level.isClientSide() && probability != 1 && random.nextFloat() > probability) {
			return false;
		}
		if (!BaseFireBlock.canBePlacedAt(level, currentPos, Direction.DOWN)) {
			return false;
		}
		return true;
	}
	
	@Override
	public BlockState getState() {
		if (!currentLevel.isClientSide()) {
			currentLevel.scheduleTick(currentPos, Blocks.FIRE, 1);
		}
		return BaseFireBlock.getState(currentLevel, currentPos);
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
