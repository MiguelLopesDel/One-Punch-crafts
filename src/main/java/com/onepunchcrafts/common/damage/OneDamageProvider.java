package com.onepunchcrafts.common.damage;

import com.brandon3055.draconicevolution.init.DEDamage;
import com.onepunchcrafts.OnePunchCrafts;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

import static com.onepunchcrafts.OnePunchCrafts.MODID;

//modify later
public class OneDamageProvider extends TagsProvider<DamageType> {


    public OneDamageProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> future, ExistingFileHelper helper) {
        super(output, Registries.DAMAGE_TYPE, future, MODID, helper);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        ResourceKey<DamageType> seriousPunch = DamagesRegistry.SERIOUS_PUNCH;
        ResourceKey<DamageType> seriousPunchSecond = DamagesRegistry.SERIOUS_PUNCH_SECOND;
        this.tag(DamageTypeTags.DAMAGES_HELMET).add(seriousPunch);
        this.tag(DamageTypeTags.BYPASSES_ARMOR).add(seriousPunch);
        this.tag(DamageTypeTags.BYPASSES_SHIELD).add(seriousPunch);
        this.tag(DamageTypeTags.BYPASSES_INVULNERABILITY).add(seriousPunch);
        this.tag(DamageTypeTags.BYPASSES_EFFECTS).add(seriousPunch);
        this.tag(DamageTypeTags.BYPASSES_RESISTANCE).add(seriousPunch);
        this.tag(DamageTypeTags.BYPASSES_ENCHANTMENTS).add(seriousPunch);
        this.tag(DamageTypeTags.IS_EXPLOSION).add(seriousPunch);
        this.tag(DamageTypeTags.IS_LIGHTNING).add(seriousPunch);
        this.tag(DamageTypeTags.IGNITES_ARMOR_STANDS).add(seriousPunch);
        this.tag(DamageTypeTags.BURNS_ARMOR_STANDS).add(seriousPunch);
        this.tag(DamageTypeTags.AVOIDS_GUARDIAN_THORNS).add(seriousPunch);
        this.tag(DamageTypeTags.ALWAYS_HURTS_ENDER_DRAGONS).add(seriousPunch);
        this.tag(DEDamage.Tags.CHAOTIC).add(seriousPunch);

        this.tag(DamageTypeTags.DAMAGES_HELMET).add(seriousPunchSecond);
        this.tag(DamageTypeTags.BYPASSES_ARMOR).add(seriousPunchSecond);
        this.tag(DamageTypeTags.BYPASSES_SHIELD).add(seriousPunchSecond);
        this.tag(DamageTypeTags.BYPASSES_INVULNERABILITY).add(seriousPunchSecond);
        this.tag(DamageTypeTags.BYPASSES_EFFECTS).add(seriousPunchSecond);
        this.tag(DamageTypeTags.BYPASSES_RESISTANCE).add(seriousPunchSecond);
        this.tag(DamageTypeTags.BYPASSES_ENCHANTMENTS).add(seriousPunchSecond);
        this.tag(DamageTypeTags.IS_EXPLOSION).add(seriousPunchSecond);
        this.tag(DamageTypeTags.IS_LIGHTNING).add(seriousPunchSecond);
        this.tag(DamageTypeTags.IGNITES_ARMOR_STANDS).add(seriousPunchSecond);
        this.tag(DamageTypeTags.BURNS_ARMOR_STANDS).add(seriousPunchSecond);
        this.tag(DamageTypeTags.AVOIDS_GUARDIAN_THORNS).add(seriousPunchSecond);
        this.tag(DamageTypeTags.ALWAYS_HURTS_ENDER_DRAGONS).add(seriousPunchSecond);
        this.tag(DEDamage.Tags.CHAOTIC).add(seriousPunchSecond);

        // CSRC pierces most defenses (armor, shields, potion effects, wither
        // immunities) but deliberately NOT command/creative invulnerability.
        ResourceKey<DamageType> csrc = DamagesRegistry.CSRC;
        this.tag(DamageTypeTags.DAMAGES_HELMET).add(csrc);
        this.tag(DamageTypeTags.BYPASSES_ARMOR).add(csrc);
        this.tag(DamageTypeTags.BYPASSES_SHIELD).add(csrc);
        this.tag(DamageTypeTags.BYPASSES_EFFECTS).add(csrc);
        this.tag(DamageTypeTags.BYPASSES_RESISTANCE).add(csrc);
        this.tag(DamageTypeTags.BYPASSES_ENCHANTMENTS).add(csrc);
        this.tag(DamageTypeTags.IS_EXPLOSION).add(csrc);
        this.tag(DamageTypeTags.AVOIDS_GUARDIAN_THORNS).add(csrc);
        this.tag(DamageTypeTags.ALWAYS_HURTS_ENDER_DRAGONS).add(csrc);
        this.tag(DEDamage.Tags.CHAOTIC).add(csrc);
    }
}
