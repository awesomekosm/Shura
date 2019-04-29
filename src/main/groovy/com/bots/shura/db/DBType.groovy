package com.bots.shura.db

enum DBType {
    SHURA('org.hibernate.dialect.SQLiteDialect')

    private String dialect

    DBType(String dialect) {
        this.dialect = dialect
    }

    String getDialect() {
        return dialect
    }
}
