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

public class MultithreadingConfigScreen extends Screen {

	Button multithreadExplosions = null;
	ForgeSlider maxExplosionThreads = null;
	Button maxExplosionThreadsResetButton = null;
	
	HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 20, 40);
	
	public MultithreadingConfigScreen() {
		super(Component.translatable("luckytntlib.config.multithreading_title"));
	}

	@Override
	public void init() {
		LinearLayout linear = layout.addToHeader(new LinearLayout(0, 0, Orientation.VERTICAL));
		linear.addChild(new StringWidget(Component.translatable("luckytntlib.config.multithreading_title"), font), LayoutSettings.defaults().alignHorizontallyCenter());
		GridLayout grid = new GridLayout();
		
		grid.defaultCellSetting().paddingHorizontal(4).paddingBottom(4).alignHorizontallyCenter();
		RowHelper rows = grid.createRowHelper(3);
		rows.addChild(multithreadExplosions = new Button.Builder(LuckyTNTLibConfigValues.MULTITHREADED_EXPLOSIONS.get().booleanValue() ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF, button -> {
			nextBooleanValue(LuckyTNTLibConfigValues.MULTITHREADED_EXPLOSIONS, button);
			maxExplosionThreads.active = LuckyTNTLibConfigValues.MULTITHREADED_EXPLOSIONS.get();
			maxExplosionThreadsResetButton.active = LuckyTNTLibConfigValues.MULTITHREADED_EXPLOSIONS.get();
		}).width(100).build());
		multithreadExplosions.setTooltip(Tooltip.create(Component.translatable("luckytntlib.config.multithreaded_explosions_tooltip")));
		rows.addChild(new CenteredStringWidget(Component.translatable("luckytntlib.config.multithreaded_explosions"), font));
		rows.addChild(new Button.Builder(Component.translatable("luckytntlib.config.reset"), button -> resetBooleanValue(LuckyTNTLibConfigValues.MULTITHREADED_EXPLOSIONS, multithreadExplosions)).width(100).build());
		rows.addChild(maxExplosionThreads = new ForgeSlider(0, 0, 100, 20, Component.empty(), Component.empty(), 1, 20, LuckyTNTLibConfigValues.MAX_EXPLOSION_THREADS.get(), true));
		maxExplosionThreads.setTooltip(Tooltip.create(Component.translatable("luckytntlib.config.max_explosion_threads_tooltip")));
		rows.addChild(new CenteredStringWidget(Component.translatable("luckytntlib.config.max_explosion_threads"), font));
		rows.addChild(maxExplosionThreadsResetButton = new Button.Builder(Component.translatable("luckytntlib.config.reset"), button -> resetIntValue(LuckyTNTLibConfigValues.MAX_EXPLOSION_THREADS, maxExplosionThreads)).width(100).build());
		
		layout.addToContents(grid);
		GridLayout footerGrid = new GridLayout();
		footerGrid.defaultCellSetting().paddingHorizontal(4).paddingBottom(4).alignHorizontallyCenter();
		RowHelper footerRows = footerGrid.createRowHelper(3);
		Button backButton = new Button.Builder(CommonComponents.GUI_BACK, button -> {}).width(100).build();
		backButton.active = false;
		@SuppressWarnings("removal")
		Button nextButton = new Button.Builder(CommonComponents.GUI_CONTINUE, button -> {
			onClose();
			minecraft.setScreen(new DeprecatedConfigScreen());
		}).width(100).build();
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
		if(maxExplosionThreads != null) {
			LuckyTNTLibConfigValues.MAX_EXPLOSION_THREADS.set((int)maxExplosionThreads.getValue());
		}
		super.onClose();
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
