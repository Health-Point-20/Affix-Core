package net.yixi_xun.affix_core.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.yixi_xun.affix_core.affix.Affix;
import net.yixi_xun.affix_core.affix.AffixManager;
import net.yixi_xun.affix_core.gui.BaseScreen;
import net.yixi_xun.affix_core.gui.BaseWidget;
import net.yixi_xun.affix_core.gui.RenderUtils;
import net.yixi_xun.affix_core.gui.widget.SimpleButton;
import net.yixi_xun.affix_core.network.NetworkManager;
import net.yixi_xun.affix_core.network.UpdateAffixC2SPacket;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ResetAffixScreen extends BaseScreen {
    private final ItemStack itemStack;
    private final int slotIndex;
    private boolean confirmMode = false;
    private boolean showPreview = false; // 新增：是否显示预览

    public ResetAffixScreen(ItemStack itemStack, int slotIndex) {
        super(Component.translatable("gui.affix_core.reset_affix.title"));
        this.itemStack = itemStack;
        this.slotIndex = slotIndex;
    }

    @Override
    public void init() {
        super.init();
        clearWidgets();
        layout(); // 调用布局方法
    }
    
    @Override
    protected void layout() {
        int centerX = width / 2;
        int centerY = height / 2;

        // 标题
        addWidget(new BaseWidget((int)(centerX - width * 0.15), (int) (height * 0.02), (int) (width * 0.3), 20, Component.translatable("gui.affix_core.reset_affix.title")) {
            @Override
            public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                guiGraphics.drawCenteredString(font, message.getString(), centerX, (int) (height * 0.025), 0xFFFF00);
            }
        });

        // 物品显示区域 (占屏幕的20%宽，15%高)
        int itemDisplayWidth = (int) (width * 0.3);
        int itemDisplayHeight = (int) (height * 0.15);
        int itemDisplayX = centerX - itemDisplayWidth / 2;
        int itemDisplayY = (int) (height * 0.1);

        addWidget(new BaseWidget(itemDisplayX, itemDisplayY, itemDisplayWidth, itemDisplayHeight, Component.empty()) {
            @Override
            public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                // 绘制标题
                guiGraphics.drawString(font, Component.translatable("gui.affix_core.label.item_details").getString(), itemDisplayX + 10, itemDisplayY + 5, 0xFFFFFF);
                
                // 绘制物品图标和名称
                guiGraphics.renderItem(itemStack, itemDisplayX + 20, itemDisplayY + 25);
                String itemName = itemStack.getDisplayName().getString();
                // 截断过长的物品名
                if (font.width(itemName) > itemDisplayWidth - 45) {
                    itemName = font.plainSubstrByWidth(itemName, itemDisplayWidth - 50) + "...";
                }
                guiGraphics.drawString(font, itemName, itemDisplayX + 45, itemDisplayY + 30, 0xFFFFFF);
                
                // 绘制边框
                RenderUtils.drawBorderRect(guiGraphics, itemDisplayX, itemDisplayY, itemDisplayWidth, itemDisplayHeight, 0xFF555555, 0x88000000);
            }
        });

        // 词缀数量显示
        List<Affix> affixes = AffixManager.getAffixes(itemStack);
        addWidget(new BaseWidget(itemDisplayX, itemDisplayY + itemDisplayHeight + 5, itemDisplayWidth, 20, Component.literal(Component.translatable("gui.affix_core.label.affix_count").getString() + affixes.size())) {
            @Override
            public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                int color = !affixes.isEmpty() ? 0xFF6666 : 0x66FF66; // 有词缀时红色，无词缀时绿色
                guiGraphics.drawString(font, Component.translatable("gui.affix_core.label.affix_count").getString() + affixes.size(), itemDisplayX + 10, itemDisplayY + itemDisplayHeight + 10, color);
            }
        });

        // 重置按钮 - 根据确认模式显示不同文本
        int buttonWidth = 100;
        int buttonHeight = 25;
        if (!confirmMode) {
            addWidget(new SimpleButton(centerX - buttonWidth / 2, (int) (height * 0.5), buttonWidth, buttonHeight, Component.translatable("gui.affix_core.button.reset"), btn -> {
                // 切换到确认模式
                confirmMode = true;
                showPreview = true; // 同时显示预览
                layout(); // 重新布局
            }));
        } else {
            // 确认重置模式 - 显示确认和取消按钮
            addWidget(new SimpleButton(centerX - buttonWidth - 10, (int) (height * 0.5), buttonWidth, buttonHeight, Component.translatable("gui.affix_core.button.confirm"), btn -> resetAffixes())).setActive(true);

            addWidget(new SimpleButton(centerX + 10, (int) (height * 0.5), buttonWidth, buttonHeight, Component.translatable("gui.affix_core.button.cancel"), btn -> {
                // 返回非确认模式
                confirmMode = false;
                showPreview = false; // 不再显示预览
                layout(); // 重新布局
            }));
        }

        // 返回按钮
        addWidget(new SimpleButton(centerX - buttonWidth / 2, (int) (height * 0.5) + 40, buttonWidth, buttonHeight, Component.translatable("gui.affix_core.button.back"), btn -> {
            if (minecraft != null) {
                minecraft.setScreen(null);
            }
        }));

        // 词缀列表预览（最多显示前5个，仅在确认模式下显示）
        if (showPreview && !affixes.isEmpty()) {
            int previewWidth = (int) (width * 0.6);
            int previewX = centerX - previewWidth / 2;
            int previewY = itemDisplayY + itemDisplayHeight + 40;
            
            addWidget(new BaseWidget(previewX, previewY, previewWidth, 15, Component.translatable("gui.affix_core.label.affixes_to_remove")) {
                @Override
                public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                    guiGraphics.drawString(font, message.getString(), previewX + 10, previewY + 5, 0xFFAAAA);
                }
            });

            int startY = previewY + 20;
            for (int i = 0; i < Math.min(affixes.size(), 5); i++) {
                Affix affix = affixes.get(i);
                addWidget(new BaseWidget(previewX, startY + i * 25, previewWidth, 22, Component.literal("")) {
                    @Override
                    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                        boolean hovered = isHovered(mouseX, mouseY);
                        RenderUtils.drawBorderRect(guiGraphics, x, y, width, height, 
                            hovered ? 0xFFFFFFFF : 0xFF555555, 
                            hovered ? 0xDD333333 : 0xDD111111);
                        
                        // 绘制词缀信息
                        String triggerText = Component.translatable("gui.affix_core.label.trigger").getString() + affix.trigger();
                        String opText = " | " + Component.translatable("gui.affix_core.label.operation").getString() + affix.operation().getType();
                        
                        // 截断过长的文本
                        int maxTriggerWidth = (width / 2) - 10;
                        if (font.width(triggerText) > maxTriggerWidth) {
                            triggerText = font.plainSubstrByWidth(triggerText, maxTriggerWidth) + "...";
                        }
                        
                        guiGraphics.drawString(font, triggerText, x + 5, y + 5, 0xFFFFFF);
                        guiGraphics.drawString(font, opText, x + width / 2, y + 5, 0xAAAAAA);
                    }
                });
            }
            
            if (affixes.size() > 5) {
                addWidget(new BaseWidget(previewX, startY + 5 * 25, previewWidth, 20, Component.translatable("gui.affix_core.label.more_affixes", affixes.size() - 5)) {
                    @Override
                    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                        guiGraphics.drawString(font, message.getString(), x + 5, y + 5, 0x888888);
                    }
                });
            }
        } else if (!affixes.isEmpty()) {
            // 如果没有词缀，显示提示信息
            addWidget(new BaseWidget(centerX - 150, itemDisplayY + itemDisplayHeight + 40, 300, 20, Component.translatable("gui.affix_core.label.no_affixes")) {
                @Override
                public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                    guiGraphics.drawString(font, message.getString(), centerX - 150 + 10, itemDisplayY + itemDisplayHeight + 45, 0x66FF66);
                }
            });
        }
    }

    private void resetAffixes() {
        // 发送清空所有词缀的网络包
        if (minecraft != null && minecraft.player != null) {
            // 创建一个标记为清除全部的NBT标签
            var clearTag = new net.minecraft.nbt.CompoundTag();
            clearTag.putBoolean("ClearAll", true);
            // 添加一个请求ID用于追踪此操作
            clearTag.putInt("RequestId", (int) System.currentTimeMillis());
            
            // 添加物品校验信息到NBT
            var itemInSlot = minecraft.player.getInventory().getItem(slotIndex);
            if (!itemInSlot.isEmpty()) {
                clearTag.putString("ExpectedItemId", ForgeRegistries.ITEMS.getKey(itemInSlot.getItem()).toString());
                clearTag.putInt("ExpectedItemCount", itemInSlot.getCount());
            }
            
            NetworkManager.CHANNEL.sendToServer(new UpdateAffixC2SPacket(slotIndex, clearTag, false));
            
            // 关闭界面
            minecraft.setScreen(null);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // 在确认模式下显示警告信息
        if (confirmMode) {
            // 绘制半透明遮罩
            guiGraphics.fill(0, 0, width, height, 0x88000000);
            
            // 获取警告框区域
            WarningBoxInfo warningBox = getWarningBox();
            
            // 绘制警告框
            RenderUtils.drawBorderRect(guiGraphics, warningBox.x, warningBox.y, warningBox.width, warningBox.height, 
                                     0xFFFF0000, 0xAA000000);
            
            // 绘制警告文本
            List<Affix> affixes = AffixManager.getAffixes(itemStack);
            String warningText = Component.translatable("gui.affix_core.warning.reset_with_details", 
                itemStack.getDisplayName().getString(), 
                affixes.size()).getString();
            
            guiGraphics.drawCenteredString(font, warningText, width / 2, warningBox.y + 10, 0xFF5555);
            guiGraphics.drawCenteredString(font, Component.translatable("gui.affix_core.warning.are_you_sure").getString(), 
                                         width / 2, warningBox.y + 25, 0xFFFFFF);
            guiGraphics.drawCenteredString(font, Component.translatable("gui.affix_core.button.continue").getString(), 
                                         width / 2, warningBox.y + 40, 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 如果在确认模式下点击警告框外面，取消确认模式
        if (confirmMode) {
            WarningBoxInfo warningBox = getWarningBox();
            
            if (!(mouseX >= warningBox.x && mouseX <= warningBox.x + warningBox.width && 
                  mouseY >= warningBox.y && mouseY <= warningBox.y + warningBox.height)) {
                confirmMode = false;
                showPreview = false; // 退出预览模式
                layout(); // 重新布局
                return true;
            }
        }
        
        // 检查按钮点击
        for (BaseWidget widget : widgets) {
            if (widget.isVisible() && widget.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    // 新增：获取警告框区域信息，确保渲染和点击检测使用相同的数据
    private WarningBoxInfo getWarningBox() {
        List<Affix> affixes = AffixManager.getAffixes(itemStack);
        String warningText = Component.translatable("gui.affix_core.warning.reset_with_details", 
            itemStack.getDisplayName().getString(), 
            affixes.size()).getString();
        String sureText = Component.translatable("gui.affix_core.warning.are_you_sure").getString();
        String continueText = Component.translatable("gui.affix_core.button.continue").getString();
        
        int textWidth1 = Math.max(font.width(warningText), Math.max(font.width(sureText), font.width(continueText)));
        int maxWidth = Math.min(textWidth1 + 20, (int) (width * 0.6)); // 限制最大宽度为屏幕的60%
        
        int boxX = (width - maxWidth) / 2;
        int boxY = height / 2 - 60; // 调整位置
        int boxHeight = 75; // 增加高度以容纳三行文本
        
        return new WarningBoxInfo(boxX, boxY, maxWidth, boxHeight);
    }

    // 新增：警告框信息内部类
        private record WarningBoxInfo(int x, int y, int width, int height) {
    }
}