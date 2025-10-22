package com.onepunchcrafts.common.skills.saitama;

import com.onepunchcrafts.common.skills.*;
import com.onepunchcrafts.common.skills.sync.FieldRegistry;
import com.onepunchcrafts.common.skills.sync.SyncStrategy;
import com.onepunchcrafts.common.skills.sync.Syncable;
import com.onepunchcrafts.common.skills.sync.SyncableSkillPack;
import com.onepunchcrafts.constant.NbtBooleanValues;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.PlayerSyncPacket;
import com.onepunchcrafts.util.HelpUtility;
import it.unimi.dsi.fastutil.shorts.ShortConsumer;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class SaitamaPack extends SyncableSkillPack {

    @Setter
    @Getter
    @Syncable(key = "seriousfart", strategy = SyncStrategy.TOGGLE)
    private boolean seriousFartActive;
    @Setter
    @Getter
    @Syncable(key = "extremespeedactive", strategy = SyncStrategy.TOGGLE)
    private boolean extremeSpeedActive;
    @Setter
    @Getter
    @Syncable(key = "extremejump", strategy = SyncStrategy.TOGGLE)
    private boolean extremeJump;
    @Setter
    @Getter
    @Syncable(key = "superspeedsaitama", strategy = SyncStrategy.VALIDATED)
    private short speed;
    @Setter
    @Getter
    @Syncable(key = "breakblocksquickly", strategy = SyncStrategy.TOGGLE)
    private boolean breakBlocksQuickly;
    @Setter
    @Getter
    @Syncable(key = "weight", strategy = SyncStrategy.VALIDATED)
    private short weight;
    @Setter
    @Getter
    @Syncable(key = "saitamaknockbackresistance", strategy = SyncStrategy.VALIDATED)
    private short knockbackResistance;
    @Setter
    @Getter
    @Syncable(key = "saitamaattackknockback", strategy = SyncStrategy.VALIDATED)
    private short attackKnockback;
    @Setter
    @Getter
    @Syncable(key = "saitamaswimspeed", strategy = SyncStrategy.VALIDATED)
    private short swimSpeed;
    private final SaitamaPack self = this;

    @Override
    protected @NotNull List<List<Skill>> initializeSkills() {
        List<List<Skill>> groupList = new ArrayList<>();
        List<Skill> list = new ArrayList<>();
        list.add(0, new WeakPunch());
        list.add(1, new NormalPunch());
        list.add(2, new SeriousPunch());
        list.add(3, new Skill() {
            @Override
            public SkillExecutionResult execute(Player p) {
                if (p instanceof ServerPlayer player) {
                    setSeriousFartActive(!isSeriousFartActive());
                    NetworkRegister.sendToPlayer(player, new PlayerSyncPacket(self));
                }
                return null;
            }

            @Override
            public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
                guiGraphics.drawString(font, Component.translatable("skill.saitama.serious_fart"), width / 2 - defaultReduce, height / 2 + defaultAdd, isSeriousFartActive() ? Color.GREEN.getRGB() : Color.RED.getRGB(), false);
            }
        });
        list.add(new WeakeningPunch());
        list.add(5, new QuickBackStab());
        list.add(6, new Skill() {
            @Override
            public SkillExecutionResult execute(Player player) {
                return null;
            }

            @Override
            public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
                guiGraphics.drawString(font, Component.translatable("skill.saitama.super_speed", getSpeed()), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
            }
        });
        list.add(7, new Skill() {
            @Override
            public SkillExecutionResult execute(Player p) {
                if (p instanceof ServerPlayer player) {
                    setBreakBlocksQuickly(!isBreakBlocksQuickly());
                    NetworkRegister.sendToPlayer(player, new PlayerSyncPacket(self));
                }
                return null;
            }

            @Override
            public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
                guiGraphics.drawString(font, Component.translatable("skill.saitama.break_blocks_quickly"), width / 2 - defaultReduce, height / 2 + defaultAdd, isBreakBlocksQuickly() ? Color.GREEN.getRGB() : Color.RED.getRGB(), false);
            }
        });
        list.add(new Skill() {
            @Override
            public SkillExecutionResult execute(Player player) {
                return null;
            }

            @Override
            public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
                guiGraphics.drawString(font, Component.translatable("skill.saitama.set_weight", getWeight()), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
            }
        });
        list.add(new Skill() {
            @Override
            public SkillExecutionResult execute(Player player) {
                return null;
            }

            @Override
            public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
                guiGraphics.drawString(font, Component.translatable("skill.saitama.knockback_resistance", getKnockbackResistance()), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
            }
        });
        list.add(new Skill() {
            @Override
            public SkillExecutionResult execute(Player player) {
                return null;
            }

            @Override
            public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
                guiGraphics.drawString(font, Component.translatable("skill.saitama.attack_knockback", getAttackKnockback()), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
            }
        });
        list.add(new Skill() {
            @Override
            public SkillExecutionResult execute(Player player) {
                return null;
            }

            @Override
            public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
                guiGraphics.drawString(font, Component.translatable("skill.saitama.swim_speed", getSwimSpeed()), width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
            }
        });
        list.add(12, new NormalPunchesInArea());
        list.add(13, new SaitamaDash());

        List<Skill> list2 = new ArrayList<>();
        list2.add(new ExtremeSpeed());
        list2.add(new Skill() {
            @Override
            public SkillExecutionResult execute(Player player) {
                if (player instanceof ServerPlayer serverPlayer) {
                    HelpUtility.verifyIsSaitamaAndGetCapability(serverPlayer).ifPresent(sai -> {
                        sai.setExtremeJump(!sai.isExtremeJump());
                        NetworkRegister.sendToPlayer(serverPlayer, new PlayerSyncPacket(sai));
                    });
                }
                return null;
            }

            @Override
            public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
                guiGraphics.drawString(font, Component.translatable("skill.saitama.extreme_jump"), width / 2 - defaultReduce, height / 2 + defaultAdd, isExtremeJump() ? Color.GREEN.getRGB() : Color.RED.getRGB(), false);
            }
        });
        groupList.add(list);
        groupList.add(list2);
        return groupList;
    }

    @Override
    public void manageFlux(LivingEvent event) {
        packFlux(event);
    }

    @Override
    public void playerRespawn(PlayerEvent.PlayerRespawnEvent event) {

    }

    private void packFlux(LivingEvent event) {
        if (event instanceof LivingDamageEvent damageEvent && HelpUtility.isSaitamaServerSide(damageEvent.getEntity())) {
            event.setCanceled(true);
        } else if (event instanceof LivingDeathEvent deathEvent && HelpUtility.isSaitamaServerSide(deathEvent.getEntity())) {
            LivingEntity player = deathEvent.getEntity();
            player.setHealth(player.getMaxHealth());
            event.setCanceled(true);
        }
    }

    @Override
    public Tag writeNBT() {
        CompoundTag nbt = (CompoundTag) super.writeNBT();
        nbt.putBoolean(NbtBooleanValues.seriousFartActive.getValue(), this.seriousFartActive);
        nbt.putBoolean(NbtBooleanValues.breakBlocksQuickly.getValue(), this.isBreakBlocksQuickly());
        nbt.putBoolean(NbtBooleanValues.extremeSpeed.getValue(), this.isExtremeSpeedActive());
        nbt.putShort("weight", this.weight);
        nbt.putShort(NbtBooleanValues.superSpeed.getValue(), this.speed);
        nbt.putShort("saitamaknockbackresistance", this.knockbackResistance);
        nbt.putShort("saitamaattackknockback", this.attackKnockback);
        nbt.putShort("saitamaswimspeed", this.swimSpeed);
        nbt.putBoolean("extremejump", this.extremeJump);
        return nbt;
    }

    @Override
    public void readNBT(Tag tag) {
        super.readNBT(tag);
        CompoundTag nbt = (CompoundTag) tag;
        this.seriousFartActive = nbt.getBoolean(NbtBooleanValues.seriousFartActive.getValue());
        this.breakBlocksQuickly = nbt.getBoolean(NbtBooleanValues.breakBlocksQuickly.getValue());
        this.extremeSpeedActive = nbt.getBoolean(NbtBooleanValues.extremeSpeed.getValue());
        this.weight = nbt.getShort("weight");
        this.speed = nbt.getShort(NbtBooleanValues.superSpeed.getValue());
        this.knockbackResistance = nbt.getShort("saitamaknockbackresistance");
        this.attackKnockback = nbt.getShort("saitamaattackknockback");
        this.swimSpeed = nbt.getShort("saitamaswimspeed");
        this.extremeJump = nbt.getBoolean(NbtBooleanValues.extremeJump.getValue());
    }

    private int getLastSkill() {
        return skills.get(getCurrentGroupIndex()).size() - 1;
    }

    @Override
    public int getMaxNumSkill() {
        return 13;
    }

    @Override
    public void syncVariables(SkillPack serverData) {
        if (!(serverData instanceof SaitamaPack data))
            return;
        this.setCurrentSkill(data.getCurrentSkillIndex());
        this.setSeriousFartActive(data.isSeriousFartActive());
        this.setSpeed(data.getSpeed());
        this.setBreakBlocksQuickly(data.isBreakBlocksQuickly());
        this.setWeight(data.getWeight());
        this.setKnockbackResistance(data.getKnockbackResistance());
        this.setAttackKnockback(data.getAttackKnockback());
        this.setSwimSpeed(data.getSwimSpeed());
        this.extremeJump = data.extremeJump;
    }

//    @Override
//    public ArrayList<String> compareTo(SkillPack otherData) {
//        if (!(otherData instanceof SaitamaPack saitamaPack))
//            return new ArrayList<>();
//        ArrayList<String> changed = new ArrayList<>();
//        if (this.currentSkillIndex != saitamaPack.currentSkillIndex) {
//            changed.add("currentSkillIndex");
//        }
//        if (this.speed != saitamaPack.speed) {
//            changed.add(NbtBooleanValues.superSpeed.getValue());
//        }
//        if (this.breakBlocksQuickly != saitamaPack.breakBlocksQuickly) {
//            changed.add(NbtBooleanValues.breakBlocksQuickly.getValue());
//        }
//        if (this.getWeight() != saitamaPack.getWeight()) {
//            changed.add("weight");
//        }
//        if (this.getKnockbackResistance() != saitamaPack.getKnockbackResistance()) {
//            changed.add("saitamaknockbackresistance");
//        }
//        if (this.getAttackKnockback() != saitamaPack.getAttackKnockback()) {
//            changed.add("saitamaattackknockback");
//        }
//        if (this.getSwimSpeed() != saitamaPack.getSwimSpeed()) {
//            changed.add("saitamaswimspeed");
//        }
//        if (this.currentGroupIndex != saitamaPack.currentGroupIndex) {
//            changed.add("currentGroupIndex");
//        }
//        return changed;
//    }
//
//    @Override
//    public void handleTheDifferences(ServerPlayer player, ArrayList<String> differences, SkillPack serverLayer, SkillPack clientLayer) {
//        if (!(serverLayer instanceof SaitamaPack serverData) || !(clientLayer instanceof SaitamaPack clientData))
//            return;
//        differences.forEach(item -> {
//            switch (item) {
//                case "currentSkillIndex":
//                    updateCurrentSkill(serverData, clientData);
//                    break;
//                case "seriousfart":
//                    updateSeriousFart(player, serverData, clientData);
//                    break;
//                case "superspeedsaitama":
//                    updateFieldWithValidation(serverData::setSpeed, clientData.getSpeed(), player, serverData);
//                    break;
//                case "breakblocksquickly":
//                    updateBreakBlocksQuickly(player, serverData, clientData);
//                    break;
//                case "weight":
//                    updateFieldWithValidation(serverData::setWeight, clientData.getWeight(), player, serverData);
//                    break;
//                case "saitamaknockbackresistance":
//                    updateFieldWithValidation(serverData::setKnockbackResistance, clientData.getKnockbackResistance(), player, serverData);
//                    break;
//                case "saitamaattackknockback":
//                    updateFieldWithValidation(serverData::setAttackKnockback, clientData.getAttackKnockback(), player, serverData);
//                    break;
//                case "saitamaswimspeed":
//                    updateFieldWithValidation(serverData::setSwimSpeed, clientData.getSwimSpeed(), player, serverData);
//                    break;
//                case "currentGroupIndex":
//                    updateCurrentGroup(serverData, clientData);
//                    break;
//            }
//        });
//    }

    private void updateCurrentGroup(SaitamaPack serverData, SaitamaPack clientData) {
        int diff = Math.abs(serverData.currentGroupIndex - clientData.currentGroupIndex);
        if (diff == 1 || diff == serverData.skills.size() - 1)
            serverData.currentGroupIndex = clientData.currentGroupIndex;
    }

    private static void updateBreakBlocksQuickly(ServerPlayer player, SaitamaPack serverData, SaitamaPack clientData) {
        serverData.setBreakBlocksQuickly(clientData.isBreakBlocksQuickly());
        NetworkRegister.sendToPlayer(player, new PlayerSyncPacket(serverData));
    }

    private void updateFieldWithValidation(Consumer<Short> setter, short clientValue, ServerPlayer player, SaitamaPack serverData) {
        boolean isInvalid = clientValue < 0;
        setter.accept(isInvalid ? 0 : clientValue);
        if (isInvalid)
            NetworkRegister.sendToPlayer(player, new PlayerSyncPacket(serverData));
    }

    private static void updateSeriousFart(ServerPlayer player, SaitamaPack serverData, SaitamaPack clientData) {
        serverData.setSeriousFartActive(clientData.isSeriousFartActive());
        NetworkRegister.sendToPlayer(player, new PlayerSyncPacket(serverData));
    }

    private static void updateCurrentSkill(SaitamaPack serverData, SaitamaPack clientData) {
        int diff = Math.abs(serverData.currentSkillIndex - clientData.currentSkillIndex);
        boolean b = diff == 1 || diff == serverData.getLastSkill();
        if (b || serverData.getCurrentGroupIndex() != clientData.getCurrentGroupIndex())
            serverData.currentSkillIndex = clientData.currentSkillIndex;
    }

    @Override
    public void adjustAbility(double scrollDelta) {
        switch (getCurrentSkillIndex()) {
            case 6 -> setSpeed(getSpeed() + scrollDelta < 0 ? 0 : (short) (getSpeed() + scrollDelta));
            case 8 -> setWeight(getWeight() + scrollDelta < 0 ? 0 : (short) (getWeight() + scrollDelta));
            case 9 ->
                    setKnockbackResistance(getKnockbackResistance() + scrollDelta < 0 ? 0 : (short) (getKnockbackResistance() + scrollDelta));
            case 10 ->
                    setAttackKnockback(getAttackKnockback() + scrollDelta < 0 ? 0 : (short) (getAttackKnockback() + scrollDelta));
            case 11 -> setSwimSpeed(getSwimSpeed() + scrollDelta < 0 ? 0 : (short) (getSwimSpeed() + scrollDelta));
        }
    }

    private static final Map<Player, Integer> shiftHoldTime = new HashMap<>();

    @Override
    public void tick(TickEvent.PlayerTickEvent event) {
        if (event.player instanceof ServerPlayer serverPlayer) {
            manageEffectsAndAttributes(event);
            explodeNormalMobs(serverPlayer);
        }
        super.tick(event);
    }

    private void manageEffectsAndAttributes(TickEvent.PlayerTickEvent event) {
        ServerPlayer player = (ServerPlayer) event.player;
        modifyAttributes(player);
        if (player.isOnFire())
            player.clearFire();
        removeNegativeEffectsOfSaitama(player);
        HelpUtility.applySaitamaEffectsSet(player);
        if (event.phase == TickEvent.Phase.END && !this.isExtremeJump())
            handlerJumpPower(player);

    }

    private void handlerJumpPower(ServerPlayer player) {
        int value = shiftHoldTime.getOrDefault(player, 0);
        if (player.isShiftKeyDown()) {
            shiftHoldTime.put(player, ++value);
        } else {
            shiftHoldTime.remove(player);
        }
        if (shiftHoldTime.containsKey(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 1, Math.min(value, 127)));
        }
    }

    private void modifyAttributes(ServerPlayer player) {
        if (player.isSpectator())
            return;
        //0.08
        if (this.getWeight() != 0)
            player.getAttribute(ForgeMod.ENTITY_GRAVITY.get()).setBaseValue((double) this.getWeight() / 10);
        else
            player.getAttribute(ForgeMod.ENTITY_GRAVITY.get()).setBaseValue(0.08);
        if (this.getSpeed() != 0)
            player.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue((double) this.getSpeed() / 9);
        else
            player.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.1F);

        if (this.getAttackKnockback() != 0)
            player.getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(this.getAttackKnockback());
        else
            player.getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(0);

        if (this.getKnockbackResistance() != 0)
            player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(this.getKnockbackResistance());
        else
            player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0);

        if (this.getSwimSpeed() != 0)
            player.getAttribute(ForgeMod.SWIM_SPEED.get()).setBaseValue(this.getSwimSpeed());
        else
            player.getAttribute(ForgeMod.SWIM_SPEED.get()).setBaseValue(1.0D);

        player.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(500F);
    }

    private static void removeNegativeEffectsOfSaitama(ServerPlayer player) {
        if (player.getEffect(MobEffects.DARKNESS) != null) {
            player.removeEffect(MobEffects.DARKNESS);
        }
        if (player.getEffect(MobEffects.MOVEMENT_SLOWDOWN) != null) {
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
        if (player.getEffect(MobEffects.BLINDNESS) != null) {
            player.removeEffect(MobEffects.BLINDNESS);
        }
        if (player.getEffect(MobEffects.WEAKNESS) != null) {
            player.removeEffect(MobEffects.WEAKNESS);
        }
        if (player.getEffect(MobEffects.LEVITATION) != null) {
            player.removeEffect(MobEffects.LEVITATION);
        }
        if (player.getEffect(MobEffects.POISON) != null) {
            player.removeEffect(MobEffects.POISON);
        }
        if (player.getEffect(MobEffects.DIG_SLOWDOWN) != null) {
            player.removeEffect(MobEffects.DIG_SLOWDOWN);
        }
        if (player.getEffect(MobEffects.CONFUSION) != null) {
            player.removeEffect(MobEffects.CONFUSION);
        }
    }

    private static void explodeNormalMobs(ServerPlayer player) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        AABB aabb = new AABB(x - 100, y - 100, z - 100, x + 100, y + 100, z + 100);
        ServerLevel level = (ServerLevel) player.level();
        level.getEntitiesOfClass(LivingEntity.class, aabb, e -> e.getTags().contains("targetnormalpunch")).forEach(
                e -> {
                    double x1 = e.getX();
                    double y1 = e.getY();
                    double z1 = e.getZ();
                    HelpUtility.explodeWithoutKnockBackFor(player, x1, y1 + 0.0625D, z1, 12.0F);
                    level.sendParticles(ParticleTypes.FLAME, x1, y1, z1, 10, 0, 0, 0, 0);
                    level.sendParticles(ParticleTypes.FLASH, x1, y1, z1, 10, 0, 0, 0, 0);
                    level.sendParticles(ParticleTypes.FIREWORK, x1, y1, z1, 10, 0, 0, 0, 0);
                    level.sendParticles(ParticleTypes.FIREWORK, x1, y1, z1, 10, 0, 0, 0, 0);
                }
        );
    }
}
