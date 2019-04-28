package com.bots.shura.db.entities

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
class State {
    @Id
    @GeneratedValue
    Long id
}
