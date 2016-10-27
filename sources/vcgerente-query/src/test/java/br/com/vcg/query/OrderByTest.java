package br.com.vcg.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import br.com.vcg.query.api.QueryFilter;
import br.com.vcg.query.domain.Cidade;
import br.com.vcg.query.domain.QCidade;
import br.com.vcg.query.domain.QUf;
import br.com.vcg.query.domain.Uf;
import br.com.vcg.query.repository.QbeRepositoryImpl;

/**
 * Testa o funcionamento das opções de ordenação disponibilizadas pelo QBE.
 * O principal objetivo é gerantir que a integração QBE + QueryDsl está funcionando corretamente.
 * @author augusto
 */
public class OrderByTest extends QbeTestBase {

	/**
	 * Consulta uma entidade com ordenação ascendente simples
	 */
	@Test
	public void simpleAscOrdering() {
		
		// executa a consulta com HQL correto equivalente
		String hql = "from " + Uf.class.getSimpleName() + " order by sigla asc";
		@SuppressWarnings("unchecked")
		List<Uf> ufsHQL = getQuerier().executeQuery(hql);
		
		// deve haver pelo menos uma Uf cadastrada
		if (ufsHQL == null || ufsHQL.isEmpty()) {
			fail("Não há Ufs cadastradas, não é possível proceder com o teste");
		}		
		
		// executa a consulta utilizando QBE
		QbeRepositoryImpl qbe = new QbeRepositoryImpl(getEntityManager());

		QueryFilter<Uf> filter = new QueryFilter<Uf>(QUf.uf);
		filter.orderBy(QUf.uf.sigla.asc());
		List<Uf> ufsQbe = qbe.findAllBy(filter);
		
		// compara as listas
		assertTrue("O resultado da consulta QBE nao confere com a consulta HQL equivalente", ufsHQL.containsAll(ufsQbe));
		assertTrue("O resultado da consulta QBE nao confere com a consulta HQL equivalente", ufsQbe.containsAll(ufsHQL));
		
		for (int i = 0; i < ufsHQL.size(); i++) {
			assertEquals("As Ufs deveriam ser iguais em todas as posicoes. Posicao de falha: " + i, ufsHQL.get(i), ufsQbe.get(i));
		}
		
	}
	
	/**
	 * Consulta uma entidade com ordenação descendente simples
	 */	
	@Test
	public void simpleDescOrdering() {
		
		// executa a consulta com HQL correto equivalente
		String hql = "from " + Uf.class.getSimpleName() + " order by sigla desc";
		@SuppressWarnings("unchecked")
		List<Uf> ufsHQL = getQuerier().executeQuery(hql);
		
		// deve haver pelo menos uma Uf cadastrada
		if (ufsHQL == null || ufsHQL.isEmpty()) {
			fail("Não há Ufs cadastradas, não é possível proceder com o teste");
		}		
		
		// executa a consulta utilizando QBE
		QbeRepositoryImpl qbe = new QbeRepositoryImpl(getEntityManager());

		QueryFilter<Uf> filter = new QueryFilter<Uf>(QUf.uf);
		filter.orderBy(QUf.uf.sigla.desc());
		List<Uf> ufsQbe = qbe.findAllBy(filter);
		
		// compara as listas
		assertTrue("O resultado da consulta QBE nao confere com a consulta HQL equivalente", ufsHQL.containsAll(ufsQbe));
		assertTrue("O resultado da consulta QBE nao confere com a consulta HQL equivalente", ufsQbe.containsAll(ufsHQL));
		
		for (int i = 0; i < ufsHQL.size(); i++) {
			assertEquals("As Ufs deveriam ser iguais em todas as posicoes. Posicao de falha: " + i, ufsHQL.get(i), ufsQbe.get(i));
		}
		
	}
	
	/**
	 * Realiza a ordenação em uma propriedade aninhada.
	 */
	@Test
	public void nestedPropOrdering() {
		
		// executa a consulta com HQL correto equivalente
		String hql = "from " + Cidade.class.getSimpleName() + " order by uf.sigla asc, nome asc";
		@SuppressWarnings("unchecked")
		List<Cidade> cidadesHQL = getQuerier().executeQuery(hql);
		
		// deve haver pelo menos uma cidade cadastrada
		if (cidadesHQL == null || cidadesHQL.isEmpty()) {
			fail("Não há cidades cadastradas, não é possível proceder com o teste");
		}		
		
		// executa a consulta utilizando QBE
		QbeRepositoryImpl qbe = new QbeRepositoryImpl(getEntityManager());

		QueryFilter<Cidade> filter = new QueryFilter<Cidade>(QCidade.cidade);
		filter.orderBy(QCidade.cidade.uf().sigla.asc());
		filter.orderBy(QCidade.cidade.nome.asc());
		List<Cidade> cidadesQbe = qbe.findAllBy(filter);
		
		// compara as listas
		assertTrue("O resultado da consulta QBE não confere com a consulta HQL equivalente", cidadesHQL.containsAll(cidadesQbe));
		assertTrue("O resultado da consulta HQL não confere com a consulta QBE equivalente", cidadesQbe.containsAll(cidadesHQL));
		
		for (int i = 0; i < cidadesHQL.size(); i++) {
			assertEquals("As cidades deveriam ser iguais em todas as posições. Posição de falha: " + i, cidadesHQL.get(i), cidadesQbe.get(i));
		}
		
	}	
		
}
