package luckytntlib.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import luckytntlib.client.ClientAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class ClientboundUpdateChunkSectionPacket {

	private final List<Short> changed;
	private final boolean updateLight, empty;
	private final int sectionX, sectionY, sectionZ;
	
	public ClientboundUpdateChunkSectionPacket(SectionPos pos, List<Short> changed, boolean empty, boolean updateLight) {
		this.sectionX = pos.getX();
		this.sectionY = pos.getY();
		this.sectionZ = pos.getZ();
		this.empty = empty;
		this.updateLight = updateLight;
		this.changed = changed;
	}
	
	public ClientboundUpdateChunkSectionPacket(FriendlyByteBuf buffer) {
		sectionX = buffer.readInt();
		sectionY = buffer.readInt();
		sectionZ = buffer.readInt();
		empty = buffer.readBoolean();
		updateLight = buffer.readBoolean();
		
		if(!empty) {
			int length = buffer.readShort();
			List<Short> list = new ArrayList<>(length);
			for(int i = 0; i < length; i++) {
				list.add(buffer.readShort());
			}
			changed = list;
		} else {
			changed = new ArrayList<>(0);
		}
	}
	
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeInt(sectionX);
		buffer.writeInt(sectionY);
		buffer.writeInt(sectionZ);
		buffer.writeBoolean(empty);
		buffer.writeBoolean(updateLight);
		
		if(!empty) {
			buffer.writeShort(changed.size());
			for(Short s : changed) {
				buffer.writeShort(s);
			}
		}
	}
	
	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientAccess.updateChunkSection(SectionPos.of(sectionX, sectionY, sectionZ), changed, empty, updateLight));
		});
		ctx.get().setPacketHandled(true);
	}
}
