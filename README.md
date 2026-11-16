# Escritor — Android

Aplicativo nativo para criar, organizar e escrever histórias em português.

## Recursos desta versão

- Projetos para romance, conto, light novel, web novel, roteiro, poesia, crônica e fanfic.
- Sinopse associada ao projeto.
- Organização e reordenação de capítulos.
- Cenas com objetivo, resumo e conflito.
- Planejamento de personagens, lugares, universo, relações e cronologia.
- Notas de pesquisa e metas de escrita.
- Diagnóstico heurístico de repetições e frases longas.
- Snapshots manuais e restauração de versões de capítulos.
- Editor de texto com contagem de palavras e caracteres.
- Tema escuro e tema claro com preferência persistente.
- Salvamento automático local com debounce.
- Exclusão de projetos e capítulos com confirmação.
- Importação de TXT e Markdown.
- Exportação em TXT, Markdown, DOCX e EPUB.
- Backup ZIP textual do manuscrito.
- Biblioteca inicial de técnicas narrativas.
- Ícone adaptativo próprio para Android.
- Banco de dados Room/SQLite; funciona sem conta e sem conexão.

## Tecnologia

- Kotlin, Android nativo e Jetpack Compose.
- Material 3.
- Room / SQLite.
- Kotlin Coroutines e Flow.
- Gradle 8.9, JDK 17 e Android SDK 35.

## Compilação

O GitHub Actions compila o APK de depuração em pushes na branch `main`, pull requests e execução manual.

Arquivo gerado:
`app/build/outputs/apk/debug/app-debug.apk`

Artefato do Actions:
`Escritor-debug-apk`

## Planejamento

O roadmap completo está em [Arquitetura — Roadmap de desenvolvimento](docs/ARQUITETURA.md#8-roadmap-de-desenvolvimento). Ele distingue recursos implementados, pendências e integrações futuras.

## Documentação

- [Arquitetura](docs/ARQUITETURA.md)
- [Técnicas de escrita](docs/TECNICAS-DE-ESCRITA.md)
- [Comparação de aplicativos](docs/ECOSSISTEMA-DE-APPS.md)

## Privacidade

Os manuscritos são armazenados no banco local do aplicativo. Esta versão não exige conta, não possui telemetria e não envia textos para serviços externos. Importação e exportação são iniciadas pelo usuário. O backup ZIP atual preserva o manuscrito em Markdown e metadados básicos, não é uma cópia integral do banco de dados.
