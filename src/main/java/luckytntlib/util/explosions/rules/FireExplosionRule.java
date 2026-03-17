package luckytntlib.util.explosions.rules;

import com.google.gson.JsonObject;

import luckytntlib.LuckyTNTLib;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class FireExplosionRule implements ExplosionRule {

	public static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(LuckyTNTLib.MODID, "fire");
	
	private final float probability;
	
	private Level currentLevel;
	private BlockPos currentPos;
	
	public FireExplosionRule(float probability) {
		this.probability = probability;
	}
	
	public FireExplosionRule() {
		probability = 1f;
	}
	
	@Override
	public boolean shouldApply(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ) {
		currentPos = BlockPos.containing(center).offset(offX, offY, offZ);
		currentLevel = level;
		if (level.isClientSide()) {
			return true;
		}
		if (probability != 1 && level.getRandom().nextFloat() > probability) {
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
		return root;
	}
	
	public static ExplosionRule decode(JsonObject root) {
		return new FireExplosionRule();
	}
}
