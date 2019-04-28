package com.bots.shura.db

import org.springframework.context.ApplicationContext

class DatabaseContextHolder {
    private static final ThreadLocal<DBType> contextHolder = new ThreadLocal<DBType>()

    static void set(DBType dbType) {
        contextHolder.set(dbType)
    }

    static DBType get() {
        return contextHolder.get()
    }
}
