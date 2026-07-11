package staport.finance.gBanque.moyensDePaiements.synchronisation.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import staport.finance.gBanque.moyensDePaiements.synchronisation.dtos.FunPaymentDto;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Client centralisé pour l'appel à l'API FunPaiement.
 * <p>
 * Extrait de {@code PaymentItemReader} (10/07/2026) pour isoler la logique WebClient
 * (architecture.md — ne jamais dupliquer une logique métier).
 */
@Component
public class FunPaiementClient {

    private static final Logger log = LoggerFactory.getLogger(FunPaiementClient.class);

    private final WebClient webClient;

    public FunPaiementClient(WebClient.Builder webClientBuilder) {
        String paymentServiceBaseUrl = "http://staport.dyndns.org/saidbouriane";
        this.webClient = webClientBuilder.baseUrl(paymentServiceBaseUrl).build();
    }

    /**
     * Récupère la totalité du flux FunPaiement (bloquant, timeout 60s, 3 retries backoff).
     * FunPaiement renvoie l'intégralité des paiements connus (pas seulement les ENCOURS) —
     * cf. confirmation HMAYDA du 10/07/2026.
     */
    public List<FunPaymentDto> fetchAll() {
        List<FunPaymentDto> allPayments = fetchPaymentsFromExternalAPI()
                .collectList()
                .block(Duration.ofSeconds(60));

        if (allPayments == null) {
            log.warn("FunPaiementClient: réponse vide de l'API externe.");
            return Collections.emptyList();
        }
        return allPayments;
    }

    private Flux<FunPaymentDto> fetchPaymentsFromExternalAPI() {
        return webClient.get()
                .uri("/Paiement")
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("Error from external payment service. Status: {} - Body: {}", response.statusCode(), body);
                                    return Mono.error(new RuntimeException("Error from payment service: " + response.statusCode() + " " + body));
                                }))
                .bodyToFlux(FunPaymentDto.class)
                .doOnNext(payment -> log.trace("FunPaiementClient: payment reçu: {}", payment.getNumero()))
                .doOnError(e -> log.error("FunPaiementClient: erreur lors de la récupération asynchrone. Type: {}. Message: {}", e.getClass().getName(), e.getMessage(), e))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof RuntimeException || throwable instanceof WebClientResponseException))
                .onErrorResume(e -> {
                    log.error("FunPaiementClient: échec final après plusieurs tentatives. Cause: {}", e.getMessage(), e);
                    return Flux.error(new IllegalStateException("FunPaiementClient: impossible de récupérer les paiements après plusieurs tentatives.", e));
                });
    }
}
