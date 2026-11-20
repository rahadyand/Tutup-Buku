package id.ac.uns.tutupbuku.persistence;

import java.sql.*;

/** Discriminator aditif menjaga seluruh FK owner lama tanpa mengarang NIM/password tamu. */
final class GuestSchema {
    private GuestSchema() {}

    static void migrate(Connection connection) throws SQLException {
        connection.setAutoCommit(false);
        try (var statement = connection.createStatement()) {
            statement.execute(
                    "ALTER TABLE users ADD COLUMN account_kind TEXT NOT NULL DEFAULT 'REGISTERED'"
                        + " CHECK(account_kind IN ('REGISTERED','LOCAL'))");
            statement.execute(
                    "CREATE UNIQUE INDEX one_local_workspace ON users(account_kind) WHERE"
                        + " account_kind='LOCAL'");
            validate(connection);
            statement.execute("PRAGMA user_version=8");
            connection.commit();
        } catch (SQLException error) {
            connection.rollback();
            throw error;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    static void validate(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.executeQuery("SELECT account_kind FROM users LIMIT 0").close();
        }
    }
}
