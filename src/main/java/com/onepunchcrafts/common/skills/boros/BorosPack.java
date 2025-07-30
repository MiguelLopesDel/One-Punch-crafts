package com.onepunchcrafts.common.skills.boros;

import com.onepunchcrafts.common.skills.AbstractSkillPack;
import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.common.skills.SkillPack;
import com.onepunchcrafts.common.skills.SkillPassive;
import com.onepunchcrafts.constant.NbtBooleanValues;
import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.util.TickScheduler;
import it.unimi.dsi.fastutil.shorts.ShortConsumer;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class BorosPack extends AbstractSkillPack {

    @Setter
    @Getter
    private boolean breakBlocksQuickly;
    @Setter
    @Getter
    private short speed;
    @Setter
    @Getter
    private short knockbackResistance;
    @Setter
    @Getter
    private short attackKnockback;
    @Setter
    @Getter
    private short swimSpeed;
    @Getter
    private final float MAX_HEALTH = 1_000_000_000f;
    @Getter
    private final byte state = 0;
    @Getter
    private final BorosPack self = this;
    @Setter
    @Getter
    private int ticksToUseUltraRegeneration = 0;

    @Override
    protected @NotNull List<List<Skill>> initializeSkills() {
        List<List<Skill>> groupList = new ArrayList<>();
        ArrayList<Skill> list = new ArrayList<>();
        list.add(0, new BorosRegeneration());

        groupList.add(list);
        return groupList;
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

    @Override
    public ArrayList<String> compareTo(SkillPack otherData) {
        return null;
    }

    private void decreaseTimeToUltraRegeneration() {
        if (ticksToUseUltraRegeneration > 0)
            ticksToUseUltraRegeneration--;
    }

    @Override
    public int getMaxNumSkill() {
        return 0;
    }

    @Override
    public void syncVariables(SkillPack serverData) {

    }

    @Override
    public void renderSkills(int width, int height, Font font, GuiGraphics guiGraphics) {

    }

    @Override
    public void handleTheDifferences(ServerPlayer player, ArrayList<String> differences, SkillPack serverData, SkillPack clientData) {

    }

    @Override
    public void adjustAbility(ShortConsumer setter, short currentValue, double scrollDelta) {

    }

    @Override
    public void tick(TickEvent.PlayerTickEvent event) {
        if (event.player instanceof ServerPlayer) {
            decreaseTimeToUltraRegeneration();
            manageEffectsAndAttributes(event);
        }
        super.tick(event);
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
