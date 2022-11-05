package luckytntlib.network;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class ClientboundExplosionPacket {

	public final String className;
	public final String toExecute;
	public final BlockPos pos;
	@Nullable public final float radius;
	@Nullable public final float strength;
	@Nullable public final int ownerId;
	
	public ClientboundExplosionPacket(String className, String toExecute, BlockPos pos, @Nullable float radius, @Nullable float strength, @Nullable int ownerId) {
		this.className = className;
		this.toExecute = toExecute;
		this.pos = pos;
		this.radius = radius;
		this.strength = strength;
		this.ownerId = ownerId;
	}
	
	public ClientboundExplosionPacket(FriendlyByteBuf buffer) {
		className = buffer.readUtf();
		toExecute = buffer.readUtf();
		pos = buffer.readBlockPos();
		radius = buffer.readFloat();
		strength = buffer.readFloat();
		ownerId = buffer.readInt();
	}
	
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeUtf(className);
		buffer.writeUtf(toExecute);
		buffer.writeBlockPos(pos);
		buffer.writeFloat(radius);
		buffer.writeFloat(strength);
		buffer.writeInt(ownerId);
	}
	
	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			try {
				Class<?> clazz = Class.forName(className);
				try {
					Method runnable = clazz.getMethod(toExecute, new Class[] {BlockPos.class, float.class, float.class, int.class});
					runnable.setAccessible(true);
					DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
						try {
							runnable.invoke(null, pos, radius, strength, ownerId);
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
