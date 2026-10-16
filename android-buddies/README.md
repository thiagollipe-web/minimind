# MiniBuddies Educacionais — piloto Android 0.3.0

Doze bichinhos virtuais instaláveis separadamente, com código compartilhado em Java e conteúdo local em JSON. Android 8.0 ou superior. Aplicativos sem permissão de Internet, publicidade ou chave de API.

| Disciplina | Bichinho | APK |
|---|---|---|
| Matemática | Mati | MiniBuddy_matematica.apk |
| Inglês | Pip | MiniBuddy_ingles.apk |
| Português | Lumi | MiniBuddy_portugues.apk |
| Espanhol | Sol | MiniBuddy_espanhol.apk |
| Ciências | Nino | MiniBuddy_ciencias.apk |
| História | Clio | MiniBuddy_historia.apk |
| Geografia | Geo | MiniBuddy_geografia.apk |
| Sociologia | Soci | MiniBuddy_sociologia.apk |
| Filosofia | Sofia | MiniBuddy_filosofia.apk |
| Física | Volt | MiniBuddy_fisica.apk |
| Química | Atom | MiniBuddy_quimica.apk |
| Biologia | Bia | MiniBuddy_biologia.apk |

O percurso BNCC inicial contém **42 miniaulas, 42 exercícios e 42 revisões** no conjunto: 4 miniaulas por matéria do Fundamental e 3 por matéria do Médio. Cada ano/série disponível começa com uma miniaula e um exercício com revisão. Os 36 itens livres da edição anterior foram preservados para revisões antigas. O vínculo com cada habilidade é parcial, não uma cobertura curricular completa. Consulte o [mapeamento BNCC e as instruções dos balões](docs/BNCC.md). Cada personagem tem silhueta, rosto, acessórios, movimentos e ícone próprios. Os desenhos são originais. Veja a [família de personagens](docs/PERSONAGENS.md).

## Instalar e experimentar

1. Baixe e extraia o pacote dos APKs. Se já instalou a edição 0.1.0 ou 0.2.0 entregue nesta conversa, instale o novo APK por cima, sem desinstalar; a versão 0.3.0 usa os mesmos IDs e a mesma assinatura, preservando a memória local. Escolha uma matéria e instale seu APK. Autorize a instalação pelo aplicativo que abriu o arquivo, se solicitado pelo Android.
2. Abra o app e escolha **Miniaulas e exercícios BNCC no balão**. É necessária a permissão de aparecer sobre outros apps. No próprio balão, selecione o ano/série e use **Miniaula**, **Exercício** ou **Ensinar**. A resposta recebe correção ali mesmo. O painel **Estudar, ensinar e ver aprendizagem** também usa a seleção curricular.
3. Em **Modo aprendiz**, explique um conceito com um exemplo. O texto fica pendente. Em **Revisar, editar ou aprovar**, revise a exatidão antes de aprovar. Essa é uma revisão humana local, sem autenticação de professor.
4. Após 30 minutos da tentativa e da última abertura da miniaula correspondente, faça uma **Revisão sem ajuda**. Não precisa manter o app aberto durante a espera. O resultado fica associado à atividade original.
5. Para o bichinho flutuante, permita aparecer sobre outros apps e toque em **Mostrar mascote flutuante**. Arraste pelo corpo; toque para carinho; segure ou toque em `⋯` para abrir aulas, exercícios e ensino no balão. Há botões para lado esquerdo, direito e centro.
6. Ative balões periódicos se desejar: intervalo de 1, 3 ou 6 horas. O padrão é desligado. O modo usado por último no balão é lembrado. O app respeita 22h–7h do celular, descanso, tela apagada e painel aberto. Depende do serviço ativo e pode ser interrompido pelo Android; não há promessa de execução 24 horas.
7. Os sons usam o volume de mídia e podem ser silenciados. Há controle de volume e teste de som.

Ao ativar outro mascote da família, ele solicita que os outros encerrem os serviços de mascote/captura. A coordenação usa uma permissão de assinatura e não transmite textos nem capturas. Os APKs precisam ter sido assinados com a mesma chave para esse recurso funcionar.

## O que aprende

- **Estratégia pedagógica:** bandit contextual epsilon-greedy (12% de exploração) escolhe entre explicação, exemplo, pergunta orientadora e pré-requisito. O contexto combina habilidade e existência de erros anteriores. A escolha e sua probabilidade são registradas. Só uma revisão após 30 minutos, sem ajuda e de uma tentativa em que a dica da estratégia foi efetivamente exibida, pode atualizar a estratégia adaptativa; cada tentativa recebe essa atualização no máximo uma vez. Controles fixo e aleatório permitem registrar comparações, mas não demonstram causalidade por si só.
- **Memória ensinada:** registros textuais pendentes/aprovados/retirados, com datas e histórico de versões. Consultas retornam trechos aprovados que compartilham palavras com a busca, com identificação da origem local. Não há compreensão livre de linguagem ou geração de explicações.
- **Aprendizagem visual experimental:** rede LIF com STDP local para padrões visuais simples, a partir de capturas reduzidas autorizadas pelo usuário ou da demonstração sintética. Esse módulo está separado dos registros educacionais. STDP ajusta conexões; não verifica fatos, não entende textos e não aprende uma matéria inteira ao observar a tela.
- **Preferências de reação:** feedback explícito atualiza associações entre contexto e reação. Carinho também pode reforçar a reação corrente; isso não conta como resultado pedagógico.
- **Jogo:** energia, felicidade, vínculo, descanso, alimentação, missões, XP e acessórios. Não há morte nem perda de necessidades por tempo ausente. Os indicadores do jogo não medem inteligência.

Os perfis educativos isolam tentativas, estratégias e ensinamentos. Cuidados, rede visual e memória textual geral do mascote são compartilhados entre os perfis daquele APK; cada matéria tem armazenamento privado independente. Perfis locais não são contas protegidas por senha. O catálogo não muda automaticamente ao adicionar um ensinamento. Os códigos foram conferidos no PDF BNCC/MEC; a seriação do Ensino Médio é sugerida pelo app e Espanhol tem vínculo complementar à área de Linguagens.

## Ver evidências e guardar registros

O painel mostra tentativas e acertos sem ajuda, revisões posteriores e recompensas por estratégia. As três missões educativas premiam uma vez por perfil: primeiro acerto sem ajuda, primeira revisão posterior e primeira explicação aprovada (25 XP cada). Esse bônus soma-se ao nível do mascote; o total de XP educativo é mostrado no painel de estudos.

**Exportar estudo e memória JSON** exporta os ensinamentos, versões, aberturas de miniaulas e tentativas do perfil ativo, incluindo BNCC, ano/série e origem dos novos exercícios. O arquivo contém os textos ensinados: compartilhe conscientemente. Ainda não há restauração/importação desse formato educativo. A memória textual geral tem seu próprio importador/exportador e não deve ser confundida com a memória revisada de estudo. Desinstalar o app apaga os dados locais.

## Compilar e testar

Requisitos: Python 3, JDK 17 com módulo `jdk.compiler`, `keytool`, Android SDK com plataforma `android-35` e Build Tools 35.0.0. O caminho passado a `--sdk` pode conter espaços. Execute na pasta `android-buddies`:

```sh
python tools/test_project.py
python build_family.py --sdk /caminho/android-sdk
```

O builder compila Java/Dex uma vez, gera 12 manifestos com application IDs diferentes, inclui apenas o catálogo da matéria em cada APK, alinha, assina e verifica os pacotes. Saída em `dist/`, acompanhada de SHA-256. Não depende do projeto Gradle/Kotlin da raiz. A chave de desenvolvimento é gerada em `signing/` e não deve ir para o Git. Guarde a mesma chave para atualizar uma instalação sem desinstalar. APKs de builds com chaves diferentes exigem desinstalação, com perda dos dados não exportados. Para distribuição pública, prepare assinatura de release e testes em aparelhos.

O workflow do GitHub compila a família e disponibiliza os APKs como artefato; não publica na Play Store.

## Limites desta versão

- Interface, reprodução de áudio, teclado no balão, sobreposição, captura e coordenação entre apps ainda precisam de teste em aparelho real. Nenhum emulador foi usado nesta entrega.
- O teste SQLite executa o esquema e consultas em SQLite de desktop, não a classe Android inteira. Não substitui testes instrumentados, de ciclo de vida ou de concorrência.
- Catálogo inicial com seleção de ano/série; sem avaliação discursiva automática, fala/reconhecimento de voz, sincronização ou busca online. O exemplo curto e a explicação compartilham conteúdo neste piloto.
- Android pode encerrar processos; perguntas e captura só funcionam durante execução autorizada. A captura não é necessária para estudar e começa desligada. Não grava imagens completas no banco.
- O teste visual sintético já acertava 12/12 antes do treino; alterar pesos não comprova ganho de reconhecimento.
- A nova edição é Android; o programa Windows anterior não foi adaptado neste conjunto.

A arquitetura e a validação estão em [`docs`](docs/).
