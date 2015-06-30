package br.com.vocegerente.vcgerente.query.api;

import com.mysema.query.jpa.JPAQueryMixin;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.path.PathBuilder;

/**
 * Filtro que dirige a integração do conceito de QBE (QueryByExample) com o
 * framework QueryDsl.
 * 
 * @author augusto
 */

public class QueryFilter<ENTITY> extends JPAQueryMixin<QueryFilter<ENTITY>> {

	/**
	 * Objeto cujos valores encontrados em seus atributos e associações serão
	 * considerados como restrições dinâmicas na execução da query.
	 */
	private ENTITY example;

	/**
	 * Configura até qual nível de profundidade nas associações o QBE deverá
	 * buscar por propriedades preenchidas.
	 */
	private int deepLevel = 2;

	/**
	 * Indicação da entidade base para realização da consulta.
	 */
	private EntityPath<? extends ENTITY> from;
	
	private EntityPath<? extends ENTITY> projection;
	
	/**
	 * @param from
	 *            Q-type da entidade primária para a construção da consulta.
	 */
	public <SUBTIPO extends ENTITY> QueryFilter(EntityPath<SUBTIPO> from) {
		from(from);
		this.from = from;
		setProjection(from);
		setSelf(this); // para funcionamento das chamadas aninhadas
	}
	
	/**
	 * @param entityType Tipo da entidade associada a este Filter.
	 */
	public <SUBTIPO extends ENTITY> QueryFilter(Class<SUBTIPO> entityType) {
		this(buildEntityPath(entityType));
	}
	
	protected static <ENTITY> EntityPath<ENTITY> buildEntityPath(Class<ENTITY> entityType) {
		// o alias padrão é o nome da classe com o primeiro caractere minúsculo
		String aliasName = buildAliasName(entityType);
		PathBuilder<ENTITY> entityPath = new PathBuilder<ENTITY>(entityType, aliasName); 
		return entityPath;
	}

	protected static <ENTITY> String buildAliasName(Class<ENTITY> entityType) {
		String simpleName = entityType.getSimpleName();
		String aliasName = simpleName.toLowerCase().charAt(0) +  simpleName.substring(1);
		return aliasName;
	}

	public ENTITY getExample() {
		return example;
	}

	public QueryFilter<ENTITY> setExample(ENTITY example) {
		this.example = example;
		return this;
	}

	public int getDeepLevel() {
		return deepLevel;
	}

	public QueryFilter<ENTITY> setDeepLevel(int deepLevel) {
		this.deepLevel = deepLevel;
		return this;
	}

	public EntityPath<? extends ENTITY> getProjection() {
		return projection;
	}

	public void setProjection(EntityPath<? extends ENTITY> projection) {
		this.projection = projection;
	}

	public EntityPath<? extends ENTITY> getFrom() {
		return from;
	}

	public void setFrom(EntityPath<ENTITY> from) {
		this.from = from;
	}
	
}
