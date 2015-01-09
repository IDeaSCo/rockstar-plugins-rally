package com.ideas.rally;

import com.rallydev.rest.RallyRestApi;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class RallyConfiguration {
    public static String RALLY_PROJECT = "";
    static String postUrl = "";
    static String alternatePostUrl = "";
    static String postEmail = "";

    private static String jdbcURL = "";
    private static String RALLY_URL = "";
    private static String mysqlUser = "";
    private static String mysqlPassword = "";
    private static RallyRestApi restApi;
    private static String RALLY_USER_NAME = "";
    private static String RALLY_USER_PASS = "";
    public static boolean testRun=false;

    private static final Properties configurationProperties = new Properties();

    static {
        try {
            configurationProperties.load(new FileReader("rally_star.properties"));
            jdbcURL = configurationProperties.getProperty("mysql.jdbc.url");
            System.out.println("mysql.jdbc.url:" + jdbcURL);
            mysqlUser = configurationProperties.getProperty("mysql.user");
            mysqlPassword = decodeBase64String(configurationProperties.getProperty("mysql.password"));
            postUrl = configurationProperties.getProperty("rockstar.postUrl");
            System.out.println("rockstar.postUrl:" + postUrl);
            alternatePostUrl = configurationProperties.getProperty("rockstar.alternatePostUrl");
            System.out.println("rockstar.alternatePostUrl:" + alternatePostUrl);
            postEmail = configurationProperties.getProperty("rockstar.email");
            RALLY_URL = configurationProperties.getProperty("rally.url");
            System.out.println("rally.url:" + RALLY_URL);
            RALLY_USER_NAME = configurationProperties.getProperty("rally.user.name");
            RALLY_USER_PASS = decodeBase64String(configurationProperties.getProperty("rally.user.password"));
            RALLY_PROJECT = configurationProperties.getProperty("rally.project");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static RallyRestApi getRallyRestApi() throws Exception {
        if (restApi == null) {
            restApi = new RallyRestApi(new URI(RALLY_URL), RALLY_USER_NAME, RALLY_USER_PASS);
            restApi.setApplicationName("STAR Integration");
        }
        return restApi;
    }

    public static Connection getConnection() throws Exception {
        if(testRun){
            return DriverManager.getConnection("jdbc:h2:./rallytest;MODE=MySQL", "sa", "");
        }
        Class.forName("com.mysql.jdbc.Driver");
        return DriverManager.getConnection(jdbcURL, mysqlUser, mysqlPassword);
    }

    public static void main(String args[]) throws Exception {
        System.out.println(encodeStringToBase64(args[0]));
    }

    private static String encodeStringToBase64(String inputValue) throws Exception {
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(inputValue.getBytes("utf-8"));
    }

    private static String decodeBase64String(String base64Value) throws Exception {
        BASE64Decoder decoder = new BASE64Decoder();
        byte[] decodedByteArray = decoder.decodeBuffer(base64Value);
        return new String(decodedByteArray);
    }

    static void createSchema() throws Exception {
        StringBuilder storyHistory = new StringBuilder()
                .append(" CREATE TABLE if not exists storyhistory ( ")
                .append("   iteration varchar(20) DEFAULT NULL, ")
                .append("   storyNumber varchar(10) DEFAULT '', ")
                .append("   storyOwner varchar(80) DEFAULT NULL, ")
                .append("   planEstimate float DEFAULT NULL, ")
                .append("   state varchar(12) DEFAULT NULL, ")
                .append("   planEstimateChanged int DEFAULT NULL, ")
                .append("   stateChanged int DEFAULT NULL, ")
                .append("   iterationChanged int DEFAULT NULL, ")
                .append("   spillover int DEFAULT '0', ")
                .append("   PRIMARY KEY (storyNumber) ")
                .append(" ) ");
        SQLExecutor.executeUpdate(storyHistory.toString());

        StringBuilder storyUsers = new StringBuilder()
                .append(" CREATE TABLE if not exists  storyusers ( ")
                .append("   storyNumber varchar(10) DEFAULT '', ")
                .append("   storyTaskOwner varchar(80) DEFAULT '', ")
                .append("   PRIMARY KEY (storyNumber,storyTaskOwner) ")
                .append(" ) ");
        SQLExecutor.executeUpdate(storyUsers.toString());
        StringBuilder taskHistory = new StringBuilder()
                .append(" CREATE TABLE if not exists taskhistory ( ")
                .append("   iteration varchar(20) DEFAULT '', ")
                .append("   taskNumber varchar(10) DEFAULT '', ")
                .append("   taskOwner varchar(80) DEFAULT '', ")
                .append("   actuals float DEFAULT NULL, ")
                .append("   toDo float DEFAULT NULL, ")
                .append("   state varchar(12) DEFAULT NULL, ")
                .append("   taskChanged int DEFAULT NULL, ")
                .append("   PRIMARY KEY (iteration,taskNumber,taskOwner) ")
                .append(" ) ");
        SQLExecutor.executeUpdate(taskHistory.toString());
        StringBuilder user = new StringBuilder()
                .append(" CREATE TABLE if not exists user ( ")
                .append("   userName varchar(90) DEFAULT '', ")
                .append("   email varchar(90) DEFAULT NULL, ")
                .append("   leadAndAbove int DEFAULT '0', ")
                .append("   PRIMARY KEY (userName) ")
                .append(" ) ");
        SQLExecutor.executeUpdate(user.toString());

    }

    static void optimizeTables() throws Exception {
        SQLExecutor.executeUpdate(
                "optimize table storyhistory",
                "optimize table storyusers",
                "optimize table taskhistory",
                "optimize table user"
        );
    }

    static int post(String urlParameters, String url) {
        int responseCode;
        try {
            System.out.println("\nSending 'POST' request to URL : " + postUrl + "/trophy/save");
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            con.setRequestMethod("POST");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Content-Length", "" + urlParameters.length());

            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
            responseCode = con.getResponseCode();
            System.out.println("Response Code:" + responseCode);
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        } catch (Exception e) {
            System.out.println("Could not post the score:" + e.getMessage());
            e.printStackTrace();
            return 500;
        }
        return responseCode;
    }

    static boolean post(String emailId, int score, String reason, String badge) {
        String urlParameters = "{ \"fromUserEmailID\":\"" + postEmail + "\", \"toUserEmailID\":\"" +
                emailId + "\"" +
                ",\"trophies\":" +
                score +
                ",\"badgeName\":" +
                "\"" + badge + "\"" +
                ",\"reason\":\"" + reason + "\"}";

        System.out.println("Post parameters : " + urlParameters);
        int responseCode = post(urlParameters, postUrl);

        if (responseCode == 500) {
            System.out.println("Attempting alternate URL.");
            responseCode = post(urlParameters, alternatePostUrl);
        }

        return responseCode == 200;
    }
}
