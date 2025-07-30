//package com.onepunchcrafts.common.skills.boros;
//
//import net.minecraftforge.common.ForgeConfigSpec;
//
//public class BorosConfig {
//    public final ForgeConfigSpec.IntValue passiveRegenAmount;
//    public final ForgeConfigSpec.LongValue ultraRegenAmount;
//    public final ForgeConfigSpec.IntValue ultraRegenDurationSeconds;
//    public final ForgeConfigSpec.IntValue ultraRegenCooldownTicks;
//
//    public BorosConfig(ForgeConfigSpec.Builder builder) {
//        builder.push("Boros Regeneration");
//        passiveRegenAmount = builder.comment("Cura passiva por tick para Boros")
//                .defineInRange("passiveRegenAmount", 100_000, 0, Integer.MAX_VALUE);
//        ultraRegenAmount = builder.comment("Cura por pulso da regeneração ultra")
//                .defineInRange("ultraRegenAmount", 15_000_000L, 0, Long.MAX_VALUE);
//        ultraRegenDurationSeconds = builder.comment("Duração em segundos da regeneração ultra")
//                .defineInRange("ultraRegenDurationSeconds", 20, 1, 600);
//        ultraRegenCooldownTicks = builder.comment("Cooldown em ticks para a regeneração ultra")
//                .defineInRange("ultraRegenCooldownTicks", 24_000, 0, Integer.MAX_VALUE);
//        builder.pop();
//    }
//}