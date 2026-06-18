package br.com.jasperia.controller;

import br.com.jasperia.Main;
import br.com.jasperia.context.TenantContext;
import br.com.jasperia.service.HfAiService;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import br.com.jasperia.config.DatabaseConfig;

/**
 * Controlador responsável por receber as requisições das etapas do formulário,
 * mapear o prompt ideal para a IA e gerenciar o fluxo multi-tenant.
 */
public class ReportController implements HttpHandler {

    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 1. Aplica as regras de CORS (Garante o fluxo do navegador de Cross-Origin)
        Main.aplicarCors(exchange);

        // 2. Bloqueia qualquer método que não seja POST para esta rota de sincronização
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            Map<String, String> erro = new HashMap<>();
            erro.put("mensagem", "Método HTTP não permitido. Use POST.");
            Main.enviarRespostaJson(exchange, 405, gson.toJson(erro));
            return;
        }

        try {
            // 3. Captura o Tenant através do cabeçalho HTTP customizado enviado pelo frontend
            String tenantId = exchange.getRequestHeaders().getFirst("X-Tenant-ID");
            if (tenantId == null || tenantId.trim().isEmpty()) {
                tenantId = "1001"; // Fallback padrão caso não enviado
            }

            // Injeta o ID no nosso Contexto Multi-Tenant baseado em ThreadLocal
            TenantContext.setTenantId(tenantId);

            // 4. Lê o payload JSON vindo do Angular 1 de forma compatível com Java 8 puro
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            String body = sb.toString();

            // Converte o payload em um mapa genérico de propriedades
            Map<String, Object> requisicaoRelatorio = gson.fromJson(body, Map.class);

            String acao = (String) requisicaoRelatorio.get("acao"); // Captura a ação vinda do front (ESTRUTURA, QUERY ou LAYOUT)
            String categoria = (String) requisicaoRelatorio.get("categoria");
            String descricao = (String) requisicaoRelatorio.get("descricao");
            String sqlAtual = (String) requisicaoRelatorio.get("sql");
            String jrxmlAtual = (String) requisicaoRelatorio.get("jrxml");
            String instrucaoLayout = (String) requisicaoRelatorio.get("instrucaoLayout");

            System.out.println("🔄 Processando ação '" + acao + "' para o Tenant: " + TenantContext.getTenantId());

            if ("EXECUTE".equalsIgnoreCase(acao)) {
                Map<String, Object> respostaSucesso = new HashMap<>();
                List<String> colunas = new ArrayList<>();
                List<Map<String, Object>> linhas = new ArrayList<>();
                
                try (Connection conn = DatabaseConfig.getConnection(TenantContext.getTenantId());
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sqlAtual)) {
                    
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    for (int i = 1; i <= columnCount; i++) {
                        colunas.add(metaData.getColumnName(i));
                    }
                    
                    while (rs.next()) {
                        Map<String, Object> linha = new HashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            Object valor = rs.getObject(i);
                            // Se for nulo ou outro formato, mantemos como String ou Object
                            linha.put(metaData.getColumnName(i), valor != null ? valor.toString() : "");
                        }
                        linhas.add(linha);
                    }
                    
                    respostaSucesso.put("colunas", colunas);
                    respostaSucesso.put("linhas", linhas);
                    respostaSucesso.put("statusSql", "Query executada no Oracle!");
                    Main.enviarRespostaJson(exchange, 200, gson.toJson(respostaSucesso));
                    return;
                } catch (Exception e) {
                    System.err.println("❌ Erro ao executar query no Oracle: " + e.getMessage());
                    Map<String, String> respostaErro = new HashMap<>();
                    respostaErro.put("mensagem", "Erro SQL: " + e.getMessage());
                    Main.enviarRespostaJson(exchange, 500, gson.toJson(respostaErro));
                    return;
                }
            }

            String systemPrompt = "";
            String userPrompt = "";

            // 🤖 ENGENHARIA DE PROMPT DINÂMICA SEM SINTAXE DE CRASES PARA NÃO QUEBRAR O COMPILADOR
            if ("ESTRUTURA".equalsIgnoreCase(acao)) {
                systemPrompt = "Você é um analista de sistemas especialista em banco de dados do Sankhya ERP.\n" +
                        "Baseado na categoria e descrição fornecidas, crie uma lista de campos com os nomes físicos reais das colunas no Oracle do Sankhya (ex: CODVEND, NOMEPARC, VLRNOTA).\n" +
                        "Retorne ESTRITAMENTE um array JSON puro. Não utilize nenhuma tag de bloco de código markdown como crases ou palavras explicativas antes ou depois do JSON.\n" +
                        "A sua resposta deve conter apenas e somente a estrutura de chaves e tipos Java como no modelo a seguir:\n" +
                        "[{\"nome\":\"COLUNA_ORACLE\",\"tipo\":\"java.lang.String\",\"descricao\":\"Rótulo Amigável\"}]";

                userPrompt = "Gere os campos para a categoria livre '" + categoria + "' com o objetivo funcional: " + descricao;

            } else if ("QUERY".equalsIgnoreCase(acao)) {
                systemPrompt = "Você é um DBA Oracle especialista no modelo de dados do Sankhya ERP.\n" +
                        "Gere uma query SQL corporativa performática e válida para o banco Oracle, respeitando os relacionamentos padrão (ex: TGFCAB, TGFITE, TGFPAR).\n" +
                        "Retorne APENAS a instrução SQL em texto limpo e puro. Não use marcas de bloco de código ou explicações textuais.";

                userPrompt = "Otimize ou crie a query baseada no objetivo: " + descricao + "\nQuery Atual:\n" + sqlAtual;

            } else { // Caso padrão: "LAYOUT" (Passo 4)
                systemPrompt = "Você é um Arquiteto especialista na engine clássica do iReport 4.0.1 e Sankhya.\n" +
                        "Modifique o código JRXML XML fornecido. Certifique-se de manter as tags antigas perfeitamente compatíveis com a DTD clássica e language=\"groovy\".\n" +
                        "Retorne APENAS o código XML limpo e cru. Não adicione textos informativos ou marcadores de bloco.";

                userPrompt = "Modifique o JRXML atual seguindo essa instrução de layout: " + instrucaoLayout + "\nQuery SQL do Relatório:\n" + sqlAtual + "\nLayout JRXML Atual:\n" + jrxmlAtual;
            }

            // 5. Dispara a chamada real para o Llama 3 via Hugging Face
            String resultadoIa = HfAiService.consultarLlama(systemPrompt, userPrompt);

            // 6. Estrutura o mapa de resposta dependendo do que o frontend espera processar
            Map<String, Object> respostaSucesso = new HashMap<>();

            if ("ESTRUTURA".equalsIgnoreCase(acao)) {
                // Transforma a string JSON gerada pela IA em uma lista de objetos Java real para o Gson serializar corretamente
                java.util.List camposSugeridos = gson.fromJson(resultadoIa.trim(), java.util.List.class);
                respostaSucesso.put("campos", camposSugeridos);
                respostaSucesso.put("statusSql", "Campos estruturados com sucesso pela IA!");
            } else if ("QUERY".equalsIgnoreCase(acao)) {
                respostaSucesso.put("sql", resultadoIa.trim());
                respostaSucesso.put("statusSql", "Query SQL gerada e validada com sucesso pelo Llama 3!");
            } else {
                respostaSucesso.put("jrxml", resultadoIa.trim());
                respostaSucesso.put("statusSql", "Layout XML atualizado com sucesso com base na nova Query!");
            }

            // Devolve HTTP 200 OK com o JSON gerado
            Main.enviarRespostaJson(exchange, 200, gson.toJson(respostaSucesso));

        } catch (Exception e) {
            System.err.println("❌ Erro ao processar requisição de relatório: " + e.getMessage());
            Map<String, String> respostaErro = new HashMap<>();
            respostaErro.put("mensagem", "Erro interno no servidor ao processar o layout: " + e.getMessage());
            Main.enviarRespostaJson(exchange, 500, gson.toJson(respostaErro));
        } finally {
            // Limpa o ThreadLocal para evitar vazamento de memória (Memory Leak) entre requisições
            TenantContext.clear();
        }
    }
}