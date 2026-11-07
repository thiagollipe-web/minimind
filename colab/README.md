# Verificação automática no Colab

1. Abra `minimind_lora_verification.ipynb` no Google Colab (Arquivo → Fazer upload, ou via GitHub).
2. Ambiente de execução → Alterar tipo → GPU T4 (opcional; funciona em CPU).
3. Ambiente de execução → **Executar tudo**.

O notebook valida os presets LoRA do app, treina o preset MiniMind (SmolLM2-135M como substituto) e imprime um relatório PASS/FAIL. A última célula falha (`assert`) se algum teste falhar.

Nota: o notebook não foi executado no ambiente de desenvolvimento (sem acesso ao Hugging Face); apenas a sintaxe foi validada.
