# ADR 0002 — Rede com autoridade do servidor

**Status:** aceito

## Decisão

O cliente envia apenas intents (`ActivateTechnique`, `SelectTechnique`,
`SwapTechnique`, `AdjustTechnique`). O servidor valida o PowerSet e suas páginas
de Techniques e devolve deltas de componente. Snapshot
com Codec é reservado para login, respawn, migração e troca de identidade.

O NBT legado de Saitama é migrado na primeira leitura. Timelines em andamento
adotam a política inicial `CANCEL_ON_LOGOUT`; elas nunca deixam lambdas órfãs.

Preferências de apresentação também sobem como intent. O servidor persiste a
escolha por Technique e inclui o perfil no evento VFX emitido pelo autor; o
cliente observador não substitui a aparência pela própria preferência.

## Consequências

O cliente não pode mais reenviar um pack inteiro, reflection diff não participa
do fluxo de poderes e seleção/toggles/atributos têm uma única autoridade.
