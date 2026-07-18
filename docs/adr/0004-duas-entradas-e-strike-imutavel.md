# ADR 0004 — Duas entradas por Technique e Strike imutável

**Status:** aceito

Uma Technique selecionada pode receber dois comandos semanticamente distintos:
`Primary Attack` é o clique que executa o soco em si, enquanto `Technique
Activation` executa uma ação composta ou outra forma de entregar o golpe.
A seleção roteia apenas o início do comando; `AttackPlan` e `Timeline` capturam
os IDs dos Strikes e nunca reconsultam nem alteram a seleção durante a ação.

Normal e Weak Punch emitem um Strike no clique e suas respectivas rajadas na
ativação. Serious Punch usa a mesma sequência no clique e na ativação, mas só o
clique adiciona o alvo direto. Techniques compostas invocam Strikes explicitamente:
Normal Punches in Area e Quick Backstab emitem `NORMAL_PUNCH` sem trocar a Technique
visível do jogador.

Isso preserva a intenção do gameplay antigo sem transformar a seleção da UI em
um relay de combate. Trocar a seleção depois que uma ação começou não transmuta
os golpes já planejados.
