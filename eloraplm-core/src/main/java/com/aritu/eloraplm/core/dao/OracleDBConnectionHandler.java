/*
 * (C) Copyright 2015 Aritu S Coop (http://aritu.com/).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package com.aritu.eloraplm.core.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import oracle.jdbc.pool.OracleDataSource;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */
public class OracleDBConnectionHandler {

    public static Connection getDBConnection(String jdbcUrl, String user,
            String password) throws SQLException {
        OracleDataSource ds;
        ds = new OracleDataSource();
        ds.setURL(jdbcUrl);
        Connection conn = ds.getConnection(user, password);
        return conn;
    }

    public static void closeAll(Connection conn, Statement stmt) {

        closeConnection(conn);
        closeStatement(stmt);
    }

    public static void closeAll(Connection conn, Statement stmt, ResultSet rset) {

        closeAll(conn, stmt);
        closeResultSet(rset);
    }

    public static void closeAll(Statement stmt, ResultSet rset) {

        closeStatement(stmt);
        closeResultSet(rset);
    }

    public static void closeAll(Connection conn, ResultSet rset) {
        closeResultSet(rset);
        closeConnection(conn);
    }

    private static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void closeResultSet(ResultSet rset) {
        if (rset != null) {
            try {
                rset.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

}
