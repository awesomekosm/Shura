package com.bots.shura.db.entities

import com.bots.shura.audio.TrackOrigin

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
class Track {
    @Id
    @GeneratedValue
    Long id
    String name
    String link
    String playlistName
    TrackOrigin origin
    int time
}
