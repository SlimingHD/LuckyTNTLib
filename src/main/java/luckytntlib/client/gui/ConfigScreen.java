package luckytntlib.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;

import luckytntlib.config.LuckyTNTLibConfigValues;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
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
		addRenderableWidget(new Button((width - 100) / 2, height - 30, 100, 20, Component.literal("Done"), button -> onClose()));
		addRenderableWidget(explosion_performance_factor_slider = new ForgeSlider(20, 60, 200, 20, MutableComponent.create(new LiteralContents("")), MutableComponent.create(new LiteralContents("")), 30d, 60d, LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * 100, true));
		addRenderableWidget(new Button(width - 220, 60, 200, 20, Component.literal("Reset"), button -> resetDoubleValue(LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR, 0.3d, explosion_performance_factor_slider)));
		addRenderableWidget(performant_explosion = new Button(20, 40, 200, 20, LuckyTNTLibConfigValues.PERFORMANT_EXPLOSION.get().booleanValue() ? Component.literal("True") : Component.literal("False"), button -> nextBooleanValue(LuckyTNTLibConfigValues.PERFORMANT_EXPLOSION, performant_explosion)));
	}
	
	@Override
	public void render(PoseStack stack, int mouseX, int mouseY, float partialTicks) {
		renderBackground(stack);
		drawCenteredString(stack, font, title, width / 2, 8, 0xFFFFFF);
		drawCenteredString(stack, font, Component.literal("Performant Explosion"), width / 2, 46, 0xFFFFFF);
		drawCenteredString(stack, font, Component.literal("Explosion Performance Factor"), width / 2, 66, 0xFFFFFF);
		super.render(stack, mouseX, mouseY, partialTicks);
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
