package br.com.jasperia.controller;

import br.com.jasperia.Main;
import br.com.jasperia.context.TenantContext;
import br.com.jasperia.config.DatabaseConfig;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para testar e salvar configurações de conexões a bancos de dados
 * customizados enviados pelo cliente em tempo de execução.
 */
public class DatabaseConfigController implements HttpHandler {

    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Aplica as políticas globais de CORS
        Main.aplicarCors(exchange);

        // Apenas requisições POST são aceitas
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            Map<String, String> erro = new HashMap<>();
            erro.put("mensagem", "Método HTTP não permitido. Use POST.");
            Main.enviarRespostaJson(exchange, 405, gson.toJson(erro));
            return;
        }

        try {
            // Captura o Tenant ID do cabeçalho HTTP
            String tenantId = exchange.getRequestHeaders().getFirst("X-Tenant-ID");
            if (tenantId == null || tenantId.trim().isEmpty()) {
                tenantId = "1001";
            }
            TenantContext.setTenantId(tenantId);

            // Lê o payload JSON da requisição
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            String body = sb.toString();

            // Converte em mapa genérico para extrair propriedades
            Map<String, Object> requisicao = gson.fromJson(body, Map.class);
            String acao = (String) requisicao.get("acao");

            // Popula a estrutura DbConnectionInfo
            DatabaseConfig.DbConnectionInfo info = new DatabaseConfig.DbConnectionInfo();
            info.setDbType((String) requisicao.get("dbType"));
            info.setHost((String) requisicao.get("host"));

            // Converte a porta de forma segura
            Object portObj = requisicao.get("port");
            int port = 0;
            if (portObj instanceof Number) {
                port = ((Number) portObj).intValue();
            } else if (portObj instanceof String) {
                port = Integer.parseInt((String) portObj);
            }
            info.setPort(port);

            info.setDatabaseName((String) requisicao.get("databaseName"));
            info.setUsername((String) requisicao.get("username"));
            info.setPassword((String) requisicao.get("password"));

            System.out.println("🔌 Solicitando ação '" + acao + "' de Banco para o Tenant: " + tenantId + " (" + info.getDbType() + ")");

            if ("TEST".equalsIgnoreCase(acao)) {
                // Apenas testa a conexão
                DatabaseConfig.testConnection(info);
                Map<String, String> resposta = new HashMap<>();
                resposta.put("status", "CONECTADO");
                Main.enviarRespostaJson(exchange, 200, gson.toJson(resposta));
            } else if ("SAVE".equalsIgnoreCase(acao)) {
                // Testa e depois salva no cache em memória do tenant
                DatabaseConfig.testConnection(info);
                DatabaseConfig.setConnectionInfo(tenantId, info);
                Map<String, String> resposta = new HashMap<>();
                resposta.put("status", "SALVO");
                Main.enviarRespostaJson(exchange, 200, gson.toJson(resposta));
            } else {
                Map<String, String> erro = new HashMap<>();
                erro.put("mensagem", "Ação de configuração não suportada.");
                Main.enviarRespostaJson(exchange, 400, gson.toJson(erro));
            }

        } catch (Exception e) {
            System.err.println("❌ Falha na configuração de banco: " + e.getMessage());
            Map<String, String> erro = new HashMap<>();
            erro.put("mensagem", e.getMessage());
            Main.enviarRespostaJson(exchange, 500, gson.toJson(erro));
        } finally {
            // Limpa o ThreadLocal do Contexto
            TenantContext.clear();
        }
    }
}
