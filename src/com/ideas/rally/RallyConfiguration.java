package com.ideas.rally;

import com.rallydev.rest.RallyRestApi;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class RallyConfiguration {
	private static String jdbcURL="";
	static String postUrl = "";
	static String alternatePostUrl = "";
	private static String RALLY_URL = "";
	private static String mysqlUser="";
	private static String mysqlPassword="";
	static String postEmail="";
	private static RallyRestApi restApi;
	private static String RALLY_USER_NAME = "";
	private static String RALLY_USER_PASS = "";
	public static String RALLY_PROJECT = "";

	private static final Properties configurationProperties = new Properties();
	static {
		try {
		
			configurationProperties.load( new FileReader("rally_star.properties"));
			jdbcURL = configurationProperties.getProperty("mysql.jdbc.url");
			System.out.println("mysql.jdbc.url:"+jdbcURL);
			mysqlUser = configurationProperties.getProperty("mysql.user");
			mysqlPassword = decodeBase64String(configurationProperties.getProperty("mysql.password"));
			postUrl = configurationProperties.getProperty("rockstar.postUrl");	
			System.out.println("rockstar.postUrl:"+postUrl);
			alternatePostUrl = configurationProperties.getProperty("rockstar.alternatePostUrl");			
			System.out.println("rockstar.alternatePostUrl:"+alternatePostUrl);
			postEmail = configurationProperties.getProperty("rockstar.email");
			RALLY_URL = configurationProperties.getProperty("rally.url");
			System.out.println("rally.url:"+RALLY_URL);
			RALLY_USER_NAME = configurationProperties.getProperty("rally.user.name");
			RALLY_USER_PASS = decodeBase64String(configurationProperties.getProperty("rally.user.password"));
			RALLY_PROJECT = configurationProperties.getProperty("rally.project");
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	


	public static RallyRestApi getRallyRestApi() throws URISyntaxException{
        // Rally parameters
		if(restApi == null) {
	        String rallyURL = RALLY_URL;
	        String userName = RALLY_USER_NAME;
	        String userPassword = RALLY_USER_PASS;
	        String applicationName = "STAR Integration";
	
	        // Create and configure a new instance of RallyRestApi
	        restApi = new RallyRestApi(
	                                                    new URI(rallyURL), 
	                                                    userName,
	                                                    userPassword
	                                                );
	        restApi.setApplicationName(applicationName);
		}
        return restApi;
	}
	

	public static Connection getConnection() throws ClassNotFoundException, SQLException{
        Class.forName("com.mysql.jdbc.Driver");        
        return DriverManager.getConnection(jdbcURL,mysqlUser,mysqlPassword);
		
	}
	
	public static void main(String args[]) throws IOException {
		System.out.println(encodeStringToBase64(args[0]));
	}
	
	
	private static String encodeStringToBase64(String inputValue) throws UnsupportedEncodingException {
		BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(inputValue.getBytes("utf-8") );
	}
	
	private static String decodeBase64String(String base64Value) throws IOException {
		BASE64Decoder decoder = new BASE64Decoder();
		byte[] decodedByteArray = decoder.decodeBuffer(base64Value);

		return new String(decodedByteArray);
	}
	
}
