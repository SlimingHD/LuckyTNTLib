package luckytntlib.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import luckytntlib.util.LTLHeaderAndFooterLayoutExtension;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;

@Mixin(HeaderAndFooterLayout.class)
public abstract class HeaderAndFooterLayoutMixin implements LTLHeaderAndFooterLayoutExtension {

	@Shadow
	private FrameLayout contentsFrame;
	
	@Override
	@Unique
	public FrameLayout getContentsFrameLayoutLTL() {
		return contentsFrame;
	}
}

