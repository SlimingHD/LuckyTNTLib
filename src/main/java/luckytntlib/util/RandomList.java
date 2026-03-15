package luckytntlib.util;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * A helper class to group items together and weigh them for weighted random retrieval of an object
 * @param <T>  the type of item you want to store
 */
public class RandomList<T> {

	private final ImmutableList<T> items;
	private final ImmutableList<Float> weights;
	private final float totalWeight;
	
	public RandomList(List<T> items, List<Float> weights) {
		if (items.size() != weights.size()) {
			throw new IllegalArgumentException("There need to be as many items as there are weights and the other way around!");
		}
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
	
	/**
	 * Create a RandomList where all items have the same weight
	 * @param <T>  the type of item you want to store
	 * @param items  the items you want to have in the list
	 * @return a new RandomList where all items can be selected with equal probability
	 */
	@SuppressWarnings("unchecked")
	public static <T> RandomList<T> ofEqualProbability(T... items) {
		return ofEqualProbability(List.of(items));
	}
	
	/**
	 * Create a RandomList where all items have the same weight
	 * @param <T>  the type of item you want to store
	 * @param items  the items you want to have in the list
	 * @return a new RandomList where all items can be selected with equal probability
	 */
	public static <T> RandomList<T> ofEqualProbability(List<T> items) {
		FloatBuilder<T> builder = RandomList.<T>floatBuilder().allowUnnormalizedProbabilities();
		for(T item : items) {
			builder.addEntry(item, 1f);
		}
		return builder.build();
	}
	
	/**
	 * Creates a new builder for a RandomList where you can weigh all items using {@code float}s
	 * @param <T>  the type of item you want to store
	 * @return a new {@link FloatBuilder} for a RandomList
	 */
	public static <T> FloatBuilder<T> floatBuilder() {
		return new FloatBuilder<T>();
	}
	
	/**
	 * Creates a new builder for a RandomList where you can weigh all items using {@code int}s
	 * @param <T>  the type of item you want to store
	 * @return a new {@link IntegerBuilder} for a RandomList
	 */
	public static <T> IntegerBuilder<T> intBuilder() {
		return new IntegerBuilder<T>();
	}
	
	/**
	 * A builder for {@link RandomList} where the items are weighted using {@code float}s
	 * @param <T>  the type of item you want to store
	 */
	public static class FloatBuilder<T> {
		
		private final ArrayList<T> items = new ArrayList<>();
		private final ArrayList<Float> weights = new ArrayList<>();
		private boolean allowUnnormalizedProbabilities = false;

		private FloatBuilder() {
		}
		
		public FloatBuilder<T> addEntry(T item, float weight) {
			items.add(item);
			weights.add(weight);
			return this;
		}
		
		/**
		 * Activating this will not require all probabilities to sum up to 1.
		 * If this is not activated and the probabilities do not sum up to 1 an exception will be thrown upon trying to build a {@link RandomList} from this builder.
		 */
		public FloatBuilder<T> allowUnnormalizedProbabilities() {
			allowUnnormalizedProbabilities = true;
			return this;
		}
		
		public RandomList<T> build() {
			if (!allowUnnormalizedProbabilities && !Mth.equal(weights.stream().reduce(0f, (f1, f2) -> f1 + f2), 1f)) {
				throw new IllegalArgumentException("The probabilities do not sum up to 1");
			}
			return new RandomList<T>(items, weights);
		}
	}
	
	/**
	 * A builder for {@link RandomList} where the items are weighted using {@code int}s
	 * @param <T>  the type of item you want to store
	 */
	public static class IntegerBuilder<T> {
		
		private final ArrayList<T> items = new ArrayList<>();
		private final ArrayList<Float> weights = new ArrayList<>();
		
		private IntegerBuilder() {
		}
		
		public IntegerBuilder<T> addEntry(T item, int weight) {
			items.add(item);
			weights.add((float)weight);
			return this;
		}

		public RandomList<T> build() {
			return new RandomList<T>(items, weights);
		}
	}
}