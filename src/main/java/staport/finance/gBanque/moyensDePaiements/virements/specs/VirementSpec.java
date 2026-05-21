package staport.finance.gBanque.moyensDePaiements.virements.specs;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
import staport.finance.gBanque.moyensDePaiements.virements.dtos.VirementSearchDto;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

public final class VirementSpec {

    /**
     * Specification to filter Virements by their numero (case-insensitive).
     * @param numero The virement number to match.
     * @return A Specification for the virement number.
     */
    public static Specification<Virement> hasNumero(String numero) {
        return (root, query, criteriaBuilder) -> {
            if (numero == null || numero.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("numero")),
                    "%" + numero.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Virement> hasDateEmission(LocalDate dateEmission) {
        return (root, query, criteriaBuilder) -> {
            if (dateEmission == null ) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("dateEmission"), dateEmission);

        };
    }



    public static Specification<Virement> hasEmetteur(String emetteur) {
        return (root, query, criteriaBuilder) -> {
            if (emetteur == null || emetteur.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("emetteur")),
                    "%" + emetteur.toLowerCase() + "%"
            );
        };
    }


    /**
     * Specification to filter Virements by their beneficiary (case-insensitive).
     * @param beneficiaire The beneficiary name to match.
     * @return A Specification for the beneficiary.
     */
    public static Specification<Virement> hasBeneficiaire(String beneficiaire) {
        return (root, query, criteriaBuilder) -> {
            if (beneficiaire == null || beneficiaire.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("beneficiaire")),
                    "%" + beneficiaire.toLowerCase() + "%"
            );
        };
    }


    public static Specification<Virement> hasBanqueBeneficiaire(String banqueBeneficiaireNom) {
        return (root, query, criteriaBuilder) -> {
            if (banqueBeneficiaireNom == null || banqueBeneficiaireNom.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            String searchPattern = "%" + banqueBeneficiaireNom.toLowerCase() + "%";

            Predicate banqueReferenceePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("banqueBeneficiare").get("nom")),
                    searchPattern
            );

            Predicate banqueLibrePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("nomBanqueBeneficiaireLibre")),
                    searchPattern
            );

            return criteriaBuilder.or(banqueReferenceePredicate, banqueLibrePredicate);
        };
    }

    /**
     * Specification to filter Virements by the name of the associated emitting bank (case-insensitive).
     * @param nomBanqueEmettrice The emitting bank name to match.
     * @return A Specification for the emitting bank name.
     */
    public static Specification<Virement> hasNomBanqueEmettrice(String nomBanqueEmettrice) {
        return (root, query, criteriaBuilder) -> {
            if (nomBanqueEmettrice == null || nomBanqueEmettrice.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("banqueEmettrice").get("nom")),
                    "%" + nomBanqueEmettrice.toLowerCase() + "%"
            );
        };
    }

    /**
     * Specification to filter Virements by the beneficiary's RIB (case-insensitive).
     * @param ribBeneficiaire The beneficiary's RIB to match.
     * @return A Specification for the RIB.
     */
    public static Specification<Virement> hasRibBeneficiaire(String ribBeneficiaire) {
        return (root, query, criteriaBuilder) -> {
            if (ribBeneficiaire == null || ribBeneficiaire.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("ribBeneficiaire")),
                    "%" + ribBeneficiaire.toLowerCase() + "%"
            );
        };
    }

    /**
     * Specification to filter Virements by their  status.
     * @return A Specification for the active status.
     */
    public static Specification<Virement> hasStatut(StatutMoyenPaiement statut) {
        return (root, query, criteriaBuilder) -> {
            if (statut == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("statut"), statut);
        };
    }

    public static Specification<Virement> notAnnule() {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.notEqual(root.get("statut"), StatutMoyenPaiement.ANNULE);
    }


    public static Specification<Virement> hasMontant(BigDecimal montant) {
        return (root, query, criteriaBuilder) -> {
            if (montant == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("montant"), montant);
        };
    }


    public static Specification<Virement> hasType(Type type) {
        return (root, query, criteriaBuilder) -> {
            if (type == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("type"), type);
        };
    }

    /**
     * Combines multiple specifications from a VirementSearchDto into a single Specification.
     * @param searchDto The DTO containing search criteria.
     * @return A combined Specification for the Virement entity.
     */
    public static Specification<Virement> bySearchDto(VirementSearchDto searchDto) {
        Specification<Virement> spec = Specification.where(null);

        if (searchDto.getNumero() != null) {
            spec = spec.and(hasNumero(searchDto.getNumero()));
        }

        if( searchDto.getDateEmission() !=null){
            spec=spec.and(hasDateEmission(searchDto.getDateEmission()));
        }

        if (searchDto.getBeneficiaire() != null) {
            spec = spec.and(hasBeneficiaire(searchDto.getBeneficiaire()));
        }

        if (searchDto.getBanqueBeneficiaire() != null) {
            spec = spec.and(hasBanqueBeneficiaire(searchDto.getBanqueBeneficiaire()));
        }

        if (searchDto.getNomBanqueEmettrice() != null) {
            spec = spec.and(hasNomBanqueEmettrice(searchDto.getNomBanqueEmettrice()));
        }

        if (searchDto.getRibBeneficiaire() != null) {
            spec = spec.and(hasRibBeneficiaire(searchDto.getRibBeneficiaire()));
        }

        if (searchDto.getStatut() != null) {
            spec = spec.and(hasStatut(searchDto.getStatut()));
        }

        if(searchDto.getType() != null) {
            spec = spec.and(hasType(searchDto.getType()));
        }

        if(searchDto.getEmetteur() != null) {
            spec = spec.and(hasEmetteur(searchDto.getEmetteur()));
        }

        if(searchDto.getMontant() != null) {
            spec = spec.and(hasMontant(searchDto.getMontant()));
        }

        return spec;
    }

    public static Specification<Virement> encoursDecaissement(Long banqueId, LocalDate until) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("statut"), StatutMoyenPaiement.ENCOURS));
            predicates.add(criteriaBuilder.equal(root.get("banqueEmettrice").get("id"), banqueId));
            predicates.add(criteriaBuilder.equal(root.get("type"), Type.DECAISSEMENT));
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateEmission"), until));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Remplace encoursEncaissement
    public static Specification<Virement> encoursEncaissement(String banqueName, LocalDate until) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("statut"), StatutMoyenPaiement.ENCOURS));
            predicates.add(criteriaBuilder.equal(root.get("banqueBeneficiare").get("nom"), banqueName));
            predicates.add(criteriaBuilder.equal(root.get("type"), Type.ENCAISSEMENT));
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateEmission"), until));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Nouveaux — pour le recap (deux méthodes car jointures différentes)
    public static Specification<Virement> outstandingBetweenDecaissement(Long banqueId, LocalDate from, LocalDate until) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("statut"), StatutMoyenPaiement.ENCOURS));
            predicates.add(criteriaBuilder.equal(root.get("banqueEmettrice").get("id"), banqueId));
            predicates.add(criteriaBuilder.equal(root.get("type"), Type.DECAISSEMENT));
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dateEmission"), from));
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateEmission"), until));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Virement> outstandingBetweenEncaissement(String banqueName, LocalDate from, LocalDate until) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("statut"), StatutMoyenPaiement.ENCOURS));
            predicates.add(criteriaBuilder.equal(root.get("banqueBeneficiare").get("nom"), banqueName));
            predicates.add(criteriaBuilder.equal(root.get("type"), Type.ENCAISSEMENT));
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dateEmission"), from));
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateEmission"), until));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}