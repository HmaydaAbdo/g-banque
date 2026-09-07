package staport.finance.gBanque.moyensDePaiements.recap.entities;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entité JPA en lecture seule mappée sur la vue SQL {@code v_recap_moyens_paiement}
 * (voir {@code sql/v_recap_moyens_paiement.sql} — créée manuellement par HMAYDA le
 * 31/07/2026 pour le module "Recap Encaissement / Recap Décaissement" sous "Les moyens de
 * paiement", mais jamais versionnée dans le dépôt jusqu'au 07/09/2026, malgré cette javadoc
 * qui l'affirmait déjà à tort auparavant — voir l'historique en tête du fichier .sql pour le
 * détail).
 *
 * <p>Consolide les 5 moyens de paiement de la section "Les moyens de paiement" — Cheque, Lcn,
 * Virement, VirementInternational, MiseADisposition — tous statuts confondus (contrairement à
 * {@code RecapitulatifEncoursView}, qui se limite aux encours du mois). Le même pattern
 * {@code @Subselect} est repris pour bénéficier de la pagination/tri/agrégation réels en base.</p>
 *
 * <p>{@code @Immutable} : Hibernate ne doit jamais tenter d'écrire sur cette entité (c'est une
 * vue). {@code @Subselect} mappe l'entité sur un select libre (la vue) sans générer de DDL de
 * table au démarrage. {@code @Synchronize} liste les tables sources pour que Hibernate flush les
 * écritures en attente sur ces tables avant toute lecture de cette entité dans la même
 * transaction.</p>
 */
@Entity
@Immutable
@Subselect("SELECT * FROM v_recap_moyens_paiement")
@Synchronize({"cheques", "lcns", "virements", "mises_a_disposition", "virements_internationaux"})
public class RecapMoyenPaiementView {

    @EmbeddedId
    private RecapMoyenPaiementId id;

    private Long banqueId;

    private String banqueNom;

    @Enumerated(EnumType.STRING)
    private Type type;

    @Enumerated(EnumType.STRING)
    private StatutMoyenPaiement statut;

    private String numero;

    private LocalDate dateEmission;

    private LocalDate dateOperation;

    private LocalDate dateValeur;

    private LocalDate dateEcheance;

    private BigDecimal montant;

    private String libelle;

    private String observation;

    private String beneficiaire;

    private String emetteur;

    protected RecapMoyenPaiementView() {
        // Requis par JPA
    }

    public RecapMoyenPaiementId getId() {
        return id;
    }

    public TransactionSource getSourceType() {
        return id.getSourceType();
    }

    public Long getEntityId() {
        return id.getEntityId();
    }

    public Long getBanqueId() {
        return banqueId;
    }

    public String getBanqueNom() {
        return banqueNom;
    }

    public Type getType() {
        return type;
    }

    public StatutMoyenPaiement getStatut() {
        return statut;
    }

    public String getNumero() {
        return numero;
    }

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public LocalDate getDateOperation() {
        return dateOperation;
    }

    public LocalDate getDateValeur() {
        return dateValeur;
    }

    public LocalDate getDateEcheance() {
        return dateEcheance;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getObservation() {
        return observation;
    }

    public String getBeneficiaire() {
        return beneficiaire;
    }

    public String getEmetteur() {
        return emetteur;
    }
}
