package luckytntlib.util.explosions.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import luckytntlib.LuckyTNTLib;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * {@link ExplosionRule} that only applies if a block affected by an explosion is a certain block or in a specific state.
 */
public class FilterBlockExplosionRule implements ExplosionRule {
	
	public static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(LuckyTNTLib.MODID, "filter_block");
	
	private static final Supplier<? extends ResourceKey<Block>> ENCODE_FALLBACK = () -> ResourceKey.create(Registries.BLOCK, new ResourceLocation("air"));
	private static final Supplier<? extends Holder<Block>> DECODE_FALLBACK = () -> Holder.direct(Blocks.AIR);

	private final ExplosionRule rule;
	@Nullable
	private final Collection<Block> blocks;
	@Nullable
	private final Collection<BlockState> states;
	
	protected FilterBlockExplosionRule(ExplosionRule rule, @Nullable Collection<Block> blocks, @Nullable Collection<BlockState> states) {
		this.rule = rule;
		this.blocks = blocks;
		this.states = states;
	}
	
	@Override
	public boolean shouldApply(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ) {
		if (blocks != null) {
			for (Block b : blocks) {
				if (state.is(b)) {
					return rule.shouldApply(level, state, center, offX, offY, offZ);
				}
			}
		}
		if (states != null && states.contains(state)) {
			return rule.shouldApply(level, state, center, offX, offY, offZ);
		}
		return false;
	}

	@Override
	public BlockState getState() {
		return rule.getState();
	}

	@Override
	public JsonObject encode(JsonObject root) {
		root.addProperty("type", RESOURCE_LOCATION.toString());
		
		root.add("rule", rule.encode(new JsonObject()));
		
		JsonArray jsonBlocks = new JsonArray();
		for (Block block : blocks) {
			jsonBlocks.add(ForgeRegistries.BLOCKS.getResourceKey(block).orElseGet(ENCODE_FALLBACK).location().toString());
		}
		root.add("blocks", jsonBlocks);
		
		JsonArray jsonStates = new JsonArray();
		for (BlockState state : states) {
			jsonStates.add(BlockState.CODEC.encodeStart(JsonOps.COMPRESSED, state).getOrThrow(false, s -> {}));
		}
		root.add("states", jsonStates);
		
		return root;
	}
	
	public static ExplosionRule decode(JsonObject root) {
		ExplosionRule decodedRule = ExplosionRule.parse(root.get("rule").getAsJsonObject());
		
		JsonArray jsonBlocks = root.get("blocks").getAsJsonArray();
		ArrayList<Block> decodedBlocks = null;
		if (jsonBlocks.size() > 0) {
			decodedBlocks = new ArrayList<>(jsonBlocks.size());
			for (int i = 0; i < jsonBlocks.size(); ++i) {
				decodedBlocks.add(ForgeRegistries.BLOCKS.getHolder(new ResourceLocation(jsonBlocks.get(i).getAsString())).orElseGet(DECODE_FALLBACK).get());
			}
		}
		
		JsonArray jsonStates = root.get("states").getAsJsonArray();
		ArrayList<BlockState> states = null;
		if (jsonStates.size() > 0) {
			states = new ArrayList<>(jsonStates.size());
			for (int i = 0; i < jsonStates.size(); ++i) {
				states.add(BlockState.CODEC.parse(JsonOps.COMPRESSED, jsonStates.get(i)).result().orElseGet(() -> Blocks.AIR.defaultBlockState()));
			}
		}
		
		return new FilterBlockExplosionRule(decodedRule, decodedBlocks, states);
	}
	
	public static FilterBlockExplosionRule applyOnlyWhen(Block block, ExplosionRule ruleToWrap) {
		return new FilterBlockExplosionRule(ruleToWrap, List.of(block), null);
	}
	
	public static FilterBlockExplosionRule applyOnlyWhen(BlockState state, ExplosionRule ruleToWrap) {
		return new FilterBlockExplosionRule(ruleToWrap, null, List.of(state));
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		
		private Collection<Block> blocks = new LinkedList<>();
		private Collection<BlockState> states = new LinkedList<>();
		
		private Builder() {
		}
		
		public Builder filterForBlock(Block blockToFilter) {
			blocks.add(blockToFilter);
			return this;
		}
		
		public Builder filterForBlocks(Block... blocksToFilter) {
			return filterForBlocks(List.of(blocksToFilter));
		}
		
		public Builder filterForBlocks(Collection<Block> blocksToFilter) {
			blocks.addAll(blocksToFilter);
			return this;
		}
		
		public Builder filterForState(BlockState stateToFilter) {
			states.add(stateToFilter);
			return this;
		}
		
		public Builder filterForStates(BlockState... statesToFilter) {
			return filterForStates(List.of(statesToFilter));
		}
		
		public Builder filterForStates(Collection<BlockState> statesToFilter) {
			states.addAll(statesToFilter);
			return this;
		}
		
		public FilterBlockExplosionRule build(ExplosionRule ruleToWrap) {
			return new FilterBlockExplosionRule(ruleToWrap, blocks, states);
		}
	}
}
