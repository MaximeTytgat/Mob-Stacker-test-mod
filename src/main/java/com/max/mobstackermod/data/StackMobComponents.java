package com.max.mobstackermod.data;

import com.max.mobstackermod.MobStackerMod;
import com.max.mobstackermod.config.ServerConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class StackMobComponents {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MobStackerMod.MOD_ID);

    public static final Supplier<AttachmentType<StackedEntityHandler>> STACKED_ENTITIES = ATTACHMENT_TYPES.register(
            "stacked_entities", () -> AttachmentType.serializable(() -> new StackedEntityHandler(ServerConfig.mobStackLimit)).build()
    );

    public static final Supplier<AttachmentType<StackedEntityNameHandler>> STACKED_NAMEABLE = ATTACHMENT_TYPES.register(
            "stacked_nameable", () -> AttachmentType.serializable(() -> new StackedEntityNameHandler()).build()
    );

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}