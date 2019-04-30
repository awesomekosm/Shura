package com.bots.shura.db.repositories

import com.bots.shura.db.entities.Command
import org.springframework.data.jpa.repository.JpaRepository

interface CommandRepository extends JpaRepository<Command, Long> {
}
