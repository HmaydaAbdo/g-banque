package staport.finance.gBanque.department.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.department.dtos.DepartmentRequest;
import staport.finance.gBanque.department.dtos.DepartmentResponse;
import staport.finance.gBanque.department.entities.Department;
import staport.finance.gBanque.department.exceptions.DepartmentInUseException;
import staport.finance.gBanque.department.mappers.DepartmentMapper;
import staport.finance.gBanque.department.repositories.DepartmentRepository;

import java.util.List;

/**
 * Gestion des départements métier (Trésorerie, Crédits, Paiements...).
 *
 * <p>Les départements sont entièrement administrables — pas d'enum Java —
 * via cette couche service, exposée par {@code DepartmentController} réservé
 * au {@code SYSTEM_ADMIN}. Ne renvoie jamais l'entité {@code Department}
 * brute en réponse API — toujours {@link DepartmentResponse} via {@link DepartmentMapper}.
 *
 * <p>Aucune dépendance vers le domaine {@code user} : la garde-fou "département
 * encore utilisé" repose sur la contrainte FK en base
 * ({@code user_department_role.department_id}), jamais sur un appel cross-domaine
 * (Repository ou Service) — voir {@link #deleteDepartment(Long)}.
 */
@Service
public class DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentService.class);

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    public DepartmentService(DepartmentRepository departmentRepository,
                              DepartmentMapper departmentMapper) {
        this.departmentRepository = departmentRepository;
        this.departmentMapper = departmentMapper;
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = Department.class)
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        if (departmentRepository.existsByName(request.name())) {
            throw new DuplicateEntryException("Un département nommé '" + request.name() + "' existe déjà.");
        }
        Department saved = departmentRepository.save(departmentMapper.toEntity(request));
        log.info("Département créé id={} name={}", saved.getId(), saved.getName());
        return departmentMapper.toDto(saved);
    }

    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(departmentMapper::toDto)
                .toList();
    }

    public DepartmentResponse getDepartmentById(Long id) {
        return departmentMapper.toDto(findEntityById(id));
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Department.class)
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = findEntityById(id);

        boolean nameChanged = !department.getName().equalsIgnoreCase(request.name());
        if (nameChanged && departmentRepository.existsByName(request.name())) {
            throw new DuplicateEntryException("Un département nommé '" + request.name() + "' existe déjà.");
        }

        department.setName(request.name());
        department.setDescription(request.description());
        DepartmentResponse updated = departmentMapper.toDto(departmentRepository.save(department));
        log.info("Département mis à jour id={} name={}", id, updated.name());
        return updated;
    }

    /**
     * Supprime un département.
     *
     * @throws DepartmentInUseException si des utilisateurs y sont encore assignés —
     * détecté via la contrainte FK ({@code user_department_role.department_id}, RESTRICT
     * par défaut sur MySQL/InnoDB) plutôt qu'un appel cross-domaine vers le domaine
     * {@code user}, pour garder ce service totalement encapsulé. Le {@code flush()}
     * force l'exécution immédiate du DELETE pour intercepter la violation dans ce
     * try/catch (sinon elle ne surviendrait qu'au commit, hors de portée du catch).
     */
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = Department.class)
    public void deleteDepartment(Long id) {
        Department department = findEntityById(id);

        try {
            departmentRepository.delete(department);
            departmentRepository.flush();
        } catch (DataIntegrityViolationException e) {
            log.warn("Suppression bloquée département id={} name={} : utilisateurs encore assignés",
                    id, department.getName());
            throw new DepartmentInUseException(
                    "Impossible de supprimer le département '" + department.getName() +
                            "' : des utilisateurs y sont encore assignés. Retirez-les d'abord."
            );
        }
        log.info("Département supprimé id={} name={}", id, department.getName());
    }

    /**
     * Lookup interne — entité brute, jamais exposée via Controller/API.
     *
     * <p>Méthode publique par exception : consommée par {@code UserService} pour
     * construire une relation {@code UserDepartmentRole} (JPA exige une référence
     * d'entité managée, pas un DTO). Seul sens de dépendance autorisé entre les
     * domaines {@code department} et {@code user} — jamais l'inverse.
     */
    public Department getDepartmentEntityById(Long id) {
        return findEntityById(id);
    }

    private Department findEntityById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Département introuvable avec l'ID " + id));
    }
}
