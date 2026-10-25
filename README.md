# Eternal Realms RPG — Minecraft 1.16.5 (Forge 36.2.39)

Fase 1 do plano: estrutura do mod, persistência, experiência por atividades, níveis, atributos
e habilidades secundárias (adendo obrigatório de XP). As Fases 2–7 ainda não foram iniciadas.

## Estado real (leia primeiro)

| Parte | Estado |
|---|---|
| Núcleo de regras (`core/`, Java puro) | Compilado com `--release 8` e **testado**: 89 verificações, 0 falhas (`./gradlew coreTest`) |
| Camada Forge (`event/`, `net/`, `data/`, `config/`, `client/`, `command/`) | **Escrita, mas NÃO compilada nem executada.** O ambiente onde foi feita não alcança o Maven do Forge/Mojang, então nenhuma API do 1.16.5 pôde ser checada pelo compilador |
| `.jar` | **Ainda não existe.** Gere-o com os passos abaixo |

Se o `gradlew build` acusar erro de símbolo/método, é nome de API do Minecraft errado em alguma
linha da camada Forge — envie a saída do erro que eu corrijo.

## Como compilar

Requisitos: JDK 8, acesso à internet para baixar Forge/Minecraft.

Opção A (recomendada, evita problemas de wrapper): baixe o MDK oficial
`forge-1.16.5-36.2.39-mdk` em files.minecraftforge.net, extraia-o, **substitua** `src/` pelo `src/`
deste projeto e `build.gradle` pelo deste projeto, depois:

```
./gradlew genEclipseRuns   # ou genIntellijRuns
./gradlew build
```

O jar sai em `build/libs/eternalrealms-1.16.5-0.1.0.jar`. Coloque em `mods/` de um Forge 1.16.5.

Opção B: com Gradle 7.x instalado (`gradle wrapper --gradle-version 7.6.4`) neste diretório.

Testar só o núcleo (sem Minecraft): `./gradlew coreTest`.

## O que a Fase 1 faz

- **Nível do personagem** (máx. configurável, padrão 500), independente da XP vanilla. Cada nível dá
  pontos de atributo e de habilidade; marcos (10, 25, 50, 75, 100, 150, 200, 300, 400, 500) dão extra.
- **15 habilidades secundárias** com nível, barra e XP próprios (máx. 100) e pontos por marco (a cada 25).
- **10 atributos principais** distribuíveis (limite 100 por padrão, configurável até 500). O servidor
  valida cada pedido; o cliente só envia a intenção.
- **XP por atividade**, calculada no servidor e sincronizada em lotes:
  mineração (minério/pedra/terra/obsidiana/troncos, com sobrescrita por bloco), colheita e plantio,
  construção, dano causado (corpo a corpo, arco, magia — sobre o dano real), dano recebido e bloqueio
  com escudo (só de hostis), abates (dividido por contribuição em multiplayer, dentro do raio),
  criação de animais, fabricação de equipamentos, fundição, reparo na bigorna, descoberta de biomas e dimensões.
- **Antiexploração**: bloco colocado por jogador não rende XP de mineração; colocar/quebrar na mesma
  posição não rende XP de construção de novo; teto de XP por fonte por minuto com retorno decrescente;
  retorno decrescente em receitas repetidas; mobs muito abaixo do nível rendem menos; FakePlayers e
  criativo/espectador não ganham; **sem XP passiva** por ficar parado.
- **Persistência** em dados do mundo por UUID (sobrevive a morte, troca de dimensão e reinício).
- **Interface**: tecla `K` abre a ficha (nível, XP, pontos, atributos com botão `+`, habilidades);
  HUD com barra compacta e notificações agrupáveis; tudo configurável (`eternalrealms-client.toml`).
- **Comandos**: `/rpg info`; (op) `/rpg givexp <n>`, `/rpg giveskillxp <hab> <n>`, `/rpg givepoints attr|skill <n>`.
- **Configuração** (`eternalrealms-common.toml`): níveis, curva, pontos, limites, tetos por fonte,
  fontes desativáveis, raio/participação mínima de abate, XP por bloco.

## Limitações conhecidas desta fase

- Nível dos mobs é **estimado** por vida e dano até a Fase 6 trazer níveis reais.
- Sem fonte de XP ainda: pesca, preparo de poções, descoberta de estruturas, missões/desafios,
  sobrevivência a noites, invocações, dano de sangramento/veneno/queimadura (dependem das fases 4–6).
  As habilidades Pesca, Alquimia, Sobrevivência, Domínio de armas e Invocação existem, mas ainda não evoluem.
- Atributos já são salvos e distribuíveis, mas **ainda não alteram o combate** (Fase 2).
- Abates por dano que não vem de um jogador (veneno, fogo) não registram participação.
- Quebra de bloco é detectada no evento `BreakEvent` (antes de o bloco sumir); outro mod que cancele
  em prioridade mais baixa ainda pode negar a quebra depois de a XP ser dada.

## Mods de referência (analisados)

| Mod | Versão | Loader | Uso |
|---|---|---|---|
| Origins | 1.16.5 0.7.3.9 | Forge | Referência para raças (Fase 4) |
| Project MMO | 1.16.5 3.69.0 | Forge | Referência de habilidades por atividade |
| Pufferfish's Skills | 1.16.5 0.5.0 | Forge | Referência de árvore de habilidades (Fase 3) |
| Passive Skill Tree | **1.20.1** 0.7.6e | Forge 47 | **Incompatível** com 1.16.5: só como inspiração de layout |

Nada foi copiado: tudo é implementação própria.
