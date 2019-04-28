package com.bots.shura.db

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

class DataSourceRouter extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        return DatabaseContextHolder.get()
    }
}