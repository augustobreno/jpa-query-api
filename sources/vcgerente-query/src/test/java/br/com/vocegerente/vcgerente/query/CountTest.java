package br.com.vocegerente.vcgerente.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.persistence.Query;

import org.junit.Test;

import br.com.vocegerente.vcgerente.query.api.QueryFilter;
import br.com.vocegerente.vcgerente.query.domain.Pessoa;
import br.com.vocegerente.vcgerente.query.domain.QPessoa;
import br.com.vocegerente.vcgerente.query.domain.QUf;
import br.com.vocegerente.vcgerente.query.domain.Uf;
import br.com.vocegerente.vcgerente.query.extension.Example;
import br.com.vocegerente.vcgerente.query.repository.QbeRepository;

/**
 * Valida o comportamento da operações count em diversos cenários.
 * @author augusto
 *
 */
public class CountTest extends QbeTestBase {

	/**
	 * Testa a forma mais simples do count, contando o número de registros de um determinado tipo.
	 */
	@Test
	public void simpleCount() {
		
		// executa a consulta com HQL correto equivalente
		String hql = "select count(f.id) from " + Uf.class.getSimpleName() + " f";
		long countHQL = getQuerier().executeCountQuery(getEntityManager(), hql);
		
		// deve haver pelo menos uma Uf encontrada
		if (countHQL <= 0) {
			fail("Nao ha Ufs cadastradas, nao e possivel proceder com o teste");
		}
		
		// executa a consulta utilizando QBE
		QbeRepository qbe = createQbe();
		long countQbe = qbe.count(new QueryFilter<Uf>(QUf.uf));
		
		// compara os resultados
		assertEquals("O numero de registros encontrados deveria ser igual.", countHQL, countQbe);
		
	}
	
	/**
	 * Garante o comportamento do count mesmo quando há uma configuração de ordenação (inadequada para operações do tipo count).
	 * O objetivo é permitir ao máximo usar o mesmo QueryFilter em operações do tipo count e findBy 
	 */
	@Test
	public void countAndOrderBy() {
		
		// executa a consulta com HQL correto equivalente
		String hql = "select count(f.id) from " + Uf.class.getSimpleName() + " f"; // hql n�o aceitar count + order by
		Long UfsHQL = getQuerier().executeCountQuery(getEntityManager(), hql);
		
		// deve haver pelo menos uma Uf encontrada
		if (UfsHQL <= 0) {
			fail("Nao ha Ufs cadastradas, nao e possivel proceder com o teste");
		}
		
		// executa a consulta utilizando QBE
		QueryFilter<Uf> filter = new QueryFilter<Uf>(QUf.uf);
		filter.orderBy(QUf.uf.sigla.asc());
		
		Long UfsQbe = createQbe().count(filter);
		
		// compara os resultados
		assertEquals("O numero de registros encontrados deveria ser igual.", UfsHQL, UfsQbe);
		
	}	
	
	/**
	 * Garante o comportamento do count() quando a consulta é paginada, esta deverá ser desconsiderada. 
	 */
	@Test
	public void countAndDbPaging() {
		
		// executa a consulta com HQL correto equivalente
		String hql = "select count(f.id) from " + Uf.class.getSimpleName() + " f";
		Query query = getQuerier().createQuery(getEntityManager(), hql); // hql nao permite paginacao com count
		Long UfsHQL = (Long) query.getSingleResult();
		
		// deve haver pelo menos uma Uf encontrada
		if (UfsHQL <= 0) {
			fail("Nao ha Ufs cadastradas, nao e possivel proceder com o teste");
		}
		
		// executa a consulta utilizando QBE
		QueryFilter<Uf> filter = new QueryFilter<Uf>(QUf.uf);
		filter.limit(10);
		filter.offset(5);
		Long UfsQbe = createQbe().count(filter);		
		
		// compara os resultados
		assertEquals("O numero de registros encontrados deveria ser igual.", UfsHQL, UfsQbe);
		
	}		
	
	/**
	 * Valida o count em uma query com restrições. 
	 */
	@Test
	public void countAndRestrictions() throws Exception {
		// Cria algumas pessoas pra consulta
		Pessoa p1 = new Pessoa("p1", null, formatDate("01/01/2010"), "11111111111", "email@email.com");
		Pessoa p2 = new Pessoa("p2", null, formatDate("01/01/2012"), "22222222222", "email@email.com");
		Pessoa p3 = new Pessoa("p3", null,                     null, "22222222222", "email@email.com");
		getJpa().save(p1);
		getJpa().save(p2);
		getJpa().save(p3);
		getJpa().getEm().flush();
		getJpa().getEm().clear();
		
		// utiliza uma consulta hql para compara��o
		String hql = "select count(p.id) from Pessoa p where p.email=? and ( p.cpf=? or (p.dataNascimento is null  or p.dataNascimento > ?) ) ";
		Long pessoasHQL = getQuerier().executeCountQuery(getEntityManager(), hql, p2.getEmail(), p1.getCpf(), p1.getDataNascimento());
		
		// Configura o filtro
		Pessoa exemplo = new Pessoa(null, null, p1.getDataNascimento(), p1.getCpf(), p2.getEmail());
		
		QueryFilter<Pessoa> filter = new QueryFilter<Pessoa>(QPessoa.pessoa);
		filter.setExample(exemplo);
		
		filter.where(QPessoa.pessoa.email.eq(Example.read()));
		filter.where(QPessoa.pessoa.cpf.eq(Example.read())
						.or(QPessoa.pessoa.dataNascimento.isNull())
						.or(QPessoa.pessoa.dataNascimento.gt(Example.read())));
		
		Long pessoasQbe = createQbe().count(filter);	
		
		assertEquals("O numero de registros encontrados deveria ser igual.", pessoasHQL, pessoasQbe);
	}

}
