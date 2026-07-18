# RFC 0002 — Arquitetura v3: rewrite do núcleo

**Status:** aceita — implementação em andamento (RFC 0001 superseded)
**Data:** 2026-07-18

**Implementação:** núcleo, adapters e corte vertical do Saitama presentes em
`com.onepunchcrafts.api`, `runtime`, `content` e `minecraft`; decisões fundadoras
registradas em `docs/adr/0001` a `0005`. `v3` nomeia este RFC histórico, não os
pacotes permanentes. O port segue a estratégia C, não uma bridge no fluxo legado.

O RFC 0001 propõe evoluir o molde atual. Este RFC responde à pergunta maior:
*se o mod fosse desenhado hoje, do zero, qual seria a arquitetura?* — com os
requisitos: flexível, extensível por terceiros (API), fácil de adicionar
skills/personagens, tipos de habilidade imprevistos, timeline visual+física
unificada, performático, e estruturalmente imune às classes de bug que o
design atual produz (sincronia, auto-referência, ordem de execução).

### Estado em 2026-07-18

Este RFC descreve a arquitetura-alvo. No baseline publicado:

- **implementado:** registries congeláveis, `PowerState`, seleção por ID,
  `Technique`/`Ability`/`AttackPlan`/`Timeline`, páginas radiais, troca rápida,
  `Strike`, `Effect`, `DamagePipeline`,
  adapters Minecraft, persistência versionada, intents/deltas básicos, testes
  do núcleo e o corte vertical do Saitama;
- **transitório:** cues ainda projetam parte da apresentação por packets
  existentes, e o runtime convive com o legado para personagens não portados;
- **pendente:** port do Boros, PowerSets/balance externos ao código, conclusão
  dos deltas por componente e remoção do núcleo legado.

Uma decisão aceita não significa que toda a seção já foi portada; este quadro
é a referência de progresso até o legado desaparecer.

---

## 1. Por que o modelo atual produz essas classes de bug

O diagnóstico detalhado está no RFC 0001 §1. O resumo causal:

**Os poderes vivem *dentro* do pipeline de combate do vanilla.** Dano é
"multiplicar o evento", morte é "cancelar/des-cancelar o evento", proteção é
"prioridade LOWEST". Consequência inevitável: qualquer outro handler (do
próprio mod ou de outro mod) que mexa nos mesmos eventos entra numa guerra
de ordem de execução. As marcas estão contadas no código:

- `SeriousPunch` tem **4×** `event.setCanceled(false)` — decisão
  *intencional* (golpe indefensável: um mod X pode cancelar em
  Attack/Hurt/Damage/Death, então des-cancela-se em todos os pontos). A
  regra de domínio está certa; o mecanismo é uma corrida armamentista em
  event-space: só vence se o nosso handler rodar por último, e já foi
  derrotado por mob que nem usa eventos (Wroughtnaut sobrescreve `hurt()` —
  daí o `pierceDefenses()` com 3 fallbacks). Ver §3.4.1;
- `setInvulnerable(false)` / `invulnerableTime = 0` espalhados — porque
  i-frames do vanilla engolem hits da rajada;
- `AttackEntityEventHandler` zera invulnerabilidade do Wither à mão;
- Normal Punch marca alvos com `addTag("targetnormalpunch")` + scheduler de
  200ms para remover + **scan de AABB 200×200×200 por tick** procurando a
  tag (`explodeNormalMobs`).

**O estado sincroniza bidirecionalmente.** O cliente envia o *estado* do
pack de volta ao servidor (`PlayerSyncPacket(cap)` no `adjustAbility...`),
o servidor compara com snapshot via reflection e re-valida diff por diff
(`handleTheDifferences`). Todo eco, toda auto-referência e todo "quem ganhou,
cliente ou servidor?" nasce dessa via de mão dupla.

**Existem dois relógios.** O visual (timeline client da cinemática) e o
físico (`TickScheduler` com `Duration` de tempo real no servidor) são
agendados separadamente e torcemos para coincidirem. O windup do Serious
Punch de hoje é literalmente dois agendamentos independentes de "14 ticks".

A intuição original do **flux** — "o evento segue um fluxo e eu posso
intervir em momentos específicos, inclusive para criaturas de outros mods" —
estava **certa**. O que falhou foi o mecanismo: usar o EventBus do Forge
como esse fluxo. O objetivo (pontos de interceptação ordenados) reaparece na
v3 como mecanismo próprio (§3.4).

---

## 2. A ideia central

Inspiração declarada: **Gameplay Ability System** (Unreal) — o padrão de
indústria para exatamente este problema ("muitos personagens, muitas
habilidades compostas, replicação, extensão por terceiros") — traduzido
para os idiomas do Minecraft (capability, registries, codecs, SimpleChannel).

Oito conceitos, cada um um módulo fundo:

```
PowerSet     quem você é: atributos, passivas e páginas de Techniques
Technique    entrada selecionável: Primary Attack + ação ativa + apresentação
Ability      comportamento executável referenciado por uma Technique
Strike       um golpe atômico cuja identidade é fixada quando ele começa
Effect       a ÚNICA forma de mudar estado: instantâneo/duração/periódico
Attribute    números com base+modificadores (vida, dano, mitigação, custo)
Tag          estado declarativo hierárquico (state.form.meteoric, immune.iframes)
Cue          apresentação por id, disparada pela Timeline com o MESMO relógio
```

Regra de ouro: **o núcleo de gameplay nunca toca eventos vanilla, atributos
vanilla ou entidades de outros mods diretamente**. Tudo passa por Effects e
pela DamagePipeline; o Minecraft/Forge é adaptado nas bordas.

---

## 3. As peças

### 3.1 Componentes por jogador (estado)

Uma capability única com um mapa pequeno de componentes. O estado durável é
serializado por **Codec** (versionável, sem strings NBT à mão); timelines e
outros estados efêmeros obedecem a uma política explícita de ciclo de vida:

```java
PowerIdentity   // ResourceLocation do PowerSet ("onepunchcrafts:boros")
AttributeMap    // valores base + modificadores ativos
EffectContainer // efeitos ativos (duração, stacks, fonte)
AbilityBook     // seleção, cooldown e timelines em andamento
ResourceMap     // pools nomeados (energy, ...) com regras de regen
TagSet          // tags ativas (derivadas de efeitos e formas)
```

Cada componente tem dirty-flag; sync desce por delta de componente — nunca o
objeto inteiro, nunca por reflection-compare.

Na política inicial, timelines são `CANCEL_ON_LOGOUT`: o Codec não promete
retomar ações em andamento. Torná-las retomáveis exigirá persistir timeline,
cursor, alvos capturados e parâmetros como uma decisão posterior explícita.

### 3.2 Ability + Timeline: um relógio só

```java
public final class SeriousPunchAbility extends TimelineAbility {
    @Override protected Timeline build(AbilityContext ctx) {
        return Timeline.ticks(96)
            .cue(0,  Cues.SERIOUS_WINDUP)                  // client: hush/desat/FOV
            .at(10,  t -> t.cue(Cues.DEBRIS_PULL))
            .at(14,  t -> {
                t.strikeCone(42, Strikes.SERIOUS_PUNCH);
                t.destroyCylinder(15, 1000);
                t.cue(Cues.SERIOUS_IMPACT);                // linha/cone/flash/cracks
            })
            .at(30,  t -> t.cue(Cues.SERIOUS_AFTERMATH));
    }
}
```

- A Timeline roda **no servidor** em ticks. O pacote de Cue carrega
  `(cueId, startTick, params)`; o cliente reproduz a fase certa a partir do
  mesmo tick de origem. Um relógio, duas projeções — física e visual não
  podem mais divergir.
- Timeline é estado do `AbilityBook`: relog no meio do windup cancela *limpo*
  pela política inicial `CANCEL_ON_LOGOUT` (hoje: lambdas órfãs no
  TickScheduler capturando player morto).
- Tipos novos de habilidade = **primitivas novas de Timeline/Effect**
  (canalizada, em área persistente, transformação, contra-golpe...), não
  interfaces novas.

#### 3.2.1 Technique roteia duas entradas; não vira estado do golpe

Uma Technique de soco tem dois gatilhos com propósitos diferentes. O clique é o
soco em si; a ativação executa a técnica composta ou oferece outra forma de
entregar o mesmo golpe. A seleção decide quem recebe os gatilhos, mas não é
consultada novamente durante os eventos ou ticks resultantes:

```java
PrimaryAttackIntent(target)
    → selectedTechnique.primaryAbility.onPrimaryAttack(context, target)
    → AttackPlan                              // identidade capturada

ActivateTechniqueIntent(techniqueId)
    → technique.activeAction
    → ability.activate(context)
    → Timeline                                // identidade capturada
```

- O clique esquerdo resolve a Technique selecionada **quando o ataque começa** e
  recebe um `AttackPlan`. Trocar de seleção depois não transmuta o plano.
- A ativação resolve a ação ativa da Technique e a Ability referenciada; sua
  Timeline armazena os `StrikeId`s
  que emitirá. Uma rajada de Normal Punch continua normal mesmo se o jogador
  mudar a seleção enquanto ela roda.
- `NormalPunchesInArea` invoca `Strikes.NORMAL_PUNCH` para cada alvo;
  `QuickBackStab` também invoca `Strikes.NORMAL_PUNCH` (o índice temporário
  usado pelo código atual é 1, que corresponde a Normal Punch). Ambos
  preservam o gameplay
  atual sem `setCurrentSkill(...)` temporário.

| Technique selecionada | `PrimaryAttackIntent` (clique) | `ActivateTechniqueIntent` |
|---|---|---|
| Normal Punch | um `NORMAL_PUNCH` no alvo | sequência de `NORMAL_PUNCH` no cone |
| Weak Punch | um `WEAK_PUNCH` no alvo | sequência de `WEAK_PUNCH` em área próxima |
| Serious Punch | Strike direto + sequência Serious | sequência Serious direcional, sem exigir alvo melee |

O Serious Punch reutiliza uma `SeriousPunchSequence` comum nas duas entradas;
somente o plano do clique inclui também o alvo direto. Assim o cast alcança
alvos distantes ou difíceis de acertar sem depender de um hit vanilla.

`Technique` não substitui `Ability`: a primeira roteia inputs e apresentação;
a segunda coordena execução/timeline. Strike é a unidade ofensiva reutilizável.

### 3.3 Effects: a única porta de mudança de estado

```java
Effect.instant(Attr.HEALTH, -damage)
Effect.duration("boros.ultra_regen", 100)      // +2.5M vida/tick por 5s
      .periodic(Attr.HEALTH, +REGEN_PER_TICK)
Effect.duration("saitama.punched", 4)          // substitui addTag+scheduler+scan
      .onExpire(ctx -> ctx.cue(Cues.DELAYED_EXPLOSION))
Effect.whileForm(Form.METEORIC, Attr.SPEED_MULT, 8.0)  // substitui setBaseValue por tick
```

- Combinação de efeitos é natural: stacking/refresh/override são políticas
  do container, não `if`s espalhados.
- Atributos vanilla (MAX_HEALTH, MOVEMENT_SPEED...) são atualizados por um
  AttributeSystem **só quando o valor calculado muda** — hoje cada pack
  reescreve todos os atributos todo tick.

### 3.4 DamagePipeline: o "flux" refeito como mecanismo próprio

```java
DamageSpec spec = strikeResolver.resolve(Strikes.SERIOUS_PUNCH, context);

// estágios fixos, ordem explícita, sem prioridade de EventBus:
TARGET_POLICY(regras canônicas)  // Saitama como alvo sobrevive
OUTGOING(attacker effects)        // buffs/modificadores do atacante
INTERCEPT(api, ordered by key)    // ← o "flux" para terceiros/compat vive AQUI
INCOMING(target mitigation)       // mitigação por tier/forma do alvo
CLAMP(caps, i-frame policy)       // cap por hit; tier decide se ignora i-frames
APPLY(kill-through se o tier manda)
VERIFY(resultado/fallback)        // garante Unstoppable para alvo elegível
```

- **Compat com criaturas de outros mods** (o motivo original do flux):
  handlers de `INTERCEPT` registrados via API com chave de ordenação tratam
  regras conhecidas antes da aplicação. Uma entidade que simplesmente nega
  `hurt()` — como o Wroughtnaut fora do ponto fraco — é tratada pela
  verificação/fallback de Unstoppable, não exige um interceptor especial.
- `TARGET_POLICY` resolve primeiro a Saitama Invulnerability. Para qualquer
  outro alvo, `Tier.SERIOUS` **aplica** morte pelo caminho do próprio
  pipeline — nunca mais des-cancelar `LivingDeathEvent`.
- I-frames são política por tier no `CLAMP` — a rajada nunca mais luta
  contra `invulnerableTime`.
- **Adapters de dano nas bordas**: `VanillaInbound` (dano vanilla/outros mods
  atingindo um personagem → vira DamageSpec `Tier.MUNDANE`+tags) e
  `VanillaOutbound` (DamageSpec aplicado a entidade comum → `hurt()` com
  DamageSource do mod). São os únicos pontos da resolução de dano que conhecem
  Forge/Minecraft; adapters de input, persistência e apresentação formam
  outras bordas independentes.

#### 3.4.1 "Unstoppable": a intenção dos `setCanceled`, promovida a conceito

O Serious Punch foi desenhado para não poder ser negado — hoje imposto
des-cancelando 4 eventos e dependendo da ordem dos handlers. Na v3 vira
propriedade do golpe, garantida por **resultado**, não por vencer corridas:

```java
DamageSpec.tier(SERIOUS)   // ⇒ unstoppable = true
```

- **Saitama como alvo**: `TARGET_POLICY` retorna `IMMUNE_CANON` antes de
  aplicar dano ou entrar na ladder. Isso é regra explícita e intencional:
  Saitama sobrevive ao Serious Punch de outro Saitama.
- **Demais alvos do próprio mod**: o pipeline não passa por eventos vanilla
  — não há o que cancelar; a mitigação vê o tier e sabe que não se aplica.
- **Alvos vanilla/de outros mods**: `VanillaOutbound` aplica e *verifica* o
  resultado (a vida caiu? morreu?). Se algo negou — evento cancelado em
  qualquer ponto, `hurt()` sobrescrito, i-frame, barreira — escala:
  DamageSource do mod → `fellOutOfWorld` → `setHealth`+`die()`. É o
  `pierceDefenses()` atual generalizado e promovido a garantia do núcleo.

Estritamente mais forte que os 4 `setCanceled`: derrota cancelamento em
qualquer ponto, inclusive mods que não usam eventos, sem violar a regra
canônica de invulnerabilidade do Saitama.

#### 3.4.2 Mapa: eventos vanilla → estágios do pipeline

Escolher "HurtEvent ou DamageEvent" hoje é escolher *onde na matemática do
dano* o handler se pendura — `WeakeningPunch` (Hurt, pré-armadura) e
`NormalPunch` (Damage, valor final) já exploram essa diferença sem que ela
esteja nomeada em lugar algum. Na v3 cada momento é um estágio declarado:

| Hoje (Forge 1.20.1/47.3.5) | Valor/momento exato | v3 |
|---|---|---|
| `LivingAttackEvent` | valor entregue a `hurt()`, antes de invulnerabilidade, i-frame e escudo | entrada do adapter/pipeline |
| `LivingHurtEvent` | valor admitido por `actuallyHurt()`, depois do gate de i-frame/escudo e antes de armadura, proteções/efeitos e absorção | pós-`INTERCEPT`, pré-`INCOMING` |
| `LivingDamageEvent` | valor final destinado à vida, depois de armadura, proteções/efeitos e absorção, mas antes de `setHealth` | pós-`INCOMING`/`CLAMP`, pré-`APPLY` |
| *(sem hook limpo no Forge)* | delta real de vida e resultado final (sobreviveu, totem, morreu) | observador pós-`APPLY` |
| `LivingDeathEvent` | caminho de morte alcançado depois de a prevenção por totem falhar | resultado do `APPLY` |

Interceptors e observers declaram o estágio e recebem o valor daquele
ponto. A ordem acima foi conferida na fonte da versão usada pelo projeto e
na [fonte oficial do Forge 1.20.x](https://github.com/MinecraftForge/MinecraftForge/blob/1.20.x/patches/minecraft/net/minecraft/world/entity/LivingEntity.java.patch):
`onLivingHurt` roda antes de `getDamageAfterArmorAbsorb` e
`getDamageAfterMagicAbsorb`; a absorção é consumida em seguida;
`onLivingDamage` recebe a parcela restante e roda imediatamente antes de
subtraí-la da vida. Cancelar `LivingDamageEvent` impede a subtração, mas
**não restaura** armadura ou absorção já consumidas.

Consequência concreta no código atual: o multiplicador de `WeakeningPunch`
em `LivingHurtEvent` ainda passa pelas reduções posteriores; o multiplicador
de `NormalPunch` em `LivingDamageEvent` acontece depois delas (e multiplica
somente a parcela que conseguiu atravessar a absorção). O pós-`APPLY` é o
ganho novo: informa o delta real de vida e o desfecho depois de prevenções
de morte, algo que nenhum desses dois eventos oferece.

### 3.5 Rede: intents descem decisões, nunca sobem estado

```
client → server:  CastIntent(abilityId) | AdjustIntent(statId, delta) | MoveInput(...)
server → client:  ComponentDelta(componentId, payload) | CueEvent(cueId, startTick, params)
```

Cliente não envia estado — envia *intenção*; o servidor valida e o novo
estado desce como delta. Elimina por construção: eco de sync, snapshot por
reflection, `handleTheDifferences`, e o campo inteiro de bugs de
auto-referência. (O `BorosMovementInputPacket` já segue este padrão — ele é
o modelo, não a exceção.)

### 3.6 Cues: apresentação plugável

`CueId → handler` registrado no client (partículas, VfxQuadBatch, cinemática,
PostChain, som). A infra de apresentação atual (VfxQuadBatch,
ManagedPostChain, cinemáticas) **entra inteira** — cues são só o novo
gatilho unificado com relógio compartilhado. Servidor dedicado nunca vê
classe de cliente: cue é um id.

### 3.7 PowerSet e balance como dado

```json
// data/onepunchcrafts/powersets/boros.json
{
  "attributes": { "max_health": 1.5e8, "attack_speed": 50 },
  "resources":  { "energy": { "max": 1e9, "regen": [...] } },
  "forms":      { "armored": { "mitigation": { "enhanced": 0.55 } }, ... },
  "abilities":  [ "boros/regeneration", "boros/flight", "boros/csrc", ... ]
}
```

- Personagem novo = JSON + as abilities genuinamente novas em código.
- Balance (o tuning desta semana) = editar JSON/config, zero recompile.
- **API para addons**: outro mod registra abilities/effects/cues/tiers/
  interceptors nos mesmos registries e declara um powerset próprio — a API
  pública é exatamente a mesma que o próprio mod usa (dogfooding).

### 3.8 Performance

- 4 sistemas centrais (Effect, Timeline, Attribute, Sync) iteram uma vez por
  tick sobre quem tem componente — em vez de cada pack fazer tudo por
  jogador (hoje: atributos reescritos todo tick, reflection-compare todo
  tick, scan de 100 blocos todo tick).
- Efeitos com duração expiram por fila de prioridade (tick alvo), não por
  varredura.
- Sync por dirty-flag + codec — nada de reflection no hot path.

---

## 4. Os golpes reescritos (prova de conceito no papel)

**Primary Attack / Strike selecionado:** o clique esquerdo pede um
`AttackPlan` à skill de soco selecionada. Normal/Weak emitem um único Strike;
Serious emite o Strike direto e inicia a sequência comum. A seleção roteia o
comando, mas não vira estado temporário do pipeline.

**Consecutive Normal Punches:** `TimelineAbility` de 100 ticks com cadência
acelerante (intervalo cai de 4 para 1 tick). Cada onda varre o cone de mira
direcionável com um Strike próprio de barrage, `NORMAL_BARRAGE_STRIKE`, cuja
política de i-frame é `IGNORE` e que não aplica explosão/knockback por hit.
As ondas amortecem o movimento dos alvos e o último beat lança sobreviventes;
`cue(BARRAGE)` inicia uma única apresentação sincronizada para os 100 ticks.
O `Strikes.NORMAL_PUNCH` continua reservado ao soco individual e às skills
compostas que realmente entregam um Normal Punch completo.

**Consecutive Weak Punches:** a ativação cria uma timeline de 100 ticks que
emite `Strikes.WEAK_PUNCH` contra alvos na área próxima. A timeline não
reconsulta qual skill está selecionada a cada golpe.

**Normal Punches in Area:** a timeline mantém a fila de alvos capturada no
cast; em cada passo teleporta e invoca `Strikes.NORMAL_PUNCH` diretamente.
A seleção visível do jogador permanece em Normal Punches in Area durante
toda a execução.

**Serious Punch:** §3.2/3.2.1 acima — cast e clique selecionado reutilizam a
mesma sequência; Saitama-alvo sobrevive por `TARGET_POLICY`; para os demais,
`VERIFY` garante Unstoppable. Uma timeline, quatro `setCanceled(false)` a
menos, cracks/debris/cinemática pendurados no mesmo relógio.

---

## 5. Mapa anti-bug (requisito → mecanismo)

| Classe de bug hoje | O que a elimina na v3 |
|---|---|
| Ordem de execução de eventos | Pipeline com estágios fixos; vanilla só nas bordas |
| Technique selecionada usada como relay | seleção só roteia o intent inicial; `AttackPlan`/Timeline capturam IDs |
| Des-cancelar morte/dano | `TARGET_POLICY` preserva Saitama; `APPLY`/`VERIFY` garantem SERIOUS nos demais |
| I-frames engolindo rajada | Política de i-frame por tier no `CLAMP` |
| Eco/auto-referência de sync | Intents sobem, deltas descem; estado nunca sobe |
| Timer visual ≠ timer físico | Timeline única no servidor; cue carrega `startTick` |
| Lambdas órfãs no scheduler | Timeline é estado explícito do AbilityBook e cancela no logout |
| Tag+scan global (`targetnormalpunch`) | Effect com `onExpire` no alvo |
| Atributos reescritos por tick | AttributeSystem aplica só quando muda |
| Crash client-class em servidor dedicado | Cue é id; handler só existe no client |
| Índices mágicos de skill | Tudo por ResourceLocation em registry |

---

## 6. O que se preserva do código atual

A camada de apresentação inteira (VfxQuadBatch, ManagedPostChain, as duas
cinemáticas, sons/i18n do CSRC), o pipeline de destruição do CSRC
(DestructionStages com budget adaptativo), o movimento do Boros (vira um
`MovementController` plugado ao PowerSet), a compat Draconic/Wroughtnaut
(vira interceptors), e os *valores* de balance recém-calibrados (viram o
primeiro JSON).

---

## 7. Custo honesto

- **Núcleo** (componentes+codecs, Effect/Timeline/Attribute/Sync systems,
  DamagePipeline, registries, intents/deltas): é um framework pequeno — a
  maior fatia do trabalho, e precisa nascer com testes (é 100% testável sem
  Minecraft aberto, ao contrário de hoje).
- **Port Saitama** (fatia vertical: 3 socos + dash + speed): valida o núcleo.
- **Port Boros** (formas, energia, voo, beams, CSRC): o maior port, mas o
  CSRC quase todo é apresentação que já está pronta.
- **Risco real**: re-tuning de gameplay (dano absoluto por tier ≠
  multiplicador) e um período com dois sistemas coexistindo.
- Saves antigos: manter leitura do NBT legado → migrar para componentes na
  primeira carga (perda aceitável: skill selecionada).

## 8. Recomendação e relação com o RFC 0001

Três caminhos:

- **(A) Incremental (RFC 0001):** menor risco, mas mantém o teto — poderes
  continuam morando no EventBus e o sync continua bidirecional. Os itens
  F1/F2/F3 de lá são, na prática, aproximações tímidas de §3.2/§3.4/§3.5.
- **(B) Big-bang rewrite:** o mod para de evoluir até tudo portar. Não.
- **(C) Núcleo novo + port por fatia vertical** *(recomendado)*: construir o
  runtime novo nos pacotes definitivos sem dependência do legado; portar Saitama;
  portar Boros; deletar o legado. O mod compila e roda em todas as etapas —
  personagens ainda não portados continuam no sistema velho até a sua vez.

A estratégia (C) foi aceita. O RFC 0001 está **superseded**; suas boas ideias
— IDs, tiers, dados de balance e dispatcher único — aparecem aqui em forma
final. As decisões vigentes estão em `docs/adr/0001` a `0005`.
