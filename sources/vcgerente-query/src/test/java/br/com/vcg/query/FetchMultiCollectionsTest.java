package br.com.vcg.query;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hibernate.Hibernate;
import org.junit.Test;

import br.com.vcg.query.api.QueryFilter;
import br.com.vcg.query.domain.Dependente;
import br.com.vcg.query.domain.ProjetoServidor;
import br.com.vcg.query.domain.Servidor;
import br.com.vcg.query.repository.QbeRepository;
import br.com.vcg.query.domain.QServidor;

/**
 * Testa o comportamento do Qbe na realização de fetch de mútiplas coleçõews
 * @author augusto
 *
 */
public class FetchMultiCollectionsTest extends QbeTestBase {

	@Test
	public void consultarFetch2Colecoes() {

		// consulta algumas cidades, carregando também as UFs
		QbeRepository qbe = createQbe();

		QueryFilter<Servidor> filter = new QueryFilter<Servidor>(QServidor.servidor);
		
		filter.detachedFetchFilter(QServidor.servidor.projetos);
		filter.detachedFetchFilter(QServidor.servidor.dependentes);
		
		List<Servidor> servidoresQbe = qbe.findAllBy(filter);
		
		assertFalse("Não foram encontrados Servidores.", servidoresQbe.isEmpty());
		
		// verifica se as entidades carregadas via fetch estão inicializadas
		for (Servidor servidor : servidoresQbe) {
			assertTrue("Os dependentes deveriam estar inicializados", Hibernate.isInitialized(servidor.getDependentes()));
			assertTrue("Os projetos deveriam estar inicializados", Hibernate.isInitialized(servidor.getProjetos()));
			
			for (Dependente dependente : servidor.getDependentes()) {
				assertTrue("O dependente deveria estar inicializado: " + dependente.getNome(), Hibernate.isInitialized(dependente));
			}
			for (ProjetoServidor projetoServidor : servidor.getProjetos()) {
				assertTrue("O projetoServidor deveria estar inicializado: " + projetoServidor.getId(), Hibernate.isInitialized(projetoServidor));
			}
		}
	}
	
}
