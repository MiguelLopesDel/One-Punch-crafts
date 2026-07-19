# ADR 0005 — Roda de Techniques e troca rápida

**Status:** aceito

A seleção rápida usa páginas radiais de no máximo oito Techniques. Segurar a
tecla abre a roda, scroll troca a página e soltar confirma o ID apontado; um
toque curto alterna as duas Techniques mais recentes. A roda nunca executa ao
selecionar e exibe separadamente Primary Attack e Technique Activation.

Techniques de controle são manipuláveis sem sair da roda. Ao apontar um toggle,
o painel central expõe seu estado e permite ligá-lo ou desligá-lo; ao apontar
uma Technique ajustável, o painel expõe botões de menos/mais e o scroll altera
seu valor; clicar na barra define um valor absoluto, validado, limitado e
alinhado ao passo declarado no servidor. O scroll só troca a página quando a
Technique apontada não for ajustável. Barras nos próprios slots comunicam
estado e valor sem depender do texto central.

Touchpads podem bloquear movimento enquanto uma tecla está pressionada. Se o
cursor não se moveu desde a abertura, soltar a tecla mantém a roda aberta e a
seleção passa a ser confirmada por clique; com mouse normal, mover e soltar
continua sendo o caminho rápido.

O cliente envia intents com o ID escolhido e, nos sliders, o valor solicitado.
O servidor valida posse, faixa e passo, mantém a seleção e seu histórico e
devolve o delta autoritativo. Isso evita índices persistidos, pacotes de direção
dependentes da ordem do loadout e corridas entre seleção e uso.
