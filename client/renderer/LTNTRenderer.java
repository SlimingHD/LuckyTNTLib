package luckytntlib.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;

import luckytntlib.entity.PrimedLTNT;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LTNTRenderer extends EntityRenderer<PrimedLTNT>{
	private BlockRenderDispatcher blockRenderer;
	
	public LTNTRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.blockRenderer = context.getBlockRenderDispatcher();
	}
	
	@SuppressWarnings("deprecation")
	public void render(PrimedLTNT entity, float yaw, float partialTicks, PoseStack posestack, MultiBufferSource buffer, int light) {
    	posestack.pushPose();
        posestack.translate(0, 0, 0);
        int i = entity.getFuse();
        if ((float)i - partialTicks + 1.0F < 10.0F) {
           float f = 1.0F - ((float)i - partialTicks + 1.0F) / 10.0F;
           f = Mth.clamp(f, 0.0F, 1.0F);
           f *= f;
           f *= f;
           float f1 = 1.0F + f * 0.3F;
           posestack.scale(f1, f1, f1);
        }
        
        posestack.scale(entity.getEffect().getSize(), entity.getEffect().getSize(), entity.getEffect().getSize());
        posestack.mulPose(Vector3f.YP.rotationDegrees(-90f));
        posestack.translate(-0.5d, 0, 0.5d);
        posestack.mulPose(Vector3f.YP.rotationDegrees(90f));
        TntMinecartRenderer.renderWhiteSolidBlock(blockRenderer, entity.getEffect().getBlock().defaultBlockState(), posestack, buffer, light, i / 5 % 2 == 0);
        posestack.popPose();
        super.render(entity, yaw, partialTicks, posestack, buffer, light);
    }
	
	public ResourceLocation getTextureLocation(PrimedLTNT entity) {
		return InventoryMenu.BLOCK_ATLAS;
	}
}
