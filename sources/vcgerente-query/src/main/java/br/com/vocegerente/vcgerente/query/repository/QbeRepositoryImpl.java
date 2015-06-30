package br.com.vocegerente.vcgerente.query.repository;

import java.util.List;

import javax.persistence.EntityManager;

import br.com.vocegerente.vcgerente.query.api.QueryFilter;

/**
 * Motor QBE que construi e executar uma consulta QueryDsl a partir de um
 * {@link QueryFilter}
 * 
 * @author augusto
 *
 */
public class QbeRepositoryImpl implements QbeRepository {

	/**
	 * Para acesso à sessão do JPA.
	 */
	private EntityManager entityManager;

	public QbeRepositoryImpl() {
		// default
	}

	public QbeRepositoryImpl(EntityManager entityManager) {
		super();
		this.entityManager = entityManager;
	}

	/**
	 * @see br.com.vocegerente.vcgerente.query.repository.QbeRepository#findAllBy(br.com.vocegerente.vcgerente.query.api.QueryFilter)
	 */
	@Override
	public <ENTITY> List<ENTITY> findAllBy(QueryFilter<? extends ENTITY> filter) {
		return createContextProcessor(filter).findAllBy();
	}

	@Override
	public <ENTITY> long count(QueryFilter<? extends ENTITY> filter) {
		return createContextProcessor(filter).count();
	}
	
	protected <ENTITY> QbeContextProcessor<ENTITY> createContextProcessor(QueryFilter<? extends ENTITY> filter) {
		return new QbeContextProcessor<ENTITY>(entityManager, filter);
	}

	public EntityManager getEntityManager() {
		return entityManager;
	}

	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

}
