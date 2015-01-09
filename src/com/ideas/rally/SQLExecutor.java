package com.ideas.rally;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

public abstract class SQLExecutor {
    public static final int NO_ROWS_INSERTED=0;
    private final String query;
    private final Map inputMap;

    public String result;

    public SQLExecutor(Map<String,String> inputMap,String query) {
        this.query = query;
        this.inputMap=inputMap;
    }

    public static int[] executeUpdate(String... statements) throws Exception {
        int result[] = new int[statements.length];
        int queryNumber=0;
        Connection con = RallyConfiguration.getConnection();
        Statement stmt = con.createStatement();
        for (String query : statements) {
            result[queryNumber++] = stmt.executeUpdate(query);
        }
        stmt.close();
        con.close();
        return result;
    }

    public void go() throws Exception {
        Connection con = RallyConfiguration.getConnection();
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        while (rs.next()) {
            accept(rs,inputMap);
        }
        rs.close();
        stmt.close();
        con.close();
    }

    public abstract void accept(ResultSet rs, Map<String,String> input) throws Exception;
}
