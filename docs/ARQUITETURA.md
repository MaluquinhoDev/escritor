# Arquitetura de software — Escritor

## 1. Visão do produto
O Escritor é um estúdio de criação textual offline-first. Seu domínio é a obra narrativa, não apenas um arquivo de texto. O modelo precisa acomodar conto, novela, romance, série, light novel, web novel, roteiro, poesia, crônica, memória e narrativa experimental.

Princípios:
- O manuscrito pertence ao autor: armazenamento local por padrão, exportação aberta e nenhuma dependência de conta.
- Escrita sem conexão com internet.
- Baixa latência no editor; operações de banco fora da thread principal.
- Recursos opcionais não devem atrapalhar a escrita.
- Ferramentas de orientação explicam técnicas; não impõem fórmulas estéticas.
- Formatos e extensões são dados do domínio, não telas codificadas para um único gênero.

## 2. Plataforma e escolhas técnicas

| Camada | Tecnologia | Responsabilidade |
|---|---|---|
| Aplicativo | Android nativo, Kotlin | Integração com sistema e ciclo de vida |
| Interface | Jetpack Compose + Material 3 | Telas declarativas e acessíveis |
| Estado | ViewModel + StateFlow | Estado de tela resiliente a recriações |
| Persistência | Room / SQLite | Projetos, capítulos, cenas, personagens e relações |
| Preferências | DataStore | Tema, fonte, espaçamento, metas e preferências |
| Concorrência | Coroutines + Flow | I/O assíncrono e atualização reativa |
| Exportação | SAF + formatos abertos | TXT, Markdown, DOCX e EPUB em etapas |
| Testes | JUnit, AndroidX Test, Compose UI Test | Domínio, banco e interface |
| CI | GitHub Actions | Build reproduzível e APK como artefato |

Por que nativo: editor é uma interface de uso frequente e sensível a latência, teclado, seleção, acessibilidade, arquivos e ciclo de vida Android. Compose reduz a complexidade de manter interfaces e estados distintos. Room oferece consultas observáveis e relações consistentes sem exigir servidor.

## 3. Arquitetura lógica

Estrutura recomendada:
- app/ui: home, editor, outline, characters, worldbuilding, techniques, review e settings.
- app/domain/model: entidades e regras de negócio.
- app/domain/usecase: criar obra, mover cena, contar palavras e validar continuidade.
- app/domain/repository: contratos.
- app/data/local: Room, DAOs e migrations.
- app/data/repository: implementações.
- app/data/export: TXT, Markdown, DOCX e EPUB.
- app/core/editor: estado de edição, seleção e autosave.
- app/core/text: contagem, estatísticas e normalização.
- app/core/backup: exportação e importação de backup.

Fluxo recomendado:
Compose Screen → ViewModel → Use Cases → Repository → Room / arquivos locais.

A interface não deve executar SQL nem conter regras de domínio. O ViewModel expõe estado imutável e recebe ações. Repositórios isolam Room e sistema de arquivos, permitindo testar regras sem Android.

## 4. Modelo de domínio

Entidades principais:
- WritingProject: id, título, formato, gênero, sinopse, idioma, estado, datas.
- Volume: volume de uma série ou light novel.
- Chapter: capítulo com ordem, título, resumo, estado e metadados.
- Scene: unidade reorganizável dentro de capítulo, com objetivo, conflito, mudança e POV.
- Character: personagem, motivação, desejo, necessidade, contradições, arco e descrição.
- Place: localização ficcional e características.
- WorldElement: objeto, organização, espécie, sistema mágico, tecnologia ou regra.
- TimelineEvent: evento, data ficcional, participantes e relações causais.
- Relationship: relação entre personagens/entidades, tipo e evolução.
- ResearchNote: referência, anotação, origem e vínculo com elementos da obra.
- RevisionSnapshot: versão recuperável de um texto.
- WritingGoal: meta diária, sessão e histórico.

Relações: Project 1:N Volume; Project 1:N Character/Place/WorldElement; Volume 1:N Chapter; Chapter 1:N Scene; Scene N:M Character/Place; TimelineEvent N:M entidades. O conto pode usar um projeto sem volumes e com um único capítulo; uma série pode conter vários volumes.

Regras importantes:
- Ordem de capítulo/cena deve ser explícita, não depender do horário de criação.
- Exclusões de projeto devem ser transacionais e precedidas por confirmação.
- Autosave deve usar debounce e gravar apenas conteúdo alterado.
- Histórico deve preservar snapshots antes de revisões destrutivas.
- Contagem de palavras deve considerar espaços Unicode e travessões sem corromper o texto.
- Exportação deve preservar ordem, títulos e quebras de parágrafo.
- Alterações de esquema exigem migrations testadas; não usar destructive migration em manuscritos.

## 5. Módulos funcionais

### A. Estúdio de escrita
Editor sem distrações, tamanho de fonte, tema, largura de linha, foco, contador de palavras/caracteres, meta de sessão, desfazer/refazer, autosave e recuperação de rascunho.

### B. Estrutura narrativa
Árvore de projeto, volumes, atos, capítulos e cenas. Reordenação por arrastar. Modos configuráveis: linear, três atos, cinco atos, episódico, serializado e livre. Nenhum modelo obrigatório.

### C. Bíblia da história
Personagens, lugares, objetos, facções, regras do universo, glossário, relações e cronologia. Menções no manuscrito podem apontar para registros, mas sem alterar automaticamente o texto.

### D. Laboratório de técnicas
Conteúdo didático organizado por objetivo, modalidade, complexidade e exemplo. Exercícios curtos e fichas de diagnóstico. As sugestões são explicáveis e opcionais.

### E. Revisão
Leitura por cena, repetição lexical, frases longas, consistência de nomes, linha temporal, POV e checklist editorial. Diagnósticos devem mostrar evidência e trecho, não reescrever silenciosamente a voz do autor.

### F. Publicação e intercâmbio
Importar/exportar TXT e Markdown primeiro; depois DOCX e EPUB. Exportar backup completo em ZIP com manifesto JSON. Compatibilidade precisa ser validada com testes de round-trip.

## 6. Privacidade e integridade
O MVP não envia texto para servidores. A arquitetura futura deve manter análise offline como padrão. Qualquer integração externa/IA exigiria consentimento explícito por ação, indicação do conteúdo enviado e opção de não enviar manuscrito completo. Backup é cópia, não sincronização; conflitos de sincronização exigem resolução explícita.

## 7. Estratégia de testes
- Unitários: contagem de palavras, ordenação, regras de exclusão, validação de relações.
- Room: CRUD, integridade referencial, migrations e consultas Flow.
- UI: criar projeto, abrir capítulo, editar, salvar, fechar e reabrir.
- Exportação: round-trip de acentos, travessões, aspas, parágrafos e ordem.
- Desempenho: manuscrito longo, lista extensa de capítulos e digitação contínua.
- Recuperação: encerramento abrupto durante edição e restauração do último autosave.

## 8. Roadmap
1. MVP atual: projetos, formatos básicos, capítulos, editor e banco local.
2. Fundação editorial: cenas, reordenação, autosave, snapshots, backup e exportação TXT/MD.
3. Planejamento: personagens, locais, relações, cronologia e fichas.
4. Técnicas: biblioteca consultável, exercícios e checklists específicos por modalidade.
5. Revisão: ferramentas linguísticas locais, consistência e relatórios de leitura.
6. Publicação: DOCX/EPUB, modelos editoriais e preparação para plataformas.
7. Integrações opcionais: sincronização, colaboração e assistência inteligente, sempre com controle do autor.

## 9. Estado implementado e limitações do primeiro APK
O primeiro incremento contém uma aplicação nativa Compose, persistência Room para projetos/capítulos, criação e exclusão de projetos, seleção de formato, editor básico e contagem de palavras. Ainda não implementa autosave, cenas, personagens, exportação, histórico de versões, sincronização nem todos os módulos descritos. O roadmap distingue arquitetura-alvo de funcionalidades efetivamente entregues.
