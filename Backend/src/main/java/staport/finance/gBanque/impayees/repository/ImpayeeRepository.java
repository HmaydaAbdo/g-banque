package staport.finance.gBanque.impayees.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.impayees.entities.Impayee;


@Repository
public interface ImpayeeRepository extends JpaRepository<Impayee, Long>, JpaSpecificationExecutor<Impayee> {

}