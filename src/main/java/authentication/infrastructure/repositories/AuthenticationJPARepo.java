package authentication.infrastructure.repositories;

import shared.infrastructure.JPARepository;

import javax.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public abstract class AuthenticationJPARepo<E, EID> extends JPARepository<E, EID> {
    @Autowired
    @Qualifier("authenticationEntityManagerFactory")
    protected EntityManagerFactory entityManagerFactory;

    protected EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }
}
