package br.com.vcg.query;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import javax.persistence.EntityManager;

import org.junit.Ignore;

import br.com.vcg.query.repository.QbeRepository;
import br.com.vcg.query.repository.QbeRepositoryImpl;
import br.com.vcg.tests.jpa.JPAStandalone;
import br.com.vcg.tests.util.QuerierUtil;

/**
 * Classe base para os casos de testes do módulo QBE.
 * Gerencia o ciclo de vida do JUnit e dos recursos necessários para testes.
 * @author augusto
 */
@Ignore
public class QbeTestBase extends AppLocalTransactionTestBase {

	/**
	 * Converte uma String em uma Data Formatada utilizando o formato padrão de data.
	 * @param data	String a ser formatada.
	 * @return		Data Formatada no formato padrão.
	 * @throws ParseException 	Se a String for incompatível com o formato padrão.
	 */
	public static Date formatDate(String data) throws ParseException {
		return formatDate(data, "dd/MM/yyyy");
	}
	
	/**
	 * Converte uma String em uma Data Formatada utilizando um padrão qualquer.
	 * @param data		String a ser formatada.
	 * @param pattern	Padrão utilizado na formatação.
	 * @return			Data Formatada.
	 * @throws ParseException	Se a String ou o Padrão forem incompatíveis.
	 */
	public static Date formatDate(String data, String pattern) throws ParseException {
		Date retorno = null;
		if (data != null && !data.isEmpty()) {
			DateFormat formatter = new SimpleDateFormat(pattern);
			retorno = (Date) formatter.parse(data);
		}
		return retorno;
	}

	/**
	 * Permite o acesso a uma instância qualquer de {@link JPAStandalone} registrada no repositório. Recomenda-se utilizar
	 * este método em testes configurados com uma única Persistence Unit.
	 * @return A primeira instância de {@link JPAStandalone} encontrada no registro, ou null caso nenhuma seja encontrada.
	 */
	public JPAStandalone getJpa() {
		Collection<JPAStandalone> all = getJpaRepository().getAll();
		return all.iterator().hasNext() ? all.iterator().next() : null;
	}
	
	/**
	 * Permite o acesso a um querier de uma instância qualquer de {@link JPAStandalone} registrada no repositório. Recomenda-se utilizar
	 * este método em testes configurados com uma única Persistence Unit.
	 * @return O {@link QuerierUtil} associado à primeira instância de {@link JPAStandalone} encontrada no registro, ou null caso nenhuma seja encontrada.
	 */
	public QuerierUtil getQuerier() {
		JPAStandalone jpa = getJpa();
		return jpa != null ? jpa.getQuerier() : null;
	}
	
	/**
	 * @return Nova instância de {@link QbeRepository} configurado com o {@link EntityManager} padrão.
	 */
	protected QbeRepository createQbe() {
		return new QbeRepositoryImpl(getEntityManager());
	}
	
	/**
	 * @return EntityManager associado ao {@link JPAStandalone} default.
	 */
	public EntityManager getEntityManager() {
		return getJpa().getEm();
	}

}
