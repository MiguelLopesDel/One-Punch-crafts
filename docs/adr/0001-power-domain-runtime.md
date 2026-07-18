# ADR 0001 — Runtime de domínio de poderes

**Status:** aceito

## Decisão

O gameplay novo usa `PowerSet`, `Technique`, `Ability`, `Timeline`, `Strike`, `Effect`,
`Attribute`, `Tag` e `Cue`, identificados por IDs namespaced e registrados
antes de o runtime ser congelado. O núcleo em `com.onepunchcrafts.api` e
`com.onepunchcrafts.runtime` não conhece Entity, EventBus ou classes client.

Minecraft/Forge entra apenas pelos Adapters de `com.onepunchcrafts.minecraft`.
O Mixin do clique é um Adapter de entrada; ele não mantém identidade de golpe
em ThreadLocal nem reconsulta a seleção durante eventos.

Os pacotes permanentes descrevem seus seams (`api`, `runtime`, `content` e
`minecraft`), nunca a geração do rewrite. `v3` continua sendo o nome histórico
do RFC, não parte do namespace importado por addons. Versões no pacote só serão
consideradas se duas interfaces públicas incompatíveis precisarem coexistir.

## Consequências

- seleção roteia o comando inicial; `AttackPlan` e `Timeline` guardam IDs;
- Technique composta emite Strike diretamente;
- Timeline física e Cue compartilham `startTick` do servidor;
- UI não faz parte da interface `Ability`;
- addons chamam `OnePunchRuntime.register(...)` durante sua construção, pela mesma
  API usada pelo conteúdo do mod; os registries congelam no common setup.
