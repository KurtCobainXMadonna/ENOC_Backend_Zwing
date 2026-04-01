package org.eci.ZwingBackend.rack.infraestructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
@Table(name = "channels")
public class ChannelEntity {
    @Id
    private UUID channelId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rack_id", nullable = false)
    private ChannelRackEntity rack;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private UUID soundId;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private float volume = 1.0f;

    /**
     * 16-step grid stored as comma-separated booleans: "true,false,true,..."
     * Mapper converts to/from boolean[].
     */
    @Column(nullable = false, length = 127)
    private String steps;

    @Column(nullable = false)
    private int position;
}
