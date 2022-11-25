package luckytntlib.network;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class ClientboundExplosionPacket {

	public final String className;
	public final String toExecute;
	public final BlockPos pos;
	
	public ClientboundExplosionPacket(String className, String toExecute, BlockPos pos) {
		this.className = className;
		this.toExecute = toExecute;
		this.pos = pos;
	}
	
	public ClientboundExplosionPacket(FriendlyByteBuf buffer) {
		className = buffer.readUtf();
		toExecute = buffer.readUtf();
		pos = buffer.readBlockPos();
	}
	
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeUtf(className);
		buffer.writeUtf(toExecute);
		buffer.writeBlockPos(pos);
	}
	
	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			try {
				Class<?> clazz = Class.forName(className);
				try {
					Method runnable = clazz.getMethod(toExecute, new Class[] {BlockPos.class});
					runnable.setAccessible(true);
					DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
						try {
							runnable.invoke(null, pos);
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							e.printStackTrace();
						}
					});
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		});
		ctx.get().setPacketHandled(true);
	}
}
