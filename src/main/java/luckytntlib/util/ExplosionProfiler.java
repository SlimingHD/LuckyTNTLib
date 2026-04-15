package luckytntlib.util;

import java.util.BitSet;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicInteger;

import luckytntlib.config.LuckyTNTLibConfigValues;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class ExplosionProfiler {
	
	private final ServerLevel serverLevel;
	
	private final EnumMap<Benchmark, Profile> profiles = new EnumMap<>(Benchmark.class);
	private final EnumMap<Benchmark, Long> stopTimes = new EnumMap<>(Benchmark.class);
	
	private boolean disabled;
	private String titleTranslationKey = "luckytntlib.benchmarking.explosion_title";
	
	public ExplosionProfiler(Level level) {
		if (!level.isClientSide()) {
			serverLevel = (ServerLevel)level;
		} else {
			serverLevel = null;
			disabled = true;
		}
	}
	
	public void disable() {
		disabled = true;
	}
	
	public void setTitle(String titleTranslationKey) {
		this.titleTranslationKey = titleTranslationKey;
	}
	
	public void startBenchmark(Benchmark benchmark, String translationKey) {
		if (!LuckyTNTLibConfigValues.BENCHMARK_EXPLOSIONS.get() || disabled) {
			return;
		}
		profiles.put(benchmark, new Profile(System.currentTimeMillis(), translationKey, new EnumMap<Counter, Integer>(Counter.class)));
	}
	
	public void stopBenchmarkTime(Benchmark benchmark) {
		profiles.computeIfPresent(benchmark, (benchmarkKey, profile) -> {
			stopTimes.put(benchmarkKey, System.currentTimeMillis());
			return profile;
		});
	}
	
	public void addCounterTo(Benchmark benchmark, Counter counter, int initialValue) {
		profiles.computeIfPresent(benchmark, (benchmarkKey, profile) -> {
			profile.counters.put(counter, initialValue);
			return profile;
		});
	}
	
	public void countUp(Benchmark benchmark, Counter counter, int amount) {
		profiles.computeIfPresent(benchmark, (benchmarkKey, profile) -> {
			profile.counters.computeIfPresent(counter, (counterKey, value) -> value + amount);
			return profile;
		});
	}
	
	public void countUp(Benchmark benchmark, Counter counter, BitSet amount) {
		profiles.computeIfPresent(benchmark, (benchmarkKey, profile) -> {
			profile.counters.computeIfPresent(counter, (counterKey, value) -> value + amount.cardinality());
			return profile;
		});
	}
	
	public int getCount(Benchmark benchmark, Counter counter) {
		Profile profile = profiles.get(benchmark);
		if (profile == null) {
			return 0;
		}
		return profiles.get(benchmark).counters.getOrDefault(counter, 0);
	}
	
	public void printResults() {
		if (!LuckyTNTLibConfigValues.BENCHMARK_EXPLOSIONS.get() || disabled) {
			return;
		}
		MutableComponent message = Component.translatable(titleTranslationKey).append(":").withStyle(ChatFormatting.AQUA);
		profiles.forEach((benchmark, profile) -> {
			double time = (stopTimes.getOrDefault(benchmark, System.currentTimeMillis()) - profile.startTime) / 1000d;
			Object[] args = new Object[1 + profile.counters.size()];
			args[0] = Component.literal(String.format("%1$.2f", time)).withStyle(ChatFormatting.GOLD);
			AtomicInteger i = new AtomicInteger(1);
			profile.counters.forEach((counter, value) -> {
				int index = i.get();
				args[index] = Component.literal(String.format("%" + index + "$,d", value)).withStyle(ChatFormatting.GOLD);
				i.set(index + 1);
			});
			message.append("\n  ").append(Component.translatable(profile.translationKey, args).withStyle(ChatFormatting.WHITE));
		});
		for (ServerPlayer player : serverLevel.players()) {
			player.sendSystemMessage(message, false);
		}
	}
	
	private static record Profile(long startTime, String translationKey, EnumMap<Counter, Integer> counters) {}
	
	public static enum Benchmark {
		BENCHMARK_0,
		BENCHMARK_1,
		BENCHMARK_2,
		BENCHMARK_3,
		BENCHMARK_4,
		BENCHMARK_5,
		BENCHMARK_6,
		BENCHMARK_7,
		BENCHMARK_8,
		BENCHMARK_9
	}
	
	public static enum Counter {
		COUNTER_0,
		COUNTER_1,
		COUNTER_2,
		COUNTER_3,
		COUNTER_4
	}
}