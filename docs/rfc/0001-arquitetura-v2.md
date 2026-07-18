# RFC 0001 — Arquitetura v2 do OnePunchCrafts

**Status:** proposta (aguardando decisões da seção 6)
**Data:** 2026-07-18

A arquitetura atual foi desenhada há mais de um ano e cresceu por acreção:
cada personagem novo, skill nova e mecânica nova foi encaixada no molde que
existia. Este RFC reavalia esse molde com a linguagem de módulos fundos
(interface pequena, implementação funda, seam no lugar certo) e propõe uma
evolução incremental — não um rewrite.

---

## 1. Diagnóstico da arquitetura atual

### O fluxo hoje

```
tecla/scroll (client)
  → SpecialSkillPacket (payload vazio = "executa o selecionado")
  → OnePunchPlayer (capability) → SkillPack.execute() → Skill.execute(player)

combate (server)
  → LivingDamage/Hurt/Death events
  → PunchSkillEvents (relay estático por skill) + passServerFluxToAllPlayers
  → multiplicadores/cancelamentos por prioridade de evento

sync: @Syncable + FieldComparator (reflection) + snapshot NBT por tick
persistência: writeNBT/readNBT manuais por pack, identidade por simpleName
```

### Problemas, cada um com incidente real

**P1 — `Skill` mistura cliente e servidor.** `renderName(Font, GuiGraphics)`
vive na mesma interface que `execute()`. Consequências: nenhuma skill pode
ser `@EventBusSubscriber` (crash `DEDICATED_SERVER` já ocorrido), o que
forçou os relays estáticos em `PunchSkillEvents` — um contrato invisível:
quem cria skill de combate precisa *saber* que deve ir lá registrar o relay.

**P2 — Identidade de skill por posição na lista.** Seleção é um índice;
`getMaxNumSkill()` devolve `13` hardcoded; `adjustAbility` faz `switch` nos
índices 6/8/9/10/11. Reordenar a lista quebra saves, quebra o switch e
quebra a UI silenciosamente. Skills de stat (peso, knockback...) são 7
classes anônimas dentro do `SaitamaPack` só para ter um nome na HUD.

**P3 — Duas entradas intencionais acopladas ao estado de seleção mutável.**
Uma skill de soco responde a dois comandos distintos: clique esquerdo e
ativação. Normal/Weak dão um Strike no clique e, quando ativados, executam
vários Strikes daquele tipo; Serious executa o golpe tanto no clique quanto
por ativação direcional, útil contra alvos distantes. A regra está certa; o
acoplamento é que é frágil. `NormalPunch` e `WeakPunch` implementam a
ativação chamando `player.attack()` repetidamente, e cada evento redescobre
a seleção por `verifyIsSaitamaAndSkill(...)`. `NormalPunchesInArea` e
`QuickBackStab` chegam a trocar `currentSkill` temporariamente para reutilizar
o mesmo fluxo. A v2 deve rotear as duas entradas explicitamente e capturar a
identidade dos Strikes/timeline quando a ação começa.

**P4 — Pack god-object.** `BorosPack` tem 1.059 linhas: movimento adaptativo,
energia, regeneração, atributos, mitigação, partículas e sync no mesmo
módulo, sem seams internos. Nada é testável isoladamente.

**P5 — Sync e persistência duplicadas.** `@Syncable` declara chave e
estratégia, mas `writeNBT`/`readNBT` re-listam cada campo com string literal
("borosknockbackresistance"). O snapshot de sync cria uma instância nova por
reflection e faz round-trip por NBT a cada mudança. A identidade do pack no
save é `getClass().getSimpleName()` — renomear a classe corrompe saves.

**P6 — Balance hardcoded.** O rebalanceamento do Boros desta semana (HP,
mitigação, regen) exigiu recompilar. Todo tuning futuro também exigirá.

**P7 — Dano por multiplicador gigante + sentinela mágica.** Socos multiplicam
o dano vanilla (punho=1) por 10⁵/10⁷/10¹⁶; a mitigação distingue "Serious
Punch" por `dano ≥ 10¹²`. Semântica estranha: soco com espada encantada
multiplica a espada; i-frames vanilla interferem na rajada; o sistema
inteiro raciocina sobre magnitudes, não sobre *tipos* de golpe.

**P8 — Infra utilitária difusa.** `HelpUtility` é um grab-bag (capability,
efeitos, explosões, teleporte, ataques). `TickScheduler` é estático global
com `Duration` de tempo real — o hack `System.currentTimeMillis()/50` no
`exhaustedTimestamp` existe exatamente porque tick e relógio divergem.

### O que já está bom (não mexer)

- **Estratégias de sync** (`TOGGLE`/`VALIDATED`/`SERVER_AUTHORITY`) — o
  conceito de validação server-side por campo é sólido; o problema é só a
  duplicação com NBT.
- **Capability única** (`OnePunchPlayer` segurando um `SkillPack`) — seam
  correto para "quem eu sou".
- **Camada de apresentação pós-refactor** — `VfxQuadBatch`,
  `ManagedPostChain`, cinemáticas com timeline determinística client-side,
  packets de VFX por proximidade. Essa é a referência de qualidade para o
  resto.

---

## 2. Princípios da v2

1. **Identidade por ID, nunca por posição.** Skills e packs têm
   `ResourceLocation` (`onepunchcrafts:serious_punch`). Ordem é dado de
   apresentação, não identidade.
2. **Seam cliente/servidor na interface, não na disciplina.** Código comum
   não pode *referenciar* classe de cliente; apresentação registra-se do
   lado de lá, keyed por ID.
3. **Um dispatcher, não N relays.** A ordem dos efeitos de combate
   (multiplicador → mitigação → cap → bypass) é decidida em UM lugar
   legível, não por prioridade de EventBus.
4. **Domínio antes de número.** "Serious Punch derrota todo alvo que não é
   Saitama" é uma regra sobre um *tier* de dano e uma política de alvo, não
   sobre `1e12`.
5. **Balance é dado.** Números de tuning vivem em config, não em constante.
6. **Pack é composição.** Cada subsistema (recursos, movimento, combate,
   loadout) é um módulo fundo com interface própria; o Pack só compõe.

---

## 3. Proposta

### 3.1 Registro de skills (`SkillType`)

```java
public record SkillType(ResourceLocation id, Supplier<Skill> factory) {}

// registro simples (mapa ordenado), sem cerimônia Forge por enquanto
SkillTypes.register("saitama/normal_punch", NormalPunch::new);
```

- O **Loadout** de um pack é uma lista ordenada de IDs por grupo — dado, não
  código: reordenar não quebra nada.
- Seleção persiste/sincroniza por ID (string), com fallback índice→ID na
  primeira migração de save.
- Pack no NBT também vira ID (`onepunchcrafts:saitama`), matando o switch
  por `simpleName`.

### 3.2 `Skill` dividida em contrato comum + apresentação client

```java
// comum — zero classes de cliente
public interface Skill {
    SkillExecutionResult activate(SkillContext ctx);      // era execute()
    default AttackPlan onPrimaryAttack(PrimaryAttackContext ctx) {
        return AttackPlan.vanilla();
    }
    default void onDamageDealt(SkillContext ctx, LivingDamageEvent e) {}
    default void onDamageTaken(SkillContext ctx, LivingDamageEvent e) {}
    default void onTick(SkillContext ctx) {}
    default void adjust(SkillContext ctx, double delta) {} // mata o switch de índices
}

public record SkillContext(ServerPlayer player, Pack pack, ServerLevel level) {}
public record PrimaryAttackContext(SkillContext skill, Entity target) {}
```

```java
// client — registrado no client setup, keyed por ID
SkillHud.register(SkillTypes.NORMAL_PUNCH, (gfx, ctx) -> ...);
```

- `PunchSkillEvents` morre: um **CombatDispatcher** único entrega
  `PrimaryAttackIntent` à skill selecionada quando o clique começa. Ela
  devolve um `AttackPlan` imutável; os estágios posteriores nunca voltam a
  consultar a seleção.
- `activate()` é a segunda entrada, independente. A ativação de Normal/Weak
  cria uma timeline que emite vários Strikes já identificados; trocar a
  seleção durante a sequência não muda os golpes em andamento. Serious cria
  a sequência direcional sem exigir alvo de melee.
- Skills compostas reutilizam golpes por ID (`strike(NORMAL_PUNCH, target)`),
  sem `setCurrentSkill(...)` temporário. Preserva-se o resultado de
  `NormalPunchesInArea` e `QuickBackStab` sem usar estado de UI como relay.
- Skills viram instâncias com estado permitido (cooldowns, combos) — o
  estado que hoje é impossível porque tudo é estático.
- Stats e toggles deixam de ser skills anônimas: o Loadout declara
  `AdjustableStat("weight", 0..N)` / `Toggle("serious_fart")` e a HUD os
  renderiza genericamente. Sete classes anônimas somem.

### 3.3 Tiers de dano como conceito de domínio

```java
public enum DamageTier { MUNDANE, ENHANCED, DRAGON, SERIOUS }
```

- Cada golpe do mod aplica dano com um `DamageSource` do mod que carrega o
  tier (o registry de damage types já existe — `DamagesRegistry`).
- Mitigação do Boros raciocina por tier: reduz `ENHANCED`, sofre `DRAGON`,
  e `SERIOUS` bypassa por definição. A sentinela `1e12` morre.
- A política do alvo roda antes do tier: Saitama Invulnerability impede dano
  e morte mesmo quando o tier é `SERIOUS`.
- Dano do soco vira **valor absoluto por tier em config**, não multiplicador
  do punho vanilla — a semântica "soco × espada" desaparece, e o dispatcher
  controla i-frames explicitamente por tier (a rajada deixa de lutar contra
  o `invulnerableTime` do vanilla).

### 3.4 Pack como composição

```java
public final class BorosPack extends Pack {
    private final EnergyPool energy;            // custo/regen/exhaustion
    private final MitigationRules mitigation;   // tiers × forma
    private final BorosMovement movement;       // vault/ravine/air control
    private final RegenerationRules regen;      // passiva + ultra
}
```

- Cada módulo tem interface pequena e é testável sem Minecraft rodando
  (aceitam dependências, devolvem resultados).
- `BorosPack` encolhe para composição + delegação; o conteúdo dos módulos é
  o código atual movido, não reescrito.

### 3.5 Balance em config

`config/onepunchcrafts-balance.toml` (ForgeConfigSpec, server-side, synca
para o client): HP e mitigação por forma do Boros, dano por tier dos socos,
custos e taxas de energia, cooldowns. O rebalanceamento desta semana teria
sido edição de arquivo.

### 3.6 Sync/NBT com fonte única

`@Syncable` passa a dirigir **também** a serialização NBT (mesma varredura
de reflection, mesmas chaves). `writeNBT`/`readNBT` manuais dos packs somem;
estratégias de validação ficam como estão.

### 3.7 Scheduler por ticks

`TickScheduler` passa a aceitar ticks (`int`) em vez de `Duration` de tempo
real para lógica de gameplay; agendamentos limpam-se em unload de mundo.
Cooldowns persistentes ficam em campos `@Syncable` dos packs (como
`ticksToUseUltraRegeneration` já faz), nunca no scheduler.

---

## 4. Migração em fases (sempre compilando, cada fase shippável)

| Fase | Conteúdo | Risco | Tamanho |
|---|---|---|---|
| **F1** | SkillType registry + seleção por ID (fallback de índice p/ saves) | baixo | médio |
| **F2** | Split da interface Skill + CombatDispatcher; deletar PunchSkillEvents | médio | médio |
| **F3** | Stats/toggles genéricos no Loadout; matar `adjustAbility` switch | baixo | pequeno |
| **F4** | DamageTier + balance em config; matar multiplicadores e `1e12` | **alto** (re-tuning) | médio |
| **F5** | Decomposição dos packs + @Syncable→NBT unificado | médio | grande |

F4 é a única fase que muda *números sentidos em jogo* — exige uma sessão de
re-tuning com o jogo aberto. As demais são refactors de forma com
comportamento preservado.

---

## 5. O que explicitamente fica como está

Capability única, SimpleChannel/NetworkRegister, estratégias de @Syncable,
toda a camada de apresentação (VfxQuadBatch, ManagedPostChain, cinemáticas,
CSRC), TickScheduler para efeitos fire-and-forget.

---

## 6. Decisões

**Decidida — duas entradas por skill de soco:** a skill selecionada recebe o
clique esquerdo como `PrimaryAttackIntent` e a tecla de skill como
`CastIntent`. Normal/Weak emitem um Strike no clique e uma sequência de
Strikes na ativação. Serious dispara no clique com alvo direto ou por
ativação direcional sem alvo de melee. Cada plano/timeline captura sua
identidade ao começar; skills compostas invocam Strikes sem alterar a
seleção.

**Em aberto:**

1. **Semântica do dano** (bloqueia F4): dano absoluto por tier (proposta) ou
   manter multiplicador sobre o hit vanilla (soco escala com arma/crit)?
2. **Registry**: mapa próprio (proposta, simples) ou custom registry Forge
   (necessário só se quisermos skills definidas por datapack no futuro)?
3. **Config**: TOML server config (proposta) ou datapack JSON por pack?

Decididas as três restantes, cada uma vira um ADR curto em `docs/adr/`.
