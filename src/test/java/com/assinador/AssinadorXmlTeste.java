package com.assinador;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.nio.charset.StandardCharsets;

public class AssinadorXmlTeste {
    public static void main(String[] args) {
        try {
            // --- CONFIGURAÇÃO DO TESTE ---
            String caminhoCertificado = System.getProperty("cert.path");
            if (caminhoCertificado == null) {
                throw new IllegalArgumentException("O caminho do certificado é obrigatório! Use o argumento JVM: -Dcert.path=\"CAMINHO_DO_ARQUIVO\"");
            }
            String senhaCertificado = System.getProperty("cert.password"); 
            if (senhaCertificado == null) {
                throw new IllegalArgumentException("A senha do certificado é obrigatória! Use o argumento JVM: -Dcert.password=\"SUA_SENHA\"");
            }
            String caminhoXmlOriginal = "dps-original.xml";
            String noParaAssinar      = "infNFSe";
            String noPaiDaAssinatura  = "NFSe";
            // -----------------------------

            System.out.println("=== Iniciando Teste de Integração (Modo Lib Dinâmico) ===");

            // 1. Simula o GeneXus lendo o arquivo para uma String
            if (!Files.exists(Paths.get(caminhoXmlOriginal))) {
                System.err.println("ERRO: Arquivo " + caminhoXmlOriginal + " não encontrado na raiz do projeto!");
                return;
            }
            
            byte[] encoded = Files.readAllBytes(Paths.get(caminhoXmlOriginal));
            String xmlOriginalStr = new String(encoded, StandardCharsets.UTF_8);

            // 2. Instancia a Lib atualizada
            AssinadorXml assinador = new AssinadorXml();

            // 3. Executa a assinatura via método genérico
            System.out.println("Assinando o nó <" + noParaAssinar + "> dentro de <" + noPaiDaAssinatura + ">...");
            
            // IMPORTANTE: Use o nome do novo método que criamos na Lib
            String xmlAssinadoStr = assinador.assinarXml(
                xmlOriginalStr, 
                caminhoCertificado, 
                senhaCertificado,
                noParaAssinar,
                noPaiDaAssinatura
            );

            // 4. Salva o resultado em disco
            Files.write(Paths.get("dps-assinado.xml"), xmlAssinadoStr.getBytes(StandardCharsets.UTF_8));

            System.out.println("✅ Sucesso!");
            System.out.println("Arquivo gerado: dps-assinado.xml");
            System.out.println("Dica: Valide o arquivo no site do ITI (Assinatura Digital).");

        } catch (Exception e) {
            System.err.println("❌ Erro durante o teste:");
            e.printStackTrace();
        }
    }
}