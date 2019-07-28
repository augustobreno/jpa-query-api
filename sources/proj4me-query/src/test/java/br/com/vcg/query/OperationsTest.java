package br.com.vcg.query;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import br.com.vcg.query.api.QueryFilter;
import br.com.vcg.query.domain.Cidade;
import br.com.vcg.query.domain.QCidade;
import br.com.vcg.query.domain.QUf;
import br.com.vcg.query.domain.Uf;
import br.com.vcg.query.extension.Example;
import br.com.vcg.query.repository.QbeRepositoryImpl;


/**
 * Classe de teste que avalia as op��es de operadores das consultas QBE 
 * @author augusto
 */
public class OperationsTest extends QbeTestBase {

	/**
	 * Testa uma verredura nos dados de uma entidade.
	 */
	@Test
	public void simpleFullScan() {
		
		QbeRepositoryImpl qbe = new QbeRepositoryImpl(getEntityManager());
		QueryFilter<Uf> filter = new QueryFilter<Uf>(QUf.uf); 
		assertNotEmpty(qbe.findAllBy(filter));
		
	}
	
	/**
	 * Testa consulta utilizando uma propriedade simples em um exemplo como filtro
	 */
	@Test
	public void simpleFindByExample() {
		// cria duas cidades com nomes semelhantes e uma com nome diferente
		Uf uf = getQuerier().findAny(Uf.class);
		String nomeBase = "XXYYZZ";
		Cidade cidade1 = new Cidade("1" + nomeBase + "1", uf);
		Cidade cidade2 = new Cidade("2" + nomeBase + "2", uf);
		Cidade cidade3 = new Cidade("33", uf);
		
		getEntityManager().persist(cidade1);
		getEntityManager().persist(cidade2);
		getEntityManager().persist(cidade3);
		getEntityManager().flush();
		
		// cria um exemplo com parametros compativeis com as cidades semelhantes
		Cidade exemplo = new Cidade();
		exemplo.setNome(nomeBase);
		
		// executa uma consulta com HQL filtrando pelo nome do exemplo
		String hql = "select cid from " + Cidade.class.getSimpleName() + " cid where cid.nome like ?0";
		List<Cidade> cidadesHQL = getQuerier().searchAndValidateNotEmpty(hql, "%" + exemplo.getNome() + "%");		
		
		// executa a consulta utilizando QBE
		QueryFilter<Cidade> filter = new QueryFilter<Cidade>(QCidade.cidade);
		filter.setExample(exemplo);

		QbeRepositoryImpl qbe = new QbeRepositoryImpl(getEntityManager());
		List<Cidade> cidadesQbe = qbe.findAllBy(filter);
		
		// compara as listas
		assertContentEqual(cidadesHQL, cidadesQbe);		
	}
	
	/**
	 * Consulta utilizando o valor de uma propriedade em um exemplo, porém customizando o operador a ser utilizado sobre a propriedade.
	 * O operador default para String é like, e será customizado para utilizar equals
	 */
	@Test
	public void findByCustomOperatorOnExample() {

		/*
		 * Cria duas cidades com nomes semelhantes e uma com nome diferente.
		 * O nome da cidade1 está contido no nome a cidade2 propositalmente para tentar garantir a diferença
		 * do comportamento entre os operadores eq e like.
		 */
		Uf uf = getQuerier().findAny(Uf.class);
		String nomeBase = "XXYYZZ";
		Cidade cidade1 = new Cidade("1" + nomeBase, uf);
		Cidade cidade2 = new Cidade("1" + nomeBase + "1", uf); 
		Cidade cidade3 = new Cidade("33", uf);
		
		getEntityManager().persist(cidade1);
		getEntityManager().persist(cidade2);
		getEntityManager().persist(cidade3);
		getEntityManager().flush();
		
		// cria um exemplo com parmetros compat�veis com a cidade encontrada
		Cidade exemplo = new Cidade();
		exemplo.setNome(cidade1.getNome());
		
		// executa uma consulta com HQL filtrando pelo nome do exemplo
		String hql = "from " + Cidade.class.getSimpleName() + " cid where cid.nome = ?0";
		List<Cidade> cidadesHQL = getQuerier().searchAndValidateNotEmpty(hql, exemplo.getNome());		
		
		// executa a consulta utilizando QBE
		QueryFilter<Cidade> filter = new QueryFilter<Cidade>(QCidade.cidade);
		filter.setExample(exemplo);
		filter.where(QCidade.cidade.nome.eq(Example.read()));
		
		
		QbeRepositoryImpl qbe = new QbeRepositoryImpl(getEntityManager());
		List<Cidade> cidadesQbe = qbe.findAllBy(filter);
		
		// compara as listas
		assertContentEqual(cidadesHQL, cidadesQbe);
		
	}
	
	/**
	 * Realizar consulta utilizando predicados mistos, lendo do exemplo e com valores constantes.
	 */
	@Test
	public void findByMixingCustomAndExampleOperation() {

		/*
		 * Cria duas cidades com nomes iguais mas uf diferentes.
		 */
		Uf uf1 = getQuerier().findAt(Uf.class, 0);
		Uf uf2 = getQuerier().findAt(Uf.class, 1);
		
		String nomeBase = "XXYYZZ";
		Cidade cidade1 = new Cidade(nomeBase, uf1);
		Cidade cidade2 = new Cidade(nomeBase, uf2); 
		
		getEntityManager().persist(cidade1);
		getEntityManager().persist(cidade2);
		getEntityManager().flush();
		
		// cria um exemplo configurando o nome comum, mas executa a consulta customizando
		// o filtro com um valor estático para uf1
		Cidade exemplo = new Cidade();
		exemplo.setNome(cidade1.getNome());
		
		// executa a consulta utilizando QBE
		QueryFilter<Cidade> filter = new QueryFilter<Cidade>(QCidade.cidade);
		filter.setExample(exemplo);
		filter.where(QCidade.cidade.nome.eq(Example.read()));
		filter.where(QCidade.cidade.uf().eq(uf1));
		
		
		QbeRepositoryImpl qbe = new QbeRepositoryImpl(getEntityManager());
		List<Cidade> cidadesQbe = qbe.findAllBy(filter);
		
		// compara as listas
		Assert.assertEquals(1, cidadesQbe.size());
		Assert.assertEquals(cidade1, cidadesQbe.get(0));
		
	}
	
	/**
	 * Realiza uma consulta utilizando como restrição uma propriedade do tipo associação. 
	 * Ex: Cidade.uf
	 */
	@Test
	public void findByAssociationParameter() {
		// Recupera uma Uf qualquer para usar como parâmetro da consulta, e buscar todas as suas cidades associadas. 
		Uf uf = getQuerier().findAny(Uf.class);
		
		Cidade exemplo = new Cidade();
		exemplo.setUf(uf);
		
		// executa uma consulta com HQL filtrando pelo nome do exemplo
		String hql = "from " + Cidade.class.getSimpleName() + " cid where cid.uf = ?0";
		List<Cidade> cidadesHQL = getQuerier().searchAndValidateNotEmpty(hql, uf);		
		
		// executa a consulta utilizando QBE
		QueryFilter<Cidade> filter = new QueryFilter<Cidade>(QCidade.cidade);
		filter.setExample(exemplo);

		QbeRepositoryImpl qbe = new QbeRepositoryImpl(getEntityManager());
		List<Cidade> cidadesQbe = qbe.findAllBy(filter);
		
		// compara as listas
		assertContentEqual(cidadesHQL, cidadesQbe);		
	}
	
	/**
	 * Realiza uma consulta utilizando como restrição dois predicados aninhados. 
	 */
	@Test
	public void findByNestedPredicateParameter() {
		// Recupera uma Uf qualquer para usar como parâmetro da consulta, e buscar todas as suas cidades associadas. 
		Uf uf1 = getQuerier().findAt(Uf.class, 0);
		Uf uf2 = getQuerier().findAt(Uf.class, 1);
		
		Cidade cidade1 = new Cidade("Maria", uf1);
		Cidade cidade2 = new Cidade("Maria", uf2);
		getJpa().save(cidade1);
		getJpa().save(cidade2);
		
		Cidade exemplo = new Cidade();
		exemplo.setUf(uf1);
		exemplo.setNome("Maria");
		
		// executa a consulta utilizando QBE
		QueryFilter<Cidade> filter = new QueryFilter<Cidade>(QCidade.cidade);
		filter.setExample(exemplo);
		filter.where(QCidade.cidade.nome.eq(Example.read()).and(QCidade.cidade.uf().eq(Example.read())));

		QbeRepositoryImpl qbe = new QbeRepositoryImpl(getEntityManager());
		List<Cidade> cidadesQbe = qbe.findAllBy(filter);
		
		// compara as listas
		Assert.assertEquals(1, cidadesQbe.size());
		Assert.assertTrue(cidadesQbe.get(0).equals(cidade1));
	}
	
	/**
	 * Realiza uma consulta utilizando como restrição dois predicados aninhados com AND cujos valores serão obtidos a partir
	 * do objeto exemplo, porém um dos valores não foi preenchio (=null). 
	 */
	@Test
	public void nestedANDFromExample() {
		// Recupera uma Uf qualquer para usar como parâmetro da consulta, e buscar todas as suas cidades associadas.
		Uf uf1 = new Uf("T1");
		Uf uf2 = new Uf("T2");
		getJpa().save(uf1);
		getJpa().save(uf2);
		
		Cidade cidade1 = new Cidade("Maria", uf1);
		Cidade cidade2 = new Cidade("Maria", uf2);
		Cidade cidade3 = new Cidade("João", uf1);
		getJpa().save(cidade1);
		getJpa().save(cidade2);
		getJpa().save(cidade3);
		
		Cidade exemplo = new Cidade();
		exemplo.setUf(uf1);
		
		/*
		 *  Configurando uma operação para o parâmetro "nome", indicando para recuperar seu valor a partir do exemplo. 
		 *  No entanto, como este atributo não foi preenchido no objeto exemplo, o predicado deverá ser descartado, filtrando apenas
		 *  pela UF 
		 */
		QueryFilter<Cidade> filter = new QueryFilter<Cidade>(QCidade.cidade);
		filter.setExample(exemplo);
		filter.where(QCidade.cidade.nome.eq(Example.read()).and(QCidade.cidade.uf().eq(Example.read())));

		QbeRepositoryImpl qbe = new QbeRepositoryImpl(getEntityManager());
		List<Cidade> cidadesQbe = qbe.findAllBy(filter);
		
		// compara as listas
		Assert.assertEquals(2, cidadesQbe.size());
		assertContentEqual(Arrays.asList(cidade1, cidade3), cidadesQbe);
	}
	
	/**
	 * Realiza uma consulta utilizando como restrição dois predicados aninhados com OR cujos valores serão obtidos a partir
	 * do objeto exemplo, porém um dos valores não foi preenchio (=null). 
	 */
	@Test
	public void nestedORFromExample() {
		// Recupera uma Uf qualquer para usar como parâmetro da consulta, e buscar todas as suas cidades associadas.
		Uf uf1 = new Uf("T1");
		Uf uf2 = new Uf("T2");
		getJpa().save(uf1);
		getJpa().save(uf2);
		
		Cidade cidade1 = new Cidade("João", uf1);
		Cidade cidade2 = new Cidade("Maria", uf2);
		Cidade cidade3 = new Cidade("João", uf1);
		getJpa().save(cidade1);
		getJpa().save(cidade2);
		getJpa().save(cidade3);
		
		Cidade exemplo = new Cidade();
		exemplo.setUf(uf1);
		
		/*
		 *  Configurando uma operação para o parâmetro "nome", indicando para recuperar seu valor a partir do exemplo. 
		 *  No entanto, como este atributo não foi preenchido no objeto exemplo, o predicado deverá ser descartado, filtrando apenas
		 *  pela UF 
		 */
		QueryFilter<Cidade> filter = new QueryFilter<Cidade>(QCidade.cidade);
		filter.setExample(exemplo);
		filter.where(QCidade.cidade.nome.eq(Example.read()).or(QCidade.cidade.uf().eq(Example.read())));

		QbeRepositoryImpl qbe = new QbeRepositoryImpl(getEntityManager());
		List<Cidade> cidadesQbe = qbe.findAllBy(filter);
		
		// compara as listas
		Assert.assertEquals(2, cidadesQbe.size());
		assertContentEqual(Arrays.asList(cidade1, cidade3), cidadesQbe);
	}
	
	/**
	 * Realiza uma consulta utilizando como restrição dois predicados aninhados com NOT cujos valores serão obtidos a partir
	 * do objeto exemplo, porém um dos valores não foi preenchio (=null). 
	 */
	@Test
	public void nestedAndNotFromExample() {
		// Recupera uma Uf qualquer para usar como parâmetro da consulta, e buscar todas as suas cidades associadas.
		Uf uf1 = new Uf("T1");
		Uf uf2 = new Uf("T2");
		getJpa().save(uf1);
		getJpa().save(uf2);
		
		Cidade cidade1 = new Cidade("João", uf1);
		Cidade cidade2 = new Cidade("Maria", uf2);
		Cidade cidade3 = new Cidade("José", uf1);
		getJpa().save(cidade1);
		getJpa().save(cidade2);
		getJpa().save(cidade3);
		
		Cidade exemplo = new Cidade();
		exemplo.setUf(uf1);
		
		/*
		 *  Configurando uma operação para o parâmetro "nome", indicando para recuperar seu valor a partir do exemplo. 
		 *  No entanto, como este atributo não foi preenchido no objeto exemplo, o predicado deverá ser descartado, filtrando apenas
		 *  pela UF 
		 */
		QueryFilter<Cidade> filter = new QueryFilter<Cidade>(QCidade.cidade);
		filter.setExample(exemplo);
		filter.where(QCidade.cidade.nome.eq(Example.read()).not().and(QCidade.cidade.uf().eq(Example.read())));

		QbeRepositoryImpl qbe = new QbeRepositoryImpl(getEntityManager());
		List<Cidade> cidadesQbe = qbe.findAllBy(filter);
		
		// compara as listas
		Assert.assertEquals(2, cidadesQbe.size());
		assertContentEqual(Arrays.asList(cidade1, cidade3), cidadesQbe);
	}
	
	/**
	 * Realiza uma consulta utilizando como restrição dois predicados aninhados com NOT cujos valores serão obtidos a partir
	 * do objeto exemplo, porém um dos valores não foi preenchio (=null). 
	 */
	@Test
	public void notFromExample() {
		// Recupera uma Uf qualquer para usar como parâmetro da consulta, e buscar todas as suas cidades associadas.
		Uf uf1 = new Uf("T1");
		Uf uf2 = new Uf("T2");
		getJpa().save(uf1);
		getJpa().save(uf2);
		
		Cidade cidade1 = new Cidade("João", uf1);
		Cidade cidade2 = new Cidade("Maria", uf2);
		Cidade cidade3 = new Cidade("José", uf1);
		getJpa().save(cidade1);
		getJpa().save(cidade2);
		getJpa().save(cidade3);
		
		Cidade exemplo = new Cidade();
//		exemplo.setNome("João");
		
		/*
		 *  Configurando uma operação para o parâmetro "nome", indicando para recuperar seu valor a partir do exemplo. 
		 *  No entanto, como este atributo não foi preenchido no objeto exemplo, o predicado deverá ser descartado. 
		 */
		QueryFilter<Cidade> filter = new QueryFilter<Cidade>(QCidade.cidade);
		filter.setExample(exemplo);
		filter.where(QCidade.cidade.nome.eq(Example.read()).not());

		QbeRepositoryImpl qbe = new QbeRepositoryImpl(getEntityManager());
		List<Cidade> cidadesQbe = qbe.findAllBy(filter);
		
		// compara as listas
		Assert.assertTrue(cidadesQbe.containsAll(Arrays.asList(cidade1, cidade2, cidade3)));
	}
	
	/**
	 * Realiza uma consulta utilizando como restrição um predicados cujo valor é obtido a partir
	 * do objeto exemplo, porém ele não foi preechido (=null). 
	 */
	@Test
	public void fromExample() {
		// Recupera uma Uf qualquer para usar como parâmetro da consulta, e buscar todas as suas cidades associadas.
		Uf uf1 = new Uf("T1");
		Uf uf2 = new Uf("T2");
		getJpa().save(uf1);
		getJpa().save(uf2);
		
		Cidade cidade1 = new Cidade("João", uf1);
		Cidade cidade2 = new Cidade("Maria", uf2);
		Cidade cidade3 = new Cidade("José", uf1);
		getJpa().save(cidade1);
		getJpa().save(cidade2);
		getJpa().save(cidade3);
		
		Cidade exemplo = new Cidade();
//		exemplo.setNome("João"); não preenchido
		
		/*
		 *  Configurando uma operação para o parâmetro "nome", indicando para recuperar seu valor a partir do exemplo. 
		 *  No entanto, como este atributo não foi preenchido no objeto exemplo, o predicado deverá ser descartado. 
		 */
		QueryFilter<Cidade> filter = new QueryFilter<Cidade>(QCidade.cidade);
		filter.setExample(exemplo);
		filter.where(QCidade.cidade.nome.eq(Example.read()));

		QbeRepositoryImpl qbe = new QbeRepositoryImpl(getEntityManager());
		List<Cidade> cidadesQbe = qbe.findAllBy(filter);
		
		// compara as listas
		Assert.assertTrue(cidadesQbe.containsAll(Arrays.asList(cidade1, cidade2, cidade3)));
	}
	
	/**
	 * Teste para garantir que o operador customizado está sobrescrevendo
	 * corretamente um operador implícito vindo do objeto exemplo. No caso
	 * abaixo, o atributo "cidade.uf" foi preenchido no objeto exemplo, que
	 * deverá gerar um predicado implícito o tipo EQ. Porém, o mesmo atributo
	 * foi customizado com a operação NOT EQ, portanto este deverá prevalecer.
	 */
	@Test
	public void replacingPredicateFromExample() {
		Uf uf1 = new Uf("T1");
		Uf uf2 = new Uf("T2");
		getJpa().save(uf1);
		getJpa().save(uf2);
		
		Cidade cidade1 = new Cidade("Maria", uf1);
		Cidade cidade2 = new Cidade("Maria", uf2);
		Cidade cidade3 = new Cidade("José", uf2);
		getJpa().save(cidade1);
		getJpa().save(cidade2);
		getJpa().save(cidade3);
		
		Cidade exemplo = new Cidade();
		exemplo.setUf(uf1);
		exemplo.setNome("Maria");
		
		/*
		 *  Espera-se encontrar todas as cidades != uf1 e com nome like "Maria"
		 */
		QueryFilter<Cidade> filter = new QueryFilter<Cidade>(QCidade.cidade);
		filter.setExample(exemplo);
		filter.where(QCidade.cidade.uf().eq(Example.read()).not());

		QbeRepositoryImpl qbe = new QbeRepositoryImpl(getEntityManager());
		List<Cidade> cidadesQbe = qbe.findAllBy(filter);
		
		// compara as listas
		Assert.assertEquals(1, cidadesQbe.size());
		assertContentEqual(Arrays.asList(cidade2), cidadesQbe);
	}
}
