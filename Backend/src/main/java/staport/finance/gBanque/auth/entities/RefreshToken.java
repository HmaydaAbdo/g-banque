package staport.finance.gBanque.auth.entities;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Jeton de rafraîchissement d'une session (cookie {@code HttpOnly}, cf. {@code AuthCookieService}).
 *
 * <p>Seule l'empreinte SHA-256 du jeton est stockée : une fuite de la table ne permet pas de
 * rejouer une session. Tous les jetons d'une même connexion partagent un {@code familyId} et la
 * même échéance absolue de session ({@code expiresAt}) : la rotation à chaque rafraîchissement ne
 * prolonge jamais la session au-delà de la durée fixée à la connexion.
 *
 * <p>Non {@code @Audited} : donnée technique de sécurité (empreinte de secret, champ sensible au
 * sens de conventions/Audit.md) renouvelée toutes les quelques minutes par utilisateur ; la table
 * elle-même trace la vie de chaque session (création, rotation, révocation).
 */
@Entity
@Table(
        name = "refresh_token",
        indexes = {
                @Index(name = "idx_refresh_token_family", columnList = "family_id"),
                @Index(name = "idx_refresh_token_expires_at", columnList = "expires_at")
        }
)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "family_id", nullable = false, length = 36)
    private String familyId;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Échéance absolue de la session, identique pour tous les jetons de la famille. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Renseigné quand le jeton a été échangé contre un nouveau (rotation). */
    @Column(name = "rotated_at")
    private Instant rotatedAt;

    /** Renseigné à la déconnexion ou sur détection de réutilisation : le jeton n'est plus utilisable. */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected RefreshToken() {
    }

    public RefreshToken(String tokenHash, String familyId, String username, Instant createdAt, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.username = username;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public String getTokenHash() { return tokenHash; }
    public String getFamilyId() { return familyId; }
    public String getUsername() { return username; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRotatedAt() { return rotatedAt; }
    public Instant getRevokedAt() { return revokedAt; }

    public void markRotated(Instant at) { this.rotatedAt = at; }
    public void markRevoked(Instant at) { this.revokedAt = at; }
}
