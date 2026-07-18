# ADR 0003 — DamagePipeline, Unstoppable e política do Saitama

**Status:** aceito

## Decisão

O dano v3 percorre estágios de ordem fixa. Interceptors têm ID e ordem
determinística. `SERIOUS` implica `unstoppable`.

`Saitama como alvo` é resolvido em `TARGET_POLICY` antes de aplicação e
verificação: ele sobrevive inclusive a Serious Punch. Para qualquer outro alvo,
Serious Punch exige resultado letal; o Adapter tenta a fonte do mod, depois
`fellOutOfWorld` e por fim saúde/morte direta.

## Consequências

Os quatro `setCanceled(false)` deixam de ser a garantia. O pipeline verifica o
desfecho, inclusive contra entidades que sobrescrevem `hurt()`. Criativo e
espectador permanecem exceções explícitas no Adapter Minecraft.
