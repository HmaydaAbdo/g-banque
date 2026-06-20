package staport.finance.gBanque.security.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.security.dtos.DepartmentRequest;
import staport.finance.gBanque.security.dtos.DepartmentResponse;
import staport.finance.gBanque.security.entities.Department;
import staport.finance.gBanque.security.exceptions.DepartmentInUseException;
import staport.finance.gBanque.security.mappers.DepartmentMapper;
import staport.finance.gBanque.security.repositories.DepartmentRepository;
import staport.finance.gBanque.security.repositories.UserDepartmentRoleRepository;

import java.util.List;

/**
 * Gestion des départements métier (Trésorerie, Crédits, Paiements...).
 *
 * <p>Les départements sont entièrement administrables — pas d'enum Java —
 * via cette couche service, exposée par {@code DepartmentController} réservé
 * au {@code SYSTEM_ADMIN}. Ne renvoie jamais l'entité {@code Department}
 * brute — toujours {@link DepartmentResponse} via {@link DepartmentMapper}.
 */
@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserDepartmentRoleRepository userDepartmentRoleRepository;
    private final DepartmentMapper departmentMapper;

    public DepartmentService(DepartmentRepository departmentRepository,
                              UserDepartmentRoleRepository userDepartmentRoleRepository,
                              DepartmentMapper departmentMapper) {
        this.departmentRepository = departmentRepository;
        this.userDepartmentRoleRepository = userDepartmentRoleRepository;
        this.departmentMapper = departmentMapper;
    }

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        if (departmentRepository.existsByName(request.name())) {
            throw new DuplicateEntryException("Un département nommé '" + request.name() + "' existe déjà.");
        }
        Department saved = departmentRepository.save(departmentMapper.toEntity(request));
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
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = findEntityById(id);

        boolean nameChanged = !department.getName().equalsIgnoreCase(request.name());
        if (nameChanged && departmentRepository.existsByName(request.name())) {
            throw new DuplicateEntryException("Un département nommé '" + request.name() + "' existe déjà.");
        }

        department.setName(request.name());
        department.setDescription(request.description());
        return departmentMapper.toDto(departmentRepository.save(department));
    }

    /**
     * Supprime un département.
     *
     * @throws DepartmentInUseException si des utilisateurs y sont encore assignés —
     * l'administrateur doit d'abord les retirer ou les réassigner ailleurs.
     */
    @Transactional
    public void deleteDepartment(Long id) {
        Department department = findEntityById(id);

        if (userDepartmentRoleRepository.existsByDepartmentId(id)) {
            throw new DepartmentInUseException(
                    "Impossible de supprimer le département '" + department.getName() +
                            "' : des utilisateurs y sont encore assignés. Retirez-les d'abord."
            );
        }

        departmentRepository.delete(department);
    }

    /**
     * Lookup interne — entité brute, jamais exposée hors de ce service.
     */
    private Department findEntityById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Département introuvable avec l'ID " + id));
    }
}
