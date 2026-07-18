# ADR 0002 — Rede com autoridade do servidor

**Status:** aceito

## Decisão

O cliente envia apenas intents (`CastAbility`, `SelectAbility`, `AdjustPower`).
O servidor valida o PowerSet/loadout e devolve deltas de componente. Snapshot
com Codec é reservado para login, respawn, migração e troca de identidade.

O NBT legado de Saitama é migrado na primeira leitura. Timelines em andamento
adotam a política inicial `CANCEL_ON_LOGOUT`; elas nunca deixam lambdas órfãs.

## Consequências

O cliente não pode mais reenviar um pack inteiro, reflection diff não participa
do fluxo v3 e seleção/toggles/atributos têm uma única autoridade.
