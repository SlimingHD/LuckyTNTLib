package luckytntlib.client.gui;

import luckytntlib.config.LuckyTNTLibConfigValues;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.GridLayout.RowHelper;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.LinearLayout.Orientation;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.widget.ForgeSlider;
import net.minecraftforge.common.ForgeConfigSpec;

public class QualityConfigScreen extends Screen {

	Button updateBlockLight = null;
	ForgeSlider blockUpdateThreshold = null;
	
	HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 20, 40);
	
	public QualityConfigScreen() {
		super(Component.translatable("luckytntlib.config.quality_title"));
	}
	
	@Override
	public void init() {
		LinearLayout linear = layout.addToHeader(new LinearLayout(0, 0, Orientation.VERTICAL));
		linear.addChild(new StringWidget(title, font), LayoutSettings.defaults().alignHorizontallyCenter());
		GridLayout grid = new GridLayout();
		grid.defaultCellSetting().paddingHorizontal(4).paddingBottom(4).alignHorizontallyCenter();
		RowHelper rows = grid.createRowHelper(3);
		
		rows.addChild(updateBlockLight = new Button.Builder(LuckyTNTLibConfigValues.UPDATE_BLOCK_LIGHT.get().booleanValue() ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF, button -> nextBooleanValue(LuckyTNTLibConfigValues.UPDATE_BLOCK_LIGHT, button)).width(100).build());
		updateBlockLight.setTooltip(Tooltip.create(Component.translatable("luckytntlib.config.update_block_light_tooltip")));
		rows.addChild(new CenteredStringWidget(Component.translatable("luckytntlib.config.update_block_light"), font));
		rows.addChild(new Button.Builder(Component.translatable("luckytntlib.config.reset"), button -> resetBooleanValue(LuckyTNTLibConfigValues.UPDATE_BLOCK_LIGHT, updateBlockLight)).width(100).build());
		rows.addChild(blockUpdateThreshold = new ForgeSlider(0, 0, 100, 20, Component.empty(), Component.empty(), 30, 300, LuckyTNTLibConfigValues.BLOCK_UPDATE_THRESHOLD.get(), true) {
			@Override
			protected void applyValue() {
				LuckyTNTLibConfigValues.BLOCK_UPDATE_THRESHOLD.set((int)getValue());
			}
		});
		blockUpdateThreshold.setTooltip(Tooltip.create(Component.translatable("luckytntlib.config.block_update_threshold_tooltip")));
		rows.addChild(new CenteredStringWidget(Component.translatable("luckytntlib.config.block_update_threshold"), font));
		rows.addChild(new Button.Builder(Component.translatable("luckytntlib.config.reset"), button -> resetIntValue(LuckyTNTLibConfigValues.BLOCK_UPDATE_THRESHOLD, blockUpdateThreshold)).width(100).build());
	
		layout.addToContents(grid);
		GridLayout footerGrid = new GridLayout();
		footerGrid.defaultCellSetting().paddingHorizontal(4).paddingBottom(4).alignHorizontallyCenter();
		RowHelper footerRows = footerGrid.createRowHelper(3);
		
		Button backButton = new Button.Builder(CommonComponents.GUI_BACK, button -> {
			onClose();
		}).width(100).build();
		Button nextButton = new Button.Builder(CommonComponents.GUI_CONTINUE, button -> {
			minecraft.pushGuiLayer(new DebugConfigScreen());
		}).width(100).build();
		footerRows.addChild(backButton);
		footerRows.addChild(new Button.Builder(CommonComponents.GUI_DONE, button -> minecraft.setScreen(null)).width(100).build());
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
		renderDirtBackground(graphics);
		super.render(graphics, mouseX, mouseY, partialTicks);
	}
	
	private void resetIntValue(ForgeConfigSpec.IntValue config, ForgeSlider slider) {
		config.set(config.getDefault());
		slider.setValue(config.getDefault());
	}
	
	private void resetBooleanValue(ForgeConfigSpec.BooleanValue config, Button button) {
		config.set(config.getDefault());
		button.setMessage(config.getDefault() ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
	}
	
	private void nextBooleanValue(ForgeConfigSpec.BooleanValue config, Button button) {
		boolean value = !config.get().booleanValue();
		config.set(value);
		button.setMessage(value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
	}
}
