package com.onepunchcrafts.v3.minecraft;

import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.v3.OnePunchV3;
import com.onepunchcrafts.v3.api.Id;
import com.onepunchcrafts.v3.api.effect.EffectSpec;
import com.onepunchcrafts.v3.content.SaitamaContent;
import com.onepunchcrafts.v3.core.state.EffectContainer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Effect Adapter for entities that do not own a PowerState capability. */
@Mod.EventBusSubscriber
public final class MinecraftEffectRuntime {
    private static final Map<UUID, Entry> ACTIVE = new HashMap<>();

    private MinecraftEffectRuntime() {}

    public static void apply(LivingEntity target, Id effectId) {
        if (!(target.level() instanceof ServerLevel level)) return;
        Entry entry = ACTIVE.computeIfAbsent(target.getUUID(), ignored -> new Entry(level, new EffectContainer()));
        entry.effects.apply(OnePunchV3.REGISTRIES.effects.require(effectId), level.getGameTime(),
                operation -> execute(target, operation));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Iterator<Map.Entry<UUID, Entry>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Entry> active = iterator.next();
            if (!(active.getValue().level.getEntity(active.getKey()) instanceof LivingEntity target)) {
                iterator.remove();
                continue;
            }
            active.getValue().effects.tick(active.getValue().level.getGameTime(), operation -> execute(target, operation));
            if (active.getValue().effects.count(SaitamaContent.EFFECT_PUNCHED) == 0) iterator.remove();
        }
    }

    private static void execute(LivingEntity target, EffectSpec.Operation operation) {
        if (operation instanceof EffectSpec.Operation.Cue cue && cue.cue().equals(SaitamaContent.CUE_DELAYED_EXPLOSION)) {
            HelpUtility.explodeWithoutKnockBackFor(target, target.getX(), target.getY(), target.getZ(), 5);
        } else if (target instanceof ServerPlayer player) {
            if (operation instanceof EffectSpec.Operation.AddTag add) HelpUtility.getSkillData(player).getPowerState().tags().add(add.tag());
            if (operation instanceof EffectSpec.Operation.RemoveTag remove) HelpUtility.getSkillData(player).getPowerState().tags().remove(remove.tag());
            if (operation instanceof EffectSpec.Operation.AttributeDelta delta) {
                var attributes = HelpUtility.getSkillData(player).getPowerState().attributes();
                attributes.setBase(delta.attribute(), attributes.base(delta.attribute()) + delta.amount());
            }
            if (operation instanceof EffectSpec.Operation.ResourceDelta delta)
                HelpUtility.getSkillData(player).getPowerState().resources().add(delta.resource(), delta.amount());
        }
    }

    private record Entry(ServerLevel level, EffectContainer effects) {}
}
