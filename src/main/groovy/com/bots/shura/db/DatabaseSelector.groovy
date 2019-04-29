package com.bots.shura.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.orm.hibernate5.LocalSessionFactoryBean
import org.springframework.stereotype.Component

@Component
class DatabaseSelector {

    @Autowired
    @Lazy
    private LocalSessionFactoryBean sessionFactory

    public void setDatabase(DBType dbType) {
        DatabaseContextHolder.set(dbType)
        sessionFactory.getHibernateProperties().setProperty('hibernate.dialect', dbType.getDialect())
    }
}
