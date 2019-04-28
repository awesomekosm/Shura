package com.bots.shura.db.repositories

import com.bots.shura.db.entities.State
import org.springframework.data.jpa.repository.JpaRepository

interface StateRepository extends JpaRepository<State, Long> {
}
