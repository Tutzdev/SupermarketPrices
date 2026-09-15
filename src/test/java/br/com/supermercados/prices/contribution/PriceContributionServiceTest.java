package br.com.supermercados.prices.contribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.supermercados.prices.admin.AdminAuditService;
import br.com.supermercados.prices.auth.AuthenticatedUser;
import br.com.supermercados.prices.common.ApiException;
import br.com.supermercados.prices.datasource.DataSource;
import br.com.supermercados.prices.price.PriceObservation;
import br.com.supermercados.prices.price.PriceObservationRules;
import br.com.supermercados.prices.price.PriceRecordResponse;
import br.com.supermercados.prices.price.PriceService;
import br.com.supermercados.prices.price.StockAvailability;
import br.com.supermercados.prices.product.ProductService;
import br.com.supermercados.prices.store.StoreService;
import br.com.supermercados.prices.user.UserRole;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PriceContributionServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-15T12:00:00Z");

    @Mock private PriceContributionRepository contributions;
    @Mock private ProductService products;
    @Mock private StoreService stores;
    @Mock private PriceService prices;
    @Mock private ContributionSource contributionSource;
    @Mock private AdminAuditService audit;
    @Mock private DataSource source;
    @Mock private PriceRecordResponse priceRecord;

    private PriceContributionService service;

    @BeforeEach
    void setUp() {
        service = new PriceContributionService(contributions, products, stores, prices,
                new PriceObservationRules(), contributionSource, audit,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void approvalPublishesExactlyOneImmutableObservationWhenReplayed() {
        UUID contributorId = UUID.randomUUID();
        UUID moderatorId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        when(contributions.save(any(PriceContribution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        PriceContributionResponse submitted = service.submit(
                new AuthenticatedUser(contributorId, UserRole.USER, true), request());
        ArgumentCaptor<PriceContribution> stored = ArgumentCaptor.forClass(PriceContribution.class);
        verify(contributions).save(stored.capture());
        when(contributions.findByIdForUpdate(submitted.id())).thenReturn(Optional.of(stored.getValue()));
        when(contributionSource.requireEnabledSource()).thenReturn(source);
        when(source.getId()).thenReturn(sourceId);
        when(priceRecord.id()).thenReturn(recordId);
        when(prices.appendUserContribution(any(PriceObservation.class), org.mockito.ArgumentMatchers.eq(submitted.id())))
                .thenReturn(priceRecord);

        PriceContributionResponse first = service.approve(moderatorId, submitted.id());
        PriceContributionResponse replay = service.approve(moderatorId, submitted.id());

        assertThat(first.status()).isEqualTo(ContributionStatus.APPROVED);
        assertThat(first.priceRecordId()).isEqualTo(recordId);
        assertThat(replay.priceRecordId()).isEqualTo(recordId);
        verify(prices).appendUserContribution(any(PriceObservation.class),
                org.mockito.ArgumentMatchers.eq(submitted.id()));
    }

    @Test
    void rejectedContributionNeverPublishesPriceAndCannotLaterBeApproved() {
        UUID moderatorId = UUID.randomUUID();
        PriceContribution contribution = new PriceContribution(UUID.randomUUID(), request(), NOW);
        when(contributions.findByIdForUpdate(contribution.getId())).thenReturn(Optional.of(contribution));

        PriceContributionResponse rejected = service.reject(
                moderatorId, contribution.getId(), new RejectContributionRequest("Imagem sem data legível"));

        assertThat(rejected.status()).isEqualTo(ContributionStatus.REJECTED);
        assertThat(rejected.priceRecordId()).isNull();
        assertThatThrownBy(() -> service.approve(moderatorId, contribution.getId()))
                .isInstanceOf(ApiException.class);
        verify(prices, never()).appendUserContribution(any(), any());
    }

    @Test
    void pendingContributionDoesNotPublishPrice() {
        when(contributions.save(any(PriceContribution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PriceContributionResponse response = service.submit(
                new AuthenticatedUser(UUID.randomUUID(), UserRole.USER, true), request());

        assertThat(response.status()).isEqualTo(ContributionStatus.PENDING);
        assertThat(response.priceRecordId()).isNull();
        verifyNoInteractions(prices, contributionSource, audit);
    }

    @Test
    void contributionDetailIsScopedToItsOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID contributionId = UUID.randomUUID();
        when(contributions.findByIdAndContributorId(contributionId, ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findMine(ownerId, contributionId))
                .isInstanceOf(ApiException.class);

        verify(contributions).findByIdAndContributorId(contributionId, ownerId);
    }

    @Test
    void unverifiedUserCannotSubmitContribution() {
        assertThatThrownBy(() -> service.submit(
                new AuthenticatedUser(UUID.randomUUID(), UserRole.USER, false), request()))
                .isInstanceOf(ApiException.class);

        verifyNoInteractions(products, stores, prices);
    }

    private SubmitPriceContributionRequest request() {
        return new SubmitPriceContributionRequest(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("10.00"), new BigDecimal("9.00"), "BRL", NOW.minusSeconds(60),
                NOW.plusSeconds(3600), NOW.plusSeconds(1800), StockAvailability.UNKNOWN);
    }
}
