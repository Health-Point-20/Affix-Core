package net.yixi_xun.affix_core.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.yixi_xun.affix_core.affix.Affix;
import net.yixi_xun.affix_core.affix.AffixManager;

import java.util.function.Supplier;

public class UpdateAffixC2SPacket {
    private final int slotIndex;
    private final CompoundTag affixData;
    private final boolean isAddition; // true for add, false for remove

    public UpdateAffixC2SPacket(int slotIndex, CompoundTag affixData, boolean isAddition) {
        this.slotIndex = slotIndex;
        this.affixData = affixData;
        this.isAddition = isAddition;
    }

    public UpdateAffixC2SPacket(FriendlyByteBuf buf) {
        this.slotIndex = buf.readInt();
        this.affixData = buf.readNbt();
        this.isAddition = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(slotIndex);
        buf.writeNbt(affixData);
        buf.writeBoolean(isAddition);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                // 首先检查槽位索引是否合法
                if (slotIndex < 0 || slotIndex >= player.getInventory().getContainerSize()) {
                    // 日志记录非法操作
                    System.err.println("Invalid slot index: " + slotIndex + " for player: " + player.getName().getString());
                    return;
                }
                
                // 获取玩家指定槽位的物品
                var itemStack = player.getInventory().getItem(slotIndex);
                
                // 额外的物品校验：检查物品是否仍然存在且未被替换
                if (itemStack.isEmpty()) {
                    // 物品已不存在，可能是玩家移动了物品
                    System.err.println("Item no longer exists in slot " + slotIndex + " for player: " + player.getName().getString());
                    return;
                }
                
                // 如果affixData中包含物品标识符，进行物品匹配验证
                if (affixData.contains("ExpectedItemId")) {
                    String expectedItemId = affixData.getString("ExpectedItemId");
                    String actualItemId = ForgeRegistries.ITEMS.getKey(itemStack.getItem()).toString();
                    
                    if (!expectedItemId.equals(actualItemId)) {
                        // 物品类型不匹配，说明玩家可能移动了物品
                        System.err.println("Item type mismatch in slot " + slotIndex + ". Expected: " + expectedItemId + ", Actual: " + actualItemId);
                        return;
                    }
                }
                
                // 如果affixData中包含物品Count，进行数量匹配验证
                if (affixData.contains("ExpectedItemCount")) {
                    int expectedCount = affixData.getInt("ExpectedItemCount");
                    int actualCount = itemStack.getCount();
                    
                    if (expectedCount != actualCount) {
                        // 物品数量不匹配，说明玩家可能移动了物品
                        System.err.println("Item count mismatch in slot " + slotIndex + ". Expected: " + expectedCount + ", Actual: " + actualCount);
                        return;
                    }
                }
                
                try {
                    if (isAddition) {
                        // 添加词缀
                        Affix affix = Affix.fromNBT(affixData, AffixManager.getAffixes(itemStack).size());
                        AffixManager.addAffix(itemStack, affix);
                    } else {
                        // 移除词缀 (假设affixData包含索引信息用于定位要删除的词缀)
                        if (affixData.contains("Index")) {
                            int affixIndex = affixData.getInt("Index");
                            AffixManager.removeAffix(itemStack, affixIndex, player.level(), player);
                        } else if (affixData.getBoolean("ClearAll")) {
                            // 如果标记为清除全部，则清空所有词缀
                            AffixManager.clearAffixes(itemStack);
                        }
                    }
                    
                    // 更新物品到客户端 (这会自动同步)
                    player.getInventory().setItem(slotIndex, itemStack);
                } catch (Exception e) {
                    // 捕获解析NBT过程中的异常，防止崩溃
                    System.err.println("Error processing affix data: " + e.getMessage());
                }
            }
        });
    }
}