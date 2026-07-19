# Referências visuais — Techniques de Saitama

**Data:** 2026-07-19
**Escopo:** referências para direção visual e critérios usados na implementação.

## Fontes primárias

- [Índice oficial de vídeos do anime](https://onepunchman-anime.net/news/movie/):
  inclui a abertura sem créditos da primeira temporada e retrospectivas oficiais.
  A abertura é a principal referência para a leitura de uma parede ampla de
  socos, não apenas impactos isolados.
- [Guia oficial de episódios da primeira temporada — TV Tokyo](https://www.tv-tokyo.co.jp/anime/onepunchman/sp/episodes/index.html):
  episódio 5 (treino de Saitama e Genos) para força controlada e pressão que
  continua atrás do alvo; episódio 12 (Boros) para Consecutive Normal Punches,
  salto lunar e Serious Punch.
- [Guia oficial da segunda temporada — TV Tokyo](https://www.tv-tokyo.co.jp/anime/onepunchman2/episodes/index_2.html):
  episódio 14 para a disputa de velocidade com Sonic e o uso de pós-imagens.
- [One-Punch Man, capítulo 168 — VIZ/Shonen Jump](https://www.viz.com/shonenjump/one-punch-man-chapter-168/chapter/25406):
  referência oficial para o Serious Fart e para a escala ambiental/cômica dos
  movimentos tardios da Serious Series.
- [Anúncio oficial de One Punch Man: World](https://onepunchman-anime.net/news/archives/4763):
  referência secundária de adaptação: jogo licenciado que recria combates do
  anime e precisa traduzir suas cenas em leitura interativa.

## Evidência canônica versus inferência

Há referência direta para Normal Punch, Consecutive Normal Punches, Serious
Punch, velocidade/pós-imagens, saltos extremos e Serious Fart. Weakening Punch,
Quick Backstab, Normal Punches in Area e os controles de atributos são domínio
do OnePunchCrafts, não golpes canônicos com apresentação fixa. O VFX deles deve
usar a gramática física do personagem — ar comprimido, poeira, detritos, água,
pós-imagem e reação do alvo — sem inventar aura ou energia mágica.

## Gramática visual resultante

1. Saitama permanece visualmente simples; o mundo ao redor denuncia sua força.
2. Branco, preto/cinza, poeira do material e água dominam. Vermelho e dourado
   são acentos breves ligados ao traje, não uma fonte de energia.
3. Weak, Normal e Serious se distinguem por composição e consequência, não por
   apenas multiplicar o tamanho da mesma explosão.
4. Controles persistentes só geram VFX quando se manifestam: movimento, impacto,
   salto, nado, quebra ou resistência. Ativar o estado não cria aura contínua.
5. Pós-processamento, flashes e camera shake precisam de opções de intensidade;
   observadores distantes devem ler o efeito pelo ambiente, não pela câmera.

## Decisões de implementação

- Weak Punch, Normal Punch, Serious Punch e suas rajadas mantêm o renderizador
  anterior como perfil `Original`; o perfil `Novo` pode ser escolhido ao vivo
  no menu de VFX da tecla H.
- A escolha do perfil é local: cada jogador pode comparar as apresentações sem
  alterar dano, timeline ou a preferência visual dos demais clientes.
- Weak Punch novo usa contato pequeno e controlado; Normal Punch usa disco de
  ar e pressão que atravessa o alvo; as duas rajadas possuem composições
  distintas (bolha curta alternada versus parede ampla de punhos).
- Serious Punch novo usa silêncio, matéria atraída para o punho, linha branca e
  separação do céu/mundo. A frente de destruição continua acompanhando o avanço
  confirmado pelo servidor.
- As demais técnicas recebem eventos semânticos somente quando o fenômeno
  ocorre: teleporte, knockback, passo/pouso, nado, salto, quebra, deslocamento
  extremo ou acionamento do Serious Fart. Toggle parado não emite aura.
