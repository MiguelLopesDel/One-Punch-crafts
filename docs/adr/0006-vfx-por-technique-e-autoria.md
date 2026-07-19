# ADR 0006 — Perfil de VFX por Technique e autoria visual

**Status:** aceito

## Decisão

`Original` e `New` são perfis de apresentação escolhidos por Technique, não uma
opção global do cliente observador. A preferência pertence ao jogador que
executa a Technique, é persistida no `PowerState` e alterada por intent validada
no servidor.

Todo evento visual que possui versões A/B carrega o perfil resolvido pelo
servidor. Assim, todos os clientes renderizam a escolha do autor: se A usa
Normal Punch `New`, A e B veem o golpe de A como `New`; a preferência de B não
reinterpreta esse evento. Gameplay, dano e Timeline nunca consultam o perfil.

Techniques que não possuíam apresentação anterior tratam `Original` como a
ausência daquele VFX adicional. Dash e Speed preservam seus renderizadores
anteriores de forma explícita.

## Consequências

- o menu H edita uma matriz por Technique;
- preferências sobrevivem à troca de PowerSet;
- addons podem criar outros perfis no futuro sem tornar a câmera do observador
  dona da aparência de ataques alheios;
- pacotes de apresentação mudam de protocolo quando passam a carregar perfil.
