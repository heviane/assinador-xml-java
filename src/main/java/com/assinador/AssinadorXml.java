package com.assinador;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class AssinadorXml {

    public String assinarXml(String xmlConteudo, String caminhoCertificado, String senhaCertificado, 
                             String nomeNoParaAssinar, String nomeAtributoId, String namespace,
                             String algoritmoCanonicalizacao) throws Exception { 
        
        File file = new File(caminhoCertificado);
		if (!file.exists()) {
			throw new FileNotFoundException("Certificado nao encontrado no caminho: " + caminhoCertificado);
		}

        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(caminhoCertificado)) {
            ks.load(fis, senhaCertificado.toCharArray());
        }
        
        // String alias = ks.aliases().nextElement();
        String alias = null;
        Enumeration<String> aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            String currentAlias = aliases.nextElement();
            if (ks.isKeyEntry(currentAlias)) {
                alias = currentAlias;
                break;
            }
        }
        
        if (alias == null) {
            throw new KeyStoreException("Nenhuma chave privada encontrada no arquivo do certificado.");
        }

        KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry) ks.getEntry(
                alias, new KeyStore.PasswordProtection(senhaCertificado.toCharArray()));

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xmlConteudo.getBytes(StandardCharsets.UTF_8)));

        Document docAssinado = signNode(doc, keyEntry, nomeNoParaAssinar, nomeAtributoId, namespace, algoritmoCanonicalizacao);

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(docAssinado), new StreamResult(writer));

        String xmlFinal = writer.toString();

        // Replace necessário para SIL Tecnologia (Vila Velha ES)
        return xmlFinal.replaceAll("\r", "")
                       .replaceAll("\n", "")
                       .replaceAll("&#13;", "")
                       .replace(" standalone=\"no\"", "");
    }

    private Document signNode(Document doc, KeyStore.PrivateKeyEntry keyEntry, String targetNode, String nomeAtributoId, String namespace, String algoritmoCanonicalizacao) throws Exception {
        
        // 1. Localiza o elemento onde a assinatura será inserida (ex: infNFSe)
        NodeList elements = doc.getElementsByTagName(targetNode);
        if (elements.getLength() == 0) {elements = doc.getElementsByTagNameNS("*", targetNode);}
        if (elements.getLength() == 0) throw new IllegalArgumentException("Tag <" + targetNode + "> não encontrada no XML para inserir a assinatura.");
        
        Element signatureParent = (Element) elements.item(0); 

        // 2. Define o URI de referência e prepara o contexto da assinatura
        String referenceURI;
        DOMSignContext dsc = new DOMSignContext(keyEntry.getPrivateKey(), signatureParent);
        dsc.putNamespacePrefix(XMLSignature.XMLNS, "");
        if (namespace != null && !namespace.isEmpty()) {
            dsc.putNamespacePrefix(namespace, "");// Ajuda o validador do servidor a encontrar o nó dentro do namespace correto
        }

        // Se um atributo de ID foi especificado, a referência será a esse elemento.
        // Caso contrário, a referência é ao documento inteiro (URI="").
        if (nomeAtributoId != null && !nomeAtributoId.isEmpty()) {
            Element elementToSign = signatureParent;

            // Se o atributo de ID não existir no nó, cria um dinamicamente.
            if (!elementToSign.hasAttribute(nomeAtributoId)) {
                String idGerado = "id-" + java.util.UUID.randomUUID().toString();
                elementToSign.setAttribute(nomeAtributoId, idGerado);
            }
            
            elementToSign.setIdAttribute(nomeAtributoId, true); 
            elementToSign.setIdAttributeNS(null, nomeAtributoId, true);
            Attr idAttr = elementToSign.getAttributeNode(nomeAtributoId);
            elementToSign.setIdAttributeNode(idAttr, true);
            dsc.setIdAttributeNS(elementToSign, null, nomeAtributoId);
            referenceURI = "#" + elementToSign.getAttribute(nomeAtributoId);
        } else {
            referenceURI = ""; // Assina o documento inteiro (enveloped)
        }

        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        // 4. Transformações (Mantendo o Enveloped e Exclusive C14N)        
        Transform enveloped = fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null);
        List<Transform> transformList = List.of(enveloped);

        // 5. Referência 
        Reference ref = fac.newReference(
            referenceURI, 
            fac.newDigestMethod(DigestMethod.SHA1, null), // Constante: http://www.w3.org/2000/09/xmldsig#sha1
            transformList, null, null
        );
        
        SignedInfo si = fac.newSignedInfo(
            fac.newCanonicalizationMethod(algoritmoCanonicalizacao, (C14NMethodParameterSpec) null),
            fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null), // Constante: http://www.w3.org/2000/09/xmldsig#rsa-sha1
            Collections.singletonList(ref)
        );

        // 6. Certificado
        X509Certificate cert = (X509Certificate) keyEntry.getCertificate();
        KeyInfoFactory kif = fac.getKeyInfoFactory();
        X509Data x509Data = kif.newX509Data(Collections.singletonList(cert));
        KeyInfo ki = kif.newKeyInfo(Collections.singletonList(x509Data));

        // 7. Assinatura
        fac.newXMLSignature(si, ki).sign(dsc); 

        return doc;
    }
}