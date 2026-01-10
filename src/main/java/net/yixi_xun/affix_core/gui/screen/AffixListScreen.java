package net.yixi_xun.affix_core.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.yixi_xun.affix_core.affix.Affix;
import net.yixi_xun.affix_core.affix.AffixManager;
import net.yixi_xun.affix_core.gui.BaseScreen;
import net.yixi_xun.affix_core.gui.BaseWidget;
import net.yixi_xun.affix_core.gui.RenderUtils;
import net.yixi_xun.affix_core.gui.screen.ResetAffixScreen;
import net.yixi_xun.affix_core.gui.widget.ScrollablePanel;
import net.yixi_xun.affix_core.gui.widget.SimpleButton;
import net.yixi_xun.affix_core.network.NetworkManager;
import net.yixi_xun.affix_core.network.UpdateAffixC2SPacket;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AffixListScreen extends BaseScreen {
    private final ItemStack itemStack;
    private ScrollablePanel scrollPanel;

    public AffixListScreen(ItemStack itemStack) {
        super(Component.translatable("gui.affix_core.affix_list.title"));
        this.itemStack = itemStack;
    }

    @Override
    public void init() {
        super.init();
        
        // 创建滚动面板
        scrollPanel = new ScrollablePanel(width / 2 - 150, 60, 300, height - 120);
        addWidget(scrollPanel);

        refreshList();
        
        // 添加词缀按钮
        addWidget(new SimpleButton(width / 2 - 100, height - 45, 80, 25, Component.translatable("gui.affix_core.button.add"), btn -> {
            // 打开一个空的编辑器
            // 注意：这里需要构造一个临时的 Affix 或者传递 null 让编辑器处理新建
            // 简单起见，这里仅展示逻辑
            if (minecraft != null) {
                // 获取当前玩家的槽位索引
                int slotIndex = getPlayerSlotIndex(itemStack);
                if (slotIndex != -1) {
                    minecraft.setScreen(new AffixEditorScreen(null, slotIndex, nbt -> {
                        // 发送网络包到服务器
                        NetworkManager.CHANNEL.sendToServer(new UpdateAffixC2SPacket(slotIndex, nbt, true));
                        // 关闭屏幕并刷新
                        if (minecraft != null) {
                            minecraft.setScreen(null);
                        }
                    }));
                }
            }
        }));
        
        // 重置词缀按钮
        addWidget(new SimpleButton(width / 2 + 20, height - 45, 80, 25, Component.translatable("gui.affix_core.button.reset"), btn -> {
            int slotIndex = getPlayerSlotIndex(itemStack);
            if (slotIndex != -1 && minecraft != null) {
                minecraft.setScreen(new ResetAffixScreen(itemStack, slotIndex));
            }
        }));
        
        // 返回按钮
        addWidget(new SimpleButton(width / 2 - 40, height - 15, 80, 15, Component.translatable("gui.affix_core.button.back"), btn -> {
            if (minecraft != null) {
                minecraft.setScreen(null);
            }
        }));
    }
    
    private void refreshList() {
        scrollPanel.clear();
        List<Affix> affixes = AffixManager.getAffixes(itemStack);
        
        if (affixes.isEmpty()) {
            // 如果没有词缀，显示提示信息
            BaseWidget noAffixWidget = new BaseWidget(0, 0, 0, 40, Component.literal("")) {
                @Override
                public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                    RenderUtils.drawBorderRect(guiGraphics, x, y, width, height, 
                        0xFF555555, 0xDD000000);
                    
                    // 绘制文字
                    guiGraphics.drawCenteredString(font, Component.translatable("gui.affix_core.label.no_affixes").getString(), x + width / 2, y + (height - 8) / 2, 0x888888);
                }
            };
            scrollPanel.addChild(noAffixWidget);
        } else {
            for (int i = 0; i < affixes.size(); i++) {
                final int index = i;
                Affix affix = affixes.get(i);
                
                // 创建词缀卡片组件
                BaseWidget card = new BaseWidget(0, 0, 0, 40, Component.literal("")) {
                    @Override
                    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                        boolean hovered = isHovered(mouseX, mouseY);
                        RenderUtils.drawBorderRect(guiGraphics, x, y, width, height, 
                            hovered ? 0xFFFFFFFF : 0xFF555555, 
                            hovered ? 0xDD333333 : 0xDD000000);
                        
                        // 绘制文字
                        guiGraphics.drawString(font, Component.translatable("gui.affix_core.label.trigger").getString() + affix.trigger(), x + 5, y + 5, 0xFFFFFF);
                        guiGraphics.drawString(font, Component.translatable("gui.affix_core.label.operation").getString() + affix.operation().getType(), x + 5, y + 20, 0xAAAAAA);
                    }
                    
                    @Override
                    public boolean mouseClicked(double mouseX, double mouseY, int button) {
                        if (isHovered(mouseX, mouseY)) {
                            // 打开编辑器
                            if (minecraft != null) {
                                // 获取当前玩家的槽位索引
                                int slotIndex = getPlayerSlotIndex(itemStack);
                                if (slotIndex != -1) {
                                    minecraft.setScreen(new AffixEditorScreen(affix, slotIndex, nbt -> {
                                        // 创建一个新的NBT标签，其中包含要删除的词缀索引
                                        CompoundTag removeTag = new CompoundTag();
                                        removeTag.putInt("Index", index);
                                        // 发送网络包到服务器，先删除再添加
                                        NetworkManager.CHANNEL.sendToServer(new UpdateAffixC2SPacket(slotIndex, removeTag, false));
                                        // 添加新词缀
                                        NetworkManager.CHANNEL.sendToServer(new UpdateAffixC2SPacket(slotIndex, nbt, true));
                                        // 关闭屏幕并刷新
                                        if (minecraft != null) {
                                            minecraft.setScreen(null);
                                        }
                                    }));
                                }
                            }
                            return true;
                        }
                        return false;
                    }
                };
                scrollPanel.addChild(card);
            }
        }
    }
    
    private int getPlayerSlotIndex(ItemStack targetItemStack) {
        if (minecraft == null || minecraft.player == null) {
            return -1;
        }
        
        var inventory = minecraft.player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            var item = inventory.getItem(i);
            if (item == targetItemStack || item.equals(targetItemStack, false)) { // 使用false表示不比较NBT，因为我们可能正在修改NBT
                return i;
            }
        }
        return -1;
    }
    
    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(font, this.title.getString(), width / 2, 20, 0xFFFFFF);
        
        // 绘制物品预览
        guiGraphics.renderItem(itemStack, width / 2 - 170, 25);
        guiGraphics.drawString(font, itemStack.getDisplayName().getString(), width / 2 - 150, 30, 0xFFFFFF);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}