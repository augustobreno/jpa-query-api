package br.com.vcg.query;

import org.jglue.cdiunit.ActivatedAlternatives;
import org.jglue.cdiunit.AdditionalClasses;
import org.jglue.cdiunit.CdiRunner;
import org.junit.runner.RunWith;

import br.com.vcg.query.util.AlternativeLocalEntityManagerProducer;
import br.com.vcg.tests.LocalTransactionTestBase;
import br.com.vcg.tests.cdi.LoggerForTestProducer;
import br.com.vcg.tests.jpa.JpaStandaloneProducer;

/**
 * Configura as dependências necessárias para utilização de {@link LocalTransactionTestBase}
 * no contexto da aplicação local. 
 * @author Augusto
 */
@AdditionalClasses({LoggerForTestProducer.class, JpaStandaloneProducer.class})
@ActivatedAlternatives({AlternativeLocalEntityManagerProducer.class})
@RunWith(CdiRunner.class)
public class AppLocalTransactionTestBase extends LocalTransactionTestBase {

}
