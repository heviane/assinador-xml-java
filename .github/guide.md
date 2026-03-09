# Guia de Desenvolvimento e Uso (Passo a Passo)

Este projeto é uma biblioteca Java para assinar arquivos XML digitalmente (Padrão XML-DSig) usando certificados A1 (.pfx/.p12).

## 1. Pré-requisitos

Antes de começar, certifique-se de ter instalado:

1. **Java JDK 17+**.
2. **Maven** (Gerenciador de dependências).
3. **VS Code** (Recomendado) ou IntelliJ IDEA.
    * Se usar VS Code, instale a extensão "Extension Pack for Java".

## 2. Como Baixar e Compilar

Abra o terminal na pasta onde deseja salvar o projeto:

```bash
# 1. Baixe o código (ou extraia o zip)
git clone https://github.com/seu-usuario/assinador-xml-java.git
cd assinador-xml-java

# 2. Compile e gere o arquivo .jar
mvn clean install
```

Se aparecer a mensagem **BUILD SUCCESS**, o projeto está pronto. O arquivo final (biblioteca) estará na pasta `target/assinador-1.0.0.jar`.

## 3. Como Rodar o Teste Manual

O projeto possui um arquivo de teste (`src/test/java/com/assinador/AssinadorXmlTeste.java`) que simula uma assinatura real.

### Preparação

1. Tenha em mãos um certificado digital **A1** (arquivo `.pfx` ou `.p12`) e a senha dele.
2. Crie arquivos de teste na raiz do projeto com o conteúdo XML que deseja assinar.
3. Ajuste os nomes dos arquivos de teste na classe de teste.

### Executando (Via VS Code)

O teste exige o caminho e a senha do certificado. A forma mais fácil de passar isso é configurando o VS Code:

1. Abra o arquivo `src/test/java/com/assinador/AssinadorXmlTeste.java`.
2. Vá no menu **Run** -> **Add Configuration...** -> **Java**.
3. Edite o arquivo `.vscode/launch.json` gerado, adicionando a linha `vmArgs`:

```json
{
    "type": "java",
    "name": "Rodar Teste Assinador",
    "request": "launch",
    "mainClass": "com.assinador.AssinadorXmlTeste",
    "projectName": "assinador",
    "vmArgs": "-Dcert.path=\"C:/caminho/seu-certificado.pfx\" -Dcert.password=\"sua-senha-aqui\""
}
```

4. Salve e pressione **F5**.
5. Se der certo, os arquivos `XXX-assinado.xml` serão criados na raiz do projeto.

## 4. Como Usar em Outro Projeto

Para usar esta biblioteca em sua aplicação Java:

1. Adicione a dependência no seu `pom.xml` (após ter rodado `mvn install` localmente):

```xml
<dependency>
    <groupId>com.assinador</groupId>
    <artifactId>assinador</artifactId>
    <version>2.0.0</version>
</dependency>
```

2. Instancie a classe `AssinadorXml` e chame o método `assinarXml` passando o conteúdo do XML (String), caminho do certificado, senha, nome da tag, nome do atributo e namespace para assinatura.
