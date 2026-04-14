package luckytntlib.util;

import java.util.HashMap;
import java.util.Map;

import luckytntlib.config.LuckyTNTLibConfigValues;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class ExplosionProfiler {
	
	private final ServerLevel server;
	private final Map<String, Long> startTimes = new HashMap<>();
	
	public ExplosionProfiler(ServerLevel server) {
		this.server = server;
	}
	
	public void start(String benchmark) {
		startTimes.put(benchmark, System.currentTimeMillis());
	}
	
	public void stopTime(String benchmark, String translationKey, boolean useTime, Object... args) {
		if (LuckyTNTLibConfigValues.BENCHMARK_EXPLOSIONS.get()) {
			for (int i = 0; i < args.length; i++) {
				args[i] = Component.literal(String.valueOf(args[i])).withStyle(ChatFormatting.GOLD);
			}
			
			Object[] varargs;
			if (useTime) {
				double time = (System.currentTimeMillis() - startTimes.remove(benchmark)) / 1000d;
				varargs = new Object[args.length + 1];
				varargs[0] = Component.literal(String.format("%1$.2f", time)).withStyle(ChatFormatting.GOLD);
				System.arraycopy(args, 0, varargs, 1, args.length);
			} else {
				varargs = args;
			}
			
			MutableComponent component = Component.translatable(translationKey, varargs);
			for (ServerPlayer player : server.players()) {
				player.sendSystemMessage(Component.translatable("luckytntlib.benchmarking.title").withStyle(ChatFormatting.AQUA).append(component.withStyle(ChatFormatting.WHITE)), false);
			}
		}
	}
	
	public void stop(String benchmark, String translationKey, Object... args) {
		stopTime(benchmark, translationKey, true, args);
	}
}