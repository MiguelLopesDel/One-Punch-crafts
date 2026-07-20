# Changelog

## 1.3.0.0 — mudanças desde a 1.2.0.0

### Novo seletor de técnicas

- Adicionada uma roda radial de técnicas para Saitama e Boros.
- Segure `R` para abrir a roda; toque em `R` para voltar à técnica anterior.
- O clique esquerdo executa o ataque primário da técnica selecionada; `Z` ativa seu comportamento especial, como as rajadas de Socos Normais ou Fracos.
- Técnicas agora possuem artes próprias e aparecem com a imagem completa também na HUD.
- Toggles e valores, como velocidade, podem ser ajustados diretamente na roda.
- Melhor suporte para mouse, touchpad e seleção por clique.

### Saitama

- Socos ganharam animações refeitas, com movimento corporal melhor e uso alternado dos dois braços.
- **Socos Normais Consecutivos** foi reformulado: cadência crescente, grande área de efeito, alvos pressionados pela rajada e explosão final com destruição do terreno.
- Novos VFX para as técnicas do Saitama, incluindo socos, rajadas, Dash, velocidade, saltos e habilidades de área.
- No menu `H`, cada técnica pode usar individualmente o VFX **Original** ou **Novo**.
- A escolha visual pertence ao jogador que executa a técnica e é vista da mesma forma por todos no servidor.
- O Soco Sério recebeu melhorias de cinematic, impacto, partículas e destruição.
- Vários Socos Sérios podem acontecer ao mesmo tempo sem um cancelar os efeitos do outro.
- Saitama continua invulnerável, inclusive contra o Soco Sério de outro Saitama.

### Boros

- Boros agora também usa a roda radial, com duas páginas e artes próprias para suas técnicas.
- Vida máxima ajustada para `150.000.000`.
- Formas Armored, Released e Meteoric Burst agora possuem mitigação e resistência próprias; um Soco Normal não derrota mais Boros instantaneamente.
- Regeneração, recuperação de energia e duração do estado Exhausted foram rebalanceadas.
- O CSRC consome a energia restante e fica mais poderoso conforme a quantidade de energia investida.
- VFX de rajadas de energia, Roaring Cannon e CSRC foram estabilizados e otimizados.

### Soco Dimensional

- Nova técnica acionada pela tecla `O`: escolha uma dimensão e Saitama rompe o espaço com um soco.
- Inclui animação de soco, rachadura espacial, shader, sons, partículas e impacto.
- Com Immersive Portals instalado, abre um portal dimensional real.
- Sem Immersive Portals, cria uma fenda dimensional própria, alta, emissiva e animada — o antigo cubo foi removido.
- O portal permanece disponível para travessia e fecha quando seu criador o atravessa ou quando o tempo expira.
- A tecla duplicada `K` foi removida; `O` é o único atalho do Soco Dimensional.

### Correções e melhorias

- Trocar entre Saitama, Boros e jogador comum agora remove corretamente os poderes e atributos anteriores.
- Corrigida a necessidade de morrer ou reconectar para atualizar personagem, HUD e roda de técnicas.
- Estado do personagem agora é restaurado corretamente após login, respawn e troca de dimensão.
- Corrigidos travamentos, piscadas e seleções inconsistentes da roda radial.
- Corrigido o portal dimensional sendo ativado duas vezes e se fechando imediatamente.
- Limites de vida, dano, knockback, gravidade e outros atributos são ajustados automaticamente pelo AttributeFix.
- AttributeFix agora é uma dependência obrigatória e não precisa mais ser configurado manualmente para os valores do mod.
- Diversas melhorias de desempenho e estabilidade nos sistemas de dano, timelines, rede, VFX e destruição.
