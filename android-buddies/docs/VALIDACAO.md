# Validação — 16/10/2026

Executado neste ambiente com Python 3, JDK 17, plataforma Android 35 e ferramentas Android 35.0.0.

| Verificação executada | Resultado |
|---|---|
| Compilação de todas as classes Java Android, geração de Dex e recursos | Passou |
| Geração dos 12 APKs e verificação criptográfica das assinaturas | Passou |
| Application IDs distintos e catálogo correto dentro de cada APK | Passou |
| Ausência de permissão INTERNET em cada APK | Passou |
| Núcleo STDP: spikes, pesos alterados, congelamento, limites e persistência | Passou; 83 spikes no cenário principal |
| Gestos, limiar de arraste, bordas e controle de eventos de som | Passou em lógica pura; sem toque em aparelho |
| Cuidados e missões com recompensa única | Passou em lógica pura |
| Laboratório visual sintético | 12/12 antes e 12/12 depois; 14.585 pesos alterados, variação média 0,00002290; não comprova ganho de acurácia |
| Política adaptativa com dados sintéticos | 18.211/20.000 escolhas da ação com melhor média; exploração e propensões verificadas |
| Esquema e operações SQLite com perfis e recompensa única | Passou em SQLite de desktop; não executa a classe Android |
| Estrutura dos catálogos | 12 matérias, 36 atividades + 36 revisões |
| Geração de áudio PCM | Reações distintas; 8.379 amostras, pico 8.188 sem saturação |

Não executados: instalação em dispositivo, testes de UI, audibilidade real, permissões em fabricantes diferentes, balão com teclado, rotação e encerramento do serviço pelo sistema, coordenação com todos os APKs instalados, desempenho/bateria em uso prolongado, restauração após atualização, avaliação pedagógica com estudantes, build remoto do workflow GitHub. O código foi compilado novamente após as mudanças do balão e coordenação.

Para reproduzir os testes principais: `python tools/test_project.py`. Para o áudio, após o build, compile `tests/ChirpTest.java` usando o classpath da plataforma Android e `build-family/classes`, e execute a classe com o mesmo classpath.

A assinatura é de desenvolvimento. Não há publicação na Play Store nem afirmação de prontidão para produção.
