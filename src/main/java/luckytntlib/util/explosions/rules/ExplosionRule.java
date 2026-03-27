package luckytntlib.util.explosions.rules;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;

import luckytntlib.registry.ExplosionRuleRegistry;
import luckytntlib.util.explosions.ExplosionHelper;
import luckytntlib.util.explosions.ImprovedExplosion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * ExplosionRule is used to determine if and how a block affected by an explosion should be edited.
 * It is mainly used in {@link ExplosionHelper} and {@link ImprovedExplosion}.
 */
public interface ExplosionRule {
	
	/**
	 * This method determines the state that is to be placed at the supplied position. <br>
	 * If the rule should not apply at the supplied position, return value must be {@code null}. 
	 * <p>
	 * <strong><i><font color="#E00000"> This method has to examine the same behavior on both logical sides! </font></i></strong>
	 * @param level  the current {@link Level}
	 * @param state  the {@link BlockState} being affected by an explosion
	 * @param center  the center of the explosion affecting the given {@link BlockState}
	 * @param offX  the offset of the given {@link BlockState} to the center of the explosion on the x axis
	 * @param offY  the offset of the given {@link BlockState} to the center of the explosion on the y axis
	 * @param offZ  the offset of the given {@link BlockState} to the center of the explosion on the z axis
	 * @param random  if you need random numbers, you should use this {@link RandomSource} because its automatically synchronized between server and client
	 * @return a {@link BlockState} that will be placed at the supplied position if the rule applies. If the rule doesn't apply, returns {@code null}.
	 */
	@Nullable
	BlockState getState(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ, RandomSource random);
	
	/**
	 * Encodes all data relevant to the rule on the client into the given {@link JsonObject}.
	 * @param root  the root {@link JsonObject} to write the data to
	 * @return the {@link JsonObject} that represents the object all data has been written to, should usually just be {@code root}
	 * 
	 * @implNote Additionally to this method every {@code ExplosionRule} needs a {@code public static} method that returns a {@code ExplosionRule} decoded from a given {@link JsonObject}.
	 * That method needs to be registered to {@link ExplosionRuleRegistry#EXPLOSION_RULES}. 
	 * <p>
	 * It is imperative that the given {@link JsonObject} contains a property named "type" that holds the registry name of this rule as a {@link String} after this method finishes.
	 * 
	 * @see <i> How to register: </i>
	 * @see ExplosionRuleRegistry
	 * @see ExplosionRuleRegistry <br>
	 * @see <i> Implementation examples: </i>
	 * @see FilterDistanceExplosionRule#decode(JsonObject)
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