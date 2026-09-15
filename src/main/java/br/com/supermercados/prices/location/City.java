package br.com.supermercados.prices.location;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class City {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID stateId;

    @Column(nullable = false, length = 120)
    private String name;
}
