package br.com.vcg.query.repository;

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.EntityManager;

import com.querydsl.core.Tuple;
import com.querydsl.core.support.ReplaceVisitor;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ParamExpression;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.ComparablePath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQuery;

import br.com.vcg.query.api.DetachedFetchFilter;
import br.com.vcg.query.api.QueryFilter;
import br.com.vcg.query.exception.QbeException;
import br.com.vcg.query.repository.operator.CustomTemplates;
import br.com.vcg.query.repository.visitor.ParamExtractorVisitor;
import br.com.vcg.query.repository.visitor.ParamSettingVisitor;
import br.com.vcg.query.repository.visitor.ReplaceVisitorParamExample;
import br.com.vcg.query.repository.visitor.VisitorContext;
import br.com.vcg.query.util.ReflectionUtil;

/**
 * Mantém um contexto para a execução de cada consulta realizada, permitindo o compartilhamento
 * de propriedades e valores. 
 * @author augusto
 */
public class QbeContextProcessor<ENTITY> {

	private Set<String> cacheCustomPredicates = new HashSet<String>();
	private QueryFilter<? extends ENTITY> filter;
	private EntityManager entityManager;

	public QbeContextProcessor(EntityManager entityManager, QueryFilter<? extends ENTITY> filter) {
		this.entityManager = entityManager;
		this.filter = filter;
	}
	
    public List<Tuple> findAllValuesBy(Expression<?>...expressions) {
        processPredicates();
        
        @SuppressWarnings("unchecked")
        List<Tuple> result = createJpaQuery().select(expressions).fetch();
        return result;
    }
    
    public Tuple findValuesBy(Expression<?>...expressions) {
        processPredicates();
        
        @SuppressWarnings("unchecked")
        List<Tuple> result = createJpaQuery()
        	.select(expressions)
        	.fetch();
        return result.size() > 0 ? result.get(0) : null;
    }    

	public List<ENTITY> findAllBy() {
		processPredicates();
		
		@SuppressWarnings("unchecked")
		List<ENTITY> result = (List<ENTITY>) createJpaQuery().fetch();
		
		processDetachedFetches(result);
		
		return result;
	}
	
	protected void processDetachedFetches(List<ENTITY> result) {
		if (result != null && !result.isEmpty()) {
			
			for (DetachedFetchFilter<?> detachedFilter : filter.getDetachedCollectionFetchList()) {
			
				DetachedFetchesProcessor detachedProcessor = createDetachedProcessor(result, detachedFilter);
				detachedProcessor.fetch();
				
			}
			
		}
	}

	public long count() {
		processPredicates();
		return createJpaQuery().fetchCount();
	}
	
	protected JPAQuery createJpaQuery() {
		JPAQuery jpaQuery = new JPAQuery(entityManager, CustomTemplates.INSTANCE, filter.getMetadata());
		addHints(jpaQuery);
        return jpaQuery;
	}

	private void addHints(JPAQuery jpaQuery) {
        for (Entry<String, Object> hint : filter.getHints().entrySet()) {
            jpaQuery.setHint(hint.getKey(), hint.getValue());
        }
    }

    /**
	 * Processa todos os predicados (restrições) encontrados do
	 * {@link QueryFilter}, considerando as diferenças existentes entre valores
	 * do exemplo e customizados.
	 */
	protected void processPredicates() {
		try {
			configureCustomPredicates(filter);
			configureExamplePredicates(filter);
		} catch (Exception e) {
			throw new QbeException("Falha ao processar os predicados da consulta.", e);
		}
	}

	/**
	 * Registra no {@link JPAQuery} os predicados customizados encontrados no
	 * filtro. Alguns predicados podem estar parcialmente customizados, são
	 * aqueles que esperam que o valor seja extraído do objeto exemplo, nestes
	 * casos, serão registrados apenas se o valore for encontrado.
	 * 
	 * @param filter
	 *            Filtro que contém o objeto exemplo e todas as demais
	 *            configurações desta consulta em andamento.
	 *            
	 * @return Conjunto de nomes das propriedades que tiveram valores customizados registrados.           
	 */
	protected void configureCustomPredicates(QueryFilter<?> filter) {

		Predicate predicate = filter.getMetadata().getWhere();
		if (predicate != null) {
			
			/*
			 *  Propriedades customizadas, quando devem considerar o valor do exemplo, são configuradas através de ParamExample (extends ParamExpression).
			 *  Desta forma, é necessário entender o parâmetro, e buscar o valor no exemplo para registrar na query.
			 *  Quando o valor no exemplo for =null, o parâmetro deve ser descartado.
			 */
			final VisitorContext myContext = new VisitorContext(filter, cacheCustomPredicates);
			predicate.accept(ParamExtractorVisitor.DEFAULT, myContext);
			
			/*
			 * Substituindo os nós ParamExample genéricos (tipo Object) com os tipos de verdade que estão preenchidos no exemplo.
			 * Desta forma, a árvore do modelo estará sincronizada com os valores do exemplo.
			 */
			ReplaceVisitor replaceVisitor = new ReplaceVisitorParamExample(myContext);
	        predicate = (Predicate) predicate.accept(replaceVisitor, null);
	        
	        /*
	         * Percorrendo árvore e setando os parâmetros do exemplo no jpaQuery
	         */
	        predicate.accept(ParamSettingVisitor.DEFAULT, myContext);
			
	        /*
	         * Atualiza os meta dados, para que as alterações reflitam na query. 
	         */
	        filter.getMetadata().clearWhere();
	        filter.getMetadata().addWhere(predicate);
	        
		}
		
	}
	
	/**
	 * Verifica se o contexto (na lista de parâmetros processados) possui um {@link Path} associado ao {@link ParamExpression} informado,
	 * caso exite, retorna o valor da propriedade, relativa ao path, no objeto exemplo.  
	 * @param context Contexto com os dados atualizados.
	 * @param param Parâmetro para busca do Path e, em seguida, do valor no objeto exemplo.
	 * @return Valor no objeto exemplo.
	 */
	public static Object getValueFromProperty(final VisitorContext context, ParamExpression<?> param) {
		// Extraindo o path da propriedade configurada no predicado
		Path<?> path = context.getPath(param);
		return getValueFromProperty(context, path);
	}

	/**
	 * Recupera o valor da propriedade, relativa ao path informado, no objeto exemplo.
	 * @param context Contexto com o objeto exemplo preenchido.
	 * @param path Path para identificação da propriedade do objeto exemplo.
	 * @return Valor encontrado no objeto exemplo.
	 */
	public static Object getValueFromProperty(final VisitorContext context, Path<?> path) {
		String pathPropName = path.getMetadata().getName();
		
		/*
		 * Verificando se a propriedade informada está preenchida no exemplo. Caso esteja, o predicado deverá receber
		 * o valor encontrado no exemplo e ser registrado na query
		 */
		Object value = ReflectionUtil.getValue(context.getExample(), pathPropName);
		return value;
	}

	/**
	 * Analisa o objeto exemplo (caso exista) encapsulado no filtro, extraindo
	 * todas as informações de suas propriedades preencidas, transformando-as em
	 * predicados e registrando-os no {@link JPAQuery}.
	 * 
	 * @param filter
	 *            Filtro que contém o objeto exemplo e todas as demais
	 *            configurações desta consulta em andamento.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void configureExamplePredicates(QueryFilter<?> filter) throws Exception {
		List<Property> filledProperties = createPropertiesInspector().extractFilledProperties(filter.getExample(), filter.getDeepLevel());
		for (Property property : filledProperties) {
			
			/*
			 * Processa o valor contido no exemplo apenas se este for diferente
			 * de null e não tiver sido processado por uma operação customizada.
			 * Neste último caso, as propriedades já processadas encontram-se no
			 * cache.
			 */
			String propStringPath = property.generateDotNotation();
			
			if (!cacheCustomPredicates.contains(propStringPath)) {
				Object propValue = property.getValue();
				if (property.getValue() instanceof String) {
					StringPath propPath = new StringPathCustom(filter.getFrom().getRoot(), propStringPath);
					filter.where(propPath.contains(propValue.toString()));
				} else {
					ComparablePath path = new ComparablePathCustom(propValue.getClass(), filter.getFrom().getRoot(), propStringPath);
					filter.where(path.eq(propValue));
				}
			}	

		}
	}
	
	protected ExamplePropertiesInspector createPropertiesInspector() {
		return new ExamplePropertiesInspector();
	}

	protected QueryFilter<? extends ENTITY> getFilter() {
		return filter;
	}

	protected void setFilter(QueryFilter<ENTITY> filter) {
		this.filter = filter;
	}

	public DetachedFetchesProcessor createDetachedProcessor(List<ENTITY> result, DetachedFetchFilter<?> detachedFilter) {
		return new DetachedFetchesProcessor(entityManager, detachedFilter, result);
	}

}
