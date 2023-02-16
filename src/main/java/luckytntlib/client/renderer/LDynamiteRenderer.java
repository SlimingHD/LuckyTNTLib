package luckytntlib.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import luckytntlib.entity.LExplosiveProjectile;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.inventory.InventoryMenu;

public class LDynamiteRenderer<T extends LExplosiveProjectile & ItemSupplier> extends EntityRenderer<T>{
	
	private final ItemRenderer itemRenderer;
	
	public LDynamiteRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.itemRenderer = context.getItemRenderer();
	}
	
	@Override
	public void render(T entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
		if (entity.tickCount >= 2 || !(this.entityRenderDispatcher.camera.getEntity().distanceToSqr(entity) < 12.25D)) {
			poseStack.pushPose();
			poseStack.scale(entity.getEffect().getSize(entity), entity.getEffect().getSize(entity), entity.getEffect().getSize(entity));
			poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
			poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
			this.itemRenderer.renderStatic(entity.getItem(), ItemTransforms.TransformType.GROUND, light, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.getId());
			poseStack.popPose();
			super.render(entity, yaw, partialTicks, poseStack, buffer, light);
		}
	}
	
	public ResourceLocation getTextureLocation(T entity) {
		return InventoryMenu.BLOCK_ATLAS;
	}
}
