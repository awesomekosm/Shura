package com.bots.shura.db.repositories

import com.bots.shura.db.entities.Track
import org.springframework.data.jpa.repository.JpaRepository

interface TrackRepository extends JpaRepository<Track, Long> {
    List<Track> findAllByName(String name)
}
