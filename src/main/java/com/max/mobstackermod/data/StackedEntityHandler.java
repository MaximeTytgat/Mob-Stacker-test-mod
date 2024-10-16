package com.max.mobstackermod.data;

import com.max.mobstackermod.MobStackerMod;
import com.max.mobstackermod.config.ServerConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;

import static com.max.mobstackermod.data.StackMobComponents.STACKED_ENTITIES;

public class StackedEntityHandler implements INBTSerializable<CompoundTag> {
    protected NonNullList<CompoundTag> stackedEntityTags;
    private static boolean skipDeathEvents = false;
    private float lastHurtValue = 0f;
    private float lastHpValue;

    private static LivingEntity provider;

    public StackedEntityHandler(int size) {
        // TODO: We Should use the size parameter to set the size of the NonNullList
        stackedEntityTags = NonNullList.create();
    }

    public StackedEntityHandler(LivingEntity newProvider) {
        this(ServerConfig.mobStackLimit);
        provider = newProvider;
        skipDeathEvents = false;
    }

    public static StackedEntityHandler getOrInitStackedEntityHandler(LivingEntity livingEntity) {
        if (livingEntity.hasData(STACKED_ENTITIES)) {
            return livingEntity.getData(STACKED_ENTITIES);
        } else {
            return new StackedEntityHandler(livingEntity);
        }
    }

    public LivingEntity sliceOne(Level level, Vec3 pos) {
        return sliceOne(level, pos , false);
    }

    public LivingEntity sliceOne(Level level, Vec3 pos, boolean invulnerableTime) {
        if (stackedEntityTags.isEmpty()) return provider;

        CompoundTag tag = stackedEntityTags.removeFirst();
        Optional<Entity> optionalEntity = EntityType.create(tag, level);
        if (optionalEntity.isPresent() && optionalEntity.get() instanceof LivingEntity livingEntity) {
            // Create the entity
            livingEntity.setPos(pos.x, pos.y, pos.z);
            livingEntity.tickCount = 0;
            if (invulnerableTime) livingEntity.invulnerableTime = 5;

            if (!stackedEntityTags.isEmpty()) {
                StackedEntityNameHandler nameHandler = StackedEntityNameHandler.getOnInitEntityNameHandler(livingEntity);
                nameHandler.setStackSize(stackedEntityTags.size() + 1);

                StackedEntityHandler newEntityContainer = getOrInitStackedEntityHandler(livingEntity);
                newEntityContainer.addAll(stackedEntityTags);
                newEntityContainer.setLastHurtValue(lastHurtValue);
                newEntityContainer.setLastHpValue(lastHpValue);

                stackedEntityTags.clear();

                livingEntity.setData(STACKED_ENTITIES, newEntityContainer);
                livingEntity.setData(StackMobComponents.STACKED_NAMEABLE, nameHandler);
            } else {
                livingEntity.removeData(STACKED_ENTITIES);
                livingEntity.removeData(StackMobComponents.STACKED_NAMEABLE);
            }


            level.addFreshEntity(livingEntity);

            return livingEntity;
        }

        return null;

    }

    public void dropLootAndRemoveManyEntity(ServerLevel level, DamageSource damageSource, int count, Vec3 pos) {
        for (int i = 0; i < count; i++) {
            if (stackedEntityTags.isEmpty()) break;

            CompoundTag tag = stackedEntityTags.removeFirst();
            Optional<Entity> optionalEntity = EntityType.create(tag, level);
            if (optionalEntity.isPresent() && optionalEntity.get() instanceof LivingEntity livingEntity) {
                StackedEntityHandler livingEntityContainer = getOrInitStackedEntityHandler(livingEntity);
                livingEntityContainer.setSkipDeathEvents(true);
                livingEntity.setData(STACKED_ENTITIES, livingEntityContainer);

                livingEntity.setPos(pos.x, pos.y, pos.z);
                livingEntity.dropAllDeathLoot(level, damageSource);
            }
        }
    }

    public void applyConsumerToAll(Consumer<LivingEntity> consumer, ServerLevel level) {
        NonNullList<CompoundTag> newStackedEntityTags = NonNullList.create();

        stackedEntityTags.forEach(stackTag -> {
           LivingEntity livingEntity = deserializeEntity(stackTag, level);
           if (livingEntity != null) {
               consumer.accept(livingEntity);
               CompoundTag newTag = new CompoundTag();
               livingEntity.save(newTag);
               newStackedEntityTags.add(newTag);
               livingEntity.remove(Entity.RemovalReason.DISCARDED);
           }
        });

        stackedEntityTags.clear();
        stackedEntityTags.addAll(newStackedEntityTags);
    }

    public int getSize() {
        return stackedEntityTags.size();
    }

    public boolean isEmpty() {
        return stackedEntityTags.isEmpty();
    }


    public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        ListTag nbtTagList = new ListTag();
        nbtTagList.addAll(stackedEntityTags);
        CompoundTag nbt = new CompoundTag();
        nbt.put("entities", nbtTagList);
        return nbt;
    }

    public void deserializeNBT(HolderLookup.@NotNull Provider provider, CompoundTag nbt) {
        ListTag tagList = nbt.getList("entities", 10);
        for(int i = 0; i < tagList.size(); ++i) {
            CompoundTag nbtTag = tagList.getCompound(i);
            stackedEntityTags.add(nbtTag);
        }
        this.onLoad();
    }

    public static LivingEntity deserializeEntity(CompoundTag nbt, Level level) {
        Optional<Entity> entity = EntityType.create(nbt, level);
        if (entity.isPresent() && entity.get() instanceof LivingEntity livingEntity) {
            return livingEntity;
        }
        MobStackerMod.LOGGER.error("Failed to deserialize entity");
        return null;
    }

    protected void onLoad() {
    }

    public NonNullList<CompoundTag> getStackedEntityTags() {
        return stackedEntityTags;
    }

    public void addAll(NonNullList<CompoundTag> tags) {
        stackedEntityTags.addAll(tags);
    }


    public void setSkipDeathEvents(boolean b) {
        skipDeathEvents = b;
    }

    public boolean shouldSkipDeathEvents() {
        return skipDeathEvents;
    }


    public float getLastHurtValue() {
        return lastHurtValue;
    }

    public void setLastHurtValue(float lastHurtValue) {
        this.lastHurtValue = lastHurtValue;
    }

    public float getLastHpValue() {
        return lastHpValue;
    }

    public void setLastHpValue(float lastHpValue) {
        this.lastHpValue = lastHpValue;
    }
}
