package com.onepunchcrafts.common.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Gui.class)
public abstract class MixinGui {

    @Shadow
    @Final
    protected RandomSource random;
    @Shadow
    @Final
    protected Minecraft minecraft;

    @Shadow
    protected abstract void renderHeart(GuiGraphics pGuiGraphics, Gui.HeartType pHeartType, int pX, int pY, int pYOffset, boolean pRenderHighlight, boolean pHalfHeart);

    /**
     * @author OnePunchCrafts
     * @reason Otimizar renderização de corações para valores altos de vida (Boros Pack)
     */
    @Overwrite
    protected void renderHearts(GuiGraphics pGuiGraphics, Player pPlayer, int pX, int pY, int pHeight, int pOffsetHeartIndex, float pMaxHealth, int invalid, int pDisplayHealth, int pAbsorptionAmount, boolean pRenderHighlight) {
        float pCurrentHealth = pPlayer.getHealth();
        Gui.HeartType gui$hearttype = Gui.HeartType.forPlayer(pPlayer);
        int i = 9 * (pPlayer.level().getLevelData().isHardcore() ? 5 : 0);

        // OTIMIZAÇÃO 1: Limitar o número máximo de corações renderizados
        final int MAX_RENDERABLE_HEARTS = 350; // Máximo de 350 corações (100 HP)
        final int HEARTS_PER_ROW = 10;

        // CORREÇÃO OVERFLOW: Usar Math.min para evitar overflow em valores grandes
        int j = onePunchCrafts$safeCeil((double) pMaxHealth / 2.0D);
        int k = onePunchCrafts$safeCeil((double) pAbsorptionAmount / 2.0D);

        // OTIMIZAÇÃO 2: Se a vida for muito alta, usar sistema de representação visual
        boolean useCompactMode = j > MAX_RENDERABLE_HEARTS;

        if (useCompactMode) {
            onePunchCrafts$renderCompactHealthBar(pGuiGraphics, pPlayer, pX, pY, pMaxHealth, pCurrentHealth, pDisplayHealth, pAbsorptionAmount, gui$hearttype, i);
            return;
        }

        // OTIMIZAÇÃO 3: Limitar o número de corações renderizados mesmo no modo normal
        int maxHeartsToRender = Math.min(j + k, MAX_RENDERABLE_HEARTS);
        int l = Math.min(j * 2, Integer.MAX_VALUE); // CORREÇÃO: Evitar overflow na multiplicação

        for (int i1 = maxHeartsToRender - 1; i1 >= 0; --i1) {
            int j1 = i1 / HEARTS_PER_ROW;
            int k1 = i1 % HEARTS_PER_ROW;
            int l1 = pX + k1 * 8;
            int i2 = pY - j1 * pHeight;

            // OTIMIZAÇÃO 4: Limitar número de linhas de corações
            if (j1 > 4) break; // Máximo 5 linhas de corações

            if (pCurrentHealth + pAbsorptionAmount <= 4) {
                i2 += this.random.nextInt(2);
            }

            if (i1 < j && i1 == pOffsetHeartIndex) {
                i2 -= 2;
            }

            this.renderHeart(pGuiGraphics, Gui.HeartType.CONTAINER, l1, i2, i, pRenderHighlight, false);
            int j2 = i1 * 2;
            boolean flag = i1 >= j;

            if (flag) {
                int k2 = j2 - l;
                if (k2 < pAbsorptionAmount) {
                    boolean flag1 = k2 + 1 == pAbsorptionAmount;
                    this.renderHeart(pGuiGraphics, gui$hearttype == Gui.HeartType.WITHERED ? gui$hearttype : Gui.HeartType.ABSORBING, l1, i2, i, false, flag1);
                }
            }

            if (pRenderHighlight && j2 < pDisplayHealth) {
                boolean flag2 = j2 + 1 == pDisplayHealth;
                this.renderHeart(pGuiGraphics, gui$hearttype, l1, i2, i, true, flag2);
            }

            if (j2 < pCurrentHealth) {
                boolean flag3 = j2 + 1 == pCurrentHealth;
                this.renderHeart(pGuiGraphics, gui$hearttype, l1, i2, i, false, flag3);
            }
        }
    }

    @Unique
    private void onePunchCrafts$renderCompactHealthBar(GuiGraphics pGuiGraphics, Player pPlayer, int pX, int pY, float pMaxHealth, float pCurrentHealth, int pDisplayHealth, int pAbsorptionAmount, Gui.HeartType heartType, int yOffset) {
        final int MAX_RENDERABLE_HEARTS = 350;
        final int HEARTS_PER_ROW = 10;

        // Calcula quantos corações serão renderizados (até o limite)
        int totalMaxHearts = onePunchCrafts$safeCeil((double) pMaxHealth / 2.0D);
        int heartsToRender = Math.min(totalMaxHearts, MAX_RENDERABLE_HEARTS);

        // Calcula a vida proporcional baseada nos corações que serão renderizados
        float proportionalMaxHealth = heartsToRender * 2.0f;
        float healthRatio = pCurrentHealth / pMaxHealth;
        float proportionalCurrentHealth = proportionalMaxHealth * healthRatio;

        // Renderiza os corações até o limite
        for (int i = 0; i < heartsToRender; i++) {
            int row = i / HEARTS_PER_ROW;
            int col = i % HEARTS_PER_ROW;
            int x = pX + col * 8;
            int y = pY - row * 10; // Espaçamento vertical entre linhas

            // Limita o número de linhas para não sair da tela
//            if (row > 4) break;

            // Container (fundo do coração)
            this.renderHeart(pGuiGraphics, Gui.HeartType.CONTAINER, x, y, yOffset, false, false);

            // Coração preenchido baseado na vida proporcional
            float heartValue = i * 2.0f;
            if (heartValue < proportionalCurrentHealth) {
                boolean halfHeart = (heartValue + 1.0f) > proportionalCurrentHealth && heartValue < proportionalCurrentHealth;
                this.renderHeart(pGuiGraphics, heartType, x, y, yOffset, false, halfHeart);
            }
        }

        // Renderiza absorção se existir (nos corações extras)
        if (pAbsorptionAmount > 0) {
            int absorptionHearts = onePunchCrafts$safeCeil((double) pAbsorptionAmount / 2.0D);
            int maxAbsorptionToRender = Math.min(absorptionHearts, MAX_RENDERABLE_HEARTS - heartsToRender);

            for (int i = 0; i < maxAbsorptionToRender; i++) {
                int totalIndex = heartsToRender + i;
                int row = totalIndex / HEARTS_PER_ROW;
                int col = totalIndex % HEARTS_PER_ROW;
                int x = pX + col * 8;
                int y = pY - row * 10;

                if (row > 4) break;

                this.renderHeart(pGuiGraphics, Gui.HeartType.CONTAINER, x, y, yOffset, false, false);

                int absorptionValue = i * 2;
                if (absorptionValue < pAbsorptionAmount) {
                    boolean halfHeart = (absorptionValue + 1) == pAbsorptionAmount;
                    this.renderHeart(pGuiGraphics, heartType == Gui.HeartType.WITHERED ? heartType : Gui.HeartType.ABSORBING, x, y, yOffset, false, halfHeart);
                }
            }
        }

        // Renderiza texto com os valores exatos apenas se exceder o limite
        if (totalMaxHearts > MAX_RENDERABLE_HEARTS) {
            String healthText = onePunchCrafts$formatLargeNumber(pCurrentHealth) + " / " + onePunchCrafts$formatLargeNumber(pMaxHealth);
            int textColor = pCurrentHealth <= pMaxHealth * 0.2f ? 0xFF4444 : 0xFFFFFF;
            pGuiGraphics.drawString(this.minecraft.font, healthText, pX-22, pY, textColor, true);

            if (pAbsorptionAmount > 0) {
                String absorptionText = "+" + onePunchCrafts$formatLargeNumber(pAbsorptionAmount);
                pGuiGraphics.drawString(this.minecraft.font, absorptionText, pX-70, pY + 70, 0xFFD700, true);
            }
        }
    }

    @Unique
    private String onePunchCrafts$formatLargeNumber(float number) {
        if (number < 1000) {
            return String.format("%.0f", number);
        } else if (number < 1_000_000) {
            return String.format("%.1fK", number / 1000.0);
        } else if (number < 1_000_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else {
            return String.format("%.1fB", number / 1_000_000_000.0);
        }
    }

    @Unique
    private int onePunchCrafts$safeCeil(double value) {
        // Evita overflow limitando o valor máximo que pode ser convertido para int
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) Math.ceil(value);
    }
}