package br.com.vcg.query;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import br.com.vcg.query.api.QueryFilter;
import br.com.vcg.query.domain.Cidade;
import br.com.vcg.query.domain.QCidade;
import br.com.vcg.query.domain.Uf;
import br.com.vcg.query.repository.QbeRepository;

/**
 * Testes relacionados à priorização do ID em consultas que utilizam um objeto exemplo.
 * @author augusto
 *
 */
public class IdPriorityTest extends QbeTestBase {

	/**
	 * O ID da entidade, quando preenchido deve ser priorizado sobre os demais atributos,
	 * exceto quando esta for a entidade raiz da operação.
	 */
	@Test
	public void testNaoPriorizacaoID() {
		
		// cria uma cidade qualquer
		Uf ufBD = getQuerier().findAny(Uf.class);
		
		Cidade cid1 = new Cidade("cid1", ufBD);
		getJpa().save(cid1);
		getJpa().getEm().flush();
		getJpa().getEm().clear();
		
		// em seguida tenta realizar uma consulta utilizando como restrição
		// o id da cidade criada e um nome diferente do cadastrado
		
		Cidade exemplo = new Cidade();
		exemplo.setId(cid1.getId());
		exemplo.setNome("outro nome");
		
		
		QueryFilter<Cidade> filter = new QueryFilter<Cidade>(QCidade.cidade);
		filter.setExample(exemplo);
		
		long numCidades = createQbe().count(filter);
		
		/*
		 * visto que cidade é a entidade raiz da consulta,
		 * o id não deveria ter tipo prioridade sobre as demais propriedades.
		 * Se houve resultado na consulta, significa que o nome não foi considerado.
		 */
		assertEquals(0, numCidades);
	}

	/**
	 * O ID da entidade, quando preenchido deve ser priorizado sobre os demais atributos,
	 * apenas quando se tratar de um relacionamento da exemplo base do filtro.
	 */
	@Test
	public void testPriorizacaoID() {
		
		// cria uma cidade qualquer
		Uf ufBD = getQuerier().findAny(Uf.class);
		
		Cidade cid1 = new Cidade("cid1", ufBD);
		getJpa().save(cid1);
		getJpa().getEm().flush();
		getJpa().getEm().clear();
		
		// vamos realiza duas consultas, a primeira deverá buscar todas as cidades associadas a uma UF com o mesmo ID da
		// utilizada acima. A segunda deverá consultar todas as cidades associadas a uma UF com o mesmo ID da utilizada acima,
		// mas um nome diferente. Pelo princípio da priorização da chave primária em associações, o resultado deverá ser o mesmo.
		
		QueryFilter<Cidade> filtro1 = new QueryFilter<Cidade>(QCidade.cidade);
		filtro1.setExample(new Cidade());
		filtro1.getExample().setUf(new Uf());
		filtro1.getExample().getUf().setId(ufBD.getId());
	
		QueryFilter<Cidade> filtro2 = new QueryFilter<Cidade>(QCidade.cidade);
		filtro2.setExample(new Cidade());
		filtro2.getExample().setUf(new Uf());
		filtro2.getExample().getUf().setId(ufBD.getId());
		filtro2.getExample().getUf().setSigla("ZZ");

		QbeRepository qbe = createQbe();
		List<Cidade> cidades1 = qbe.findAllBy(filtro1);
		List<Cidade> cidades2 = qbe.findAllBy(filtro2);
		
		assertContentEqual(cidades1, cidades2);
	}
	
}
