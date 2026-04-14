package luckytntlib.network;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import luckytntlib.client.ClientAccess;
import luckytntlib.util.explosions.ExplosionHelper;
import luckytntlib.util.explosions.ImprovedExplosion;
import luckytntlib.util.explosions.rules.ExplosionRule;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Packet that transmits all necessary information for the processing of an explosion on the client.
 * Needs to be transmitted prior to an explosion that doesn't use {@link Level#setBlock(BlockPos, BlockState, int)} for updating the world. <br>
 * Only used in {@link ExplosionHelper} and {@link ImprovedExplosion}.
 */
public class ClientboundSetupExplosionPacket {
	
	private static final Gson GSON = new GsonBuilder().create();
	
	@Nullable
	private final ExplosionRule rule;
	@Nullable
	private final Vec3 center;
	private final long seed;
	
	public ClientboundSetupExplosionPacket(@Nullable ExplosionRule rule, @Nullable Vec3 center, long seed) {
		this.rule = rule;
		this.center = center;
		this.seed = seed;
	}
	
	public ClientboundSetupExplosionPacket(FriendlyByteBuf buffer) {
		if (buffer.readBoolean()) {
			rule = null;
			center = null;
			seed = 0l;
			return;
		}
		JsonObject root = GsonHelper.parse(buffer.readUtf());
		rule = ExplosionRule.parse(root);
		center = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
		seed = buffer.readLong();
	}
	
	public void encode(FriendlyByteBuf buffer) {
		if (rule == null) {
			buffer.writeBoolean(true);
			return;
		}
		buffer.writeBoolean(false);
		buffer.writeUtf(GSON.toJson(rule.encode(new JsonObject())));
		buffer.writeDouble(center.x);
		buffer.writeDouble(center.y);
		buffer.writeDouble(center.z);
		buffer.writeLong(seed);
	}
	
	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientAccess.setupExplosion(rule, center, seed)));
		ctx.get().setPacketHandled(true);
	}
}
