package br.com.vocegerente.vcgerente.query.util;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;

import br.com.vocegerente.vcgerente.tests.jpa.AlternativeLocalEntityManagerProducerBase;
import br.com.vocegerente.vcgerente.tests.jpa.JPAStandalone;

/**
 * Producer alternativo para injeção de dependência de um EntityManager em ambiente de testes.
 * @author Augusto
 */
@Alternative

/*
 * Ao configurar a Proriedade abaixo, este Alternative passa a ser visível fora de módulo do CDI (test), podendo
 * ser injetado em instâncias do módulo main.
 */
@Priority(1)
public class AlternativeLocalEntityManagerProducer extends AlternativeLocalEntityManagerProducerBase {

	@Override
	@Produces
	public EntityManager produceEntityManager(JPAStandalone jpaStandalone) {
		return createEntityManager(jpaStandalone);
	}
	
}
