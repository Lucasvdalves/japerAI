package br.com.jasperia.controller;

import br.com.jasperia.Main;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class AuthController implements HttpHandler {

    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 1. Aplica as regras de CORS que definimos na Main (Garante que se for OPTIONS, já corta aqui)
        Main.aplicarCors(exchange);

        // 2. Bloqueia qualquer método que não seja POST para esta rota
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            Map<String, String> erro = new HashMap<>();
            erro.put("mensagem", "Método HTTP não permitido. Use POST.");
            Main.enviarRespostaJson(exchange, 405, gson.toJson(erro));
            return;
        }

        try {
            // 3. Lê o corpo da requisição JSON enviado pelo Angular 1
            String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));

            // 4. Converte o JSON em um mapa de objetos
            Map<String, String> credenciais = gson.fromJson(body, Map.class);
            String tenantId = credenciais.get("tenantId");
            String username = credenciais.get("username");
            String password = credenciais.get("password");

            // 5. Validação das Credenciais (Alinhado com o Mock que fizemos no Frontend)
            if ("sankhya".equals(username) && "123".equals(password)) {

                // Monta o objeto de sucesso para o front-end
                Map<String, String> respostaSucesso = new HashMap<>();
                respostaSucesso.put("token", "jwt_token_puro_gerado_pelo_saas_xyz123");
                respostaSucesso.put("status", "AUTENTICADO");

                System.out.println("🔐 Usuário autenticado com sucesso no Tenant: " + tenantId);

                // Retorna HTTP 200 OK
                Main.enviarRespostaJson(exchange, 200, gson.toJson(respostaSucesso));
            } else {
                // Credenciais inválidas
                Map<String, String> respostaErro = new HashMap<>();
                respostaErro.put("mensagem", "Usuário ou senha inválidos! (Use usuario: sankhya e senha: 123)");

                // Retorna HTTP 401 Unauthorized
                Main.enviarRespostaJson(exchange, 401, gson.toJson(respostaErro));
            }

        } catch (Exception e) {
            // Tratamento de falha interna (Ex: JSON malformado)
            Map<String, String> respostaErroFatal = new HashMap<>();
            respostaErroFatal.put("mensagem", "Erro interno ao processar o login: " + e.getMessage());
            Main.enviarRespostaJson(exchange, 500, gson.toJson(respostaErroFatal));
        }
    }
}