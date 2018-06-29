package salesforce_idp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.Response;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.XMLParserException;
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class Test {
	private static final Logger logger = LoggerFactory.getLogger(Test.class);

	public static void main(String[] args) throws XPathExpressionException, SAXException, IOException,
			ParserConfigurationException, ConfigurationException, XMLParserException, UnmarshallingException {

		DefaultBootstrap.bootstrap();
		BasicParserPool parserPool = new BasicParserPool();
		parserPool.setNamespaceAware(true);

		// InputSource is = new InputSource(new StringReader(xml));
		// InputStream is = new
		// ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
		InputStream is = new FileInputStream(new File("C:/Users/ms/Downloads/decoded (3).xml"));
		Document document = parserPool.parse(is);
		Element metadataRoot = document.getDocumentElement();
		Unmarshaller unmarshaller = Configuration.getUnmarshallerFactory().getUnmarshaller(metadataRoot);

		Response response = (Response) unmarshaller.unmarshall(metadataRoot);

		List<Assertion> assList = response.getAssertions();
		logger.debug("assertion length:" + assList.size());
		Assertion assertion = assList.get(0);
		Signature signature = assertion.getSignature();

		CertificateFactory certificateFactory;
		SignatureValidator signatureValidator = null;
		X509EncodedKeySpec publicKeySpec = null;
		InputStream certInputStream = null;

		File certificateFile = new File("C:/certifications/idp/idp_certification.crt");

		try {
			certInputStream = new FileInputStream(certificateFile);

			certificateFactory = CertificateFactory.getInstance("X.509");
			X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(certInputStream);
			// pull out the public key part of the certificate into a KeySpec
			publicKeySpec = new X509EncodedKeySpec(certificate.getPublicKey().getEncoded());
			// get KeyFactory object that creates key objects,specifying RSA
		} catch (CertificateException e1) {
			logger.debug("CertificateException thrown");
			e1.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		KeyFactory keyFactory;
		try {

			keyFactory = KeyFactory.getInstance("RSA");
			logger.debug("Security Provider: " + keyFactory.getProvider().toString());

			// generate public key to validate signatures
			PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
			// we have the public key
			logger.debug("Public Key created");
			// create credentials
			BasicX509Credential publicCredential = new BasicX509Credential();
			// add public key value
			publicCredential.setPublicKey(publicKey);
			// create SignatureValidator
			signatureValidator = new SignatureValidator(publicCredential);
			logger.debug("Algorithm:" + publicKey.getAlgorithm());
			logger.debug("Format:" + publicKey.getFormat());

			// no validation exception was thrown
			logger.debug("Signature is valid.");

		} catch (NoSuchAlgorithmException e) {
			// e.getClass().getSimpleName();
			logger.debug("NoSuchAlgorithmException thrown");
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}

		try {
			logger.debug("Validating signature....");
			signatureValidator.validate(signature);
			logger.debug("Signature is valid.");
		} catch (Exception ve) {
			logger.debug("Signature is NOT valid.");
			ve.getStackTrace();
		}
		
		// 넘어온 값 뽑기.
		Map<String, String> attributeMap = new HashMap<String, String>();
		List<AttributeStatement> attributeStatementList = assertion.getAttributeStatements();
		for (int i = 0; i < attributeStatementList.size(); i++) {
			AttributeStatement attributeStatement = attributeStatementList.get(i);
			List<Attribute> attributeList = attributeStatement.getAttributes();
			
			for (int j = 0; j < attributeList.size(); j++) {
				Attribute attribute = attributeList.get(j);
				String name = attribute.getName();
				XMLObject xmlObject = attribute.getAttributeValues().get(0);
				String value = xmlObject.getDOM().getFirstChild().getNodeValue();
				logger.debug(name + " : " + value);
				
				attributeMap.put(name, value);
			}
		}
	}
}
