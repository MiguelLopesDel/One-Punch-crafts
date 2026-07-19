package com.onepunchcrafts.util;

import com.onepunchcrafts.common.damage.DamageSourceMod;
import com.onepunchcrafts.common.damage.DamagesRegistry;
import com.onepunchcrafts.common.vfx.SeriousPunchFront;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.onepunchcrafts.OnePunchCrafts.DRACONIC_MOD;
import static com.onepunchcrafts.OnePunchCrafts.IMMERSIVE_PORTALS_MOD;

public class TickUtilities {

    private int startIndex;


    //z é a profundidade ou melhor dizendo a altura do cilindro
    //então o x e y criam um circulo inicial "2D" que é a base do cilindro tal circulo pega o eixo Y e X no desenho
    //e pode se dizer que não tem profundidade é 2D não tem um eixo Z pois só ocupa 1 bloco do eixo Z
    //Então eu preciso pegar esse circulo e extender ele ao logo do eixo Z ou seja dar a ele profundidade
    //para isso posso pegar meu vetor e escalar ele.
    public boolean fillCylinderAndEmuleEffects(ServerPlayer player, final ServerLevel level, int breakBlocksPerTick,
                                               List<BlockPos> blocksPos, Vec3 axisOrigin, Vec3 direction, float radius) {
        int currentIteration = 0;
        if (blocksPos.isEmpty() || startIndex >= blocksPos.size())
            return true;
        SeriousPunchFront.advance(level, player, blocksPos, startIndex, axisOrigin, direction, radius);
        BlockPos pStart = blocksPos.get(startIndex);
        int i = 15;
        AABB pArea = new AABB(new BlockPos(pStart.getX() - i, pStart.getY() - i, pStart.getZ() - i),
                new BlockPos(pStart.getX() + i, pStart.getY() + i, pStart.getZ() + i));
        Holder.Reference<DamageType> holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamagesRegistry.SERIOUS_PUNCH_SECOND);
        final DamageSourceMod damageSource = new DamageSourceMod(holder, null, player);
        final float amount = 10_000_000_000_000_000f;
        level.getEntitiesOfClass(EndCrystal.class, pArea).forEach(entity -> {
            entity.setInvulnerable(false);
            entity.setSecondsOnFire(60);
            entity.hurt(damageSource, amount);
        });
        if (DRACONIC_MOD.isPresent())
            DraconicCompat.hurtDraconicCrystals(level, pArea, damageSource);
        if(IMMERSIVE_PORTALS_MOD.isPresent())
            ImmersivePortalsCompat.destroyPortals(level, pArea);
        List<LivingEntity> entitiesOfClass = level.getEntitiesOfClass(LivingEntity.class, pArea);

        entitiesOfClass.forEach(entity -> {
            if (player.equals(entity))
                return;
            if (DRACONIC_MOD.isPresent() && DraconicCompat.handleIfDraconicGuardian(entity, damageSource)) return;

            entity.setInvulnerable(false);
            entity.setSecondsOnFire(60);
            boolean hurt = entity.hurt(damageSource, amount);
//            entity.hurt(player.damageSources().fellOutOfWorld(), 1f);
            if (!hurt) {
                entity.setLastHurtByPlayer(player);
                entity.hurt(new DamageSource(holder, player, null), amount);
            }
        });

        for (int c = startIndex; c < blocksPos.size(); c++) {
            BlockPos pPos = blocksPos.get(c);
            level.setBlock(pPos, Blocks.AIR.defaultBlockState(), 3);
            currentIteration++;
            startIndex++;
            if (currentIteration % breakBlocksPerTick == 0)
                return false;
        }
        SeriousPunchFront.finish(level, player, blocksPos, axisOrigin, direction, radius);
        return true;
    }
}
