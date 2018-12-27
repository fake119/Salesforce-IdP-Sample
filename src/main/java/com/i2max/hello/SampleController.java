package com.i2max.hello;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import com.i2max.saml.SAMLWrapper;

import org.apache.tomcat.util.codec.binary.Base64;
import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.Response;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.XMLParserException;
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureValidator;
import org.opensaml.xml.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

@Controller
public class SampleController {
	private static final Logger logger = LoggerFactory.getLogger(SampleController.class);

	// @ResponseBody : body 그대로 출력.
	@RequestMapping("/")
	@ResponseBody
	public String home() {
		return "Hello World!";
	}

	// view로 전달.application.properties 파일에서 설정.
	@RequestMapping("/partner/test")
	public String test(Model model) {
		model.addAttribute("name", "SpringBlog from Millky");
		return "partner/test";
	}

	@RequestMapping("/partner/loginOK")
	public String loginOK(HttpServletRequest req, HttpServletResponse res, Model model) throws MarshallingException {
		return "partner/loginOK";
	}

	@RequestMapping("/partner/logout")
	@ResponseBody
	public String logout(HttpServletRequest req, HttpServletResponse res) {
		req.getSession().invalidate();

		return "logout !!";
	}

	// 자체 메뉴 url
	@RequestMapping("/partner/menu1")
	public String menu1(HttpServletRequest req, HttpServletResponse res, Model model) throws MarshallingException {
		logger.debug("menu1 call.!!");
		// req.getSession().setAttribute("authenticated", "yes");
		String redirected = (String) req.getSession().getAttribute("redirected");
		logger.debug("redirected is {}", redirected);
		if (redirected == null) { // salesforce 인증으로 redirect 시킴
			req.getSession().setAttribute("redirected", "yes");
			return "redirect:/sso/samlRequest";
		}
		
		// 갔다 왔는데 인증이 필요한 경우
		String authenticated = (String) req.getSession().getAttribute("authenticated");
		logger.debug("authenticated is {}", authenticated);

		if (authenticated == null || "yes".equals(authenticated) == false) {
			return "sso/unauthenticated";
		}
		
		return "partner/menu1";
	}

	@RequestMapping("/sso/samlRequest")
	public String samlRequest(HttpServletRequest req, HttpServletResponse res, Model model)
			throws MarshallingException {
		SAMLWrapper samlWrapper = new SAMLWrapper();

		String signedSAML = samlWrapper.generateAuthnRequest().trim();// for debuging
		String samlResponse = Base64.encodeBase64String(signedSAML.getBytes());
		String relayState = "xyz";// req.getParameter("relayState");
		logger.debug("relayState ::: {}", relayState);

		model.addAttribute("recipientURL", SAMLWrapper.recipientURL); // for debug
		model.addAttribute("signedSAML", signedSAML.trim());
		model.addAttribute("samlResponse", samlResponse);
		model.addAttribute("relayState", relayState);

		return "sso/samlRequest";
	}

	@RequestMapping("/sso/unauthenticated")
	public String unauthenticated(HttpServletRequest req, HttpServletResponse res, Model model)
			throws MarshallingException {
		req.getSession().invalidate();

		return "sso/unauthenticated";
	}

	@RequestMapping("/sso/start")
	public String start(HttpServletRequest req, HttpServletResponse res, Model model) throws MarshallingException {
		return "sso/start";
	}

	@RequestMapping("/sso/acs")
	public String acs(HttpServletRequest req, HttpServletResponse res, Model model)
			throws MarshallingException, SAXException, IOException, ParserConfigurationException,
			XPathExpressionException, ConfigurationException, XMLParserException, UnmarshallingException {
		Map<String, String[]> pmap = req.getParameterMap();
		for (Object key : pmap.keySet()) {
			String keyStr = (String) key;
			String[] value = (String[]) pmap.get(keyStr);
			logger.debug(key + "   :   " + Arrays.toString(value));
		}

		String[] a = (String[]) pmap.get("SAMLResponse");
		String b = Arrays.toString(a);
		logger.debug("SAMLResponse :: {}", b);
		
		String xml = new String(Base64.decodeBase64(b), "UTF-8");
		logger.debug("xml :: \r\n{}", xml);
		
		String[] c = (String[]) pmap.get("RelayState");
		String relayState = Arrays.toString(c);

		DefaultBootstrap.bootstrap();
		BasicParserPool parserPool = new BasicParserPool();
		parserPool.setNamespaceAware(true);

		InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
		// InputStream is = new FileInputStream(new
		// File("C:/Users/ms/Downloads/decoded (3).xml"));
		Document document = parserPool.parse(is);
		Element metadataRoot = document.getDocumentElement();
		Unmarshaller unmarshaller = Configuration.getUnmarshallerFactory().getUnmarshaller(metadataRoot);

		Response response = (Response) unmarshaller.unmarshall(metadataRoot);

		List<Assertion> assList = response.getAssertions();
		logger.debug("assertion length:" + assList.size());
		Assertion assertion = assList.get(0);
		Signature signature = assertion.getSignature();

		File certificateFile = new File("C:/certifications/idp/idp_certification.crt");

		String assertResult = "Success.!!";
		Boolean isAssertOK = true;
		try {
			InputStream certInputStream = new FileInputStream(certificateFile);

			CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
			X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(certInputStream);
			// pull out the public key part of the certificate into a KeySpec
			X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(certificate.getPublicKey().getEncoded());
			// get KeyFactory object that creates key objects,specifying RSA

			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
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
			SignatureValidator signatureValidator = new SignatureValidator(publicCredential);
			logger.debug("Algorithm:" + publicKey.getAlgorithm());
			logger.debug("Format:" + publicKey.getFormat());

			// no validation exception was thrown
			signatureValidator.validate(signature);
			assertResult = "Signature is valid.";
		} catch (Exception ex) {
			isAssertOK = false;
			assertResult = ex.getLocalizedMessage();
		}

		if (isAssertOK == false) {
			model.addAttribute("assert_result", assertResult);
			return "sso/acs";
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
		logger.debug("attributeMap :: {}", attributeMap);
		
		model.addAllAttributes(attributeMap);
		model.addAttribute("assert_result", assertResult);
		model.addAttribute("RelayState", relayState);// salesforce에 보낸 값을 그대로 전달 받음.

		req.getSession().setAttribute("authenticated", "yes");// 한번 인증 받았으면 정책에 따라 계속 사용할 수 있다.

		return "sso/acs";
	}

}