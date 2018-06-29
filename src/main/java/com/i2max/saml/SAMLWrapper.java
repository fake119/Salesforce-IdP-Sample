package com.i2max.saml;

// import java.nio.file.Path;
// import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SAMLWrapper {
	private static final Logger logger = LoggerFactory.getLogger(SAMLWrapper.class);
	
	public final static String recipientURL = "https://ksisso-dev-ed.my.salesforce.com/idp/login?app=0sp90000000TNbj"; // idp
	// private Path publicKeyPath = Paths.get("C:/certifications", "idp_certification.crt"); // crt is PEM(base64)
	private String issuer = "https://ksisso-dev-ed.my.salesforce.com";
	private String acsUrl = "http://localhost:8080/sso/acs";
	static HashMap<String, List<String>> attributes = new HashMap<String, List<String>>();
	
	public SAMLWrapper() {}
	
	public String generateAuthnRequest() {
		// OpenSAML을 이용해서 이쁘게 만들어도 되지만 그건 나중에.. 결과적으로 최종 output이 하기와 같이 나오면 됨.
		SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		String s = dateFormatGmt.format(new Date());
		logger.debug("GMT is {}", s);
		
		String xml = "";
		
		xml +="<samlp:AuthnRequest ID='bndkmeemcaamihajeloilkagfdliilbhjjnmlmfo' Version='2.0' ";
		xml +="   IssueInstant='" + s + "'  ";
		xml +="   ProtocolBinding='urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST'  ";
		xml +="   ProviderName='google.com' IsPassive='false'  ";
		xml +="   AssertionConsumerServiceURL='" + acsUrl + "'> ";
		xml +="   <saml:Issuer>" + issuer + "</saml:Issuer> ";
		xml +="   <samlp:NameIDPolicy AllowCreate='true' Format='urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified'/> ";
		xml +="</samlp:AuthnRequest> ";
		
		return xml;
	}
}
