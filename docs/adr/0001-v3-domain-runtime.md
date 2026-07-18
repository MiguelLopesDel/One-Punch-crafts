# ADR 0001 — Runtime de domínio v3

**Status:** aceito

## Decisão

O gameplay novo usa `PowerSet`, `Ability`, `Timeline`, `Strike`, `Effect`,
`Attribute`, `Tag` e `Cue`, identificados por IDs namespaced e registrados
antes de o runtime ser congelado. O núcleo em `com.onepunchcrafts.v3.api` e
`com.onepunchcrafts.v3.core` não conhece Entity, EventBus ou classes client.

Minecraft/Forge entra apenas pelos Adapters de `com.onepunchcrafts.v3.minecraft`.
O Mixin do clique é um Adapter de entrada; ele não mantém identidade de golpe
em ThreadLocal nem reconsulta a seleção durante eventos.

## Consequências

- seleção roteia o comando inicial; `AttackPlan` e `Timeline` guardam IDs;
- skill composta emite Strike diretamente;
- Timeline física e Cue compartilham `startTick` do servidor;
- UI não faz parte da interface `Ability`;
- addons chamam `OnePunchV3.register(...)` durante sua construção, pela mesma
  API usada pelo conteúdo do mod; os registries congelam no common setup.
