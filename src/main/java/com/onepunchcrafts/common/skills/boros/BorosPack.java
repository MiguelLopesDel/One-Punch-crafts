package com.onepunchcrafts.common.skills.boros;

import com.onepunchcrafts.common.skills.*;
import com.onepunchcrafts.common.skills.saitama.SaitamaPack;
import com.onepunchcrafts.common.skills.sync.SyncStrategy;
import com.onepunchcrafts.common.skills.sync.Syncable;
import com.onepunchcrafts.common.skills.sync.SyncableSkillPack;
import com.onepunchcrafts.constant.NbtBooleanValues;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.PlayerSyncPacket;
import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.util.TickScheduler;
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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BorosPack extends SyncableSkillPack {

    private final int maxEnergy = 1_000_000_000;
    @Setter
    @Getter
    @Syncable(key = "breakblocksquickly", strategy = SyncStrategy.TOGGLE)
    private boolean breakBlocksQuickly;
    @Setter
    @Getter
    @Syncable(key = "borospeed", strategy = SyncStrategy.VALIDATED)
    private short speed;
    @Setter
    @Getter
    @Syncable(key = "borosknockbackresistance", strategy = SyncStrategy.VALIDATED)
    private short knockbackResistance;
    @Setter
    @Getter
    @Syncable(key = "borosattackknockback", strategy = SyncStrategy.VALIDATED)
    private short attackKnockback;
    @Setter
    @Getter
    @Syncable(key = "borosswimspeed", strategy = SyncStrategy.VALIDATED)
    private short swimSpeed;
    @Getter
    private final float MAX_HEALTH = 1_000_000_000f;
    @Getter
    private final byte state = 0;
    @Getter
    private final BorosPack self = this;
    @Setter
    @Getter
    @Syncable(key = "tickstoultraregeneration", strategy = SyncStrategy.SIMPLE)
    private int ticksToUseUltraRegeneration = 0;
    @Setter
    @Getter
//    @Syncable(key = "borosform", strategy = SyncStrategy.VALIDATED)
    private short currentForm = 0;
    @Setter
    @Getter
    @Syncable(key = "borosenergy", strategy = SyncStrategy.VALIDATED)
    private float energy = maxEnergy;
    @Setter
    @Getter
    @Syncable(key = "borosarmorregeneration", strategy = SyncStrategy.TOGGLE)
    private boolean armorRegenerationActive = false;

    @Setter
    @Getter
//    @Syncable(key = "borosmeteoricmode", strategy = SyncStrategy.TOGGLE)
    private boolean meteoricBurstActive = false;

    @Setter
    @Getter
    @Syncable(key = "borosflight", strategy = SyncStrategy.TOGGLE)
    private boolean flightActive = false;


    @Override
    protected @NotNull List<List<Skill>> initializeSkills() {
        List<List<Skill>> groupList = new ArrayList<>();

        ArrayList<Skill> basicSkills = new ArrayList<>();
        basicSkills.add(0, new BorosRegeneration());
        basicSkills.add(1, new BorosEnergyProjection());
        basicSkills.add(2, new BorosFlight());
        basicSkills.add(3, createSpeedSkill());

        ArrayList<Skill> transformSkills = new ArrayList<>();
        transformSkills.add(0, new BorosMeteoricBurst());
//        transformSkills.add(1, createArmorBreakSkill());

        ArrayList<Skill> ultimateSkills = new ArrayList<>();
        ultimateSkills.add(0, new BorosCollapsingStarCannon());

        groupList.add(basicSkills);
        groupList.add(transformSkills);
        groupList.add(ultimateSkills);

        return groupList;
    }

    private Skill createSpeedSkill() {
        return new Skill() {
            @Override
            public SkillExecutionResult execute(Player player) {
                return null;
            }

            @Override
            public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
                guiGraphics.drawString(font, Component.translatable("skill.boros.speed", getSpeed()),
                        width / 2 - defaultReduce, height / 2 + defaultAdd, Color.GREEN.getRGB(), false);
            }
        };
    }

    @Override
    public void execute(Player player) {
        Skill skill = getCurrentSkill();
        if (skill != null) {
            skill.execute(player);

            if (skill instanceof BorosRegeneration) {
                TickScheduler.scheduleFromHere(Duration.ofSeconds(20), () -> this.setTicksToUseUltraRegeneration(24_000));
            }
        }
    }

    @Override
    public void renderSkills(int width, int height, Font font, GuiGraphics guiGraphics) {
        getCurrentSkill().renderName(width+80, height, font, guiGraphics, (int) (width * 0.05), (int) (height * 0.25));
    }

    @Override
    public void playerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        event.getEntity().getAttribute(Attributes.MAX_HEALTH).setBaseValue(this.getMAX_HEALTH());
        event.getEntity().setHealth(this.getMAX_HEALTH());
    }

    @Override
    public Tag writeNBT() {
        CompoundTag nbt = (CompoundTag) super.writeNBT();
        nbt.putBoolean(NbtBooleanValues.breakBlocksQuickly.getValue(), this.isBreakBlocksQuickly());
        nbt.putShort(NbtBooleanValues.superSpeed.getValue(), this.speed);
        nbt.putShort("saitamaknockbackresistance", this.knockbackResistance);
        nbt.putShort("saitamaattackknockback", this.attackKnockback);
        nbt.putShort("saitamaswimspeed", this.swimSpeed);
        nbt.putInt("tickstoultraregeneration", this.ticksToUseUltraRegeneration);
        return nbt;
    }

    @Override
    public void readNBT(Tag tag) {
        super.readNBT(tag);
        CompoundTag nbt = (CompoundTag) tag;
//        this.seriousFartActive = nbt.getBoolean(NbtBooleanValues.seriousFartActive.getValue());
        this.breakBlocksQuickly = nbt.getBoolean(NbtBooleanValues.breakBlocksQuickly.getValue());
//        this.extremeSpeedActive = nbt.getBoolean(NbtBooleanValues.extremeSpeed.getValue());
//        this.weight = nbt.getShort("weight");
        this.speed = nbt.getShort(NbtBooleanValues.superSpeed.getValue());
        this.knockbackResistance = nbt.getShort("saitamaknockbackresistance");
        this.attackKnockback = nbt.getShort("saitamaattackknockback");
        this.swimSpeed = nbt.getShort("saitamaswimspeed");
        this.ticksToUseUltraRegeneration = nbt.getInt("tickstoultraregeneration");
//        this.extremeJump = nbt.getBoolean(NbtBooleanValues.extremeJump.getValue());
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

    private void decreaseTimeToUltraRegeneration() {
        if (ticksToUseUltraRegeneration > 0)
            ticksToUseUltraRegeneration--;
    }

    @Override
    public int getMaxNumSkill() {
        return 3;
    }

    @Override
    public void syncVariables(SkillPack serverData) {
        if (!(serverData instanceof SaitamaPack data))
            return;
        this.setCurrentSkill(data.getCurrentSkillIndex());
        this.setSpeed(data.getSpeed());
        this.setBreakBlocksQuickly(data.isBreakBlocksQuickly());
        this.setKnockbackResistance(data.getKnockbackResistance());
        this.setAttackKnockback(data.getAttackKnockback());
        this.setSwimSpeed(data.getSwimSpeed());
    }

//    @Override
//    public void handleTheDifferences(ServerPlayer player, ArrayList<String> differences, SkillPack serverLayer, SkillPack clientLayer) {
//        if (!(serverLayer instanceof SaitamaPack serverData) || !(clientLayer instanceof SaitamaPack clientData))
//            return;
//        differences.forEach(item -> {
//            switch (item) {
//                case "currentSkillIndex":
//                    updateCurrentSkill(serverData, clientData);
//                    break;
//                case "superspeedsaitama":
//                    updateFieldWithValidation(serverData::setSpeed, clientData.getSpeed(), player, serverData);
//                    break;
//                case "currentGroupIndex":
//                    updateCurrentGroup(serverData, clientData);
//                    break;
//            }
//        });
//    }

//    private static void updateCurrentSkill(SaitamaPack serverData, SaitamaPack clientData) {
//        int diff = Math.abs(serverData.currentSkillIndex - clientData.currentSkillIndex);
//        boolean b = diff == 1 || diff == serverData.getLastSkill();
//        if (b || serverData.getCurrentGroupIndex() != clientData.getCurrentGroupIndex())
//            serverData.currentSkillIndex = clientData.currentSkillIndex;
//    }
//
//    private void updateFieldWithValidation(Consumer<Short> setter, short clientValue, ServerPlayer player, SaitamaPack serverData) {
//        boolean isInvalid = clientValue < 0;
//        setter.accept(isInvalid ? 0 : clientValue);
//        if (isInvalid)
//            NetworkRegister.sendToPlayer(player, new PlayerSyncPacket(serverData));
//    }
//
//    private void updateCurrentGroup(SaitamaPack serverData, SaitamaPack clientData) {
//        int diff = Math.abs(serverData.currentGroupIndex - clientData.currentGroupIndex);
//        if (diff == 1 || diff == serverData.skills.size() - 1)
//            serverData.currentGroupIndex = clientData.currentGroupIndex;
//    }

    @Override
    public void adjustAbility(double scrollDelta) {
        switch (getCurrentSkillIndex()) {
            case 1 -> setSpeed(getSpeed() + scrollDelta < 0 ? 0 : (short) (getSpeed() + scrollDelta));
//            case 8 -> setWeight(getWeight() + scrollDelta < 0 ? 0 : (short) (getWeight() + scrollDelta));
//            case 9 ->
//                    setKnockbackResistance(getKnockbackResistance() + scrollDelta < 0 ? 0 : (short) (getKnockbackResistance() + scrollDelta));
//            case 10 ->
//                    setAttackKnockback(getAttackKnockback() + scrollDelta < 0 ? 0 : (short) (getAttackKnockback() + scrollDelta));
//            case 11 -> setSwimSpeed(getSwimSpeed() + scrollDelta < 0 ? 0 : (short) (getSwimSpeed() + scrollDelta));
        }
    }

    @Override
    public void tick(TickEvent.PlayerTickEvent event) {
        if (event.player instanceof ServerPlayer) {
            decreaseTimeToUltraRegeneration();
            regenerateEnergy();
            manageEffectsAndAttributes(event);
            handleMeteoricBurstEffects(event);
        }
        super.tick(event);
    }

    private void regenerateEnergy() {
        if (energy < maxEnergy) {
            energy += 41666.5;
            if (energy > getEnergy()) energy = maxEnergy;
        }
    }

    private void handleMeteoricBurstEffects(TickEvent.PlayerTickEvent event) {
        ServerPlayer player = (ServerPlayer) event.player;

        if (meteoricBurstActive) {
            if (player.level().getGameTime() % 10 == 0) {
                ((ServerLevel) player.level()).sendParticles(
                        ParticleTypes.DRAGON_BREATH,
                        player.getX(), player.getY() + 1, player.getZ(),
                        5, 0.5, 1.0, 0.5, 0.1
                );
            }

            if (player.level().getGameTime() % 20 == 0) {
                energy -= 5;
                if (energy <= 0) {
                    setCurrentForm((short) 1);
                    setMeteoricBurstActive(false);
                    player.sendSystemMessage(Component.literal("§c§lMeteoric Burst deactivated - no energy!"));
                }
            }
        }
    }

    private void manageEffectsAndAttributes(TickEvent.PlayerTickEvent event) {
        ServerPlayer player = (ServerPlayer) event.player;
        modifyAttributes(player);
        HelpUtility.applyGodLevelEffectSet(player);
        if (player.getEffect(MobEffects.REGENERATION) == null) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, 255));
        }
    }

    private void modifyAttributes(ServerPlayer player) {
        if (player.isSpectator())
            return;
        //0.08
//        if (this.getWeight() != 0)
//            player.getAttribute(ForgeMod.ENTITY_GRAVITY.get()).setBaseValue((double) this.getWeight() / 10);
//        else
//            player.getAttribute(ForgeMod.ENTITY_GRAVITY.get()).setBaseValue(0.08);
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

        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(this.getMAX_HEALTH());
        player.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(50F);
    }

    @Override
    public void manageFlux(LivingEvent event) {

    }
}
