package luckytntlib.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;

import luckytntlib.util.IExplosiveEntity;
import luckytntlib.util.tnteffects.PrimedTNTEffect;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.TntBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 
 * The LTNTRenderer renders an {@link IExplosiveEntity} as a block.
 * The block can be a type of TNT, in which case it will also be animated, or any other block,
 * in which case it is rendered like a normal block.
 * The block is also scaled using the size of its {@link PrimedTNTEffect}.
 */
@OnlyIn(Dist.CLIENT)
public class LTNTRenderer extends EntityRenderer<Entity>{
	private BlockRenderDispatcher blockRenderer;
	
	public LTNTRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.blockRenderer = context.getBlockRenderDispatcher();
	}
	
	public void render(Entity entity, float yaw, float partialTicks, PoseStack posestack, MultiBufferSource buffer, int light) {
    	if(entity instanceof IExplosiveEntity ent) {
			posestack.pushPose();
	        posestack.translate(0, 0, 0);
	        int i = ent.getTNTFuse();
	        if ((float)i - partialTicks + 1.0F < 10.0F && ent.getEffect().getBlockState((IExplosiveEntity)entity).getBlock() instanceof TntBlock) {
	           float f = 1.0F - ((float)i - partialTicks + 1.0F) / 10.0F;
	           f = Mth.clamp(f, 0.0F, 1.0F);
	           f *= f;
	           f *= f;
	           float f1 = 1.0F + f * 0.3F;
	           posestack.scale(f1, f1, f1);
	        }
	        posestack.scale(ent.getEffect().getSize((IExplosiveEntity)entity), ent.getEffect().getSize((IExplosiveEntity)entity), ent.getEffect().getSize((IExplosiveEntity)entity));
	        posestack.translate(-0.5d, 0, -0.5d);
	        TntMinecartRenderer.renderWhiteSolidBlock(blockRenderer, ent.getEffect().getBlockState((IExplosiveEntity)entity), posestack, buffer, light, ent.getEffect().getBlockState((IExplosiveEntity)entity).getBlock() instanceof TntBlock ? i / 5 % 2 == 0 : false);
	        posestack.popPose();
    	}
        super.render(entity, yaw, partialTicks, posestack, buffer, light);
    }
	
	public ResourceLocation getTextureLocation(Entity entity) {
		return InventoryMenu.BLOCK_ATLAS;
	}
}
