# ADR 0003 — DamagePipeline, Unstoppable e política do Saitama

**Status:** aceito

## Decisão

O dano do runtime percorre estágios de ordem fixa. Interceptors têm ID e ordem
determinística. `SERIOUS` implica `unstoppable`.

`Saitama como alvo` é resolvido em `TARGET_POLICY` antes de aplicação e
verificação: ele sobrevive inclusive a Serious Punch. Para qualquer outro alvo
de combate elegível, Serious Punch exige resultado letal; o Adapter tenta a
fonte do mod, depois `fellOutOfWorld` e por fim saúde/morte direta. Criativo e
espectador ficam fora do conjunto elegível por política da borda Minecraft.

## Consequências

Os quatro `setCanceled(false)` deixam de ser a garantia. O pipeline verifica o
desfecho, inclusive contra entidades que sobrescrevem `hurt()`. A distinção
entre imunidade canônica do Saitama e inelegibilidade de criativo/espectador
fica explícita, em vez de surgir da ordem de eventos.
