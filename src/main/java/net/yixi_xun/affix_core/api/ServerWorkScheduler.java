package net.yixi_xun.affix_core.api;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

import static net.yixi_xun.affix_core.AffixCoreMod.LOGGER;

@Mod.EventBusSubscriber
public class ServerWorkScheduler {
    // 使用ArrayDeque作为队列实现
    private static final Queue<ScheduledTask> workQueue = new ArrayDeque<>();

    // 任务内部类，封装任务和剩余tick数
    private static class ScheduledTask {
        final Runnable action;
        int remainingTicks;

        ScheduledTask(Runnable action, int ticks) {
            this.action = action;
            this.remainingTicks = ticks;
        }
    }

    /**
     * 安排服务器端工作
     * @param tick 延迟的tick数
     * @param action 要执行的任务
     */
    public static void queueServerWork(int tick, Runnable action) {
        if (tick < 0 || action == null) {
            return;
        }

        workQueue.add(new ScheduledTask(action, tick));
    }

    @SubscribeEvent
    public void onServerTickEnd(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            processQueue();
        }
    }

    private void processQueue() {
        // 使用迭代器安全地遍历队列
        Iterator<ScheduledTask> iterator = workQueue.iterator();
        while (iterator.hasNext()) {
            ScheduledTask task = iterator.next();

            // 减少剩余tick数
            task.remainingTicks--;

            // 如果任务已到期，则执行并移除
            if (task.remainingTicks <= 0) {
                iterator.remove();
                try {
                    task.action.run();
                } catch (Exception e) {
                    // 处理任务执行中的异常
                    LOGGER.error("Error executing scheduled task: ", e);
                }
            }
        }
    }
}
