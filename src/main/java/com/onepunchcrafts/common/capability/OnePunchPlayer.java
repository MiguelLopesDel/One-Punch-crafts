package com.onepunchcrafts.common.capability;

import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.common.skills.boros.BorosPack;
import com.onepunchcrafts.common.skills.saitama.SaitamaPack;
import com.onepunchcrafts.common.skills.SkillPack;
import com.onepunchcrafts.v3.core.state.PowerState;
import com.onepunchcrafts.v3.minecraft.PowerStateCodec;
import com.onepunchcrafts.v3.OnePunchV3;
import com.onepunchcrafts.v3.content.SaitamaContent;
import it.unimi.dsi.fastutil.shorts.ShortConsumer;
import lombok.Data;
import lombok.NonNull;
import lombok.Setter;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.onepunchcrafts.OnePunchCrafts.WITHOUT_PACK;
import static com.onepunchcrafts.util.HelpUtility.syncDataWithServer;

@Data
public class OnePunchPlayer {

    @NonNull
    @Setter
    private SkillPack skillPack;
    private final PowerState powerState = new PowerState();

    public OnePunchPlayer(@NotNull SkillPack skillPack) {
        this.skillPack = skillPack;
    }

    public Tag writeNBT() {
        CompoundTag nbt = (CompoundTag) skillPack.writeNBT();
        nbt.put("v3", PowerStateCodec.encode(powerState));
        return nbt;
    }

    public void firstReadNBT(Tag tag) {
        CompoundTag nbt = (CompoundTag) tag;
        skillPack = switch (nbt.getString("skillPack")) {
            case "SaitamaPack" -> new SaitamaPack();
            case "BorosPack" -> new BorosPack();
            default -> WITHOUT_PACK;
        };
        readNBT(nbt);
        if (nbt.contains("v3", Tag.TAG_COMPOUND)) {
            PowerStateCodec.decodeInto(nbt.getCompound("v3"), powerState);
            if (powerState.powerSetId().equals(SaitamaContent.POWER_SET)) skillPack = WITHOUT_PACK;
        } else if (skillPack instanceof SaitamaPack) {
            OnePunchV3.POWERS.assign(powerState, SaitamaContent.POWER_SET);
            skillPack = WITHOUT_PACK;
        }
    }

    public void readNBT(Tag tag) {
        skillPack.readNBT(tag);
    }

    /**
     * Método que compara os campos com outro objeto e retorna um Map contendo as diferenças a chave é o valor original
     *
     * @param otherData
     * @return
     */
    public ArrayList<String> compareTo(SkillPack otherData) {
        return skillPack.compareTo(otherData);
    }

    public void setCurrentSkill(int currentSkill) {
        this.skillPack.setCurrentSkill(currentSkill);
    }

    public void decideCurrentSkill(LocalPlayer player) {
        setCurrentSkill(getActualAbility() + (player.isShiftKeyDown() ? -1 : 1));
    }

    public void decideCurrentGroup(LocalPlayer player) {
        this.skillPack.nextOrPrevious(player.isShiftKeyDown() ? -1 : 1);
    }

    public void adjustAbilityAndSyncWithServer(ShortConsumer setter, short currentValue, double scrollDelta) {
        getSkillPack().adjustAbility(scrollDelta);
        syncDataWithServer(this);
    }

    public void adjustAbilityAndSyncWithServer(double scrollDelta) {
        getSkillPack().adjustAbility(scrollDelta);
        syncDataWithServer(this);
    }

    public int getMaxNumSkill() {
        return this.skillPack.getMaxNumSkill();
    }

    public Skill getCurrentSkill() {
        return this.skillPack.getCurrentSkill();
    }

    @Deprecated
    public int getActualAbility() {
        return this.skillPack.getCurrentSkillIndex();
    }

    /**
     * Não é garantido que o evento recebido esteja no lado do servidor, faça as verificações necessarias para seu caso.
     * @param event
     */
    public void manageFlux(LivingEvent event) {
        getSkillPack().manageFlux(event);
    }

    public void playerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        getSkillPack().playerRespawn(event);
    }
}
