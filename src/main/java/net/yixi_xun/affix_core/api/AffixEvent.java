package net.yixi_xun.affix_core.api;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import net.yixi_xun.affix_core.affix.operation.OperationManager;

/**
 * 词缀事件基类
 */
public abstract class AffixEvent extends Event {

    /**
     * 当操作被注册时触发的事件
     * 允许其他模组通过此事件注册自己的操作
     */
    public static class OperationRegisterEvent extends AffixEvent {
        private final String operationType;
        private OperationManager.OperationFactory factory;
        private boolean registered = false;

        public OperationRegisterEvent(String operationType) {
            this.operationType = operationType;
            this.factory = null;
        }

        public OperationRegisterEvent(String operationType, OperationManager.OperationFactory factory) {
            this.operationType = operationType;
            this.factory = factory;
        }

        public String getOperationType() {
            return operationType;
        }

        public OperationManager.OperationFactory getFactory() {
            return factory;
        }

        public void setFactory(OperationManager.OperationFactory factory) {
            this.factory = factory;
            this.registered = true;
        }

        public boolean isRegistered() {
            return registered;
        }
    }

    /**
     * 专门用于让其他模组注册操作的事件
     */
    public static class RegisterOperationsEvent extends AffixEvent {
    }

    /**
     * 当词缀被执行时触发的事件
     */
    @Cancelable
    public static class AffixExecuteEvent extends AffixEvent {
        private final AffixContext context;

        public AffixExecuteEvent(AffixContext context) {
            this.context = context;
        }

        public AffixContext getContext() {
            return context;
        }
    }

    /**
     * 当词缀被移除时触发的事件
     */
    @Cancelable
    public static class AffixRemoveEvent extends AffixEvent {
        private final AffixContext context;

        public AffixRemoveEvent(AffixContext context) {
            this.context = context;
        }

        public AffixContext getContext() {
            return context;
        }
    }
}