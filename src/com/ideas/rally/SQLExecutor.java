package com.ideas.rally;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public abstract class SQLExecutor {
    public static final int NO_ROWS_INSERTED=0;
    private final String query;

    public String result;

    public SQLExecutor(String query) {
        this.query = query;
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
            accept(rs);
        }
        rs.close();
        stmt.close();
        con.close();
    }

    public abstract void accept(ResultSet rs) throws Exception;
}
