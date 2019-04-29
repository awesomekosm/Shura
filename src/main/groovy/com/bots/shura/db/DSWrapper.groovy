package com.bots.shura.db

import javax.sql.DataSource

/** https://docs.spring.io/spring-boot/docs/current/reference/html/howto-data-access.html **/
/** Can't create DataSource beans and use AbstractRoutingDataSource because sprint triggers out of order process to create default data source**/
class DSWrapper {
    DataSource dataSource

    DSWrapper(DataSource ds) {
        dataSource = ds
    }
}
