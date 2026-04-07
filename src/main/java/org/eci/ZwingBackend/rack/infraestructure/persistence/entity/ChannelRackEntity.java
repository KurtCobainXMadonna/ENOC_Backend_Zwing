package org.eci.ZwingBackend.rack.infraestructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(name = "channel_racks")
public class ChannelRackEntity {
    @Id
    private UUID rackId;

    @Column(nullable = false, unique = true)
    private UUID projectId;

    @Column(nullable = false)
    private int bpm = 120;

    @OneToMany(mappedBy = "rack", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("position ASC")
    private List<ChannelEntity> channels = new ArrayList<>();
}
