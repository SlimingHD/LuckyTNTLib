package luckytntlib.util.explosions.rules;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import luckytntlib.LuckyTNTLib;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
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

	private final ExplosionRule rule;
	@Nullable
	private final Collection<Block> blocks;
	@Nullable
	private final Collection<BlockState> states;
	@Nullable
	private final Collection<TagKey<Block>> tags;
	
	protected FilterBlockExplosionRule(ExplosionRule rule, @Nullable Collection<Block> blocks, @Nullable Collection<BlockState> states, @Nullable Collection<TagKey<Block>> tags) {
		this.rule = rule;
		this.blocks = blocks;
		this.states = states;
		this.tags = tags;
	}

	@Override
	@Nullable
	public BlockState getState(Level level, BlockState state, Vec3 center, int offX, int offY, int offZ, RandomSource random) {
		if (tags != null) {
			for (TagKey<Block> tag : tags) {
				if (state.is(tag)) {
					return rule.getState(level, state, center, offX, offY, offZ, random);
				}
			}
		}
		if (blocks != null) {
			for (Block b : blocks) {
				if (state.is(b)) {
					return rule.getState(level, state, center, offX, offY, offZ, random);
				}
			}
		}
		if (states != null && states.contains(state)) {
			return rule.getState(level, state, center, offX, offY, offZ, random);
		}
		return null;
	}

	@Override
	public JsonObject encode(JsonObject root) {
		root.addProperty("type", RESOURCE_LOCATION.toString());
		root.add("rule", rule.encode(new JsonObject()));
		
		JsonArray encodedBlocks = new JsonArray();
		for (Block block : blocks) {
			ResourceLocation location = ForgeRegistries.BLOCKS.getKey(block);
			if (location == null) {
				location = new ResourceLocation("air");
			}
			encodedBlocks.add(location.toString());
		}
		root.add("blocks", encodedBlocks);
		
		JsonArray encodedStates = new JsonArray();
		for (BlockState state : states) {
			encodedStates.add(BlockState.CODEC.encodeStart(JsonOps.COMPRESSED, state).getOrThrow(false, s -> {}));
		}
		root.add("states", encodedStates);
		
		JsonArray encodedTags = new JsonArray();
		for (TagKey<Block> tag : tags) {
			encodedTags.add(tag.location().toString());
		}
		root.add("tags", encodedTags);
		
		return root;
	}
	
	public static ExplosionRule decode(JsonObject root) {
		JsonArray encodedBlocks = root.get("blocks").getAsJsonArray();
		LinkedList<Block> blocks = new LinkedList<>();
		for (int i = 0; i < encodedBlocks.size(); i++) {
			Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(encodedBlocks.get(i).getAsString()));
			if (block == null) {
				block = Blocks.AIR;
			}
			blocks.add(block);
		}
		
		JsonArray encodedStates = root.get("states").getAsJsonArray();
		LinkedList<BlockState> states = new LinkedList<>();
		for (int i = 0; i < encodedStates.size(); i++) {
			BlockState state = BlockState.CODEC.parse(JsonOps.COMPRESSED, encodedStates.get(i)).get().left().orElseGet(() -> null);
			if (state == null) {
				state = Blocks.AIR.defaultBlockState();
			}
			states.add(state);
		}
		
		JsonArray encodedTags = root.get("tags").getAsJsonArray();
		LinkedList<TagKey<Block>> tags = new LinkedList<>();
		for (int i = 0; i < encodedTags.size(); i++) {
			tags.add(TagKey.create(Registries.BLOCK, new ResourceLocation(encodedTags.get(i).getAsString())));
		}
		
		return new FilterBlockExplosionRule(ExplosionRule.parse(root.get("rule").getAsJsonObject()), blocks.size() > 0 ? blocks : null, states.size() > 0 ? states : null, tags.size() > 0 ? tags : null);
	}
	
	public static FilterBlockExplosionRule applyOnlyWhen(Block block, ExplosionRule ruleToWrap) {
		return new FilterBlockExplosionRule(ruleToWrap, List.of(block), null, null);
	}
	
	public static FilterBlockExplosionRule applyOnlyWhen(BlockState state, ExplosionRule ruleToWrap) {
		return new FilterBlockExplosionRule(ruleToWrap, null, List.of(state), null);
	}
	
	public static FilterBlockExplosionRule applyOnlyWhen(TagKey<Block> tag, ExplosionRule ruleToWrap) {
		return new FilterBlockExplosionRule(ruleToWrap, null, null, List.of(tag));
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		
		private Collection<Block> blocks = new LinkedList<>();
		private Collection<BlockState> states = new LinkedList<>();
		private Collection<TagKey<Block>> tags = new LinkedList<>();
		
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
		
		@SuppressWarnings("unchecked")
		public Builder filterForTags(TagKey<Block>... tagsToFilter) {
			return filterForTags(List.of(tagsToFilter));
		}
		
		public Builder filterForTags(Collection<TagKey<Block>> tagsToFilter) {
			tags.addAll(tagsToFilter);
			return this;
		}
		
		public FilterBlockExplosionRule build(ExplosionRule ruleToWrap) {
			return new FilterBlockExplosionRule(ruleToWrap, blocks, states, tags);
		}
	}
}
