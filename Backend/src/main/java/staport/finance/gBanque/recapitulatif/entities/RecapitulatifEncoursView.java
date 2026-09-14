package staport.finance.gBanque.recapitulatif.entities;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entité JPA en lecture seule mappée sur la vue SQL {@code v_recapitulatif_encours}
 * (voir {@code v_recapitulatif_encours.sql} à la racine du projet, créée manuellement par
 * HMAYDA le 17/07/2026 — Phase 2 du sweep États des encours / Récapitulatif).
 *
 * <p>Remplace l'agrégation en mémoire de ~19 repositories sources (Leasing, CMT, Cheque, Lcn,
 * Virement, VirementInternational, MiseADisposition, Prefinancement, Factoring, DroitConstate,
 * Decompte, Restitution, Commission, Agios, Prelevement, FraisBancaire, ImpotEtTaxe, Impayee,
 * OperationErrone, Provision) par une seule requête paginée/triée au niveau base — la vue fait déjà le
 * filtre "encours" (statut PREVU/ENCOURS) et le UNION ALL.</p>
 *
 * <p>{@code @Immutable} : Hibernate ne doit jamais tenter d'écrire sur cette entité (c'est une
 * vue). {@code @Subselect} mappe l'entité sur un select libre (la vue) sans générer de DDL de
 * table au démarrage. {@code @Synchronize} liste les tables sources pour que Hibernate flush les
 * écritures en attente sur ces tables avant toute lecture de cette entité dans la même
 * transaction (cohérence si jamais un flux lit/écrit dans la même transaction).</p>
 */
@Entity
@Immutable
@Subselect("SELECT * FROM v_recapitulatif_encours")
@Synchronize({
        "echeance_leasing", "contrat_leasing", "contrat_cadre", "organisme_leasing",
        "echeance_cmt", "credit_moyen_terme",
        "cheques", "lcns", "virements", "virements_internationaux", "mises_a_disposition",
        "prefinancement", "factoring", "droit_constate", "marches",
        "decompte", "restitution", "commission", "agios", "prelevement",
        "frais_bancaire", "impot_et_taxe", "impayee", "operation_errone", "provision"
})
public class RecapitulatifEncoursView {

    @EmbeddedId
    private RecapitulatifEncoursId id;

    private Long banqueId;

    @Enumerated(EnumType.STRING)
    private Type sens;

    private LocalDate dateReference;

    private LocalDate dateEmission;

    private LocalDate dateEcheanceAffichage;

    private BigDecimal montant;

    private String libelle;

    private String observation;

    /**
     * Numero de piece porte nativement par la source (cheque, LCN, virement, mise a
     * disposition, impaye, numero de marche, numero de contrat leasing). {@code null}
     * pour les sources qui n'en ont pas (CMT, virement international, restitution,
     * commission, agios, prelevement, frais bancaire, impot et taxe, operation erronee).
     */
    private String numero;

    protected RecapitulatifEncoursView() {
        // Requis par JPA
    }

    public RecapitulatifEncoursId getId() {
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

    public Type getSens() {
        return sens;
    }

    public LocalDate getDateReference() {
        return dateReference;
    }

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public LocalDate getDateEcheanceAffichage() {
        return dateEcheanceAffichage;
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

    public String getNumero() {
        return numero;
    }
}
