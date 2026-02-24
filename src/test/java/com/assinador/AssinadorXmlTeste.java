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

            AssinadorXml assinador = new AssinadorXml();
            System.out.println("=== Iniciando Testes de Integração ===");

            // -------------------------------------------------------------------
            // CENÁRIO 1: EMISSÃO (Id="Id", Namespace=".../nfse")
            // -------------------------------------------------------------------
            String arqEmissao = "dps-original.xml";
            if (Files.exists(Paths.get(arqEmissao))) {
                System.out.println("\n>>> Testando Emissão...");
                String xmlOriginal = new String(Files.readAllBytes(Paths.get(arqEmissao)), StandardCharsets.UTF_8);
                
                String xmlAssinado = assinador.assinarXml(
                    xmlOriginal, 
                    caminhoCertificado, 
                    senhaCertificado,
                    "infNFSe",                     // Nó alvo
                    "Id",                             // Atributo ID (Maiúsculo)
                    "http://www.sped.fazenda.gov.br/nfse"  // Namespace
                );

                Files.write(Paths.get("dps-assinado.xml"), xmlAssinado.getBytes(StandardCharsets.UTF_8));
                System.out.println("✅ Emissão gerada: dps-assinado.xml");
            } else {
                System.out.println("⚠️ Arquivo de emissão (" + arqEmissao + ") não encontrado.");
            }

            // -------------------------------------------------------------------
            // CENÁRIO 2: CANCELAMENTO (Id="id", Namespace=".../nfsevia")
            // -------------------------------------------------------------------
            String arqCancelamento = "cancelamento-original.xml";
            if (Files.exists(Paths.get(arqCancelamento))) {
                System.out.println("\n>>> Testando Cancelamento...");
                String xmlOriginal = new String(Files.readAllBytes(Paths.get(arqCancelamento)), StandardCharsets.UTF_8);
                
                String xmlAssinado = assinador.assinarXml(
                    xmlOriginal, 
                    caminhoCertificado, 
                    senhaCertificado,
                    "infEventoVia",                  // Nó alvo (Ajustar conforme XML)
                    "id",                               // Atributo ID (Minúsculo)
                    "http://www.sped.fazenda.gov.br/nfsevia" // Namespace
                );

                Files.write(Paths.get("cancelamento-assinado.xml"), xmlAssinado.getBytes(StandardCharsets.UTF_8));
                System.out.println("✅ Cancelamento gerado: cancelamento-assinado.xml");
            } else {
                System.out.println("⚠️ Arquivo de cancelamento (" + arqCancelamento + ") não encontrado. Crie este arquivo para testar.");
            }

        } catch (Exception e) {
            System.err.println("❌ Erro durante o teste:");
            e.printStackTrace();
        }
    }
}