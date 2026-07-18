# ADR 0005 — Roda de Techniques e troca rápida

**Status:** aceito

A seleção rápida usa páginas radiais de no máximo oito Techniques. Segurar a
tecla abre a roda, scroll troca a página e soltar confirma o ID apontado; um
toque curto alterna as duas Techniques mais recentes. A roda nunca executa ao
selecionar e exibe separadamente Primary Attack e Technique Activation.

Touchpads podem bloquear movimento enquanto uma tecla está pressionada. Se o
cursor não se moveu desde a abertura, soltar a tecla mantém a roda aberta e a
seleção passa a ser confirmada por clique; com mouse normal, mover e soltar
continua sendo o caminho rápido.

O cliente envia o ID escolhido, mas o servidor valida a posse, mantém a seleção
e seu histórico e devolve o delta autoritativo. Isso evita índices persistidos,
pacotes de direção dependentes da ordem do loadout e corridas entre seleção e uso.
