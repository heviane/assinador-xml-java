# Assinador XML Java

> Biblioteca Java para assinatura digital de XML com XML-DSig (Enveloped), compatível com o padrão ICP-Brasil para documentos fiscais, suportando certificados A1 (.pfx) e algoritmos RSA-SHA1.

Esta biblioteca foi projetada para facilitar a integração de assinatura digital em sistemas que emitem **DF-e (Documento Fiscal Eletrônica)** e outros documentos fiscais que seguem o padrão ICP-Brasil.

## 🚀 Funcionalidades

* **Padrão XML-DSig:** Implementa a assinatura no formato *Enveloped* (a assinatura fica dentro do XML).
* **Algoritmos:** Utiliza `RSA-SHA1` para assinatura e `SHA1` para digest (padrão comum em prefeituras e SEFAZ).
* **Certificados:** Suporte nativo para arquivos **PKCS#12 (.pfx/.p12)** (Certificado A1).
* **Canonicalização:** Aplica `CanonicalizationMethod.EXCLUSIVE` para garantir a integridade do hash.
* **Zero Dependências Externas:** Utiliza apenas a API padrão do Java (`javax.xml.crypto`).

## 📦 Requisitos

* Java 17 ou superior.
* Maven (para build).

## ⚙️ Como Usar e Testar

Para um guia detalhado sobre como compilar, rodar os testes e integrar a biblioteca em seu projeto, consulte o nosso **[Guia de Desenvolvimento e Uso (Passo a Passo)](.github/guide.md)**.

## 🤝 Contribuições e Melhorias Futuras

Sugestões de melhorias e relatos de bugs são bem-vindos! Por favor, abra uma **[Issue](https://github.com/heviane/assinador-xml-java/issues)** para discutir o que você gostaria de mudar.
