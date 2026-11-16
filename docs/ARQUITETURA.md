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

## 8. Roadmap de desenvolvimento

O roadmap abaixo separa funcionalidades já presentes no aplicativo, trabalho ainda pendente e recursos de longo prazo. A ordem prioriza segurança dos manuscritos e utilidade para o processo de criação.

### Etapa 1 — Fundação editorial
- [ ] Separar a interface em telas e componentes menores.
- [ ] Introduzir ViewModels e casos de uso para reduzir lógica nas telas.
- [ ] Criar cenas dentro dos capítulos.
- [ ] Reordenar capítulos e cenas sem perder conteúdo.
- [ ] Implementar histórico de versões e recuperação de snapshots.
- [ ] Criar backup completo e restauração.
- [ ] Testar autosave, encerramento inesperado e integridade do banco.

### Etapa 2 — Planejamento narrativo
- [ ] Cadastro e edição de personagens.
- [ ] Fichas de motivação, desejo, necessidade, contradições e arco.
- [ ] Cadastro de lugares, objetos, organizações e elementos do universo.
- [ ] Relações entre personagens e entidades.
- [ ] Linha do tempo de acontecimentos.
- [ ] Vínculos entre eventos, cenas e personagens.
- [ ] Organização por volumes, atos e sequências.

### Etapa 3 — Laboratório de escrita
- [ ] Ampliar a biblioteca de técnicas por modalidade.
- [ ] Criar fichas com definição, finalidade, exemplo e limitações de cada técnica.
- [ ] Incluir exercícios práticos e checklists.
- [ ] Cobrir conto, romance, novela, light novel, web novel, roteiro, poesia, crônica e fanfic.
- [ ] Permitir marcar técnicas e exercícios como favoritos ou concluídos.

### Etapa 4 — Revisão e continuidade
- [ ] Detectar repetições lexicais e frases excessivamente longas.
- [ ] Verificar consistência de nomes e termos.
- [ ] Criar relatórios de leitura por capítulo e cena.
- [ ] Analisar continuidade temporal e relações causais.
- [ ] Sinalizar mudanças de ponto de vista e problemas de continuidade com evidências.
- [ ] Manter toda sugestão editorial opcional, sem alterar o manuscrito automaticamente.

### Etapa 5 — Editor avançado
- [ ] Desfazer e refazer.
- [ ] Personalização de fonte, tamanho, espaçamento e largura de linha.
- [ ] Modo de concentração.
- [ ] Metas de palavras e sessões de escrita.
- [ ] Estatísticas de evolução por projeto.
- [ ] Preferências persistentes, incluindo tema claro/escuro.

### Etapa 6 — Intercâmbio e publicação
- [ ] Importar TXT e Markdown.
- [ ] Exportar Markdown preservando estrutura.
- [ ] Exportar DOCX e EPUB.
- [ ] Criar modelos de formatação editorial.
- [ ] Preparar manuscritos para plataformas de publicação.
- [ ] Validar importação/exportação com testes de round-trip.

### Etapa 7 — Integrações opcionais
- [ ] Sincronização entre dispositivos.
- [ ] Resolução explícita de conflitos de versões.
- [ ] Compartilhamento e colaboração.
- [ ] Assistência inteligente opcional, com consentimento por ação e transparência sobre o conteúdo enviado.

### Critérios de conclusão
- Nenhuma operação de edição pode perder silenciosamente texto já salvo.
- Migrações de banco devem preservar dados e ter testes.
- Exportações devem preservar acentos, travessões, parágrafos, ordem e títulos.
- Recursos de análise não podem reescrever a voz do autor sem solicitação explícita.
- O app deve continuar utilizável offline para as funções centrais.
- Builds e testes automatizados devem passar antes de considerar uma etapa concluída.

# Estado da implementação (PR em desenvolvimento)

A implementação atual ampliou o MVP com persistência local para planejamento narrativo e ferramentas editoriais. O status abaixo evita confundir estrutura de dados com uma experiência já completa.

## Implementado nesta entrega
- Migração Room 1 → 2 sem descartar projetos e capítulos existentes.
- Entidades e DAOs locais para cenas, personagens, lugares, elementos do universo, relações, cronologia, notas, metas, snapshots e preferências.
- Interface inicial para cadastrar e excluir esses elementos.
- Reordenação de capítulos.
- Snapshots manuais de capítulos e restauração de conteúdo.
- Diagnóstico heurístico de termos recorrentes e frases longas.
- Importação de arquivos TXT/Markdown como novo projeto.
- Exportação TXT, Markdown, DOCX e EPUB.
- Backup ZIP textual do manuscrito.
- Preferência de tema persistida entre reinicializações.

## Ainda não concluído
- Edição completa dos registros de planejamento (a tela atual cobre cadastro e exclusão; edição detalhada de fichas ainda falta).
- Organização avançada em volumes, atos e sequências; arrastar e soltar cenas; vincular eventos a cenas e personagens.
- Revisão linguística, gramática, ponto de vista e consistência semântica.
- Editor com desfazer/refazer, tipografia configurável, foco e metas por sessão.
- Importação DOCX/EPUB e restauração de backup ZIP do banco completo.
- Modelos editoriais e pré-visualização de publicação.
- Sincronização, colaboração e resolução de conflitos: dependem de serviço remoto, autenticação e política de privacidade; não são simulados como se fossem recursos locais.

## Limites importantes
O diagnóstico textual é heurístico e não substitui revisão linguística. O ZIP é um pacote de manuscrito em Markdown, não backup binário do banco. A exportação DOCX/EPUB cria documentos básicos e ainda precisa de testes de compatibilidade com leitores e editores variados.
