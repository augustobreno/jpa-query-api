package br.com.vcg.query.repository;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.OneToMany;

import org.hibernate.Session;

import br.com.vcg.query.api.DetachedFetchFilter;
import br.com.vcg.query.exception.QbeException;
import br.com.vcg.query.util.ReflectionUtil;

import com.mysema.query.types.path.PathBuilder;

/**
 * Processa os fetches desatachados para que sejam executados em uma consulta
 * independente e depois associados ao resultado final da consulta original.
 * 
 * @author augusto
 *
 */
public class DetachedFetchesProcessor {

	private Logger logger = Logger.getLogger(DetachedFetchesProcessor.class.getSimpleName()); 
	
	private DetachedFetchFilter<?> detachedFilter;

	private List<?> result;

	private EntityManager entityManager;

	/**
	 * @param detachedFilter
	 *            Filtro com as configurações para a consulta desatachada.
	 * @param result
	 *            Resultado da consulta principal (já deve ter sido executada).
	 */
	public DetachedFetchesProcessor(EntityManager entityManager, DetachedFetchFilter<?> detachedFilter, List<?> result) {
		this.entityManager = entityManager;
		this.detachedFilter = detachedFilter;
		this.result = result;	
	}

	/**
	 * Implementa Fetch para a consulta desatachada configurada.
	 */
	protected void fetch() {

		restrictResultInDetachedFilter();

	}

	/**
	 * Considerando que será executada uma consulta secundária para carregamento
	 * dos dados de uma associação (coleção), deve-se utilizar o resultado da
	 * consulta original como restrição na configuração da consulta secundária.
	 */
	protected void restrictResultInDetachedFilter() {

		List<Object> resultRestriction = resolveRestrictionValues();
		int resultRestrictionSize = resultRestriction.size();

		if (!resultRestriction.isEmpty()) {

			/*
			 * O banco de dados suporta apenas 1000 registro na operação IN,
			 * portanto, vamos ter que realizar fetch parcial, em grupos de 1000
			 */

			// Mapa para agrupamento do resultado das consultas realizadas em
			// partialFetch
			Map<Object, Collection<Object>> fetchResultMap = new HashMap<Object, Collection<Object>>();

			// indices para manter-se dentro dos limites do banco de dados
			int startGroup = 0;
			int endGroup = resultRestrictionSize > 1000 ? 1000
					: resultRestrictionSize;

			do {
				fetch(fetchResultMap,
						resultRestriction.subList(startGroup, endGroup));

				// atualiza indices
				startGroup = endGroup;
				endGroup = resultRestrictionSize > endGroup + 1000 ? endGroup + 1000
						: resultRestrictionSize;
			} while (startGroup < resultRestrictionSize);
			
			
			// atualiza as entidades encontradas na consulta principal, preenchendo-as com o resultado
			// do fetch realizado
			updateEntitiesWithFetchResult(resultRestriction, fetchResultMap);
			
		}
	}

	/**
	 * atualiza as entidades encontradas na consulta principal, preenchendo-as com o resultado do fetch realizado.
	 * @param entitiesToUpdate Lista com as entidades a serem atualizadas com os dados carregados (fetched). 
	 * @param partialFetchResult Mapa que contém o resultado do fetch realizado.
	 */
	protected void updateEntitiesWithFetchResult(List<Object> entitiesToUpdate, Map<Object, Collection<Object>> partialFetchResult) {
		
		// atualiza as entidades principais com os valores encontrados
		for (Iterator<Object> iterator = entitiesToUpdate.iterator(); iterator.hasNext();) {
			
			Object primaryEntity = iterator.next();
			Collection<?> fetchedList = getFetchedCollection(partialFetchResult, primaryEntity);
			
			try {
				
				/*
				 * O hibernate implementa um controle mais restrito para associação com cascade delete orphan, reclamando quando
				 * tentamos substituir a instância da coleção mapeada. Para estes casos, será necessário remover a entidade
				 * owner do relacionamento do cache, desabilitando o controle do hibernate. Como consequência, não será funcionará
				 * o lazy initialization - que deve ser substituído por um fetch.
				 */
				if (isDeleteOrphan()) {
					evict(primaryEntity);
					logger.fine("A associação " + detachedFilter.getParentElementType().getSimpleName() + "." +  detachedFilter.getPropertyToFetch() 
							+ " está mapeada como DELETE ORPHAN. Foi necessário remover o owner da relação do cache do hibernate. Isto não" +
							"causará problemas mas o comportamento de LazyInitialization foi desabilitado para este objeto. ");
				} 
				
				// atualiza a associação com a lista recuperada
				ReflectionUtil.setValue(primaryEntity, detachedFilter.getPropertyToFetch(), fetchedList);
			} catch (Exception e) {
				throw new QbeException("Falha ao tentar atualiza a entidade com a coleção carregada manualmente: " 
										 + detachedFilter.getPropertyToFetch(), e);
			}
		}
	}

	/**
	 * @param partialFetchResult Mapa com o resultado da consulta desatachada.
	 * @param primaryEntity Entidade que pertence à consulta original.
	 * @return A coleção carregada associada à a entidade.
	 */
	protected Collection<Object> getFetchedCollection(Map<Object, Collection<Object>> partialFetchResult, Object primaryEntity) {
		Collection<Object> collection = partialFetchResult.get(primaryEntity);
		
		if (collection == null) {
			collection = createAppropriateCollection();
			partialFetchResult.put(primaryEntity, collection);
		}
		
		return collection;
	}
	
	/**
	 * No mapeamento de coleções é mais comum a utilização de List, porém é permitido
	 * o uso de outros tipos de coleções (como Set), portanto, é necessário descobrir
	 * o tipo utilizado na entidade que contém o mapeamento.
	 * @return Um tipo concreto de coleção compatível com o tipo utilizado na entidade que detém o mapeamento. 
	 */
	protected Collection<Object> createAppropriateCollection() {
		Class<Collection<?>> collectionType = findCollectionType(detachedFilter.getParentElementType(), detachedFilter.getPropertyToFetch());
		
		Collection<Object> collection = null;
		
		if (List.class.isAssignableFrom(collectionType)) {
			collection = new ArrayList<Object>();
			
		} else if (Set.class.isAssignableFrom(collectionType)) {
			collection = new HashSet<Object>();
			
		} else {
			throw new QbeException("O tipo de coleção utilizado no mapeamento não é suportado: " + detachedFilter.getPropertyToFetch());
			
		}
		
		return collection;
	}
	
	/**
	 * Procura o tipo da coleção que se deseja realizar fetch.
	 * @param beanType Tipo do bean que contém as propriedades.
	 * @param property Nome da propriedade.
	 * @return Tipo da coleção.
	 */
	@SuppressWarnings("unchecked")
	private Class<Collection<?>> findCollectionType(Class<?> beanType, String property) {
		try {
			Field field = ReflectionUtil.getField(beanType, property);
			return (Class<Collection<?>>) field.getType();
		} catch (Exception e) {
			throw new QbeException("Não foi possível encontrar informações sobre o tipo da coleção " 
					+ beanType.getSimpleName() + "." + property, e);
		}
	}
	
	private void evict(Object primaryEntity) {
		Session session = (Session) entityManager.getDelegate();
		session.evict(primaryEntity);
	}

	/**
	 * @return true se a coleção está mapeada com a opção "orphanRemoval"
	 */
	protected boolean isDeleteOrphan() {
		Class<?> parentElementType = detachedFilter.getParentElementType();
		Field field = ReflectionUtil.getField(parentElementType, detachedFilter.getPropertyToFetch());
		OneToMany annotation = field.getAnnotation(OneToMany.class);
		return annotation != null && annotation.orphanRemoval();
	}

	/**
	 * Realiza o fetch da coleção configurada, limitando-se aos registros existentes na lista informada no parâmetro..
	 * @param fetchResultMap Mapa para preenchiento com o resultado do carregamento.
	 * @param originalEntityList Lista original que determina a base para o carregamento.
	 */
	protected void fetch(Map<Object, Collection<Object>> fetchResultMap, List<?> originalEntityList) {

		// Nome da propriedade no relacionamento inverso, configurada em mappedBy
		String mappedByProperty = findMapedByProperty();
		
		/*
		 * Recupera a expressão que dá acesso o relacionamento equivalente ao "mappedby". 
		 * Este método é gerado automaticamente pelo querydsl: 
		 */
		PathBuilder<?> pathBuilder = getEquivalentPathBuilder();
		PathBuilder<Object> mappedByPath = pathBuilder.get(mappedByProperty);
		
		// configura o filtro
		detachedFilter.leftJoin(mappedByPath).fetch();  // para evitar consultas adicionais durante a manipulação dos dados
		detachedFilter.where(mappedByPath.in(originalEntityList)); // restringindo com o relacionamento inverso (mappedby)
		

		QbeRepository qbeRepository = new QbeRepositoryImpl(entityManager);
		List<Object> fetchResult = qbeRepository.findAllBy(detachedFilter);

		
		for (Object fetchedEntity : fetchResult) {
			// recupera a associação com a entidade principal
			Object primaryEntity = ReflectionUtil.getValue(fetchedEntity, mappedByProperty);
			addToFetchResultMap(fetchResultMap, primaryEntity, fetchedEntity);
		}

	}

	/**
	 * @return Um {@link PathBuilder} equivalente à configuração da coleção para ser carregada.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected PathBuilder<?> getEquivalentPathBuilder() {
		if (detachedFilter.getFrom() instanceof PathBuilder) {
			return (PathBuilder<?>) detachedFilter.getFrom();
		}
		return new PathBuilder(detachedFilter.getEntityElementType(), detachedFilter.getFromAliasName());
	}
	

	/**
	 * Descobre o nome da propriedade do atributo simples do outro lado do relacionamento, associado à coleção. 
	 * <br/>
	 * Ex: Servidor(dependentes) 1 -- * Dependente(servidor), onde servidor é o atributo mappedBy. 
	 * @return Nome da propriedade do atributo simples no relacionamento.
	 */
	private String findMapedByProperty() {
	
		
		String mappedBy = null;
		/*
		 * Relacionamentos com collections são implementados, geralmente, com OneToMany
		 */
		try {
			Field colField = ReflectionUtil.getField(detachedFilter.getParentElementType(), detachedFilter.getPropertyToFetch());
			
			if (colField.isAnnotationPresent(OneToMany.class)) {
				OneToMany oneToMany = colField.getAnnotation(OneToMany.class);
				mappedBy = oneToMany.mappedBy();
				
			} else {
				throw new QbeException("Não foi encontrado a anotação @OneToMany na relação ." + detachedFilter.getPropertyToFetch());
			}
			
		} catch (Exception e) {
			throw new QbeException("Não foi possível encontrar informações sobre a coleção " 
					+ detachedFilter.getParentElementType() + "." + detachedFilter.getPropertyToFetch(), e);
		}
		
		return mappedBy;
	}
	
	/**
	 * Adiciona o registro encontrado à coleção cujo fetch foi solicitado.
	 * @param fetchResultMap 
	 * @param primaryEntity Entidade primária que contém a coleção cujo fetch foi colicitado. 
	 * @param fetchedEntity Objeto encontrado que deverá pertencer à coleção que foi solicitada para realização de fetch
	 */
	protected void addToFetchResultMap(Map<Object, Collection<Object>> fetchResultMap, Object primaryEntity, Object fetchedEntity) {
		Collection<Object> associationList = getFetchedCollection(fetchResultMap, primaryEntity);
		associationList.add(fetchedEntity);
	}
	
	/**
	 * Identifica qual o objeto responsável pela associação direta com a coleção
	 * configurada para fetch. Caso a coleção esteja associada diretamente à
	 * entidade principal (da consulta principal), a própria entidade é
	 * relacionada como objeto da projeção. Caso a coleção faça parte de uma
	 * associação aninhada (ex: orgao.uf.cidades), retorna o objeto responsável
	 * pela relação direta com a coleção (no caso, "orgao.uf").
	 * 
	 */

	protected List<Object> resolveRestrictionValues() {
		List<Object> resultRestriction = new ArrayList<Object>();

		if (fetchIsCloseToRoot()) {
			// é uma associação primária, o filtro é a o resultado da própria
			// consulta principal
			resultRestriction.addAll(result);

		} else {
			// é uma associação aninhada, é preciso realizar uma projeção sobre
			// o resultado da consulta principal

			String propertyToFetchParent = getFetchedPropertyParentPath();

			for (Object resultObject : result) {
				Object projectionValue = ReflectionUtil.getValue(resultObject,
						propertyToFetchParent);
				resultRestriction.add(projectionValue);
			}

		}

		return resultRestriction;
	}

	/**
	 * @return O caminho deste o objeto raíz da consulta principal até o pai da
	 *         propriedade que sofrerá o fetch
	 * 
	 */
	protected String getFetchedPropertyParentPath() {
		return detachedFilter.getTargetMetadata().getParent().getMetadata()
				.getName();
	}

	/**
	 * @return true se a configuração para fetch está associada diretamente à
	 *         entidade raíz da consulta principal.
	 */
	protected boolean fetchIsCloseToRoot() {
		return detachedFilter.getTargetMetadata().getParent().getMetadata()
				.isRoot();
	}

}
