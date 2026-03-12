package luckytntlib.util;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class RandomList<T> {

	private final ImmutableList<T> items;
	private final ImmutableList<Float> weights;
	private final float totalWeight;
	
	public RandomList(List<T> items, List<Float> weights) {
		this.items = ImmutableList.copyOf(items);
		this.weights = ImmutableList.copyOf(weights);
		this.totalWeight = weights.stream().reduce(0f, (f1, f2) -> f1 + f2);
	}
	
	public T getRandomItem(RandomSource random) {
		float remainingWeight = random.nextFloat() * totalWeight;
		for(int i = 0; i < items.size(); i++) {
			remainingWeight -= weights.get(i);
			if(remainingWeight < 0f) {
				return items.get(i);
			}
		}
		return items.get(0);
	}
	
	public List<T> getItems() {
		return items;
	}
	
	public List<Float> getWeights() {
		return weights;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> RandomList<T> ofEqualProbability(T... items) {
		return ofEqualProbability(List.of(items));
	}
	
	public static <T> RandomList<T> ofEqualProbability(List<T> items) {
		FloatBuilder<T> builder = RandomList.<T>floatBuilder().allowUnnormalizedProbabilities();
		for(T item : items) {
			builder.addEntry(item, 1f);
		}
		return builder.build();
	}
	
	public static <T> FloatBuilder<T> floatBuilder() {
		return new FloatBuilder<T>();
	}
	
	public static <T> IntegerBuilder<T> intBuilder() {
		return new IntegerBuilder<T>();
	}
	
	static abstract class Builder<T> {
		
		protected final ArrayList<T> items = new ArrayList<>();
		protected final ArrayList<Float> weights = new ArrayList<>();
		
		Builder() {
		}
		
		public abstract RandomList<T> build();
	}
	
	public static class FloatBuilder<T> extends Builder<T> {
		
		private boolean allowUnnormalizedProbabilities = false;

		private FloatBuilder() {
		}
		
		public FloatBuilder<T> addEntry(T item, float weight) {
			items.add(item);
			weights.add(weight);
			return this;
		}
		
		public FloatBuilder<T> allowUnnormalizedProbabilities() {
			allowUnnormalizedProbabilities = true;
			return this;
		}
		
		@Override
		public RandomList<T> build() {
			if(!allowUnnormalizedProbabilities && !Mth.equal(weights.stream().reduce(0f, (f1, f2) -> f1 + f2), 1f)) {
				throw new IllegalArgumentException("The probabilities do not sum up to 1");
			}
			return new RandomList<T>(items, weights);
		}
	}
	
	public static class IntegerBuilder<T> extends Builder<T> {
		
		private final ArrayList<Integer> intWeights = new ArrayList<>(); 
		
		private IntegerBuilder() {
		}
		
		public IntegerBuilder<T> addEntry(T item, int weight) {
			items.add(item);
			intWeights.add(weight);
			return this;
		}

		@Override
		public RandomList<T> build() {
			List<Float> floatWeights = intWeights.stream().map(i -> (float)i).toList();
			return new RandomList<T>(items, floatWeights);
		}
	}
}