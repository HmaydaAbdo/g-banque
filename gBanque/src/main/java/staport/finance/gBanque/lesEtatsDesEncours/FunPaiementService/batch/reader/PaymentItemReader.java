
package staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.batch.reader;

import staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.dtos.FunPaymentDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Custom Spring Batch ItemReader that directly uses WebClient to consume
 * a Flux of FunPaymentDto from an external REST API.
 * This reader fetches all available payments from the external API into an
 * in-memory list during its first 'read' call and then provides them one by one.
 * The PaymentService is no longer used for the API call.
 */
@Component
public class PaymentItemReader implements ItemReader<FunPaymentDto> {

    private static final Logger log = LoggerFactory.getLogger(PaymentItemReader.class);

    private final WebClient webClient;
    private Iterator<FunPaymentDto> paymentIterator;

    public PaymentItemReader(WebClient.Builder webClientBuilder) {
        String paymentServiceBaseUrl = "http://staport.dyndns.org/saidbouriane"; // Hardcoded URL for demonstration
        log.info("PaymentItemReader: Initializing WebClient with base URL: {}", paymentServiceBaseUrl);

        this.webClient = webClientBuilder
                .baseUrl(paymentServiceBaseUrl)
                .filter((request, next) -> {
                    log.debug("PaymentItemReader: Sending request to URL: {}", request.url());
                    return next.exchange(request)
                            .timeout(Duration.ofSeconds(60)) // Set a global request timeout
                            .onErrorResume(TimeoutException.class, e -> {
                                log.error("WebClient request timed out when consuming funPayment web service for URL: {}. Error: {}", request.url(), e.getMessage());
                                return Mono.error(new RuntimeException("External funPayment web service timed out", e));
                            })
                            .doOnError(e -> log.error("PaymentItemReader: Error during WebClient exchange for URL: {}. Error details: {}", request.url(), e.getMessage(), e));
                })
                .build();
    }

    /**
     * Reads the next item from the external payment service.
     * This method is called repeatedly by the Spring Batch framework
     * until it returns null, signaling the end of the input.

     * The first time this method is called, it will trigger the fetch
     * of all payments from the external service and populate an internal list.
     * Subsequent calls will simply iterate through this list.
     *
     * @return The next FunPaymentDto item, or null if no more items are available.

     */
    @Override
    public FunPaymentDto read()   {
        if (paymentIterator == null) {
            log.info("PaymentItemReader: Initializing by fetching all payments from external API...");
            try {
                // Fetch all payments using the WebClient and block until all are collected.
                List<FunPaymentDto> allPayments = fetchPaymentsFromExternalAPI()
                        .collectList()
                        .block();

                if (allPayments != null && !allPayments.isEmpty()) {
                    paymentIterator = allPayments.iterator();
                    log.info("PaymentItemReader: Successfully fetched {} payments for processing.", allPayments.size());
                } else {
                    paymentIterator = Collections.emptyIterator();
                    log.warn("PaymentItemReader: No payments found or fetched from the external API.");
                }
            } catch (Exception e) {
                log.error("PaymentItemReader: Failed to fetch payments from external API during initialization.", e);
                throw new IllegalStateException("Failed to initialize PaymentItemReader: " + e.getMessage(), e);
            }
        }

        if (paymentIterator.hasNext()) {
            FunPaymentDto item = paymentIterator.next();
            log.debug("PaymentItemReader: Providing item with numero: {}", item.getNumero());
            return item;
        } else {
            log.info("PaymentItemReader: All items have been read.");
            paymentIterator = null;
            return null;
        }
    }

    /**
     * Internal method to perform the actual WebClient call.
     * This logic was moved from the original PaymentService.
     *
     * @return A Flux emitting individual FunPaymentDto objects.
     */
    private Flux<FunPaymentDto> fetchPaymentsFromExternalAPI() {
        return webClient.get()
                .uri("/GetPaiement")
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("Error from external payment service. Status: {} - Body: {}", response.statusCode(), body);
                                    return Mono.error(new RuntimeException("Error from payment service: " + response.statusCode() + " " + body));
                                }))
                .bodyToFlux(FunPaymentDto.class)
                .doOnNext(payment -> log.trace("PaymentItemReader: Successfully processed individual payment: {}", payment.getNumero()))
                .doOnError(e -> log.error("PaymentItemReader: Error fetching payments asynchronously from external API. Exception type: {}. Message: {}", e.getClass().getName(), e.getMessage(), e))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof RuntimeException && throwable.getMessage().contains("timed out") || throwable instanceof WebClientResponseException))
                .onErrorResume(e -> {
                    log.error("PaymentItemReader: Final fallback after retries failed for payments. Cause: {}", e.getMessage(), e);
                    return Flux.error(new RuntimeException("PaymentItemReader: Failed to retrieve payments after multiple attempts.", e));
                });
    }
}