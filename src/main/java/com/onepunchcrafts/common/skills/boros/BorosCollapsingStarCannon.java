package com.onepunchcrafts.common.skills.boros;

import com.onepunchcrafts.common.skills.Skill;
import com.onepunchcrafts.common.skills.SkillExecutionResult;
import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.util.TickScheduler;
import com.onepunchcrafts.util.TickUtilities;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static com.onepunchcrafts.common.event.LivingDamageEventHandler.markBlocksToClear;

public class BorosCollapsingStarCannon implements Skill {

    @Override
    public SkillExecutionResult execute(Player p) {
        if (p instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
            BorosPack borosPack = HelpUtility.getSkillData(player, BorosPack.class);

            if (!borosPack.isMeteoricBurstActive() || borosPack.getEnergy() < 500) {
                player.sendSystemMessage(Component.literal("§c§lNeed Meteoric Burst + 500 Energy!"));
                return null;
            }

            borosPack.setEnergy((short) 0);

            executeCollapsingStarCannon(player, level);
        }
        return null;
    }

    private void executeCollapsingStarCannon(ServerPlayer player, ServerLevel level) {
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();

        player.sendSystemMessage(Component.literal("§6§l>> CHARGING COLLAPSING STAR ROARING CANNON <<"));

        for (int charge = 0; charge < 60; charge++) {
            TickScheduler.scheduleFromHere(Duration.ofMillis(500), () -> {
                for (int i = 0; i < 10; i++) {
                    Vec3 offset = new Vec3(
                            (Math.random() - 0.5) * 5,
                            (Math.random() - 0.5) * 5,
                            (Math.random() - 0.5) * 5
                    );
                    Vec3 particlePos = start.add(direction.scale(3)).add(offset);
                    level.sendParticles(ParticleTypes.DRAGON_BREATH,
                            particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);
                }
            });
        }

        TickScheduler.scheduleFromHere(
                Duration.ofSeconds(3), () -> {
                    fireCollapsingStarCannon(player, level, start, direction);
                }
        );
    }

    private void fireCollapsingStarCannon(ServerPlayer player, ServerLevel level, Vec3 start, Vec3 direction) {
        level.playSound(null, start.x, start.y, start.z, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 10.0F, 0.1F);

        Vec3 add = start.add(direction.scale(100));
        BlockPos center = new BlockPos((int) add.x, (int) add.y, (int) add.z);
        int raioCratera = 60;
        int profundidade = 40;
        int raioDesolacao = 300;
        int delay = 50;

        for (int r = raioCratera; r > 0; r -= 2) {
            final int raioAtual = r;
            TickScheduler.scheduleFromHere(Duration.ofMillis(delay), () -> {
                for (int y = center.getY() + 8; y > center.getY() - profundidade; y--) {
                    for (int x = -raioAtual; x <= raioAtual; x++) {
                        for (int z = -raioAtual; z <= raioAtual; z++) {
                            BlockPos pos = center.offset(x, y - center.getY(), z);
                            double dist = center.distSqr(pos);
                            if (dist <= raioAtual * raioAtual && dist > (raioAtual - 2) * (raioAtual - 2)) {
                                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                                if (level.random.nextFloat() < 0.10f && y <= center.getY() - profundidade / 2) {
                                    BlockPos firePos = pos.below();
                                    if (level.getBlockState(firePos).isSolid()) {
                                        level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
                                    }
                                }
                            }
                        }
                    }
                }
                level.sendParticles(ParticleTypes.EXPLOSION, center.getX(), center.getY(), center.getZ(), 200, raioAtual, 8, raioAtual, 0.3);
            });
            delay += 60;
        }

// Nivela a superfície ao redor da cratera, removendo tudo acima do nivelAlvo
        TickScheduler.scheduleFromHere(Duration.ofMillis(delay + 1000), () -> {
            int nivelAlvo = center.getY() - profundidade + 2;
            int minY = Math.max(10, nivelAlvo - 5);
            nivelarSuperficieEmLotes(level, center, raioDesolacao, raioCratera, nivelAlvo);
            //ta otimo
//            for (int x = -raioDesolacao; x <= raioDesolacao; x++) {
//                for (int z = -raioDesolacao; z <= raioDesolacao; z++) {
//                    BlockPos pos = center.offset(x, 0, z);
//                    double dist = center.distSqr(pos);
//                    if (dist <= raioDesolacao * raioDesolacao && dist > raioCratera * raioCratera) {
//                        // Limpa tudo acima do nivelAlvo
//                        for (int y = nivelAlvo + 64; y >= nivelAlvo; y--) {
//                            BlockPos colPos = new BlockPos(pos.getX(), y, pos.getZ());
//                            if (!level.getBlockState(colPos).isAir()) {
//                                level.setBlock(colPos, Blocks.AIR.defaultBlockState(), 3);
//                            }
//                        }
//                        // Substitui a superfície no nivelAlvo
//                        BlockPos surfPos = new BlockPos(pos.getX(), nivelAlvo - 1, pos.getZ());
//                        float rand = level.random.nextFloat();
//                        if (rand < 0.8f) {
//                            level.setBlock(surfPos, Blocks.SAND.defaultBlockState(), 3);
//                        } else if (rand < 0.9f) {
//                            level.setBlock(surfPos, Blocks.GLASS.defaultBlockState(), 3);
//                        } else {
//                            level.setBlock(surfPos, Blocks.SANDSTONE.defaultBlockState(), 3);
//                        }
//                        if (level.random.nextFloat() < 0.03f) {
//                            level.setBlock(surfPos.above(), Blocks.DEAD_BUSH.defaultBlockState(), 3);
//                        }
//                        if (level.random.nextFloat() < 0.01f) {
//                            level.setBlock(surfPos.above(), Blocks.FIRE.defaultBlockState(), 3);
//                        }
//                    }
//                }
//            }
        });


        //Menos lag, age na superficie
//        TickScheduler.scheduleFromHere(Duration.ofMillis(delay + 1000), () -> {
//            for (int x = -raioDesolacao; x <= raioDesolacao; x++) {
//                for (int z = -raioDesolacao; z <= raioDesolacao; z++) {
//                    BlockPos pos = center.offset(x, 0, z);
//                    double dist = center.distSqr(pos);
//                    if (dist <= raioDesolacao * raioDesolacao && dist > raioCratera * raioCratera) {
//                        // Descobre a superfície real do mundo nesse ponto
//                        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
//                        int profundidadeExtra = 2 + (level.random.nextFloat() < 0.1f ? level.random.nextInt(3) : 0);
//                        // Limpa só acima da superfície e um pouco abaixo
//                        for (int y = surfaceY + 16; y >= surfaceY - profundidadeExtra; y--) {
//                            BlockPos colPos = new BlockPos(pos.getX(), y, pos.getZ());
//                            if (!level.getBlockState(colPos).isAir()) {
//                                level.setBlock(colPos, Blocks.AIR.defaultBlockState(), 3);
//                            }
//                        }
//                        // Substitui a superfície
//                        BlockPos surfPos = new BlockPos(pos.getX(), surfaceY - 1, pos.getZ());
//                        float rand = level.random.nextFloat();
//                        if (rand < 0.8f) {
//                            level.setBlock(surfPos, Blocks.SAND.defaultBlockState(), 3);
//                        } else if (rand < 0.9f) {
//                            level.setBlock(surfPos, Blocks.GLASS.defaultBlockState(), 3);
//                        } else {
//                            level.setBlock(surfPos, Blocks.SANDSTONE.defaultBlockState(), 3);
//                        }
//                        // Dead bush e fogo aleatório
//                        if (level.random.nextFloat() < 0.03f) {
//                            level.setBlock(surfPos.above(), Blocks.DEAD_BUSH.defaultBlockState(), 3);
//                        }
//                        if (level.random.nextFloat() < 0.01f) {
//                            level.setBlock(surfPos.above(), Blocks.FIRE.defaultBlockState(), 3);
//                        }
//                    }
//                }
//            }
//        });


        //TA bom esse aqui
//        TickScheduler.scheduleFromHere(Duration.ofMillis(delay + 1000), () -> {
//            int nivel = center.getY() - profundidade + 2;
//            for (int x = -raioDesolacao; x <= raioDesolacao; x++) {
//                for (int z = -raioDesolacao; z <= raioDesolacao; z++) {
//                    BlockPos pos = center.offset(x, nivel - center.getY(), z);
//                    double dist = center.distSqr(pos);
//                    if (dist <= raioDesolacao * raioDesolacao && dist > raioCratera * raioCratera) {
//                        for (int y = nivel + 32; y >= nivel; y--) {
//                            BlockPos colPos = new BlockPos(pos.getX(), y, pos.getZ());
//                            if (!level.getBlockState(colPos).isAir()) {
//                                level.setBlock(colPos, Blocks.AIR.defaultBlockState(), 3);
//                            }
//                        }
//                        float rand = level.random.nextFloat();
//                        if (rand < 0.8f) {
//                            level.setBlock(pos, Blocks.SAND.defaultBlockState(), 3);
//                        } else if (rand < 0.9f) {
//                            level.setBlock(pos, Blocks.GLASS.defaultBlockState(), 3);
//                        } else {
//                            level.setBlock(pos, Blocks.SANDSTONE.defaultBlockState(), 3);
//                        }
//                        if (level.random.nextFloat() < 0.03f) {
//                            BlockPos above = pos.above();
//                            level.setBlock(above, Blocks.DEAD_BUSH.defaultBlockState(), 3);
//                        }
//                        if (level.random.nextFloat() < 0.01f) {
//                            BlockPos above = pos.above();
//                            level.setBlock(above, Blocks.FIRE.defaultBlockState(), 3);
//                        }
//                    }
//                }
//            }
//        });

        player.sendSystemMessage(Component.literal("§4§l>> COLLAPSING STAR ROARING CANNON FIRED! <<"));
    }

    private void nivelarSuperficieEmLotes(ServerLevel level, BlockPos center, int raioDesolacao, int raioCratera, int nivelAlvo) {
        int loteTamanho = 100; // Lote ainda menor
        Queue<BlockPos> blocosParaProcessar = new LinkedList<>();
        for (int x = -raioDesolacao; x <= raioDesolacao; x++) {
            for (int z = -raioDesolacao; z <= raioDesolacao; z++) {
                BlockPos pos = center.offset(x, 0, z);
                double dist = center.distSqr(pos);
                if (dist <= raioDesolacao * raioDesolacao && dist > raioCratera * raioCratera) {
                    blocosParaProcessar.add(pos);
                }
            }
        }
        processarLote(level, blocosParaProcessar, loteTamanho, nivelAlvo);
    }

    private void processarLote(ServerLevel level, Queue<BlockPos> blocos, int loteTamanho, int nivelAlvo) {
        for (int i = 0; i < loteTamanho && !blocos.isEmpty(); i++) {
            BlockPos pos = blocos.poll();
            for (int y = nivelAlvo + 64; y >= nivelAlvo; y--) {
                BlockPos colPos = new BlockPos(pos.getX(), y, pos.getZ());
                if (!level.getBlockState(colPos).isAir()) {
                    level.setBlock(colPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
            BlockPos surfPos = new BlockPos(pos.getX(), nivelAlvo - 1, pos.getZ());
            float rand = level.random.nextFloat();
            if (rand < 0.8f) {
                level.setBlock(surfPos, Blocks.SAND.defaultBlockState(), 3);
            } else if (rand < 0.9f) {
                level.setBlock(surfPos, Blocks.GLASS.defaultBlockState(), 3);
            } else {
                level.setBlock(surfPos, Blocks.SANDSTONE.defaultBlockState(), 3);
            }
            if (level.random.nextFloat() < 0.03f) {
                level.setBlock(surfPos.above(), Blocks.DEAD_BUSH.defaultBlockState(), 3);
            }
            if (level.random.nextFloat() < 0.01f) {
                level.setBlock(surfPos.above(), Blocks.FIRE.defaultBlockState(), 3);
            }
        }
        if (!blocos.isEmpty()) {
            TickScheduler.scheduleFromHere(Duration.ofMillis(100), () ->
                    processarLote(level, blocos, loteTamanho, nivelAlvo)
            );
        }
    }

    @Override
    public void renderName(int width, int height, Font font, GuiGraphics guiGraphics, int defaultReduce, int defaultAdd) {
        guiGraphics.drawString(font, Component.translatable("skill.boros.collapsing_star_cannon"),
                width / 2 - defaultReduce, height / 2 + defaultAdd, Color.YELLOW.getRGB(), false);
    }
}