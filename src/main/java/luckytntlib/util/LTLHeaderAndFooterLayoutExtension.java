package luckytntlib.util;

import luckytntlib.mixin.HeaderAndFooterLayoutMixin;
import net.minecraft.client.gui.layouts.FrameLayout;

/**
 * Interface used only in {@link HeaderAndFooterLayoutMixin} to acquire control over layouts for config screens
 */
public interface LTLHeaderAndFooterLayoutExtension {

	public FrameLayout getContentsFrameLayoutLTL();
}
