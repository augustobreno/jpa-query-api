package br.com.vocegerente.vcgerente.query;

import static org.junit.Assert.assertFalse;

import java.util.List;

import org.hibernate.Hibernate;
import org.junit.Test;

import br.com.vocegerente.vcgerente.query.api.QueryFilter;
import br.com.vocegerente.vcgerente.query.domain.Cidade;
import br.com.vocegerente.vcgerente.query.domain.Pessoa;
import br.com.vocegerente.vcgerente.query.domain.ProjetoServidor;
import br.com.vocegerente.vcgerente.query.domain.QCidade;
import br.com.vocegerente.vcgerente.query.domain.QPessoa;
import br.com.vocegerente.vcgerente.query.domain.QProjetoServidor;
import br.com.vocegerente.vcgerente.query.domain.QServidor;
import br.com.vocegerente.vcgerente.query.domain.QUf;
import br.com.vocegerente.vcgerente.query.domain.Servidor;
import br.com.vocegerente.vcgerente.query.domain.Uf;
import br.com.vocegerente.vcgerente.query.repository.QbeRepository;

/**
 * Testa o comportamento do Qbe na realização de join de associações 
 * em diversos cenários.
 * @author augusto
 *
 */
public class JoinTest extends QbeTestBase {

	/**
	 * Realiza join de uma associação simples
	 */
	@Test
	public void joinSimpleAssociation() {

		// consulta algumas cidades, carregando também as UFs
		QbeRepository qbe = createQbe();

		QueryFilter<Cidade> filter = new QueryFilter<Cidade>(QCidade.cidade);
		filter.join(QCidade.cidade.uf());
		List<Cidade> cidadesQbe = qbe.findAllBy(filter);		
		
		
		assertFalse("Não foram encontradas Cidades.", cidadesQbe.isEmpty());
		
		// verifica se a UF NÃO está inicializada
		for (Cidade cidade : cidadesQbe) {
			assertFalse("A UF NÃO deveria estar inicializada. Cidade: " + cidade.getNome(), Hibernate.isInitialized(cidade.getUf()));
		}
	}

	/**
	 * Join de uma coleção.
	 */
	@Test
	public void joinCollectionAssociation() {

		// consulta todas as UFs, carregando também as cidades
		QbeRepository qbe = createQbe();

		QueryFilter<Uf> filter = new QueryFilter<Uf>(QUf.uf);
		filter.join(QUf.uf.cidades);
		List<Uf> ufsQbe = qbe.findAllBy(filter);			
		
		
		assertFalse("Não foram encontradas UFs.", ufsQbe.isEmpty());
		
		// verifica se a lista de cidades está inicializadas
		for (Uf uf : ufsQbe) {
			assertFalse("As cidades NÃO deveriam estar inicializadas. UF: " + uf.getSigla(), Hibernate.isInitialized(uf.getCidades()));
		}
		
	}	

	/**
	 * Join de associações simples aninhadas 
	 */
	@Test
	public void joinNestedSimpleAssociation() {
		// consulta algumas pessoas, solicitando join da UF, propriedade aninhada através de cidade
		QbeRepository qbe = createQbe();

		QueryFilter<Pessoa> filter = new QueryFilter<Pessoa>(QPessoa.pessoa);
		filter.join(QPessoa.pessoa.cidade(), QCidade.cidade).join(QCidade.cidade.uf());
		List<Pessoa> pessoasQbe = qbe.findAllBy(filter);			
		
		assertFalse("Não foram encontradas Pessoas.", pessoasQbe.isEmpty());
		
		// verifica se as cidades estão inicializadas
		for (Pessoa pessoa : pessoasQbe) {
			assertFalse("A Cidade Não deveria estar inicializada. Pessoa: " + pessoa.getNome(), Hibernate.isInitialized(pessoa.getCidade()));
		}
	}
	
	/**
	 * Join de uma associação simples em uma coleção. Ex: servidor.projetos.projeto
	 */
	@Test
	public void joinNestedSimpleAssociationinCollection() {
		
		QbeRepository qbe = createQbe();
		
		// consulta alguns servidores, realizando join de seus projetos
		QueryFilter<Servidor> filter = new QueryFilter<Servidor>(QServidor.servidor);
		
		// busca apenas servidores envolvidos em projetos
		filter.where(QServidor.servidor.projetos.isNotEmpty());
		
		// carrega a relação "projetos" (coleção), inicializando também o atributo "projeto" para cada ProjetoServidor encontrado
		QProjetoServidor projetos = new QProjetoServidor("projetos"); //alias
		filter.join(QServidor.servidor.projetos, projetos).join(projetos.projeto());
		
		List<Servidor> servidoresQbe = qbe.findAllBy(filter);
		assertFalse("Não foram encontrados Servidores.", servidoresQbe.isEmpty());
		
		// verifica se a coleção "projetos" está inicializada e
		// verifica se o projeto de cada item da coleção também está inicializado
		for (Servidor servidor : servidoresQbe) {
			assertFalse("A coleção 'projetos' NÃO deveria estar inicializada. Servidor: " + servidor.getNome(), Hibernate.isInitialized(servidor.getProjetos()));
		}

	}	
	
	/**
	 * Join de ua coleção em uma associação aninhada
	 * 
	 * Ex: projetoServidor.servidor.dependentes
	 */
	@Test
	public void joinCollectionInNestedAssociation() {

		// Consulta projetos, carregando os dependentes do servidor associado ao projeto
		QbeRepository qbe = createQbe();
		
		// consulta alguns servidores, realizando join de seus projetos
		QueryFilter<ProjetoServidor> filter = new QueryFilter<ProjetoServidor>(QProjetoServidor.projetoServidor);
		
		filter.join(QProjetoServidor.projetoServidor.servidor(), QServidor.servidor)
			.innerJoin(QServidor.servidor.dependentes);
		
		List<ProjetoServidor> servidoresQbe = qbe.findAllBy(filter);
		assertFalse("Não foram encontrados ProjetoServidor.", servidoresQbe.isEmpty());
		
		// verifica se o servidor e seus dependentes estão inicializados
		for (ProjetoServidor projetoServidor : servidoresQbe) {
			assertFalse("O servidor NÃO deveria estar inicializado", Hibernate.isInitialized(projetoServidor.getServidor()));
		}
		
	}
}
