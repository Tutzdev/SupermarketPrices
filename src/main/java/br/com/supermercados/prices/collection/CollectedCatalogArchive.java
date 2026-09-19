package br.com.supermercados.prices.collection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

/** Optional public-data archive for retrying persistence without recollecting or changing timestamps. */
@Component
public class CollectedCatalogArchive {

    private final Path directory;
    private final ObjectMapper mapper;

    public CollectedCatalogArchive(@Value("${app.collection.archive-directory:}") String directory, ObjectMapper mapper) {
        this.directory = directory.isBlank() ? null : Path.of(directory).toAbsolutePath().normalize();
        this.mapper = mapper;
    }

    public void save(CollectorMetadata metadata, CollectedCatalog catalog) {
        if (directory == null) return;
        Path destination = path(metadata.code());
        try {
            Files.createDirectories(directory);
            Path staging = Files.createTempFile(directory, metadata.code(), ".tmp");
            try {
                Files.writeString(staging, mapper.writeValueAsString(new ArchivedCatalog(metadata, catalog)));
                Files.move(staging, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(staging);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível arquivar a coleta pública", exception);
        }
    }

    public CollectedCatalog read(CollectorMetadata metadata) {
        Path source = path(metadata.code());
        try {
            if (Files.size(source) > 64_000_000) throw new IllegalStateException("Arquivo de coleta excessivamente grande");
            ArchivedCatalog archived = mapper.readValue(Files.readString(source), ArchivedCatalog.class);
            if (!metadata.sourceCode().equals(archived.metadata().sourceCode())
                    || !metadata.storeSourceReference().equals(archived.metadata().storeSourceReference())) {
                throw new IllegalStateException("Arquivo pertence a outra fonte ou unidade");
            }
            return archived.catalog();
        } catch (IOException exception) {
            throw new IllegalStateException("Arquivo de coleta não encontrado ou ilegível", exception);
        }
    }

    private Path path(String code) {
        if (directory == null) throw new IllegalStateException("Arquivo de coletas não configurado");
        if (!code.matches("[a-z0-9_]+")) throw new IllegalArgumentException("Código de coletor inválido");
        return directory.resolve(code + ".json");
    }

    public record ArchivedCatalog(CollectorMetadata metadata, CollectedCatalog catalog) {
    }
}
