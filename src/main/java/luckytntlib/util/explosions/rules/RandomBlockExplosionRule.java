package luckytntlib.util.explosions.rules;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import luckytntlib.LuckyTNTLib;
import luckytntlib.util.RandomList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * {@link ExplosionRule} that chooses a random block for placement from a given selection of {@link BlockState}s that are weighted
 */
public class RandomBlockExplosionRule implements ExplosionRule {

	public static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(LuckyTNTLib.MODID, "random_block");
	
	private static final Random RANDOM = new Random();
	
	private final RandomList<BlockState> states;
	private final RandomSource random;
	private final long seed;
	
	public RandomBlockExplosionRule(RandomList<BlockState> states) {
		this(states, RANDOM.nextLong());
	}
	
	public RandomBlockExplosionRule(RandomList<BlockState> states, long seed) {
		this.states = states;
		this.random = RandomSource.create(seed);
		this.seed = seed;
	}
	
	@Override
	public boolean shouldApply(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ) {
		return true;
	}

	@Override
	public BlockState getState() {
		return states.getRandomItem(random);
	}

	@Override
	public JsonObject encode(JsonObject root) {
		root.addProperty("type", RESOURCE_LOCATION.toString());
		
		JsonArray jsonStates = new JsonArray();
		for (BlockState state : states.getItems()) {
			jsonStates.add(BlockState.CODEC.encodeStart(JsonOps.COMPRESSED, state).getOrThrow(false, s -> {}));
		}
		root.add("states", jsonStates);
		
		JsonArray jsonWeights = new JsonArray();
		for (float f : states.getWeights()) {
			jsonWeights.add(f);
		}
		root.add("weights", jsonWeights);
		
		root.addProperty("seed", seed);
		
		return root;
	}

	public static ExplosionRule decode(JsonObject root) {
		JsonArray jsonStates = root.get("states").getAsJsonArray();
		ArrayList<BlockState> states = new ArrayList<>(jsonStates.size());
		for (int i = 0; i < jsonStates.size(); i++) {
			BlockState state = Blocks.AIR.defaultBlockState();
			Optional<BlockState> optional = BlockState.CODEC.parse(JsonOps.COMPRESSED, jsonStates.get(i)).result();
			if (optional.isPresent()) {
				state = optional.get();
			}
			states.add(state);
		}
		
		JsonArray jsonWeights = root.get("weights").getAsJsonArray();
		ArrayList<Float> weights = new ArrayList<>(jsonWeights.size());
		for (int i = 0; i < jsonWeights.size(); i++) {
			weights.add(jsonWeights.get(i).getAsFloat());
		}
		
		long seed = root.get("seed").getAsLong();
		
		return new RandomBlockExplosionRule(new RandomList<BlockState>(states, weights), seed);
	}
}
