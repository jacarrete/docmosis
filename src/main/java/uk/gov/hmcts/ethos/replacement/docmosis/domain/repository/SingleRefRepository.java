package uk.gov.hmcts.ethos.replacement.docmosis.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import uk.gov.hmcts.ethos.replacement.docmosis.domain.SingleReference;

@NoRepositoryBean
public interface SingleRefRepository<T extends SingleReference> extends JpaRepository<T, Integer> {
    T findTopByOrderByIdDesc();
}