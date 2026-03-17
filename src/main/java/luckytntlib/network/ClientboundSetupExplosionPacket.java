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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Packet that transmits all necessary information for the processing of an explosion on the client.
 * Needs to be transmitted prior to an explosion sending any block updates for any chunk.
 * Only used in {@link ExplosionHelper} and {@link ImprovedExplosion}.
 */
public class ClientboundSetupExplosionPacket {

	@Nullable
	public static ExplosionRule currentRule;
	public static BlockPos currentCenter;
	
	private static final Gson GSON = new GsonBuilder().create();
	
	@Nullable
	private final ExplosionRule rule;
	@Nullable
	private final BlockPos center;
	
	public ClientboundSetupExplosionPacket(@Nullable ExplosionRule rule, @Nullable BlockPos center) {
		this.rule = rule;
		this.center = rule == null ? null : center;
	}
	
	public ClientboundSetupExplosionPacket(FriendlyByteBuf buffer) {
		if (buffer.readBoolean()) {
			rule = null;
			center = null;
			return;
		}
		JsonObject root = GsonHelper.parse(buffer.readUtf());
		rule = ExplosionRule.parse(root);
		center = buffer.readBlockPos();
	}
	
	public void encode(FriendlyByteBuf buffer) {
		if (rule == null) {
			buffer.writeBoolean(true);
			return;
		}
		buffer.writeBoolean(false);
		buffer.writeUtf(GSON.toJson(rule.encode(new JsonObject())));
		buffer.writeBlockPos(center);
	}
	
	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientAccess.setupExplosion(rule, center));
		});
		ctx.get().setPacketHandled(true);
	}
}
