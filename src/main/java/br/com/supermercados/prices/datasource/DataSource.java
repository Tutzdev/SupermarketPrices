package br.com.supermercados.prices.datasource;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "data_sources")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DataSource {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 2048)
    private String baseUrl;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private Instant verifiedAt;

    @Column(nullable = false)
    private Instant createdAt;

    DataSource(SourceRegistration registration, Instant now) {
        id = UUID.randomUUID();
        code = registration.code().strip();
        name = registration.name().strip();
        baseUrl = registration.baseUrl().strip();
        enabled = true;
        verifiedAt = registration.verifiedAt();
        createdAt = now;
    }
}
