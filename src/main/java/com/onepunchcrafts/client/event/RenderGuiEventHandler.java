package com.onepunchcrafts.client.event;

import com.onepunchcrafts.common.skills.SkillPack;
import com.onepunchcrafts.common.skills.boros.BorosConfig;
import com.onepunchcrafts.common.skills.boros.BorosPack;
import com.onepunchcrafts.util.HelpUtility;
import com.onepunchcrafts.v3.api.Id;
import com.onepunchcrafts.v3.content.SaitamaContent;
import com.onepunchcrafts.v3.core.state.PowerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.onepunchcrafts.OnePunchCrafts.ONE_PLAYER_CAPABILITY;

@Mod.EventBusSubscriber({Dist.CLIENT})
public class RenderGuiEventHandler {

    @SubscribeEvent
    public static void onRender(RenderGuiEvent.Pre event) {
        Minecraft instance = Minecraft.getInstance();
        LocalPlayer player = instance.player;
        if (player == null) return;

        final int width = event.getWindow().getGuiScaledWidth();
        final int height = event.getWindow().getGuiScaledHeight();
        GuiGraphics guiGraphics = event.getGuiGraphics();
        Font font = instance.font;

        PowerState state = HelpUtility.getSkillData(player).getPowerState();
        if (!state.powerSetId().equals(PowerState.NONE)) {
            renderV3Ability(state, width, height, font, guiGraphics);
            return;
        }
        SkillPack pack = HelpUtility.getSkillData(player).getSkillPack();
        pack.renderSkills(width, height, font, guiGraphics);

        if (pack instanceof BorosPack boros) {
            renderBorosEnergy(boros, width, height, font, guiGraphics);
        }
    }

    private static void renderV3Ability(PowerState state, int width, int height, Font font, GuiGraphics graphics) {
        Id ability = state.abilities().selectedAbility();
        String key = ability.equals(SaitamaContent.WEAK_PUNCH) ? "skill.saitama.weak_punch"
                : ability.equals(SaitamaContent.NORMAL_PUNCH) ? "skill.saitama.normal_punch"
                : ability.equals(SaitamaContent.SERIOUS_PUNCH) ? "skill.saitama.serious_punch"
                : ability.equals(SaitamaContent.WEAKENING_PUNCH) ? "skill.saitama.weakening_punch"
                : ability.equals(SaitamaContent.QUICK_BACKSTAB) ? "skill.saitama.quick_backstab"
                : ability.equals(SaitamaContent.NORMAL_PUNCHES_IN_AREA) ? "skill.saitama.normalpuncharmy"
                : ability.equals(SaitamaContent.DASH) ? "skill.saitama.dash"
                : ability.equals(SaitamaContent.SERIOUS_FART) ? "skill.saitama.serious_fart"
                : ability.equals(SaitamaContent.SPEED) ? "skill.saitama.super_speed"
                : ability.equals(SaitamaContent.BREAK_BLOCKS) ? "skill.saitama.break_blocks_quickly"
                : ability.equals(SaitamaContent.WEIGHT) ? "skill.saitama.set_weight"
                : ability.equals(SaitamaContent.KNOCKBACK_RESISTANCE) ? "skill.saitama.knockback_resistance"
                : ability.equals(SaitamaContent.ATTACK_KNOCKBACK) ? "skill.saitama.attack_knockback"
                : ability.equals(SaitamaContent.SWIM_SPEED) ? "skill.saitama.swim_speed"
                : ability.equals(SaitamaContent.EXTREME_SPEED) ? "skill.saitama.extreme_speed"
                : "skill.saitama.extreme_jump";
        Id attribute = ability.equals(SaitamaContent.SPEED) ? SaitamaContent.ATTR_SPEED
                : ability.equals(SaitamaContent.WEIGHT) ? SaitamaContent.ATTR_WEIGHT
                : ability.equals(SaitamaContent.KNOCKBACK_RESISTANCE) ? SaitamaContent.ATTR_KNOCKBACK_RESISTANCE
                : ability.equals(SaitamaContent.ATTACK_KNOCKBACK) ? SaitamaContent.ATTR_ATTACK_KNOCKBACK
                : ability.equals(SaitamaContent.SWIM_SPEED) ? SaitamaContent.ATTR_SWIM_SPEED : null;
        Component text = attribute == null ? Component.translatable(key)
                : Component.translatable(key, (int) state.attributes().base(attribute));
        boolean disabledToggle = (ability.equals(SaitamaContent.SERIOUS_FART) && !state.tags().contains(SaitamaContent.TAG_SERIOUS_FART))
                || (ability.equals(SaitamaContent.BREAK_BLOCKS) && !state.tags().contains(SaitamaContent.TAG_BREAK_BLOCKS))
                || (ability.equals(SaitamaContent.EXTREME_SPEED) && !state.tags().contains(SaitamaContent.TAG_EXTREME_SPEED))
                || (ability.equals(SaitamaContent.EXTREME_JUMP) && !state.tags().contains(SaitamaContent.TAG_EXTREME_JUMP));
        graphics.drawString(font, text, width / 2 - (int) (width * 0.05),
                height / 2 + (int) (height * 0.25), disabledToggle ? 0xFF0000 : 0x00FF00, false);
    }

    private static void renderBorosEnergy(BorosPack boros, int width, int height, Font font, GuiGraphics guiGraphics) {
        float energy = boros.getEnergy();
        float percentage = (energy / BorosConfig.MAX_ENERGY) * 100f;
        int color = percentage > 50 ? 0x00FF00
                : percentage > 20 ? 0xFFFF00
                : 0xFF0000;

        String[] formNames = {"Armadura", "Liberado", "Meteoric Burst"};
        String status = boros.getConfig().isExhausted() ? " §c§l[EXAUSTO]" : "";

        String text = String.format("Energia: %.1f%% | Forma: %s%s", percentage, formNames[boros.getCurrentForm()], status);

        int textWidth = font.width(text);
        int x = (width - textWidth) / 2;
        int y = 5;

        guiGraphics.fill(x - 2, y - 1, x + textWidth + 2, y + 10, 0x88000000);
        guiGraphics.drawString(font, Component.literal(text), x, y, color, false);
    }
}
