package com.remessa.infrastructure.persistence.repository;

import com.remessa.infrastructure.persistence.entity.TransferEntity;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Repositório técnico de remessas gerado pelo Micronaut Data; usado apenas pelo adapter. */
@JdbcRepository(dialect = Dialect.POSTGRES)
public interface TransferJdbcRepository extends CrudRepository<TransferEntity, UUID> {

    /**
     * Soma o {@code amount_brl} de todas as remessas enviadas por {@code senderId}
     * cujo {@code executed_at} caia no dia {@code date}.
     *
     * <p>CAST(executed_at AS DATE) é suportado tanto pelo H2 (em MODE=PostgreSQL)
     * quanto pelo PostgreSQL real, mantendo a portabilidade.
     *
     * <p>COALESCE garante retorno de 0 quando não há remessas no dia,
     * evitando null no chamador.
     */
    @Query("SELECT COALESCE(SUM(t.amount_brl), 0) FROM transfers t " +
           "WHERE t.sender_id = :senderId " +
           "AND CAST(t.executed_at AS DATE) = :date")
    BigDecimal sumAmountBrlBySenderAndDate(UUID senderId, LocalDate date);
}
