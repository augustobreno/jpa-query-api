package br.com.vocegerente.vcgerente.query.api;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.expr.SimpleExpression;
import com.mysema.query.types.path.ListPath;

/**
 * Armazena as configurações básicas para permtir a executação de um fetch desatachado de coleções.
 * @author augusto
 *
 */
public class DetachedFetchFilter<E> extends QueryFilter<E>{
	
	/**
	 * Coleção que será carregada de forma independente da consult principal.
	 */
	private ListPath<E, SimpleExpression<? super E>> target;
	
	/** Atalho para os metadados da configuração da coleção a ser carregada. */
	private PathMetadata<?> targetMetadata;
	
	/** Acesso à configuração da consulta pai original */
	private QueryFilter<?> parent;
	
	/**
	 * @param parent Configuração da consulta pai original.
	 * @param listPath Expressão que aponta para a coleção que será "fetched"
	 */
	public DetachedFetchFilter(QueryFilter<?> parent, ListPath<E, SimpleExpression<? super E>> listPath) {
		super(listPath.getElementType());
		this.parent = parent;
		this.target = listPath;
		targetMetadata = listPath.getMetadata();
	}

	/**
	 * @return Expressão que representa a coleção a ser fetched.
	 */
	public ListPath<E, SimpleExpression<? super E>> getTarget() {
		return target;
	}

	/**
	 * @return metadados da configuração da coleção a ser fetched.
	 */
	public PathMetadata<?> getTargetMetadata() {
		return targetMetadata;
	}

	/**
	 * @return Configuração da consulta pai original.
	 */
	public QueryFilter<?> getParent() {
		return parent;
	}

	public void setParent(QueryFilter<?> parent) {
		this.parent = parent;
	}
	
	/**
	 * @return Tipo da entidade de domínio associada a este {@link DetachedFetchFilter}
	 */
	public Class<?> getEntityElementType() {
		return getTarget().getElementType();
	}
	
	/**
	 * @return Alias utilizado na configuração deste filter. Geralmente é equivalente ao nome da entidade de domínio associada.
	 */
	public String getFromAliasName() {
		return getFrom().getMetadata().getElement().toString();
	}
	
	/**
	 * @return Tipo da entidade de domínio pai, que contém o relacionamento
	 */
	public Class<?> getParentElementType() {
		return getParent().getFrom().getType();
	}
	
	/**
	 * @return Nome da propriedade que será fetched
	 */
	public String getPropertyToFetch() {
		return getTargetMetadata().getName();
	}
	
}