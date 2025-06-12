
package staport.finance.gBanque.Bank.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import staport.finance.gBanque.Bank.dtos.BanqueRequest;
import staport.finance.gBanque.Bank.dtos.BanqueResponse;
import staport.finance.gBanque.Bank.dtos.BankAcceuilResponse;
import staport.finance.gBanque.Bank.entities.Banque;
import java.util.Base64;

@Mapper(componentModel = "spring")
public interface BanqueMapper {

    @Named("bytesToBase64")
    default String bytesToBase64(byte[] bytes) {
        return (bytes != null && bytes.length > 0) ? Base64.getEncoder().encodeToString(bytes) : null;
    }

    // For creation (BanqueRequest -> Banque)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    @Mapping(target = "soldeActuel", ignore = true)
    @Mapping(target = "logoData", ignore = true)
    Banque toEntity(BanqueRequest banqueRequest);

    // For reading (Banque -> BanqueResponse)
    @Mapping(source = "logoData", target = "logoBase64", qualifiedByName = "bytesToBase64")
    BanqueResponse toDto(Banque banque);

    java.util.List<BanqueResponse> toDtoList(java.util.List<Banque> banques);

    // For updating (BanqueRequest -> Banque)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    @Mapping(target = "numeroDeCompte", ignore = true)
    @Mapping(target = "soldeActuel", ignore = true)
    @Mapping(target = "logoData", ignore = true)
    void updateBanqueFromDto(BanqueRequest banqueRequest, @MappingTarget Banque banque);

    // --- New mapping for BankAcceuilResponse ---
    @Mapping(source = "id", target = "bankId") // Maps Banque.id to BankAcceuilResponse.bankId
    @Mapping(source = "logoData", target = "logoBase64", qualifiedByName = "bytesToBase64")
    @Mapping(source = "nom", target = "name")
    @Mapping(source = "soldeActuel", target = "soldeActuelle") // MapStruct handles this
    @Mapping(target = "plafondFC", ignore = true) // Explicitly ignore, as it's derived elsewhere
    BankAcceuilResponse toAcceuilDto(Banque banque);

    java.util.List<BankAcceuilResponse> toAcceuilDtoList(java.util.List<Banque> banques);


}