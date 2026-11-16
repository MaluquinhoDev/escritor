# Escritor

Aplicativo Android para planejar, escrever, organizar e revisar histórias em português.

## Estado atual

Primeiro MVP Android nativo:
- Projetos de escrita por formato (romance, conto, light novel e web novel).
- Estrutura de capítulos.
- Editor de texto com contagem de palavras.
- Persistência local usando Room/SQLite.
- GitHub Actions para compilar e publicar o APK como artefato.

## Arquitetura

Consulte [docs/ARQUITETURA.md](docs/ARQUITETURA.md) e [docs/TECNICAS-DE-ESCRITA.md](docs/TECNICAS-DE-ESCRITA.md).

## Build local

Requer JDK 17, Android SDK 35 e Gradle 8.9.

```bash
gradle :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Privacidade

O MVP salva os projetos no banco local do aplicativo. Não há conta, telemetria, sincronização ou envio automático de manuscritos.
