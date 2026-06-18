# Sankhya Jasper Builder (JasperAI) 🚀

O **Sankhya Jasper Builder** é uma ferramenta MVP inovadora e intuitiva projetada para automatizar e acelerar a criação de relatórios customizados no formato **JasperReports (.jrxml)** para o ecossistema **Sankhya ERP**. 

Utilizando o poder de Inteligência Artificial integrada ao banco de dados em tempo real, a aplicação gera dicionários de dados, otimiza consultas SQL complexas e estrutura a árvore XML do layout de forma automatizada e compatível com a engine clássica do iReport 4.0.1.

---

## 🌟 Principais Funcionalidades

### 1. Assistência Inteligente Passo a Passo (Wizard)
- **Passo 1 (Objetivo)**: Defina o objetivo funcional do relatório (ex: "Faturamento por vendedor mensal") e a IA sugere quais os nomes físicos de colunas corretos do ERP (ex: `CODVEND`, `APELIDO`, `VLRNOTA`).
- **Passo 2 (Estrutura)**: Grade de campos editável com suporte a tipos Java (String, BigDecimal, Timestamp).
- **Passo 3 (Query SQL)**: IADBA gera e otimiza queries SQL compatíveis com o dialeto Oracle do Sankhya ERP.
- **Passo 4 (Layout JRXML)**: Editor interativo onde você instrui a IA a modificar o XML (.jrxml) livremente (ex: "mude o título para Arial 14 e adicione uma linha cinza").
- **Passo 5 (Preview Dinâmico)**: Execução real da query no banco do cliente para validação imediata dos dados resultantes na grade (colunas e linhas dinâmicas).
- **Passo 6 (Exportar)**: Download do arquivo `.jrxml` gerado pronto para importação no Sankhya.

### 2. Ponte de Banco de Dados Dinâmica (Dynamic Database Bridge)
- **Modal de Conexão na Navbar**: Permite configurar e alternar conexões a bancos de dados externos em tempo de execução.
- **Bancos Suportados**: Oracle Database, PostgreSQL, MySQL e Microsoft SQL Server.
- **Testar & Salvar**: Permite validar a conexão JDBC antes de salvá-la temporariamente em memória associada ao tenant da sessão do usuário.

---

## 🛠️ Stack Tecnológica

### Frontend
- **HTML5 & Vanilla CSS**: Interface fluida, moderna (estilo dark mode e glassmorphism) e totalmente responsiva.
- **AngularJS 1.8.2**: Data binding bidirecional dinâmico de alta performance para o stepper de passos e manipulação de payloads.

### Backend
- **Java 8 (Puro)**: Servidor HTTP nativo (`HttpServer`) leve e concorrente rodando com pool de threads.
- **HikariCP**: Gerenciamento de pool de conexões robusto e performático.
- **Gson**: Serialização e desserialização rápida de payloads JSON.
- **Drivers JDBC nativos**: Oracle (ojdbc11), PostgreSQL, MySQL e SQL Server integrados no classpath.

### Agente de IA
- **Hugging Face Serverless API**: Integração com a API de Chat Completions usando o modelo avançado de codificação **Qwen/Qwen2.5-Coder-32B-Instruct**.

---

## 🚀 Como Executar o Projeto

### Pré-requisitos
- **Java 21** (ou Java 8+) instalado e configurado no PATH.
- **Node.js & npm** para servir o frontend.
- **Maven** para gerenciar dependências do backend.
- Banco de dados ativo (local ou VPN).

### Passo 1: Executar o Frontend
Abra o terminal na pasta raiz do projeto e execute:
```bash
npx -y http-server -c-1 -p 8000
```
O frontend estará acessível em: `http://127.0.0.1:8000/views/login.html`

### Passo 2: Executar o Backend
Abra outro terminal na pasta `jasper-ia-back` e execute:
```bash
mvn compile exec:java "-Dexec.mainClass=br.com.jasperia.Main"
```
O servidor backend iniciará na porta `8080`.

### Passo 3: Acessar a Aplicação
- Abra o navegador no link `http://127.0.0.1:8000/views/login.html`.
- Utilize as credenciais de teste:
  - **ID da Empresa (Tenant)**: 1001
  - **Usuário**: sankhya
  - **Senha**: 123
