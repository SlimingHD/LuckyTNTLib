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
import net.minecraftforge.common.ForgeConfigSpec;

public class DebugConfigScreen extends Screen {

	Button benchmarkExplosions = null;
	
	HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 20, 40);
	
	public DebugConfigScreen() {
		super(Component.translatable("luckytntlib.config.debug_title"));
	}

	@Override
	public void init() {
		LinearLayout linear = layout.addToHeader(new LinearLayout(0, 0, Orientation.VERTICAL));
		linear.addChild(new StringWidget(title, font), LayoutSettings.defaults().alignHorizontallyCenter());
		GridLayout grid = new GridLayout();
		grid.defaultCellSetting().paddingHorizontal(4).paddingBottom(4).alignHorizontallyCenter();
		RowHelper rows = grid.createRowHelper(3);
		
		rows.addChild(benchmarkExplosions = new Button.Builder(LuckyTNTLibConfigValues.BENCHMARK_EXPLOSIONS.get().booleanValue() ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF, button -> nextBooleanValue(LuckyTNTLibConfigValues.BENCHMARK_EXPLOSIONS, button)).width(100).build());
		benchmarkExplosions.setTooltip(Tooltip.create(Component.translatable("luckytntlib.config.benchmark_explosions_tooltip")));
		rows.addChild(new CenteredStringWidget(Component.translatable("luckytntlib.config.benchmark_explosions"), font));
		rows.addChild(new Button.Builder(Component.translatable("luckytntlib.config.reset"), button -> resetBooleanValue(LuckyTNTLibConfigValues.BENCHMARK_EXPLOSIONS, benchmarkExplosions)).width(100).build());
	
		layout.addToContents(grid);
		GridLayout footerGrid = new GridLayout();
		footerGrid.defaultCellSetting().paddingHorizontal(4).paddingBottom(4).alignHorizontallyCenter();
		RowHelper footerRows = footerGrid.createRowHelper(3);
		
		Button backButton = new Button.Builder(CommonComponents.GUI_BACK, button -> {
			onClose();
		}).width(100).build();
		@SuppressWarnings("removal")
		Button nextButton = new Button.Builder(CommonComponents.GUI_CONTINUE, button -> {
			minecraft.pushGuiLayer(new DeprecatedConfigScreen());
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
