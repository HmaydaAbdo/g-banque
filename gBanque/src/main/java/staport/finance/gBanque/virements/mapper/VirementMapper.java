package staport.finance.gBanque.virements.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.virements.dtos.VirementRequestDto;
import staport.finance.gBanque.virements.dtos.VirementResponseDto;
import staport.finance.gBanque.virements.entities.Virement;

@Component
public class VirementMapper {

    private final BanqueRepository banqueRepository;

    public VirementMapper(BanqueRepository banqueRepository) {
        this.banqueRepository = banqueRepository;
    }

    /**
     * Converts a Virement entity to a VirementResponseDto.
     *
     * @param virement The Virement entity to convert.
     * @return The corresponding VirementResponseDto.
     */
    public  VirementResponseDto toDto(Virement virement) {
        if (virement == null) {
            return null;
        }

        VirementResponseDto response = new VirementResponseDto();
        response.setId(virement.getId());
        response.setNumero(virement.getNumero()!=null ? virement.getNumero() :  null);
        response.setBeneficiaire(virement.getBeneficiaire());
        response.setDateEmission(virement.getDateEmission());
        response.setMontant(virement.getMontant());
        response.setBanqueEmettriceNom(virement.getBanqueEmettrice() != null ? virement.getBanqueEmettrice().getNom() : null);
        response.setBanqueBeneficiaire(virement.getBanqueBeneficiare() !=null ? virement.getBanqueBeneficiare().getNom() : virement.getNomBanqueBeneficiaireLibre());
        response.setRibBeneficiaire(virement.getRibBeneficiaire()!=null ? virement.getRibBeneficiaire() : null);
        response.setActive(virement.isActive());
        response.setDateOperation(virement.getDateOperation()!=null ? virement.getDateOperation() : null);
        response.setDateValeur(virement.getDateValeur()!=null ? virement.getDateValeur() : null);
        response.setType(virement.getType());
        response.setObservation("Virement N° " + virement.getNumero());
        response.setLibelle("Bénéficiaire " + virement.getBeneficiaire() );
        response.setEmetteur(virement.getEmetteur()!=null ? virement.getEmetteur() : null);
        response.setBanqueBeneficiaireId(virement.getBanqueBeneficiare()!=null ? virement.getBanqueBeneficiare().getId() : null);
        response.setBanqueEmettriceId(virement.getBanqueEmettrice()!=null ? virement.getBanqueEmettrice().getId() : null);
        return response;
    }

    public  Virement toEntity(VirementRequestDto   dto) {

        if(dto==null){
            return null;
        }

        Virement virement = new Virement();

        Banque banque=this.banqueRepository.findById(dto.getBanqueBeneficiaireId()).orElseThrow(
                ()->new ResourceNotFoundException("Banque with id: "+dto.getBanqueBeneficiaireId()+" not found")
        );
        virement.setBanqueBeneficiare(banque);
        virement.setDateEmission(dto.getDateReception());
        virement.setBeneficiaire(dto.getBeneficiaire());
        virement.setMontant(dto.getMontant());
        virement.setEmetteur(dto.getEmetteur());
        virement.setType(dto.getType());

        return virement;
    }
}