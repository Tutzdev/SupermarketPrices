package br.com.supermercados.prices.collection.vip;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import br.com.supermercados.prices.collection.CollectorMetadata;

public record VipStoreDefinition(
        String code, String chain, String name, URI website, String configurationPath,
        int organizationId, int storefrontId, int branchId, int distributionId, String cnpj) {

    public CollectorMetadata metadata() {
        return new CollectorMetadata(code, chain, name, "vip_" + organizationId,
                "Loja pública " + chain, website.toString(), Instant.parse("2026-09-18T00:00:00Z"),
                chain, "vip:" + organizationId + ":chain",
                "vip:" + organizationId + ":store:" + distributionId,
                UUID.fromString("5c4cb935-52e1-4bf8-8d17-902dc0837c66"));
    }
}
