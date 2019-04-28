package com.bots.shura

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class Config {

    class DSWrapper {
        DataSource dataSource
    }

    @Bean
    DSWrapper shuraDS() {

    }

}
