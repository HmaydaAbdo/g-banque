package staport.finance.gBanque.marches.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.marches.dtos.MarcheRequestDTO;
import staport.finance.gBanque.marches.dtos.MarcheResponseDTO;
import staport.finance.gBanque.marches.dtos.MarcheSearchDTO;
import staport.finance.gBanque.marches.entities.Marche;

import java.util.List;

/**
 * Contrat de service pour le module Marches.
 *
 * <p>Le contrôleur, et tout autre module consommateur, injectent uniquement cette interface
 * (DIP — Dependency Inversion Principle). La classe concrète {@link MarcheService} n'est jamais
 * référencée depuis l'extérieur du module — voir {@code architecture.md}, "Encapsulation des
 * domaines" : un Repository (ici {@code MarcheRepository}) appartient exclusivement à son
 * domaine, la communication inter-domaines passe par ce Service.
 */
public interface IMarcheService {

    MarcheResponseDTO createMarche(MarcheRequestDTO requestDTO);

    MarcheResponseDTO getMarcheById(Long id);

    MarcheResponseDTO updateMarche(Long id, MarcheRequestDTO requestDTO);

    void deleteMarche(Long id);

    MarcheResponseDTO archiverMarche(Long id);

    MarcheResponseDTO desarchiverMarche(Long id);

    Page<MarcheResponseDTO> findAllMarches(MarcheSearchDTO searchDTO, Pageable pageable);

    /**
     * Résout un Marche par ID pour les domaines qui ont besoin de l'entité JPA elle-même
     * (ex. {@code cautions}) — jamais via {@code MarcheRepository} directement (encapsulation
     * des domaines, architecture.md).
     */
    Marche findOrThrow(Long id);

    /**
     * Vérifie l'existence d'un Marche par ID, pour les domaines qui n'ont besoin que d'une
     * validation (pas de l'entité elle-même).
     */
    boolean existsById(Long id);

    /**
     * Retourne les marchés <b>actifs</b> rattachés à une banque, pour les domaines qui ont
     * besoin de parcourir les marchés de la banque (ex. calculateurs d'agios) — jamais via
     * {@code MarcheRepository} directement (encapsulation des domaines, architecture.md).
     */
    List<Marche> findByBanqueId(Long banqueId);
}
