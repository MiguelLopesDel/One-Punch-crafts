package com.onepunchcrafts.common.skills.boros;

import com.mojang.logging.LogUtils;
import com.onepunchcrafts.common.skills.*;
import com.onepunchcrafts.common.skills.sync.SyncStrategy;
import com.onepunchcrafts.common.skills.sync.Syncable;
import com.onepunchcrafts.common.skills.sync.SyncableSkillPack;
import com.onepunchcrafts.api.Id;
import com.onepunchcrafts.content.BorosContent;
import com.onepunchcrafts.content.SaitamaContent;
import com.onepunchcrafts.constant.NbtBooleanValues;
import com.onepunchcrafts.runtime.combat.BorosMitigationInterceptor;
import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.util.TickScheduler;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class BorosPack extends SyncableSkillPack {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean DEBUG_MOVEMENT = false;

    @Getter
    private final BorosConfig config = new BorosConfig();

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
    private final float MAX_HEALTH = (float) BorosMitigationInterceptor.MAX_HEALTH;
    private final double BASE_ATTACK_DAMAGE = 100_000.0;
    // Anything above this is Serious Punch territory and bypasses Boros' durability.
    private static final float SERIOUS_DAMAGE_THRESHOLD = 1.0e12f;
    private static final float MAX_DAMAGE_FRACTION_PER_HIT = 0.08f;

    @Setter
    @Getter
    @Syncable(key = "tickstoultraregeneration", strategy = SyncStrategy.SERVER_AUTHORITY)
    private int ticksToUseUltraRegeneration = 0;

    @Setter
    @Getter
    @Syncable(key = "borosform", strategy = SyncStrategy.VALIDATED)
    private short currentForm = 0; // 0: Armored, 1: Released, 2: Meteoric Burst

    @Setter
    @Getter
    @Syncable(key = "borosenergy", strategy = SyncStrategy.SERVER_AUTHORITY)
    private float energy = BorosConfig.MAX_ENERGY;

    @Setter
    @Getter
    @Syncable(key = "borosmeteoricmode", strategy = SyncStrategy.TOGGLE)
    private boolean meteoricBurstActive = false;

    @Setter
    @Getter
    @Syncable(key = "borosflight", strategy = SyncStrategy.TOGGLE)
    private boolean flightActive = false;

    @Setter
    @Getter
    @Syncable(key = "borosdestruction", strategy = SyncStrategy.TOGGLE)
    private boolean destructiveMode = true;

    private boolean infiniteEnergy = false;

    @Setter
    @Getter
    @Syncable(key = "borosburststepcooldown", strategy = SyncStrategy.SERVER_AUTHORITY)
    private int burstStepCooldown = 0;

    @Setter
    @Getter
    @Syncable(key = "boroscombowindow", strategy = SyncStrategy.SERVER_AUTHORITY)
    private int comboWindowTicks = 0;

    // Variáveis internas para controle da regeneração (não precisam de sync pois o efeito é no servidor)
    private int activeRegenTicks = 0;
    // Canon: Boros reassembles his whole body in seconds — restores 25% of max HP over 5s.
    private final float REGEN_TOTAL_AMOUNT = MAX_HEALTH * 0.25f;

    // === Sistema de Movimentação Adaptativa ===
    private int ticksInPit = 0;
    private int airJumpsUsed = 0;
    private int coyoteTimeTicks = 0;
    private boolean wasOnGround = false;
    private int launchCooldown = 0;
    private double prevX = Double.NaN;
    private double prevZ = Double.NaN;
    private double prevY = Double.NaN;
    private static final int COYOTE_TIME = 6;
    private static final int PIT_DETECTION_THRESHOLD = 20;// 1 segundo preso = ativa launch

    // ... existing code ...

    // === Sistema de Movimentação Adaptativa ===
      // Posição Y do tick anterior
    private int ticksNotMovingHorizontally = 0; // Conta ticks parado (para detectar "preso")

    private boolean inputForward;
    private boolean inputBackward;
    private boolean inputLeft;
    private boolean inputRight;
    private boolean inputJump;
    private boolean inputSneak;
    private boolean inputSprint;


    @Override
    protected @NotNull List<List<Skill>> initializeSkills() {
        List<List<Skill>> groupList = new ArrayList<>();

        ArrayList<Skill> basicSkills = new ArrayList<>();
        basicSkills.add(0, new BorosRegeneration(this));
        basicSkills.add(1, new BorosEnergyProjection(this));
        basicSkills.add(2, new BorosFlight(this));
        basicSkills.add(3, new BorosDestructiveMovement(this));
        basicSkills.add(4, new BorosDash(this));

        ArrayList<Skill> transformSkills = new ArrayList<>();
        transformSkills.add(0, new BorosForm(this));
        transformSkills.add(1, new BorosMeteoricBurst(this));

        ArrayList<Skill> ultimateSkills = new ArrayList<>();
        ultimateSkills.add(0, new BorosRoaringCannon(this));
        ultimateSkills.add(1, new BorosCollapsingStarCannon(this));

        groupList.add(basicSkills);
        groupList.add(transformSkills);
        groupList.add(ultimateSkills);

        return groupList;
    }

    public float getPowerModifier() {
        return switch (currentForm) {
            case 0 -> 1.0f;
            case 1 -> 2.5f;
            case 2 -> 10.0f;
            default -> 1.0f;
        };
    }

    /** Adapter used by the new radial Technique IDs; it does not mutate legacy selection. */
    public void executeTechnique(Id technique, Player player) {
        if (technique.equals(BorosContent.REGENERATION)) skills.get(0).get(0).execute(player);
        else if (technique.equals(BorosContent.ENERGY_PROJECTION)) skills.get(0).get(1).execute(player);
        else if (technique.equals(BorosContent.FLIGHT)) skills.get(0).get(2).execute(player);
        else if (technique.equals(BorosContent.DESTRUCTIVE_MOVEMENT)) skills.get(0).get(3).execute(player);
        else if (technique.equals(BorosContent.DASH)) skills.get(0).get(4).execute(player);
        else if (technique.equals(BorosContent.FORM)) skills.get(1).get(0).execute(player);
        else if (technique.equals(BorosContent.METEORIC_BURST)) skills.get(1).get(1).execute(player);
        else if (technique.equals(BorosContent.ROARING_CANNON)) skills.get(2).get(0).execute(player);
        else if (technique.equals(BorosContent.CSRC)) skills.get(2).get(1).execute(player);
    }

    public boolean consumeEnergy(float amount) {
        if (infiniteEnergy) {
            config.setExhausted(false);
            energy = BorosConfig.MAX_ENERGY;
            return true;
        }
        if (config.isExhausted()) return false;

        if (energy >= amount) {
            energy -= amount;

            if (energy <= 0) {
                energy = 0;
                config.setExhausted(true);
                config.setExhaustedTimestamp(System.currentTimeMillis() / 50);

                if (meteoricBurstActive) {
                    setMeteoricBurstActive(false);
                    setCurrentForm((short) 1);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void execute(Player player) {
        Skill skill = getCurrentSkill();
        if (skill != null) {
            skill.execute(player);
        }
    }

    @Override
    public void renderSkills(int width, int height, Font font, GuiGraphics guiGraphics) {
        getCurrentSkill().renderName(width + 80, height, font, guiGraphics,
                (int) (width * 0.05), (int) (height * 0.25));
    }

    @Override
    public void playerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        event.getEntity().getAttribute(Attributes.MAX_HEALTH).setBaseValue(this.getMAX_HEALTH());
        event.getEntity().setHealth(this.getMAX_HEALTH());

        this.energy = BorosConfig.MAX_ENERGY;
        this.config.setExhausted(false);
        this.setCurrentForm((short) 0);
        this.setMeteoricBurstActive(false);
        this.setFlightActive(false);
        this.burstStepCooldown = 0;
        this.comboWindowTicks = 0;
        this.setMovementInput(false, false, false, false, false, false, false);
    }

    @Override
    public Tag writeNBT() {
        CompoundTag nbt = (CompoundTag) super.writeNBT();
        nbt.putBoolean(NbtBooleanValues.breakBlocksQuickly.getValue(), this.isBreakBlocksQuickly());
        nbt.putShort(NbtBooleanValues.superSpeed.getValue(), this.speed);
        nbt.putShort("borosknockbackresistance", this.knockbackResistance);
        nbt.putShort("borosattackknockback", this.attackKnockback);
        nbt.putShort("borosswimspeed", this.swimSpeed);
        nbt.putInt("tickstoultraregeneration", this.ticksToUseUltraRegeneration);
        nbt.putShort("borosform", this.currentForm);
        nbt.putFloat("borosenergy", this.energy);
        nbt.putBoolean("borosexhausted", this.config.isExhausted());
        nbt.putLong("exhaustedtimestamp", this.config.getExhaustedTimestamp());
        nbt.putBoolean("meteoricburstactive", this.meteoricBurstActive);
        nbt.putBoolean("flightactive", this.flightActive);
        nbt.putBoolean("borosdestruction", this.destructiveMode);
        nbt.putInt("borosburststepcooldown", this.burstStepCooldown);
        nbt.putInt("boroscombowindow", this.comboWindowTicks);
        return nbt;
    }

    @Override
    public void readNBT(Tag tag) {
        super.readNBT(tag);
        CompoundTag nbt = (CompoundTag) tag;
        this.breakBlocksQuickly = nbt.getBoolean(NbtBooleanValues.breakBlocksQuickly.getValue());
        this.speed = nbt.getShort(NbtBooleanValues.superSpeed.getValue());
        this.knockbackResistance = nbt.getShort("borosknockbackresistance");
        this.attackKnockback = nbt.getShort("borosattackknockback");
        this.swimSpeed = nbt.getShort("borosswimspeed");
        this.ticksToUseUltraRegeneration = nbt.getInt("tickstoultraregeneration");
        this.currentForm = nbt.getShort("borosform");
        this.energy = nbt.getFloat("borosenergy");
        this.config.setExhausted(nbt.getBoolean("borosexhausted"));
        this.config.setExhaustedTimestamp(nbt.getLong("exhaustedtimestamp"));
        this.meteoricBurstActive = nbt.getBoolean("meteoricburstactive");
        this.flightActive = nbt.getBoolean("flightactive");
        this.destructiveMode = nbt.getBoolean("borosdestruction");
        this.burstStepCooldown = nbt.getInt("borosburststepcooldown");
        this.comboWindowTicks = nbt.getInt("boroscombowindow");
    }

    private void decreaseTimeToUltraRegeneration() {
        if (ticksToUseUltraRegeneration > 0)
            ticksToUseUltraRegeneration--;
    }

    @Override
    public int getMaxNumSkill() {
        if (skills.isEmpty() || getCurrentGroupIndex() >= skills.size()) return 0;
        return skills.get(getCurrentGroupIndex()).size() - 1;
    }

    @Override
    public void syncVariables(SkillPack serverData) {

    }

    @Override
    public void adjustAbility(double scrollDelta) {
    }


    @Override
    public void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer serverPlayer) {
            decreaseTimeToUltraRegeneration();
            handleEnergySystem(serverPlayer);
            handleBorosMovement(serverPlayer);
            handleUltraRegenerationTick(serverPlayer);
            handlePassiveRegeneration(serverPlayer);
            manageEffectsAndAttributes(event);
            handleMeteoricBurstEffects(event);
            this.tickSync(serverPlayer);
        }
        super.tick(event);
    }

    public void setMovementInput(boolean forward, boolean backward, boolean left, boolean right,
                                 boolean jump, boolean sneak, boolean sprint) {
        this.inputForward = forward;
        this.inputBackward = backward;
        this.inputLeft = left;
        this.inputRight = right;
        this.inputJump = jump;
        this.inputSneak = sneak;
        this.inputSprint = sprint;
    }

    public boolean canUseBurstStep() {
        return burstStepCooldown <= 0 && !config.isExhausted();
    }

    public void markBurstStepUsed(int cooldownTicks) {
        this.burstStepCooldown = cooldownTicks;
        this.comboWindowTicks = currentForm == 2 ? 16 : 10;
    }

    private void handleBorosMovement(ServerPlayer player) {
        if (burstStepCooldown > 0) burstStepCooldown--;
        if (comboWindowTicks > 0) comboWindowTicks--;

        if (player.isSpectator()) {
            resetMovementState();
            return;
        }

        if (flightActive) {
            handleEnergyPropulsion(player);
        } else {
            handleGroundAndAirMomentum(player);
        }
    }

    private void handleEnergyPropulsion(ServerPlayer player) {
        float tickCost = BorosConfig.FLIGHT_COST * switch (currentForm) {
            case 0 -> 0.75f;
            case 1 -> 1.2f;
            case 2 -> 2.5f;
            default -> 1.0f;
        };

        if (!consumeEnergy(tickCost)) {
            setFlightActive(false);
            player.sendSystemMessage(Component.translatable("skill.boros.flight.no_energy"));
            return;
        }

        Vec3 desired = getInputVector(player);
        if (inputJump) desired = desired.add(0, 1, 0);
        if (inputSneak) desired = desired.add(0, -1, 0);

        Vec3 motion = player.getDeltaMovement();
        double acceleration = switch (currentForm) {
            case 0 -> 0.16;
            case 1 -> 0.28;
            case 2 -> 0.48;
            default -> 0.16;
        };
        double maxSpeed = switch (currentForm) {
            case 0 -> 1.25;
            case 1 -> 2.75;
            case 2 -> 5.25;
            default -> 1.25;
        };
        if (inputSprint) {
            acceleration *= 1.35;
            maxSpeed *= 1.25;
        }

        Vec3 nextMotion;
        if (desired.lengthSqr() > 0.001) {
            Vec3 target = desired.normalize().scale(maxSpeed);
            nextMotion = motion.lerp(target, acceleration);
        } else {
            nextMotion = new Vec3(motion.x * 0.78, motion.y * 0.55, motion.z * 0.78);
        }

        player.setDeltaMovement(nextMotion);
        player.fallDistance = 0;
        player.hurtMarked = true;

        double speed = nextMotion.length();
        if (destructiveMode && currentForm > 0 && inputJump && nextMotion.y > 0.08) {
            breakCeilingWhileAscending(player, nextMotion);
        }

        if (speed > 0.25) {
            destructiveWake(player, 0.75 + currentForm * 0.85 + Math.min(speed, 5.0) * 0.25, speed);
            impactEntities(player, nextMotion, 0.8 + currentForm * 0.45);
        }

        if (player.level().getGameTime() % 2 == 0) {
            ServerLevel level = player.serverLevel();
            level.sendParticles(currentForm == 2 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.DRAGON_BREATH,
                    player.getX() - nextMotion.x * 0.6,
                    player.getY() + 0.7 - nextMotion.y * 0.3,
                    player.getZ() - nextMotion.z * 0.6,
                    currentForm == 2 ? 12 : 6,
                    0.25, 0.25, 0.25, 0.08 + speed * 0.02);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    player.getX(), player.getY() + 0.9, player.getZ(),
                    currentForm == 2 ? 5 : 2, 0.3, 0.3, 0.3, 0.08);
        }
    }

    private void breakCeilingWhileAscending(ServerPlayer player, Vec3 motion) {
        ServerLevel level = player.serverLevel();
        double height = currentForm == 2 ? 5.5 : 3.25;
        double radius = currentForm == 2 ? 1.8 : 1.15;
        AABB ceilingArea = player.getBoundingBox()
                .move(0, Math.max(0.35, motion.y * 0.5), 0)
                .expandTowards(0, height, 0)
                .inflate(radius, 0.15, radius);
        int maxBlocks = currentForm == 2 ? 240 : 90;
        int broken = 0;

        int minX = (int) Math.floor(ceilingArea.minX);
        int minY = (int) Math.floor(ceilingArea.minY);
        int minZ = (int) Math.floor(ceilingArea.minZ);
        int maxX = (int) Math.ceil(ceilingArea.maxX);
        int maxY = (int) Math.ceil(ceilingArea.maxY);
        int maxZ = (int) Math.ceil(ceilingArea.maxZ);

        for (int y = minY; y <= maxY && broken < maxBlocks; y++) {
            for (int x = minX; x <= maxX && broken < maxBlocks; x++) {
                for (int z = minZ; z <= maxZ && broken < maxBlocks; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || state.getDestroySpeed(level, pos) < 0) continue;
                    level.destroyBlock(pos, currentForm < 2, player);
                    broken++;
                }
            }
        }

        if (broken > 0 && level.getGameTime() % 3 == 0) {
            level.sendParticles(ParticleTypes.EXPLOSION,
                    player.getX(), player.getY() + player.getBbHeight() + 0.25, player.getZ(),
                    currentForm == 2 ? 3 : 1, radius * 0.35, 0.35, radius * 0.35, 0.2);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 0.8f, currentForm == 2 ? 0.8f : 1.1f);
        }
    }

    private void handleGroundAndAirMomentum(ServerPlayer player) {
        Vec3 motion = player.getDeltaMovement();
        Vec3 input = getInputVector(player);
        boolean hasInput = input.lengthSqr() > 0.001;

        if (currentForm > 0 && hasInput) {
            double acceleration = (inputSprint || player.isSprinting()) ? 0.09 : 0.045;
            acceleration *= currentForm == 2 ? 1.85 : 1.0;
            Vec3 boost = input.normalize().scale(acceleration);
            Vec3 nextMotion = motion.add(boost.x, player.onGround() ? 0 : boost.y * 0.25, boost.z);
            double cap = currentForm == 2 ? 3.2 : 1.75;
            double horizontal = Math.sqrt(nextMotion.x * nextMotion.x + nextMotion.z * nextMotion.z);
            if (horizontal > cap) {
                double scale = cap / horizontal;
                nextMotion = new Vec3(nextMotion.x * scale, nextMotion.y, nextMotion.z * scale);
            }
            player.setDeltaMovement(nextMotion);
            player.hurtMarked = true;
            motion = nextMotion;
        }

        if (!player.onGround() && currentForm > 0 && hasInput) {
            Vec3 air = input.normalize().scale(currentForm == 2 ? 0.08 : 0.04);
            player.setDeltaMovement(player.getDeltaMovement().add(air.x, 0, air.z));
            player.hurtMarked = true;
        }

        double speed = player.getDeltaMovement().horizontalDistance();
        if (currentForm > 0 && (speed > 0.45 || player.isSprinting())) {
            destructiveWake(player, currentForm == 2 ? 1.9 : 1.1, speed);
            impactEntities(player, player.getDeltaMovement(), currentForm == 2 ? 1.5 : 1.0);
        }
    }

    private Vec3 getInputVector(ServerPlayer player) {
        Vec3 look = player.getLookAngle().multiply(1, 0, 1);
        if (look.lengthSqr() < 0.001) look = Vec3.directionFromRotation(0, player.getYRot()).multiply(1, 0, 1);
        look = look.normalize();
        Vec3 right = new Vec3(-look.z, 0, look.x);

        Vec3 movement = Vec3.ZERO;
        if (inputForward) movement = movement.add(look);
        if (inputBackward) movement = movement.subtract(look);
        if (inputRight) movement = movement.add(right);
        if (inputLeft) movement = movement.subtract(right);
        return movement;
    }

    private void destructiveWake(ServerPlayer player, double radius, double speed) {
        if (!destructiveMode || currentForm <= 0) return;

        ServerLevel level = player.serverLevel();
        AABB area = player.getBoundingBox().inflate(radius);
        int maxBlocks = currentForm == 2 ? 180 : 65;
        int broken = 0;

        int minX = (int) Math.floor(area.minX);
        int minY = (int) Math.floor(area.minY);
        int minZ = (int) Math.floor(area.minZ);
        int maxX = (int) Math.ceil(area.maxX);
        int maxY = (int) Math.ceil(area.maxY);
        int maxZ = (int) Math.ceil(area.maxZ);

        for (int x = minX; x <= maxX && broken < maxBlocks; x++) {
            for (int y = minY; y <= maxY && broken < maxBlocks; y++) {
                for (int z = minZ; z <= maxZ && broken < maxBlocks; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || state.getDestroySpeed(level, pos) < 0) continue;

                    double dx = pos.getX() + 0.5 - player.getX();
                    double dy = pos.getY() + 0.5 - player.getY();
                    double dz = pos.getZ() + 0.5 - player.getZ();
                    if ((dx * dx + dy * dy + dz * dz) > radius * radius) continue;

                    level.destroyBlock(pos, currentForm < 2, player);
                    broken++;
                }
            }
        }

        if (broken > 0 && level.getGameTime() % 4 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS,
                    0.25f + currentForm * 0.2f, 1.4f);
            level.sendParticles(ParticleTypes.EXPLOSION,
                    player.getX(), player.getY() + 0.6, player.getZ(),
                    currentForm == 2 ? 3 : 1, radius * 0.2, 0.2, radius * 0.2, Math.min(speed, 2.0));
        }
    }

    private void impactEntities(ServerPlayer player, Vec3 velocity, double inflate) {
        if (velocity.lengthSqr() < 0.04) return;
        ServerLevel level = player.serverLevel();
        AABB box = player.getBoundingBox().inflate(inflate);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive())) {
            float damage = (float) Math.max(20.0, velocity.length() * BASE_ATTACK_DAMAGE * 0.025 * getPowerModifier());
            target.hurt(level.damageSources().playerAttack(player), damage);
            Vec3 knockback = velocity.normalize().scale(0.8 + getPowerModifier() * 0.2);
            target.setDeltaMovement(knockback.x, 0.35 + currentForm * 0.2, knockback.z);
            target.hurtMarked = true;
        }
    }

    private void handleAdaptiveMovement(ServerPlayer player) {
        if (player.isSpectator() || player.getAbilities().flying || flightActive) {
            resetMovementState();
            updatePreviousPosition(player);
            return;
        }

        boolean onGround = player.onGround();
        boolean inWater = player.isInWater();
        Vec3 motion = player.getDeltaMovement();

        // Detecção de movimento REAL via delta de posição (confiável no servidor)
        double dx = Double.isNaN(prevX) ? 0 : player.getX() - prevX;
        double dz = Double.isNaN(prevZ) ? 0 : player.getZ() - prevZ;
        double dy = Double.isNaN(prevY) ? 0 : player.getY() - prevY;
        double horizontalSpeed = Math.sqrt(dx * dx + dz * dz);
        boolean isMovingHorizontally = horizontalSpeed > 0.005;

        // horizontalCollision é TRUE quando o jogador empurra contra uma parede (confiável no servidor)
        boolean isTryingToMove = player.horizontalCollision;

        // Intent combinado
        boolean hasMovementIntent = isMovingHorizontally || isTryingToMove || player.isSprinting();

        if (DEBUG_MOVEMENT && player.tickCount % 20 == 0 && (!onGround || hasMovementIntent)) {
            LOGGER.info("BorosMove: Ground={}, Collision={}, HSpeed={}, DY={}, Intent={}, Pit={}",
                    onGround, player.horizontalCollision,
                    String.format("%.4f", horizontalSpeed),
                    String.format("%.4f", dy),
                    hasMovementIntent, ticksInPit);
        }

        if (launchCooldown > 0) launchCooldown--;

        // Atualiza estados de chão
        if (onGround) {
            coyoteTimeTicks = COYOTE_TIME;
            airJumpsUsed = 0;
            // Só reseta pit se NÃO está colidindo (se está colidindo no chão, pode estar preso)
            if (!isTryingToMove) {
                ticksInPit = 0;
            }
        } else {
            if (coyoteTimeTicks > 0) coyoteTimeTicks--;
        }

        // === Camada 2: Auto-Vault ===
        if (player.horizontalCollision && !onGround && !inWater && hasMovementIntent) {
            handleAutoVault(player, motion);
        }

        // === Camada 3: Ravine Escape ===
        boolean stuckCondition = false;

        // No ar: caindo e colidindo com paredes
        if (!onGround && !inWater && player.horizontalCollision && motion.y <= 0) {
            stuckCondition = true;
        }
        // No ar: caindo rápido em ravina aberta
        if (!onGround && !inWater && motion.y < -0.5 && dy < -0.3 && hasMovementIntent) {
            stuckCondition = true;
        }
        // No chão: empurrando contra parede (preso no fundo de um buraco)
        if (onGround && isTryingToMove) {
            stuckCondition = true;
        }

        if (stuckCondition && isTrappedInPit(player)) {
            ticksInPit++;
            if (DEBUG_MOVEMENT && ticksInPit % 5 == 0) {
                LOGGER.info("RavineDetect: ticksInPit={}/{}", ticksInPit, PIT_DETECTION_THRESHOLD);
            }
//            if (ticksInPit >= PIT_DETECTION_THRESHOLD && launchCooldown <= 0) {
                handleRavineEscape(player);
//            }
        } else if (!stuckCondition) {
            ticksInPit = Math.max(0, ticksInPit - 1);
        }

        // === Camada 4: Air Control ===
        if (!onGround && !inWater) {
            handleAirControl(player, motion, dx, dz);
        }

        wasOnGround = onGround;
        updatePreviousPosition(player);
    }

    private void updatePreviousPosition(ServerPlayer player) {
        prevX = player.getX();
        prevY = player.getY();
        prevZ = player.getZ();
    }

    /**
     * Camada 2: Auto-Vault.
     * Escaneia a parede à frente para encontrar a borda e aplica impulso.
     */
    private void handleAutoVault(ServerPlayer player, Vec3 motion) {
        BlockPos playerPos = player.blockPosition();
        BlockPos posFrente = playerPos.relative(player.getDirection());

        int maxScanHeight = switch (currentForm) {
            case 0 -> 12;
            case 1 -> 40;
            case 2 -> 100;
            default -> 12;
        };

        int wallHeight = -1;
        for (int y = 0; y <= maxScanHeight; y++) {
            BlockPos checkPos = posFrente.above(y);
            if (player.level().isEmptyBlock(checkPos) && player.level().isEmptyBlock(checkPos.above())) {
                wallHeight = y;
                break;
            }
        }

        if (wallHeight >= 0) {
            // Vault: impulso vertical proporcional à altura da parede
            double vaultStrength = 0.6 + (wallHeight * 0.15);
            double formBonus = switch (currentForm) {
                case 0 -> 6.0;
                case 1 -> 8.4;
                case 2 -> 12.8;
                default -> 6.0;
            };
            vaultStrength *= formBonus;

            double horizontalBoost = player.isSprinting() ? 0.3 : 0.15;
            Vec3 lookHorizontal = player.getLookAngle().multiply(1, 0, 1).normalize();

            player.setDeltaMovement(
                    lookHorizontal.x * horizontalBoost + motion.x * 0.5,
                    vaultStrength,
                    lookHorizontal.z * horizontalBoost + motion.z * 0.5
            );
            player.fallDistance = 0;
            player.hurtMarked = true;

            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.CLOUD,
                        player.getX(), player.getY() + 0.5, player.getZ(),
                        4, 0.2, 0.1, 0.2, 0.02);
            }
            if (DEBUG_MOVEMENT) LOGGER.info("AutoVault: EXECUTADO com força {}", vaultStrength);
        } else {
            // Parede mais alta que maxScanHeight: Wall Run
            handleWallRun(player, motion);
        }
    }

    private void handleWallRun(ServerPlayer player, Vec3 motion) {
        double climbSpeed = switch (currentForm) {
            case 0 -> 0.35;
            case 1 -> 0.55;
            case 2 -> 0.85;
            default -> 0.35;
        };

        double targetY = motion.y < 0 ? climbSpeed * 0.7 : climbSpeed;

        player.setDeltaMovement(motion.x * 0.92, targetY, motion.z * 0.92);
        player.fallDistance = 0;
        player.hurtMarked = true;

        if (player.level() instanceof ServerLevel level && player.level().getGameTime() % 3 == 0) {
            level.sendParticles(ParticleTypes.CRIT,
                    player.getX(), player.getY() + 0.2, player.getZ(),
                    2, 0.1, 0.05, 0.1, 0.01);
        }
    }

    private void handleRavineEscape(ServerPlayer player) {
        double launchPower = switch (currentForm) {
            case 0 -> 5;
            case 1 -> 6;
            case 2 -> 12;
            default -> 5;
        };

        Vec3 motion = player.getDeltaMovement();
        Vec3 lookHorizontal = player.getLookAngle().multiply(1, 0, 1).normalize();

        player.setDeltaMovement(
                lookHorizontal.x * 0.5 + motion.x,
                launchPower,
                lookHorizontal.z * 0.5 + motion.z
        );

        player.fallDistance = 0;
        player.hurtMarked = true;
        ticksInPit = 0;
        launchCooldown = 40;

        if (DEBUG_MOVEMENT) LOGGER.info("RavineEscape: JOGADOR EJETADO!");

        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(
                    currentForm == 2 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.DRAGON_BREATH,
                    player.getX(), player.getY(), player.getZ(),
                    15, 0.3, 0.1, 0.3, 0.15);
            level.sendParticles(ParticleTypes.CLOUD,
                    player.getX(), player.getY(), player.getZ(),
                    8, 0.5, 0.1, 0.5, 0.05);
            level.playSound(null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS,
                    0.7f, 1.2f + (currentForm * 0.2f));
        }
    }

    private boolean isTrappedInPit(ServerPlayer player) {
        BlockPos pos = player.blockPosition();
        int solidSidesAbove = 0;

        BlockPos[] checkPositions = {
                pos.north(2).above(2),
                pos.south(2).above(2),
                pos.east(2).above(2),
                pos.west(2).above(2)
        };

        for (BlockPos check : checkPositions) {
            if (!player.level().isEmptyBlock(check)) {
                solidSidesAbove++;
            }
        }

        boolean hasGroundBelow = false;
        for (int y = 1; y <= 10; y++) {
            if (!player.level().isEmptyBlock(pos.below(y))) {
                hasGroundBelow = true;
                break;
            }
        }

        Vec3 motion = player.getDeltaMovement();
        boolean fallingAndStuck = motion.y < -0.1 && player.horizontalCollision;

        return (solidSidesAbove >= 2 && hasGroundBelow) || fallingAndStuck;
    }

    /**
     * Camada 4: Air Control.
     * Usa dx/dz (delta de posição real) para detectar input ao invés de getDeltaMovement().
     */
    private void handleAirControl(ServerPlayer player, Vec3 motion, double dx, double dz) {
        Vec3 lookHorizontal = player.getLookAngle().multiply(1, 0, 1).normalize();
        double airControlStrength = switch (currentForm) {
            case 0 -> 0.02;
            case 1 -> 0.035;
            case 2 -> 0.05;
            default -> 0.02;
        };

        // Usa posição delta (dx/dz) — confiável no servidor
        boolean hasInput = Math.sqrt(dx * dx + dz * dz) > 0.005 || player.horizontalCollision;
        if (hasInput) {
            player.setDeltaMovement(
                    motion.x + lookHorizontal.x * airControlStrength,
                    motion.y,
                    motion.z + lookHorizontal.z * airControlStrength
            );
        }

        // Air Jump via coyote time
        int maxAirJumps = switch (currentForm) {
            case 0 -> 1;
            case 1 -> 2;
            case 2 -> 4;
            default -> 1;
        };


        boolean justJumped = !wasOnGround && motion.y > 0.2 && airJumpsUsed < maxAirJumps && coyoteTimeTicks > 0;
        if (justJumped) {
            double jumpBoost = switch (currentForm) {
                case 0 -> 0.5;
                case 1 -> 0.65;
                case 2 -> 0.85;
                default -> 0.5;
            };
            player.setDeltaMovement(motion.x, jumpBoost, motion.z);
            player.fallDistance = 0;
            coyoteTimeTicks = 0;
            airJumpsUsed++;
            player.hurtMarked = true;
            if (DEBUG_MOVEMENT) LOGGER.info("AirJump: Pulo aéreo #{} executado!", airJumpsUsed);
        }
    }

    private void resetMovementState() {
        ticksInPit = 0;
        airJumpsUsed = 0;
        coyoteTimeTicks = 0;
        launchCooldown = 0;
        wasOnGround = true;
        prevX = Double.NaN;
        prevY = Double.NaN;
        prevZ = Double.NaN;
    }

    public void startUltraRegeneration() {
        this.activeRegenTicks = 100; // 5 segundos * 20 ticks
    }

    private void handleUltraRegenerationTick(ServerPlayer player) {
        if (activeRegenTicks > 0) {
            player.heal(REGEN_TOTAL_AMOUNT / 100f); // 25% da vida em 100 ticks
            activeRegenTicks--;
        }
    }

    private void handlePassiveRegeneration(ServerPlayer player) {
        if (player.getHealth() < player.getMaxHealth()) {
            float healAmount = 0;

            // Cálculo: Vida Total / (Minutos * 60 segundos * 20 ticks)
            switch (currentForm) {
                case 0: // Armadura: 10 minutos (12000 ticks)
                    healAmount = MAX_HEALTH / 12000f;
                    break;
                case 1: // Liberado: 4 minutos (4800 ticks)
                    healAmount = MAX_HEALTH / 4800f;
                    break;
                case 2: // Meteoric Burst: 1 minuto (1200 ticks)
                    healAmount = MAX_HEALTH / 1200f;
                    break;
            }

            // Aplica a cura se for maior que 0
            if (healAmount > 0) {
                player.heal(healAmount);
            }
        }
    }

    private void handleEnergySystem(ServerPlayer player) {
        long currentTick = player.level().getGameTime();
        infiniteEnergy = player.isCreative();

        if (infiniteEnergy) {
            config.setExhausted(false);
            energy = BorosConfig.MAX_ENERGY;
            return;
        }

        if (config.isExhausted()) {
            if (config.canRecover(currentTick)) {
                config.setExhausted(false);
                energy = 1f;
                player.sendSystemMessage(Component.translatable("skill.boros.energy_restored"));
            }
            return;
        }

        // Custo passivo Meteoric Burst
        if (meteoricBurstActive && currentForm == 2) {
            if (!consumeEnergy(BorosConfig.METEORIC_BURST_TICK_COST)) {
                setMeteoricBurstActive(false);
                setCurrentForm((short) 1);
                player.sendSystemMessage(Component.translatable("skill.boros.meteoric_burst.no_energy"));
            }
            return;
        }

        // Regeneração de energia
        if (energy < BorosConfig.MAX_ENERGY) {
            float regenRate = config.getRegenerationRate(energy);
            energy = Math.min(energy + regenRate, BorosConfig.MAX_ENERGY);
        }
    }

    private void handleMeteoricBurstEffects(TickEvent.PlayerTickEvent event) {
        ServerPlayer player = (ServerPlayer) event.player;

        if (meteoricBurstActive && currentForm == 2) {
            if (player.level().getGameTime() % 4 == 0) {
                ServerLevel level = (ServerLevel) player.level();
                level.sendParticles(
                        ParticleTypes.DRAGON_BREATH,
                        player.getX(), player.getY() + 1, player.getZ(),
                        12, 0.4, 0.8, 0.4, 0.1
                );
                level.sendParticles(
                        ParticleTypes.ELECTRIC_SPARK,
                        player.getX(), player.getY() + 1, player.getZ(),
                        5, 0.5, 1.0, 0.5, 0.2
                );

                if (player.level().getGameTime() % 20 == 0) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.5f, 1.5f);
                }
            }
        }
    }

    private void manageEffectsAndAttributes(TickEvent.PlayerTickEvent event) {
        ServerPlayer player = (ServerPlayer) event.player;
        modifyAttributes(player);
        HelpUtility.applyGodLevelEffectSet(player);
    }

    private void modifyAttributes(ServerPlayer player) {
        if (player.isSpectator()) return;

        float speedMultiplier = switch (currentForm) {
            case 0 -> 1.0f;
            case 1 -> 3.0f;
            case 2 -> 8.0f;
            default -> 1.0f;
        };

        if (this.speed != 0)
            player.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue((this.speed / 9.0) * speedMultiplier);
        else
            player.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.1F * speedMultiplier);

        if (this.attackKnockback != 0)
            player.getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(this.attackKnockback * speedMultiplier);
        else
            player.getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(0);

        if (this.knockbackResistance != 0)
            player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(this.knockbackResistance);
        else
            player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0);

        if (this.swimSpeed != 0)
            player.getAttribute(ForgeMod.SWIM_SPEED.get()).setBaseValue(this.swimSpeed * speedMultiplier);
        else
            player.getAttribute(ForgeMod.SWIM_SPEED.get()).setBaseValue(1.0D);

        // Step Height (Passo de Gigante) via Atributo Forge
        // Base do Minecraft é 0.6, então subtraímos 0.6 do nosso alvo para pegar o bônus
        double stepHeightBonus = switch (currentForm) {
            case 0 -> 1.5 - 0.6;
            case 1 -> 2.5 - 0.6;
            case 2 -> 4.0 - 0.6;
            default -> 0.0;
        };
        if (player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get()) != null)
            player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get()).setBaseValue(stepHeightBonus);

        // Configuração de Dano de Ataque
        // Base 100k * Multiplicador (1x, 2.5x, 10x)
        double damage = BASE_ATTACK_DAMAGE * speedMultiplier;
        player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);

        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(this.MAX_HEALTH);
        player.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(50F);
    }

    /**
     * Boros' anime-grade durability: each form soaks a large share of incoming
     * damage and no single hit (short of a Serious Punch) can take more than a
     * slice of his health bar. Runs at LOWEST priority, after Saitama's punch
     * multipliers have been applied.
     */
    @Override
    public void manageFlux(LivingEvent event) {
        if (!(event instanceof LivingDamageEvent damageEvent)) return;
        if (!(damageEvent.getEntity() instanceof ServerPlayer target)) return;
        if (HelpUtility.getSkillData(target).getSkillPack() != this) return;
        // Saitama Strikes already crossed BorosMitigationInterceptor before
        // Minecraft's hurt() emitted this compatibility event.
        if (damageEvent.getSource().getEntity() instanceof ServerPlayer attacker
                && HelpUtility.getSkillData(attacker).getPowerState().powerSetId().equals(SaitamaContent.POWER_SET))
            return;

        float amount = damageEvent.getAmount();
        // Serious Punch territory — nothing in Boros' arsenal stops it (canon).
        if (amount >= SERIOUS_DAMAGE_THRESHOLD) return;

        // Tuned so a plain Normal Punch (10M) kills in ~20-30 hits per form.
        float taken = switch (currentForm) {
            case 0 -> 0.55f; // Armored: the armor soaks part of the blow
            case 1 -> 0.75f; // Released: armor gone, raw vitality only
            case 2 -> 0.65f; // Meteoric Burst: life force flooding the body
            default -> 0.75f;
        };
        float cap = MAX_HEALTH * MAX_DAMAGE_FRACTION_PER_HIT;
        damageEvent.setAmount(Math.min(amount * taken, cap));
    }
}
