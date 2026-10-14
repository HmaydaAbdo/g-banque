package staport.finance.gBanque.Bank.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import staport.finance.gBanque.Bank.dtos.request.BanqueRequest;
import staport.finance.gBanque.Bank.dtos.response.BankAccueilResponse;
import staport.finance.gBanque.Bank.dtos.response.BanqueResponse;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.recapitulatif.enums.PeriodeRecap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;

@Mapper(componentModel = "spring")
public interface BanqueMapper {

    /**
     * Mappe un {@link BanqueRequest} vers une entité {@link Banque}.
     * Les champs gérés par le framework (id, dates) et le logo (géré séparément
     * en binaire) sont ignorés.
     */
    @Mapping(target = "id",               ignore = true)
    @Mapping(target = "logoData",         ignore = true)
    @Mapping(target = "dateCreation",     ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    Banque toEntity(BanqueRequest request);

    /**
     * Convertit un tableau d'octets en chaîne Base64.
     * Retourne {@code null} si le tableau est vide ou null.
     */
    default String bytesToBase64(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Mappe une entité {@link Banque} vers un {@link BanqueResponse}.
     * {@code computedSolde} est passé explicitement car il n'est pas stocké
     * sur l'entité — il est calculé en temps réel depuis les transactions.
     */
    default BanqueResponse toDto(Banque banque, BigDecimal computedSolde) {
        return new BanqueResponse(
                banque.getId(),
                bytesToBase64(banque.getLogoData()),
                banque.getNom(),
                banque.getNumeroDeCompte(),
                banque.getDevise(),
                banque.getAdresse(),
                banque.getAgence(),
                computedSolde,
                banque.getDateCreation(),
                banque.getDateModification()
        );
    }

    /**
     * Mappe une entité {@link Banque} vers un {@link BankAccueilResponse} pour
     * le tableau de bord Finance. Tous les paramètres agrégés (solde, plafond FC,
     * compteurs échus) proviennent de services distincts et sont passés
     * explicitement.
     *
     * <p>{@code disponibiliteFC} (solde actuel + plafond FC) est calculée ici, pas transmise en
     * paramètre : ses deux entrées ({@code computedBalance}, {@code plafondFC}) sont déjà
     * disponibles à cet appel, pas besoin d'un service supplémentaire. Null-safe : un plafond FC
     * absent (aucune ligne FC active pour la banque) compte pour 0 dans le calcul, sans changer
     * la valeur (potentiellement null) exposée dans le champ {@code plafondFC} lui-même.
     *
     * <p>{@code encoursPlus}/{@code encoursMoins} sont les encours jusqu'à {@code dateHorizon},
     * l'horizon {@code periode} choisi sur l'accueil (fin de mois ou fin de trimestre).</p>
     */
    default BankAccueilResponse toAccueilDto(Banque banque,
                                             BigDecimal computedBalance,
                                             BigDecimal plafondFC,
                                             BigDecimal soldeDebut,
                                             long lcnsEchusCount,
                                             long virementsEchusCount,
                                             PeriodeRecap periode,
                                             LocalDate dateHorizon,
                                             BigDecimal encoursPlus,
                                             BigDecimal encoursMoins) {
        BigDecimal soldePourCalcul   = computedBalance != null ? computedBalance : BigDecimal.ZERO;
        BigDecimal plafondPourCalcul = plafondFC != null ? plafondFC : BigDecimal.ZERO;
        BigDecimal plus  = encoursPlus  != null ? encoursPlus  : BigDecimal.ZERO;
        BigDecimal moins = encoursMoins != null ? encoursMoins : BigDecimal.ZERO;

        // soldePrevu = solde + encours+ - encours- ; disponibilité prévue = soldePrevu + plafond FC
        BigDecimal soldePrevu = soldePourCalcul.add(plus).subtract(moins);

        return new BankAccueilResponse(
                banque.getId(),
                bytesToBase64(banque.getLogoData()),
                banque.getNom(),
                computedBalance,
                soldeDebut,
                plafondFC,
                soldePourCalcul.add(plafondPourCalcul),
                banque.getDevise(),
                lcnsEchusCount,
                virementsEchusCount,
                periode,
                dateHorizon,
                plus,
                moins,
                soldePrevu,
                soldePrevu.add(plafondPourCalcul)
        );
    }
}
