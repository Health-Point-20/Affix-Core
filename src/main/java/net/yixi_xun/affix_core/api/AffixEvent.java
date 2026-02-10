package net.yixi_xun.affix_core.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import net.yixi_xun.affix_core.affix.AffixContext;
import net.yixi_xun.affix_core.affix.operation.OperationManager;

/**
 * 词缀事件基类
 */
public abstract class AffixEvent extends Event {

    /**
     * 当操作被注册时触发的事件
     */
    @Cancelable
    public static class OperationRegisterEvent extends AffixEvent {
        private final String operationType;
        private OperationManager.OperationFactory factory;
        private boolean registered = false;

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
     * 当注册操作的事件
     */
    public static class RegisterOperationEvent extends AffixEvent {

        public RegisterOperationEvent() {
        }

       public void registerOperation(String operationType, OperationManager.OperationFactory factory) {
           OperationManager.registerFactory(operationType, factory);
       }
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

    /**
     * 触发自定义事件
     */
    @Cancelable
    public static class CustomMessageEvent extends AffixEvent {
        private final LivingEntity entity;
        private final String message;

        public CustomMessageEvent(LivingEntity entity, String message) {
            this.entity = entity;
            this.message = message;
        }

        public LivingEntity getEntity() {
            return entity;
        }
        public String getMessage() {
            return message;
        }
    }
}