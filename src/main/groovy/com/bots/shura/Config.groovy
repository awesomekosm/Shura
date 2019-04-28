package com.bots.shura

import com.bots.shura.db.DBType
import com.bots.shura.db.DSWrapper
import com.bots.shura.db.DataSourceRouter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

import javax.sql.DataSource

@Configuration
class Config {

    @Bean
    DSWrapper shuraWrapper(@Value('${shura.datasource.url}') String url,
                           @Value('${shura.datasource.driver}') String driver) {
        def builder = DataSourceBuilder.create().url(url).driverClassName(driver)
        return new DSWrapper(dataSource: builder.build() as DataSource)
    }

    @Bean
    @Primary
    DataSource dataSource(@Qualifier('shuraWrapper') DSWrapper shuraWrapper) {
        def targetDataSources = [(DBType): shuraWrapper.dataSource]

        def routingDataSource = new DataSourceRouter()
        routingDataSource.setTargetDataSources(targetDataSources)
        routingDataSource.setDefaultTargetDataSource(shuraWrapper.dataSource)

        return routingDataSource
    }
}
