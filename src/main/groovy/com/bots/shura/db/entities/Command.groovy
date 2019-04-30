package com.bots.shura.db.entities

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
class Command {
    @Id
    @GeneratedValue
    Long id
    String command
    String user
}
