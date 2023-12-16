package luckytntlib.client.gui;

import luckytntlib.config.LuckyTNTLibConfigValues;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents.LiteralContents;
import net.minecraftforge.client.gui.widget.ForgeSlider;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * The config screen of Lucky TNT Lib.
 * Extending this is not advised.
 */
public class ConfigScreen extends Screen{

	Button performant_explosion = null;
	
	ForgeSlider explosion_performance_factor_slider = null;
	
	public ConfigScreen() {
		super(Component.literal("Lucky TNT Lib Config"));
	}
	
	@Override
	public void init() {
		addRenderableWidget(new Button.Builder(Component.literal("Done"), button -> onClose()).bounds((width - 100) / 2, height - 30, 100, 20).build());
		addRenderableWidget(performant_explosion = new Button.Builder(LuckyTNTLibConfigValues.PERFORMANT_EXPLOSION.get().booleanValue() ? Component.literal("True") : Component.literal("False"), button -> nextBooleanValue(LuckyTNTLibConfigValues.PERFORMANT_EXPLOSION, performant_explosion)).bounds(20, 40, 200, 20).tooltip(Tooltip.create(Component.literal("Replaces the standard explosion with an identical one that reduces loading time at the expense of some detail. If you host a server you will have to change this value in the server config instead"))).build());
		addRenderableWidget(explosion_performance_factor_slider = new ForgeSlider(20, 60, 200, 20, MutableComponent.create(new LiteralContents("")), MutableComponent.create(new LiteralContents("")), 30d, 60d, LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * 100, true));
		addRenderableWidget(new Button.Builder(Component.literal("Reset"), button -> resetDoubleValue(LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR, 0.3d, explosion_performance_factor_slider)).bounds(width - 220, 60, 200, 20).build());
		explosion_performance_factor_slider.setTooltip(Tooltip.create(Component.literal("Lower values give more details while higher values give more performance. Has significant impact on the shape of the explosion. If you host a server you will have to change this value in the server config instead")));
	}
	
	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		renderBackground(graphics, mouseX, mouseY, partialTicks);
		graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
		graphics.drawCenteredString(font, Component.literal("Performant Explosion"), width / 2, 46, 0xFFFFFF);
		graphics.drawCenteredString(font, Component.literal("Explosion Performance Factor"), width / 2, 66, 0xFFFFFF);
		super.render(graphics, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public void onClose() {
		if(explosion_performance_factor_slider != null) {
			LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.set(explosion_performance_factor_slider.getValue() / 100d);
		}
		super.onClose();
	}
	
	public void resetDoubleValue(ForgeConfigSpec.DoubleValue config, double newValue, ForgeSlider slider) {
		config.set(newValue);
		slider.setValue(newValue * 100);
	}
	
	public void nextBooleanValue(ForgeConfigSpec.BooleanValue config, Button button) {
		boolean value = config.get().booleanValue();
		if(value) {
			value = false;
		} else {
			value = true;
		}
		config.set(value);
		button.setMessage(value ? Component.literal("True") : Component.literal("False"));
	}
}
