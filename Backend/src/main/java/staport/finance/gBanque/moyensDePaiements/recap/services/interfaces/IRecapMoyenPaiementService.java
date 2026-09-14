package staport.finance.gBanque.moyensDePaiements.recap.services.interfaces;

import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.moyensDePaiements.recap.dtos.request.RecapMoyenPaiementSearchDto;
import staport.finance.gBanque.moyensDePaiements.recap.dtos.response.AgregatedRecapMoyenPaiementDto;

/**
 * Façade publique du Recap Encaissement / Recap Décaissement des moyens de paiement — vue
 * consolidée, tous statuts confondus, des 5 moyens de paiement (Chèques, LCN, Virements
 * nationaux, Virements internationaux, Mises à disposition), toutes banques confondues avec
 * filtre banque.
 */
public interface IRecapMoyenPaiementService {

    AgregatedRecapMoyenPaiementDto getEncaissements(Pageable pageable, RecapMoyenPaiementSearchDto searchDto);

    AgregatedRecapMoyenPaiementDto getDecaissements(Pageable pageable, RecapMoyenPaiementSearchDto searchDto);

    /**
     * Exporte en PDF le recap encaissement (mêmes critères que {@link #getEncaissements}, sans
     * pagination).
     */
    byte[] exportEncaissementsPdf(RecapMoyenPaiementSearchDto searchDto);

    /**
     * Exporte en PDF le recap décaissement (mêmes critères que {@link #getDecaissements}, sans
     * pagination).
     */
    byte[] exportDecaissementsPdf(RecapMoyenPaiementSearchDto searchDto);
}
