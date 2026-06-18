package br.com.jasperia;

import br.com.jasperia.config.DatabaseConfig;
import br.com.jasperia.controller.AuthController;
import br.com.jasperia.controller.ReportController;
import br.com.jasperia.controller.DatabaseConfigController;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) {
        try {
            // 1. Inicializa o servidor HTTP na porta 8080
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            System.out.println("🚀 Inicializando o servidor HTTP nativo na porta 8080...");

            // 2. Mapeamento de Rotas (Handlers) do nosso MVP
            // Rota de Autenticação (Login)
            server.createContext("/api/auth/login", new AuthController());

            // Rota do Workspace (Sincronização de Layout / IA)
            server.createContext("/api/relatorios/sincronizar", new ReportController());

            // Rota de Configuração de Banco do Cliente
            server.createContext("/api/config/database", new DatabaseConfigController());

            // 3. Configura um Executor de Threads para permitir requisições concorrentes (Multi-thread)
            // Essencial para o comportamento assíncrono do SaaS
            server.setExecutor(Executors.newFixedThreadPool(10));

            // 4. Garante que o Pool de conexões Hikari feche se a aplicação cair/parar
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n🛑 Desligando o servidor... Liberando recursos.");
                DatabaseConfig.shutdown();
            }));

            // 5. Liga o servidor de fato
            server.start();
            System.out.println("🟢 Servidor SaaS JasperAI pronto e aguardando conexões do Angular 1!");

        } catch (IOException e) {
            System.err.println("❌ Erro fatal ao iniciar o servidor HTTP: " + e.getMessage());
        }
    }

    /**
     * Método Utilitário estático para aplicar as regras de CORS globais em Java Puro.
     * Deve ser chamado por todos os controladores antes de responder ao Frontend.
     */
   public static void aplicarCors(HttpExchange exchange) throws IOException {
    // Permite explicitamente a origem do seu servidor Python local
    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "http://127.0.0.1:8000");
    exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
    
    // CRÍTICO: Liberar o X-Tenant-ID para o mecanismo de preflight do Chrome aceitar a requisição
    exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Tenant-ID");
    
    // Tratamento de requisições de teste de rota (OPTIONS) feitas pelo Chrome
    if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }
}

    /**
     * Método Utilitário para enviar respostas em formato JSON de forma enxuta.
     */
    public static void enviarRespostaJson(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        byte[] bytes = jsonResponse.getBytes("UTF-8");
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
        exchange.close();
    }
}
