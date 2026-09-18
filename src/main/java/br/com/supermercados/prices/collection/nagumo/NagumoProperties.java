package br.com.supermercados.prices.collection.nagumo;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.collection.nagumo")
public class NagumoProperties {

    private URI baseUrl = URI.create("https://www.nagumo.com.br");
    private String storeId = "36";
    private String storeName = "036-V.REDONDA";
    private String expectedCity = "VOLTA REDONDA";
    private String expectedDistrict = "PONTE ALTA";
    private String expectedSourceAddress = "VIA SERGIO BRAGA, PONTE ALTA, VOLTA REDONDA, RJ";
    private String address = "Via Sergio Braga, 951 - Ponte Alta - Volta Redonda/RJ - CEP 27265-601";
    private BigDecimal latitude = new BigDecimal("-22.5295573");
    private BigDecimal longitude = new BigDecimal("-44.1360020");
    private String categoryId = "MP-GERAL";
    private List<String> additionalCategoryIds = List.of();
    private String promotionFlag = "NGM_36_M";
    private int pageSize = 50;
    private Duration requestDelay = Duration.ofSeconds(1);
    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration requestTimeout = Duration.ofSeconds(20);
    private int maxAttempts = 2;
    private int maxResponseBytes = 5_000_000;
    private ZoneId zone = ZoneId.of("America/Sao_Paulo");

    public URI getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(URI baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getExpectedCity() {
        return expectedCity;
    }

    public void setExpectedCity(String expectedCity) {
        this.expectedCity = expectedCity;
    }

    public String getExpectedDistrict() {
        return expectedDistrict;
    }

    public void setExpectedDistrict(String expectedDistrict) {
        this.expectedDistrict = expectedDistrict;
    }

    public String getExpectedSourceAddress() {
        return expectedSourceAddress;
    }

    public void setExpectedSourceAddress(String expectedSourceAddress) {
        this.expectedSourceAddress = expectedSourceAddress;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getPromotionFlag() {
        return promotionFlag;
    }

    public List<String> getAdditionalCategoryIds() {
        return additionalCategoryIds;
    }

    public void setAdditionalCategoryIds(List<String> additionalCategoryIds) {
        this.additionalCategoryIds = List.copyOf(additionalCategoryIds);
    }

    public void setPromotionFlag(String promotionFlag) {
        this.promotionFlag = promotionFlag;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public Duration getRequestDelay() {
        return requestDelay;
    }

    public void setRequestDelay(Duration requestDelay) {
        this.requestDelay = requestDelay;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getMaxResponseBytes() {
        return maxResponseBytes;
    }

    public void setMaxResponseBytes(int maxResponseBytes) {
        this.maxResponseBytes = maxResponseBytes;
    }

    public ZoneId getZone() {
        return zone;
    }

    public void setZone(ZoneId zone) {
        this.zone = zone;
    }
}
