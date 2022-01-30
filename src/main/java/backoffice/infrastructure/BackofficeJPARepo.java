package backoffice.infrastructure;

import shared.infrastructure.JPARepository;

import javax.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BackofficeJPARepo<E, EID> extends JPARepository<E, EID> {
    @Autowired
    protected EntityManagerFactory entityManagerFactory;

    protected EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }
}
