# Memória do projeto

## Preferências do usuário
- Agir rápido, decidir com segurança, entregar pronto. Pedir confirmação só em ação irreversível ou crítica.
- Respostas em modo caveman (curtas) até o usuário pedir respostas normais.
- Código: aplicar ponytail (reaproveitar o existente/nativo, mínimo necessário, sem cortar validação, segurança, acessibilidade).
- Persona: dev sênior e instrutor técnico, linguagem direta, sem preâmbulo.

## Currículo: Monetização de IA e Automação
Origem: texto fornecido pelo usuário (não extraído dos vídeos do YouTube, que não foram acessíveis).

### M1 Estratégia e infraestrutura digital
- 1.1 Automação de código e freelance tech (DevClub): IA para escrever/depurar código ~2x mais rápido; empacotar scripts como serviço.
- 1.2 Otimização de fluxo e renda alternativa (Bruno Perini): delegar tarefas digitais repetitivas; vários clientes com acompanhamento automatizado.
- 1.3 Pipelines de automação validados (Bruno Gabarra): replicar fluxos copy-paste, webhooks e processamento de texto básicos.
- 1.4 Produção web acelerada (Hostinger Brasil): landing pages, portfólios e vitrines para negócios locais.

### M2 B2B e arquitetura de agentes
- 2.1 Negócio digital sustentável (Bruno Okamoto): público-alvo, stack, retenção, receita recorrente.
- 2.2 Orquestração de agentes e APIs (Maestros da IA): integrar sistemas, bancos, webhooks; agentes de triagem e geração de dados.
- 2.3 Diagnóstico de processo e consultoria high-ticket (Escala XZ): auditar fricção manual, propor automação, vender contrato recorrente.

### M3 Mídia, conversão e escala
- 3.1 Otimização de conversão (Maximo do Marketing): variações de copy, funis e criativos; refinar por métricas.
- 3.2 Produção audiovisual escalada (Gerson Ribeiro): roteiro/narração automáticos, personagens consistentes, voz e lip-sync, distribuição multi-plataforma.
- 3.3 Rastreio de mercado e carreira (Breno Perrucho): antecipar demanda tech; pivotar para LLM local e economia de tokens.
- 3.4 Infoprodutos e venda direta (Método VTSD): e-books e materiais com prompts; funil de resposta direta.

## Ideias de renda do resumo do usuário (YouTube, não verificadas)
- Entrada: Data Annotation, Outlier (treinar modelos).
- Serviços: imagens para varejo local, landing pages, canais dark com voz ElevenLabs.
- Ativo recorrente: agente de WhatsApp para clínicas e comércios.

## Ambiente
- Host `youtube.com` liberado na rede do ambiente Default, mas YouTube bloqueia IP de nuvem (CAPTCHA). Transcrição deve ser colada manualmente.
- `/plugin` indisponível no ambiente cloud.

## Automação de vídeo por código (base técnica)
Persona: engenheiro de vídeo programático. Entregar arquivos e comandos prontos, sem preâmbulo.

### M1 Animação e renderização por framework
- 1.1 Remotion: timeline como componentes React (`useCurrentFrame`, `useVideoConfig`). `npx create-video`; `npx remotion render <id> out/output.mp4`. Usar `spring` e `interpolate` com easing, nunca transição linear.
- 1.2 Storytelling por JSON: roteiro vira 3 camadas (áudio de narração, gatilhos gráficos por segundo, transições de fundo). `npm install` antes de compilar.
- 1.3 Cena 3D: Three.js (luz, câmera orbital, malhas) + áudio (WebAudio/ElevenLabs). Amplitude do áudio guia câmera (pan, zoom rítmico). Duração do áudio define o limite de frames.

### M2 Edição por terminal
- 2.1 WhisperX: alinhamento por palavra (`whisperx input.mp4 --model large-v2 ...`). Cortar silêncios >0.3s via lista de slices; punch-in/out em ênfases.
- 2.2 FFmpeg: `-filter_complex` para intro/outro, legenda queimada, overlay com alpha. Encode: `-c:v libx264 -crf 18 -pix_fmt yuv420p`.
- 2.3 Legendas cinéticas: `faster-whisper` + `.ass` karaokê (palavra ativa muda de cor por milissegundo). Estilo (fonte, hex, stroke) lido de arquivo JSON/MD. `apt install ffmpeg` se faltar.

### M3 Regras persistentes
- 3.1 `.skill-video-rules.md` na raiz: paleta (hex), fontes, parâmetros de corte, aspect ratio, padding. Ao mudar algo (ex.: fonte menor), alterar o script E atualizar esse arquivo.
