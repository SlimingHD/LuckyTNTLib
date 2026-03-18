package luckytntlib.util.explosions.rules;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;

import luckytntlib.registry.ExplosionRuleRegistry;
import luckytntlib.util.explosions.ExplosionHelper;
import luckytntlib.util.explosions.ImprovedExplosion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * ExplosionRule is used to determine if and how a block affected by an explosion should be edited.
 * It is mainly used in {@link ExplosionHelper} and {@link ImprovedExplosion}.
 */
public interface ExplosionRule {
	
	/**
	 * This method determines whether the ExplosionRule will edit a block affected by an explosion at a given position
	 * @param level  the current {@link Level}
	 * @param state  the {@link BlockState} being affected by an explosion
	 * @param center  the center of the explosion affecting the given {@link BlockState}
	 * @param offX  the offset of the given {@link BlockState} to the center of the explosion on the x axis
	 * @param offY  the offset of the given {@link BlockState} to the center of the explosion on the y axis
	 * @param offZ  the offset of the given {@link BlockState} to the center of the explosion on the z axis
	 * @return {@code true} if this rule should edit the world at the given position and {@code false} otherwise. <br>
	 * If an ExplosionRule decides to apply at a given position, no other rule that could theoretically also apply to the same position will be able to do so.
	 * 
	 * @implNote This method will also be called on the client before calling {@link #getState()}, though it won't have an effect on whether the block will be edited or not since the server made that decision.
	 * You can, however, use that call to calculate and store information relevant to the following call of {@link #getState()}.
	 */
	boolean shouldApply(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ);
	
	/**
	 * Returns a {@link BlockState} that will be placed in the world. <br>
	 * Should only ever be called and used if {@link #shouldApply(Level, BlockState, Vec3, int, int, int)} returned {@code true} for a given position.
	 * @return a {@link BlockState} that will be placed in the world
	 */
	BlockState getState();
	
	/**
	 * Encodes all data relevant to the rule into the given {@link JsonObject}.
	 * @param root  the root {@link JsonObject} to write the data to
	 * @return the {@link JsonObject} that represents the object all data has been written to, should usually just be {@code root}
	 * 
	 * @implNote Additionally to this method every {@code ExplosionRule} needs a {@code public static} method that returns a {@code ExplosionRule} decoded from a given {@link JsonObject}.
	 * That method needs to be registered to {@link ExplosionRuleRegistry#EXPLOSION_RULES}. <p>
	 * It is imperative that the given {@link JsonObject} contains a property named "type" that holds the registry name of this rule as a {@link String} after this method finishes.
	 * 
	 * @see <i> How to register: </i>
	 * @see ExplosionRuleRegistry
	 * @see ExplosionRuleRegistry <br>
	 * @see <i> Implementation examples: </i>
	 * @see DistanceExplosionRule#decode(JsonObject)
	 * @see FilterAirExplosionRule#decode(JsonObject)
	 * @see FilterBlockExplosionRule#decode(JsonObject)
	 * @see RandomBlockExplosionRule#decode(JsonObject)
	 * @see StackedExplosionRule#decode(JsonObject)
	 */
	JsonObject encode(JsonObject root);
	
	/**
	 * Decodes a given {@link JsonObject} into an ExplosionRule
	 * @param root  the {@link JsonObject} a ExplosionRule has been encoded to
	 * @return the decoded ExplosionRule
	 */
	@Nullable
	public static ExplosionRule parse(JsonObject root) {
		return ExplosionRuleRegistry.EXPLOSION_RULES.get().getValue(new ResourceLocation(root.get("type").getAsString())).apply(root);
	}
}