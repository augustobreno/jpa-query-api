package br.com.vcg.query;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hibernate.Hibernate;
import org.junit.Test;

import br.com.vcg.query.api.QueryFilter;
import br.com.vcg.query.domain.Cidade;
import br.com.vcg.query.domain.Dependente;
import br.com.vcg.query.domain.Pessoa;
import br.com.vcg.query.domain.ProjetoServidor;
import br.com.vcg.query.domain.QCidade;
import br.com.vcg.query.domain.QPessoa;
import br.com.vcg.query.domain.QProjetoServidor;
import br.com.vcg.query.domain.QServidor;
import br.com.vcg.query.domain.QUf;
import br.com.vcg.query.domain.Servidor;
import br.com.vcg.query.domain.Uf;
import br.com.vcg.query.repository.QbeRepository;

/**
 * Testa o comportamento do Qbe na realização de fetch de associações 
 * em diversos cenários.
 * @author augusto
 *
 */
public class FetchTest extends QbeTestBase {

	/**
	 * Realiza fetch de uma associação simples
	 */
	@Test
	public void fetchSimpleAssociation() {

		// consulta algumas cidades, carregando também as UFs
		QbeRepository qbe = createQbe();

		QueryFilter<Cidade> filter = new QueryFilter<Cidade>(QCidade.cidade);
		filter.join(QCidade.cidade.uf()).fetchJoin();
		List<Cidade> cidadesQbe = qbe.findAllBy(filter);		
		
		
		assertFalse("Não foram encontradas Cidades.", cidadesQbe.isEmpty());
		
		// verifica se a UF está inicializada
		for (Cidade cidade : cidadesQbe) {
			assertTrue("A UF deveria estar inicializada. Cidade: " + cidade.getNome(), Hibernate.isInitialized(cidade.getUf()));
		}
	}

	/**
	 * Fetch de uma coleção.
	 */
	@Test
	public void fetchCollectionAssociation() {

		// consulta todas as UFs, carregando também as cidades
		QbeRepository qbe = createQbe();

		QueryFilter<Uf> filter = new QueryFilter<Uf>(QUf.uf);
		filter.join(QUf.uf.cidades).fetchJoin();
		List<Uf> ufsQbe = qbe.findAllBy(filter);			
		
		
		assertFalse("Não foram encontradas UFs.", ufsQbe.isEmpty());
		
		// verifica se a lista de cidades está inicializadas
		for (Uf uf : ufsQbe) {
			assertTrue("As cidades deveriam estar inicializadas. UF: " + uf.getSigla(), Hibernate.isInitialized(uf.getCidades()));
		}
		
	}	

	/**
	 * Fetch de associações simples aninhadas 
	 */
	@Test
	public void fetchNestedSimpleAssociation() {
		// consulta algumas pessoas, solicitando fetch da UF, propriedade aninhada através de cidade
		QbeRepository qbe = createQbe();

		QueryFilter<Pessoa> filter = new QueryFilter<Pessoa>(QPessoa.pessoa);
		filter.join(QPessoa.pessoa.cidade(), QCidade.cidade).fetchJoin()
				.join(QCidade.cidade.uf()).fetchJoin();
		List<Pessoa> pessoasQbe = qbe.findAllBy(filter);			
		
		assertFalse("Não foram encontradas Pessoas.", pessoasQbe.isEmpty());
		
		// verifica se as cidades e UFs est�o inicializadas
		for (Pessoa pessoa : pessoasQbe) {
			assertTrue("A Cidade deveria estar inicializada. Pessoa: " + pessoa.getNome(), Hibernate.isInitialized(pessoa.getCidade()));
			assertTrue("A UF deveria estar inicializada. Pessoa: " + pessoa.getNome(), Hibernate.isInitialized(pessoa.getCidade().getUf()));
		}
	}
	
	/**
	 * Fetch de uma associação simples em uma coleção. Ex: servidor.projetos.projeto
	 */
	@Test
	public void fetchNestedSimpleAssociationinCollection() {
		
		QbeRepository qbe = createQbe();
		
		// consulta alguns servidores, realizando fetch de seus projetos
		QueryFilter<Servidor> filter = new QueryFilter<Servidor>(QServidor.servidor);
		
		// busca apenas servidores envolvidos em projetos
		filter.where(QServidor.servidor.projetos.isNotEmpty());
		
		// carrega a relação "projetos" (coleção), inicializando também o atributo "projeto" para cada ProjetoServidor encontrado
		QProjetoServidor projetos = new QProjetoServidor("projetos"); //alias
		filter.join(QServidor.servidor.projetos, projetos).fetchJoin()
				.join(projetos.projeto()).fetchJoin();
		
		List<Servidor> servidoresQbe = qbe.findAllBy(filter);
		assertFalse("Não foram encontrados Servidores.", servidoresQbe.isEmpty());
		
		// verifica se a coleção "projetos" está inicializada e
		// verifica se o projeto de cada item da coleção também está inicializado
		for (Servidor servidor : servidoresQbe) {
			assertTrue("A coleção 'projetos' deveria estar inicializada. Servidor: " + servidor.getNome(), Hibernate.isInitialized(servidor.getProjetos()));
			
			for (ProjetoServidor projetoServidor : servidor.getProjetos()) {
				//verifica se o projeto também está inicializado
				assertTrue("O Projeto deveria estar inicializado. Servidor: " + servidor.getNome(), Hibernate.isInitialized(projetoServidor.getProjeto()));
			}
		}

	}	
	
	/**
	 * Fetch de ua coleção em uma associação aninhada
	 * 
	 * Ex: projetoServidor.servidor.dependentes
	 */
	@Test
	public void fetchCollectionInNestedAssociation() {

		// Consulta projetos, carregando os dependentes do servidor associado ao projeto
		QbeRepository qbe = createQbe();
		
		// consulta alguns servidores, realizando fetch de seus projetos
		QueryFilter<ProjetoServidor> filter = new QueryFilter<ProjetoServidor>(QProjetoServidor.projetoServidor);
		
		filter.join(QProjetoServidor.projetoServidor.servidor(), QServidor.servidor).fetchJoin()
				.innerJoin(QServidor.servidor.dependentes).fetchJoin();
		
		List<ProjetoServidor> servidoresQbe = qbe.findAllBy(filter);
		assertFalse("Não foram encontrados ProjetoServidor.", servidoresQbe.isEmpty());
		
		// verifica se o servidor e seus dependentes estão inicializados
		for (ProjetoServidor projetoServidor : servidoresQbe) {
			assertTrue("O servidor deveria estar inicializado", Hibernate.isInitialized(projetoServidor.getServidor()));
			assertTrue("Os dependentes do servidor deveriam estar inicializados", Hibernate.isInitialized(projetoServidor.getServidor().getDependentes()));
			assertFalse("A lista de dependentes não deveria estar vazia.", projetoServidor.getServidor().getDependentes().isEmpty());
			
			for (Dependente dependente : projetoServidor.getServidor().getDependentes()) {
				assertTrue("O dependente deveria estar inicializado.", Hibernate.isInitialized(dependente));
			}
		}
		
	}
}
