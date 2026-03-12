package luckytntlib.util.explosions.rules;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;

import luckytntlib.registry.ExplosionRuleRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public interface ExplosionRule {
	
	boolean shouldApply(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ);
	
	BlockState getState();
	
	JsonObject encode(JsonObject root);
	
	@Nullable
	public static ExplosionRule byName(String name, JsonObject root) {
		return ExplosionRuleRegistry.EXPLOSION_RULES.get().getValue(new ResourceLocation(name)).apply(root);
	}
}