package com.onepunchcrafts.util;

import com.onepunchcrafts.OnePunchCrafts;
import com.onepunchcrafts.common.damage.DamagesRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class TickUtilities {

    private int startIndex;


    //z é a profundidade ou melhor dizendo a altura do cilindro
    //então o x e y criam um circulo inicial "2D" que é a base do cilindro tal circulo pega o eixo Y e X no desenho
    //e pode se dizer que não tem profundidade é 2D não tem um eixo Z pois só ocupa 1 bloco do eixo Z
    //Então eu preciso pegar esse circulo e extender ele ao logo do eixo Z ou seja dar a ele profundidade
    //para isso posso pegar meu vetor e escalar ele.
    public boolean fillCylinderAndEmuleEffects(ServerPlayer player, final ServerLevel level, int breakBlocksPerTick, List<BlockPos> blocksPos) {
        int currentIteration = 0;
        if(blocksPos.isEmpty() || startIndex >= blocksPos.size())
            return true;
        BlockPos pStart = blocksPos.get(startIndex);
        int i = 15;
        AABB pArea = new AABB(new BlockPos(pStart.getX() - i, pStart.getY() - i, pStart.getZ() - i),
                new BlockPos(pStart.getX() + i, pStart.getY() + i, pStart.getZ() + i));
        List<LivingEntity> entitiesOfClass = level.getEntitiesOfClass(LivingEntity.class, pArea);
        entitiesOfClass.forEach(entity -> {
            if (player.equals(entity))
                return;
            entity.setInvulnerable(false);
            entity.setSecondsOnFire(60);
            DamageSource damagesource = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamagesRegistry.SERIOUS_PUNCH_SECOND), null, player);
            entity.hurt(damagesource, 10_000_000_000_000_000f);
        });

        for (int c = startIndex; c < blocksPos.size(); c++) {
            BlockPos pPos = blocksPos.get(c);
            level.setBlock(pPos, Blocks.AIR.defaultBlockState(), 3);
//            level.sendParticles(ParticleTypes.FLAME, pPos.getX(), pPos.getY(), pPos.getZ(), 1, 0, 0, 0, 0);
//            level.sendParticles(ParticleTypes.FLASH, pPos.getX(), pPos.getY(), pPos.getZ(), 1, 0, 0, 0, 0);
//            level.sendParticles(ParticleTypes.FIREWORK, pPos.getX(), pPos.getY(), pPos.getZ(), 1, 0, 0, 0, 0);
//            level.sendParticles(ParticleTypes.FIREWORK, pPos.getX(), pPos.getY(), pPos.getZ(), 1, 0, 0, 0, 0);
            currentIteration++;
            startIndex++;
            if (currentIteration % breakBlocksPerTick == 0)
                return false;
        }
        return true;
    }
}
