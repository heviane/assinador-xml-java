package com.assinador;

import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.*;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class AssinadorXml {

    public String assinarXml(String xmlConteudo, String caminhoCertificado, String senhaCertificado, 
                             String nomeNoParaAssinar, String nomeAtributoId, String namespace) throws Exception {
        
        java.io.File file = new java.io.File(caminhoCertificado);
		if (!file.exists()) {
			throw new Exception("Certificado nao encontrado no caminho: " + caminhoCertificado);
		}

        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(caminhoCertificado)) {
            ks.load(fis, senhaCertificado.toCharArray());
        }
        
        String alias = ks.aliases().nextElement();
        KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry) ks.getEntry(
                alias, new KeyStore.PasswordProtection(senhaCertificado.toCharArray()));

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xmlConteudo.getBytes("UTF-8")));

        Document docAssinado = signNode(doc, keyEntry, nomeNoParaAssinar, nomeAtributoId, namespace);

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "no");
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.ENCODING, "UTF-8");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(docAssinado), new StreamResult(writer));

        String xmlFinal = writer.toString();

        return xmlFinal.replaceAll("\r", "")
                       .replaceAll("\n", "")
                       .replaceAll("&#13;", "")
                       .replace(" standalone=\"no\"", "");
    }

    private Document signNode(Document doc, KeyStore.PrivateKeyEntry keyEntry, String targetNode, String nomeAtributoId, String namespace) throws Exception {
        
        // 1. Localiza o elemento que será assinado (ex: infNFSe)
        NodeList elements = doc.getElementsByTagName(targetNode);
        if (elements.getLength() == 0) {elements = doc.getElementsByTagNameNS("*", targetNode);}
        if (elements.getLength() == 0) throw new Exception("Tag <" + targetNode + "> não encontrada.");
        
        Element elementToSign = (Element) elements.item(0); 

        String id = elementToSign.getAttribute(nomeAtributoId);
        if (id == null || id.isEmpty()) {
            throw new Exception("O atributo '" + nomeAtributoId + "' na tag <" + targetNode + "> está vazio ou ausente.");
        }
        
        elementToSign.setIdAttribute(nomeAtributoId, true); 
        elementToSign.setIdAttributeNS(null, nomeAtributoId, true); // Garante reconhecimento em diferentes parsers

        // Opcional: Alguns servidores SIL antigos só validam se o ID for setado via Attr
        Attr idAttr = elementToSign.getAttributeNode(nomeAtributoId);
        elementToSign.setIdAttributeNode(idAttr, true);

        // 3. Contexto de Assinatura (DENTRO do elemento para ser Enveloped)
        DOMSignContext dsc = new DOMSignContext(keyEntry.getPrivateKey(), elementToSign);
        dsc.putNamespacePrefix(XMLSignature.XMLNS, "");
        dsc.setIdAttributeNS(elementToSign, null, nomeAtributoId);
        if (namespace != null && !namespace.isEmpty()) {
            dsc.putNamespacePrefix(namespace, "");// Ajuda o validador do servidor a encontrar o nó dentro do namespace correto
        }

        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        // 4. Transformações (Mantendo o Enveloped e Exclusive C14N)        
        Transform enveloped = fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null);
        Transform c14nExc = fac.newTransform(CanonicalizationMethod.EXCLUSIVE, (TransformParameterSpec) null);
        List<Transform> transformList = List.of(enveloped, c14nExc);

        // 5. Referência 
        Reference ref = fac.newReference(
            "#" + elementToSign.getAttribute(nomeAtributoId), 
            fac.newDigestMethod(DigestMethod.SHA1, null), // Constante: http://www.w3.org/2000/09/xmldsig#sha1
            transformList, null, null
        );
        
        SignedInfo si = fac.newSignedInfo(
            fac.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, (C14NMethodParameterSpec) null),
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