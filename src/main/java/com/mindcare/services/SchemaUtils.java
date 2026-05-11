package com.mindcare.services;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class SchemaUtils {

    private SchemaUtils() {
    }

    static Set<String> loadColumns(DatabaseMetaData metaData, String table) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (ResultSet rs = metaData.getColumns(null, null, table, null)) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
        }
        return columns;
    }

    static String firstExisting(Set<String> columns, List<String> candidates) {
        for (String candidate : candidates) {
            if (columns.contains(candidate.toLowerCase(Locale.ROOT))) {
                return candidate;
            }
        }
        return null;
    }

    static String firstExistingRequired(Set<String> columns, List<String> candidates, String label) {
        String value = firstExisting(columns, candidates);
        if (value == null) {
            throw new IllegalStateException("Required column not found for " + label + ": " + candidates);
        }
        return value;
    }
}
