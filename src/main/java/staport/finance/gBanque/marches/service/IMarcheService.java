package staport.finance.gBanque.marches.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.marches.dtos.request.MarcheRequestDTO;
import staport.finance.gBanque.marches.dtos.request.MarcheSearchDTO;
import staport.finance.gBanque.marches.dtos.response.MarcheResponseDTO;
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

    /**
     * Crée un marché. La {@link Banque} est résolue par l'appelant (le Controller, via
     * {@code IBanqueService}) et passée déjà chargée — {@code MarcheService} n'injecte
     * volontairement aucune dépendance vers le module Bank pour éviter un cycle de bean Spring
     * (voir javadoc de {@link MarcheService}).
     */
    MarcheResponseDTO createMarche(MarcheRequestDTO requestDTO, Banque banque);

    MarcheResponseDTO getMarcheById(Long id);

    /**
     * Met à jour un marché. {@code banque} est {@code null} si le marché ne change pas de
     * banque (champ optionnel à la mise à jour) — voir {@link #createMarche} pour la raison de
     * cette résolution en amont.
     */
    MarcheResponseDTO updateMarche(Long id, MarcheRequestDTO requestDTO, Banque banque);

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
