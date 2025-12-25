package staport.finance.gBanque.batch.jobs.moyensDePaiements.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;
import staport.finance.gBanque.moyensDePaiements.synchronisation.dtos.FunPaymentDto;

import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Corrected Spring Batch ItemReader that uses WebClient to consume
 * a Flux of FunPaymentDto from an external REST API.
 * The fetch logic is now triggered for every job execution within the
 * open() method, ensuring fresh data for each run.
 */
@Component
public class PaymentItemReader implements ItemStreamReader<FunPaymentDto> {

    private static final Logger log = LoggerFactory.getLogger(PaymentItemReader.class);

    private final WebClient webClient;
    private Iterator<FunPaymentDto> paymentIterator;

    public PaymentItemReader(WebClient.Builder webClientBuilder) {
        String paymentServiceBaseUrl = "http://staport.dyndns.org/saidbouriane";
        log.info("PaymentItemReader: Initializing WebClient with base URL: {}", paymentServiceBaseUrl);
        this.webClient = webClientBuilder.baseUrl(paymentServiceBaseUrl).build();
    }

    /**
     * This method is part of the ItemStreamReader interface and is called
     * at the beginning of each step execution.
     * It's the correct place to fetch all payments to ensure a fresh dataset
     * for every batch run.
     * @param executionContext The current execution context.
     * @throws ItemStreamException if an error occurs during initialization.
     */
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        log.info("PaymentItemReader: Initializing for a new job execution by fetching all payments from external API...");
        try {
            List<FunPaymentDto> allPayments = fetchPaymentsFromExternalAPI()
                    .collectList()
                    .block(Duration.ofSeconds(60));

            if (allPayments != null && !allPayments.isEmpty()) {
                this.paymentIterator = allPayments.iterator();
                log.info("PaymentItemReader: Successfully fetched {} payments for processing.", allPayments.size());
            } else {
                this.paymentIterator = Collections.emptyIterator();
                log.warn("PaymentItemReader: No payments found or fetched from the external API.");
            }
        } catch (Exception e) {
            log.error("PaymentItemReader: Failed to fetch payments from external API during open().", e);
            throw new ItemStreamException("Failed to initialize PaymentItemReader during open(): " + e.getMessage(), e);
        }
    }

    /**
     * Reads the next item from the in-memory list of payments.
     * This method is called repeatedly by the Spring Batch framework
     * until it returns null, signaling the end of the input.
     * @return The next FunPaymentDto item, or null if no more items are available.
     */
    @Override
    public FunPaymentDto read() {
        if (paymentIterator.hasNext()) {
            FunPaymentDto item = paymentIterator.next();
            log.debug("PaymentItemReader: Providing item with numero: {}", item.getNumero());
            return item;
        } else {
            log.info("PaymentItemReader: All items have been read.");
            return null;
        }
    }

    /**
     * No-op method as we don't need to persist state.
     */
    @Override
    public void update(ExecutionContext executionContext) {
        // No state to save
    }

    /**
     * This method is part of the ItemStreamReader interface and is called
     * at the end of each step execution. We reset the iterator here.
     * @throws ItemStreamException if an error occurs during closing.
     */
    @Override
    public void close() throws ItemStreamException {
        log.info("PaymentItemReader: Closing the reader and resetting iterator.");
        this.paymentIterator = null;
    }

    /**
     * Internal method to perform the actual WebClient call.
     * This logic is now a private helper method called by open().
     * @return A Flux emitting individual FunPaymentDto objects.
     */
    private Flux<FunPaymentDto> fetchPaymentsFromExternalAPI() {
        return webClient.get()
                .uri("/Paiement")
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("Error from external payment service. Status: {} - Body: {}", response.statusCode(), body);
                                    return reactor.core.publisher.Mono.error(new RuntimeException("Error from payment service: " + response.statusCode() + " " + body));
                                }))
                .bodyToFlux(FunPaymentDto.class)
                .doOnNext(payment -> log.trace("PaymentItemReader: Successfully processed individual payment: {}", payment.getNumero()))
                .doOnError(e -> log.error("PaymentItemReader: Error fetching payments asynchronously from external API. Exception type: {}. Message: {}", e.getClass().getName(), e.getMessage(), e))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof RuntimeException || throwable instanceof WebClientResponseException))
                .onErrorResume(e -> {
                    log.error("PaymentItemReader: Final fallback after retries failed for payments. Cause: {}", e.getMessage(), e);
                    return Flux.error(new IllegalStateException("PaymentItemReader: Failed to retrieve payments after multiple attempts.", e));
                });
    }
}
