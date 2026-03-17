package luckytntlib.client.gui;

import luckytntlib.config.LuckyTNTLibConfigValues;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.GridLayout.RowHelper;
import net.minecraft.client.gui.layouts.LinearLayout.Orientation;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.widget.ForgeSlider;
import net.minecraftforge.common.ForgeConfigSpec;

@Deprecated(since = "47.2.32.1", forRemoval = true)
public class DeprecatedConfigScreen extends Screen {

	Button performantExplosion = null;
	ForgeSlider explosionPerformanceFactor = null;
	
	HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 20, 40);
	
	public DeprecatedConfigScreen() {
		super(Component.translatable("luckytntlib.config.deprecated_title"));
	}

	@Override
	public void init() {
		LinearLayout linear = layout.addToHeader(new LinearLayout(0, 0, Orientation.VERTICAL));
		linear.addChild(new StringWidget(Component.translatable("luckytntlib.config.deprecated_title"), font), LayoutSettings.defaults().alignHorizontallyCenter());
		GridLayout grid = new GridLayout();
		
		grid.defaultCellSetting().paddingHorizontal(4).paddingBottom(4).alignHorizontallyCenter();
		RowHelper rows = grid.createRowHelper(3);
	
		rows.addChild(performantExplosion = new Button.Builder(LuckyTNTLibConfigValues.PERFORMANT_EXPLOSION.get().booleanValue() ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF, button -> nextBooleanValue(LuckyTNTLibConfigValues.PERFORMANT_EXPLOSION, button)).width(100).build());
		performantExplosion.setTooltip(Tooltip.create(Component.translatable("luckytntlib.config.performant_explosion_tooltip")));
		rows.addChild(new CenteredStringWidget(Component.translatable("luckytntlib.config.performant_explosion"), font));
		rows.addChild(new Button.Builder(Component.translatable("luckytntlib.config.reset"), button -> resetBooleanValue(LuckyTNTLibConfigValues.PERFORMANT_EXPLOSION, performantExplosion)).width(100).build());
		rows.addChild(explosionPerformanceFactor = new ForgeSlider(0, 0, 100, 20, Component.empty(), Component.empty(), 30d, 60d, LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * 100, true));
		explosionPerformanceFactor.setTooltip(Tooltip.create(Component.translatable("luckytntlib.config.explosion_performance_factor_tooltip")));
		rows.addChild(new CenteredStringWidget(Component.translatable("luckytntlib.config.explosion_performance_factor"), font));
		rows.addChild(new Button.Builder(Component.translatable("luckytntlib.config.reset"), button -> resetPerformanceFactor(LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR, explosionPerformanceFactor)).width(100).build());
		
		layout.addToContents(grid);
		GridLayout footerGrid = new GridLayout();
		footerGrid.defaultCellSetting().paddingHorizontal(4).paddingBottom(4).alignHorizontallyCenter();
		RowHelper footerRows = footerGrid.createRowHelper(3);
		Button backButton = new Button.Builder(CommonComponents.GUI_BACK, button -> {
			onClose();
			minecraft.setScreen(new MultithreadingConfigScreen());
		}).width(100).build();
		Button nextButton = new Button.Builder(CommonComponents.GUI_CONTINUE, button -> {}).width(100).build();
		nextButton.active = false;
		footerRows.addChild(backButton);
		footerRows.addChild(new Button.Builder(CommonComponents.GUI_DONE, button -> onClose()).width(100).build());
		footerRows.addChild(nextButton);
		layout.addToFooter(footerGrid);
		layout.visitWidgets(this::addRenderableWidget);
		repositionElements();
	}
	
    @Override
    public void repositionElements() {
        layout.arrangeElements();
    }

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		renderBackground(graphics);
		super.render(graphics, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public void onClose() {
		if(explosionPerformanceFactor != null) {
			LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.set(explosionPerformanceFactor.getValue() / 100d);
		}
		super.onClose();
	}
	
	private void resetPerformanceFactor(ForgeConfigSpec.DoubleValue config, ForgeSlider slider) {
		config.set(config.getDefault());
		slider.setValue(config.getDefault() * 100);
	}
	
	private void resetBooleanValue(ForgeConfigSpec.BooleanValue config, Button button) {
		config.set(config.getDefault());
		button.setMessage(config.getDefault() ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
	}
	
	private void nextBooleanValue(ForgeConfigSpec.BooleanValue config, Button button) {
		boolean value = config.get().booleanValue();
		if(value) {
			value = false;
		} else {
			value = true;
		}
		config.set(value);
		button.setMessage(value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
	}
}
