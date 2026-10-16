# Arquitetura implementada

## Três mecanismos separados

1. `EducationDb` armazena perfis, tentativas, estatísticas de estratégias, ensinamentos, versões e recompensas. As consultas de estudo usam o ID do perfil. O sandbox de cada application ID separa as matérias. Não há backend.
2. `PedagogyPolicy` escolhe estratégias por recompensa posterior. Para uma ação a no contexto c, usa a média Q(c,a) = soma das recompensas / número de revisões. Com probabilidade 0,12 explora uma das quatro ações uniformemente; no restante escolhe a maior média. Registra a propensão real da escolha (0,91 para a ação preferida; 0,03 para as demais). Empates usam a primeira ação. Uma revisão correta vale +1 e incorreta −1. Sem dica efetivamente solicitada não há atribuição de recompensa à estratégia. Atualização e marcação de revisão ocorrem na mesma transação SQLite.
3. `Brain` reconhece descritores visuais simples: 24×16 pixels, canais ON/OFF, 768 entradas, 48 neurônios LIF e inibição competitiva. Captura opcional a até 4 quadros por segundo; cada quadro usa 50 passos de simulação. Eventos são codificados a partir das mudanças locais de luminância. As redes da tela e da demonstração têm arquivos independentes.

O neurônio LIF acumula corrente e perde potencial por vazamento; ao cruzar o limiar, dispara e reinicia. A plasticidade usa traços exponencialmente decrescentes de eventos pré e pós-sinápticos. Em um evento pré, subtrai 0,00084 vezes o traço pós; em um disparo pós, adiciona 0,0008 vezes o traço pré, com limites de pesos e normalização suave. Assim, pré antes de pós tende a potencialização de longo prazo (**LTP**) e pós antes de pré tende a depressão de longo prazo (**LTD**). Não há retropropagação. A implementação é uma aproximação computacional local, não um modelo completo da aprendizagem biológica.

## Interface e execução

- `MainActivity`: painel do pet, permissão de sobreposição, consentimento de captura, som, posição, tamanho, memória geral e perguntas opcionais.
- `EducationActivity`: atividades com alternativas embaralhadas, revisão após 30 minutos, escolha de perfil e estratégia, ensino, aprovação humana, consultas e exportação.
- `BuddyArt`: doze personagens vetoriais com silhuetas e animações próprias. O mesmo código de desenho gera o mascote Android, os ícones de instalação e a prévia SVG.
- `OverlayService` + `RobotView`: pet arrastável, carinho, animações e som. Um laço no serviço verifica perguntas opcionais; sem alarmes exatos ou execução garantida em segundo plano.
- `StudyBubble`: janela pequena com campo de resposta, botão salvar pendente e atalho ao estudo. A captura ignora quadros enquanto o balão ou a interface estão visíveis, para não treinar com a própria UI.
- `CaptureService`: serviço de projeção somente com autorização, redução da imagem e mascaramento do pet. Não persiste screenshots completos.
- `FamilyCoordinator`: receiver protegido por permissão de assinatura. Ativar um mascote envia sinal de parada aos outros onze, sem compartilhar memória.
- `PetCare`/`PetState`: necessidades e missões. XP de estudo é um bônus derivado das recompensas únicas do perfil ativo, sem modificar pesos neurais.

A memória educativa usa `education.db`; memória geral e preferências de reação usam `memoria.db`; pesos visuais são arquivos binários locais. A aprovação de um texto não altera o catálogo de exercícios nem transforma o texto em pesos STDP.

## Extensão planejada, ainda não implementada

Ampliar e revisar editorialmente os catálogos; níveis escolares; importação e restauração de perfis; autenticação de professor; testes instrumentados; revisões inéditas suficientes para reduzir memorização; avaliação de retenção em dias; testes comparativos com atribuição aleatória e amostra adequada; porte desktop desta edição. O catálogo JSON tem estrutura simples para expansão, mas não deve ser tratado como um currículo validado.
