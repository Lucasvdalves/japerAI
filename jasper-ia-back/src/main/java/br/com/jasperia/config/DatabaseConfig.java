package br.com.jasperia.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseConfig {

    private static final Map<String, DbConnectionInfo> tenantConnections = new ConcurrentHashMap<>();

    public static void setConnectionInfo(String tenantId, DbConnectionInfo info) {
        tenantConnections.put(tenantId, info);
    }

    public static void testConnection(DbConnectionInfo info) throws Exception {
        String jdbcUrl = "";
        String driverClassName = "";

        switch (info.getDbType().toUpperCase()) {
            case "ORACLE":
                if (info.getDatabaseName().startsWith("/") || info.getDatabaseName().startsWith(":")) {
                    jdbcUrl = "jdbc:oracle:thin:@" + info.getHost() + ":" + info.getPort() + info.getDatabaseName();
                } else {
                    jdbcUrl = "jdbc:oracle:thin:@//" + info.getHost() + ":" + info.getPort() + "/" + info.getDatabaseName();
                }
                driverClassName = "oracle.jdbc.OracleDriver";
                break;
            case "POSTGRESQL":
                jdbcUrl = "jdbc:postgresql://" + info.getHost() + ":" + info.getPort() + "/" + info.getDatabaseName();
                driverClassName = "org.postgresql.Driver";
                break;
            case "MYSQL":
                jdbcUrl = "jdbc:mysql://" + info.getHost() + ":" + info.getPort() + "/" + info.getDatabaseName();
                driverClassName = "com.mysql.cj.jdbc.Driver";
                break;
            case "SQLSERVER":
                jdbcUrl = "jdbc:sqlserver://" + info.getHost() + ":" + info.getPort() + ";databaseName=" + info.getDatabaseName() + ";trustServerCertificate=true";
                driverClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
                break;
            default:
                throw new SQLException("Banco de dados não suportado: " + info.getDbType());
        }

        Class.forName(driverClassName);
        try (Connection conn = DriverManager.getConnection(jdbcUrl, info.getUsername(), info.getPassword())) {
            // Conexão realizada com sucesso
        }
    }

    public static Connection getConnection(String tenantId) throws SQLException {
        DbConnectionInfo info = tenantConnections.get(tenantId);
        if (info == null) {
            return dataSource.getConnection(); // Fallback para Oracle XE padrão
        }

        String jdbcUrl = "";
        String driverClassName = "";

        switch (info.getDbType().toUpperCase()) {
            case "ORACLE":
                if (info.getDatabaseName().startsWith("/") || info.getDatabaseName().startsWith(":")) {
                    jdbcUrl = "jdbc:oracle:thin:@" + info.getHost() + ":" + info.getPort() + info.getDatabaseName();
                } else {
                    jdbcUrl = "jdbc:oracle:thin:@//" + info.getHost() + ":" + info.getPort() + "/" + info.getDatabaseName();
                }
                driverClassName = "oracle.jdbc.OracleDriver";
                break;
            case "POSTGRESQL":
                jdbcUrl = "jdbc:postgresql://" + info.getHost() + ":" + info.getPort() + "/" + info.getDatabaseName();
                driverClassName = "org.postgresql.Driver";
                break;
            case "MYSQL":
                jdbcUrl = "jdbc:mysql://" + info.getHost() + ":" + info.getPort() + "/" + info.getDatabaseName();
                driverClassName = "com.mysql.cj.jdbc.Driver";
                break;
            case "SQLSERVER":
                jdbcUrl = "jdbc:sqlserver://" + info.getHost() + ":" + info.getPort() + ";databaseName=" + info.getDatabaseName() + ";trustServerCertificate=true";
                driverClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
                break;
            default:
                throw new SQLException("Banco de dados não suportado: " + info.getDbType());
        }

        try {
            Class.forName(driverClassName);
            return DriverManager.getConnection(jdbcUrl, info.getUsername(), info.getPassword());
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver JDBC não encontrado para " + info.getDbType(), e);
        }
    }

    public static class DbConnectionInfo {
        private String dbType;
        private String host;
        private int port;
        private String databaseName;
        private String username;
        private String password;

        public String getDbType() { return dbType; }
        public void setDbType(String dbType) { this.dbType = dbType; }
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getDatabaseName() { return databaseName; }
        public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    private static HikariDataSource dataSource;

    static {
        try {
            HikariConfig config = new HikariConfig();

            // Configuração da URL de conexão com o Oracle Docker local
            config.setJdbcUrl("jdbc:oracle:thin:@localhost:1522:XE");
            config.setUsername("system");
            config.setPassword("oracle"); // Ajuste para a senha definida no seu container
            config.setDriverClassName("oracle.jdbc.OracleDriver");

            // Configurações de otimização do Pool para o MVP
            config.setMaximumPoolSize(10); // Máximo de conexões simultâneas ativos
            config.setMinimumIdle(2);      // Mínimo de conexões em espera
            config.setIdleTimeout(30000);
            config.setConnectionTimeout(20000);

            // Otimizações específicas para performance de queries Oracle
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);
            System.out.println("💪 Pool de conexões HikariCP com Oracle configurado com sucesso!");
        } catch (Exception e) {
            System.err.println("❌ Erro ao inicializar o pool do banco de dados: " + e.getMessage());
            throw new ExceptionInInitializerError(e);
        }
    }

    // Método thread-safe para o controlador buscar uma conexão limpa do pool
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // Fecha o pool quando o servidor for desligado
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}