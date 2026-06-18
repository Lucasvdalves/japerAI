package br.com.jasperia.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Serviço responsável por conectar o backend em Java 8 com a API Serverless
 * do Hugging Face utilizando o padrão de Chat Completions.
 */
public class HfAiService {

    // Endpoint unificado compatível com o padrão global de Chat
    private static final String API_URL = "https://router.huggingface.co/v1/chat/completions";

    // Modelo imbatível para codificação e SQL, livre de restrições de licença
    private static final String MODEL_NAME = "Qwen/Qwen2.5-Coder-32B-Instruct";

    // Token de acesso do usuário
    private static final String HF_TOKEN = "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";

    private static final Gson gson = new Gson();

    /**
     * Consulta a API do Hugging Face usando a API de Chat.
     */
    public static String consultarLlama(String systemPrompt, String userPrompt) throws Exception {

        // 1. Estruturação do histórico de mensagens (System / User)
        JsonArray messages = new JsonArray();

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemPrompt);
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userPrompt);
        messages.add(userMsg);

        // 2. Criação do Payload padrão Chat Completion
        JsonObject payload = new JsonObject();
        payload.addProperty("model", MODEL_NAME);
        payload.add("messages", messages);
        payload.addProperty("temperature", 0.1); // Baixa temperatura garante precisão no código/SQL
        payload.addProperty("max_tokens", 2048);

        String jsonRequestBody = gson.toJson(payload);

        // 3. Configuração da Conexão Clássica do Java 8
        URL url = new URL(API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + HF_TOKEN);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setDoOutput(true);
        connection.setConnectTimeout(20000); // 20 segundos para conectar
        connection.setReadTimeout(60000);    // 60 segundos para processar

        System.out.println("🤖 [IA] Enviando requisição para " + MODEL_NAME + "...");

        // 4. Envio do corpo do payload
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonRequestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int statusCode = connection.getResponseCode();
        System.out.println("🤖 [IA] Resposta recebida. HTTP Status: " + statusCode);

        // 5. Leitura do retorno
        if (statusCode == 200) {
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            // Realiza o parse baseado na resposta padrão: choices[0].message.content
            JsonObject responseObj = gson.fromJson(response.toString(), JsonObject.class);
            return responseObj.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

        } else {
            // Se falhar, lê o buffer de erro para exibir no console do Java
            StringBuilder errorResponse = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    errorResponse.append(line.trim());
                }
            }
            System.err.println("❌ [IA ERROR] Erro na API do Hugging Face: " + errorResponse.toString());
            throw new RuntimeException("Erro na API Hugging Face (" + statusCode + "): " + errorResponse.toString());
        }
    }
}