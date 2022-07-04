package com.sum.client;

import jakarta.persistence.*;
import lombok.*;

@Builder
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "app_profile")
@NamedQueries({
        @NamedQuery(name = "AppProfile.findByName", query = "select a from AppProfile a where a.name = :name")
})
@NoArgsConstructor
public class AppProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Hibernate")
    @SequenceGenerator(name = "Hibernate", sequenceName = "Hibernate_Sequence", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name")
    private String name;

    @PostLoad
    public void postRemove() {

    }
}