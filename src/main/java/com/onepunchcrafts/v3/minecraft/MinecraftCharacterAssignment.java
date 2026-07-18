package com.onepunchcrafts.v3.minecraft;

import com.onepunchcrafts.common.capability.OnePunchPlayer;
import com.onepunchcrafts.common.skills.boros.BorosPack;
import com.onepunchcrafts.network.NetworkRegister;
import com.onepunchcrafts.network.packet.PlayerSyncPacket;
import com.onepunchcrafts.network.packet.PowerStateSnapshotPacket;
import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.v3.OnePunchV3;
import com.onepunchcrafts.v3.content.SaitamaContent;
import com.onepunchcrafts.v3.core.character.CharacterIdentity;
import com.onepunchcrafts.v3.core.character.CharacterTransition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

import static com.onepunchcrafts.OnePunchCrafts.WITHOUT_PACK;

/** Minecraft boundary for atomically replacing a player's character. */
public final class MinecraftCharacterAssignment {
    private MinecraftCharacterAssignment() {}

    public static void assign(ServerPlayer player, CharacterIdentity target) {
        OnePunchPlayer capability = HelpUtility.getSkillData(player);
        CharacterTransition.apply(target, new Runtime(player, capability));
    }

    private static final class Runtime implements CharacterTransition.Runtime {
        private final ServerPlayer player;
        private final OnePunchPlayer capability;

        private Runtime(ServerPlayer player, OnePunchPlayer capability) {
            this.player = player;
            this.capability = capability;
        }

        @Override
        public void resetProjection() {
            SaitamaMinecraftSystem.clear(player);
            HelpUtility.resetCharacterProjection(player);
        }

        @Override
        public void clearLegacyPack() {
            capability.setSkillPack(WITHOUT_PACK);
        }

        @Override
        public void clearPowerSet() {
            OnePunchV3.POWERS.clear(capability.getPowerState());
        }

        @Override
        public void installSaitama() {
            OnePunchV3.POWERS.assign(capability.getPowerState(), SaitamaContent.POWER_SET);
            SaitamaMinecraftSystem.tick(player);
        }

        @Override
        public void installBoros() {
            BorosPack boros = new BorosPack();
            capability.setSkillPack(boros);
            player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(boros.getMAX_HEALTH());
            player.setHealth(boros.getMAX_HEALTH());
        }

        @Override
        public void sync() {
            NetworkRegister.sendToPlayer(player, new PlayerSyncPacket(capability.getSkillPack()));
            NetworkRegister.sendToPlayer(player,
                    new PowerStateSnapshotPacket(PowerStateCodec.encode(capability.getPowerState())));
        }
    }
}
