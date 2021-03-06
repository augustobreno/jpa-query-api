package br.com.vcg.query.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.querydsl.core.types.CollectionExpression;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.dsl.ListPath;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPAQueryMixin;

/**
 * Filtro que dirige a integração do conceito de QBE (QueryByExample) com o
 * framework QueryDsl.
 * 
 * @author augusto
 */

public class QueryFilter<ENTITY> extends JPAQueryMixin<QueryFilter<ENTITY>> {

    /** Tamanho padrão para paginação */
    public static final Long PAGE_SIZE = 10L;

    /** Índice padrão para consulta paginada */
    public static final Long PAGE_INDEX = 1L;

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

    /**
     * Para configuração da projeção do retorno da consulta.
     */
    private EntityPath<? extends ENTITY> projection;

    /**
     * Alguns fetchs são interessantes de serem feitos de forma desatachada (em
     * uma consulta independente):
     * 
     * - Mutiplas coleções: O hibernate não suporta consulta com fetch de
     * múltiplas coleções, então os fetches podem ser realizados de forma
     * independente e em seguida atachados ao resultado da consulta original.
     * 
     */
    private List<DetachedFetchFilter<?>> detachedCollectionFetchList = new ArrayList<DetachedFetchFilter<?>>();

    /**
     * Para configuração da Query.
     */
    private Map<String, Object> hints = new HashMap<String, Object>();

    /**
     * @param from
     *            Q-type da entidade primária para a construção da consulta.
     */
    public <SUBTIPO extends ENTITY> QueryFilter(EntityPath<SUBTIPO> from) {
        from(from);
        this.from = from;
        setProjectionEntity(from);
        setSelf(this); // para funcionamento das chamadas aninhadas
    }

    /**
     * @param entityType
     *            Tipo da entidade associada a este Filter.
     */
    public <SUBTIPO extends ENTITY> QueryFilter(Class<SUBTIPO> entityType) {
        this(buildEntityPath(entityType));
    }

    public static <ENTITY> EntityPath<ENTITY> buildEntityPath(
            Class<ENTITY> entityType) {
        // o alias padrão é o nome da classe com o primeiro caractere minúsculo
        String aliasName = buildAliasName(entityType);
        PathBuilder<ENTITY> entityPath = new PathBuilder<ENTITY>(entityType,
                aliasName);
        return entityPath;
    }

    protected static <ENTITY> String buildAliasName(Class<ENTITY> entityType) {
        String simpleName = entityType.getSimpleName();
        String aliasName = simpleName.toLowerCase().charAt(0)
                + simpleName.substring(1);
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

    public void setProjectionEntity(EntityPath<? extends ENTITY> projection) {
        this.projection = projection;
    }

    public EntityPath<? extends ENTITY> getFrom() {
        return from;
    }

    public void setFrom(EntityPath<ENTITY> from) {
        this.from = from;
    }

    public <E> DetachedFetchFilter<E> detachedFetchFilter(CollectionExpression<?, E> target) {
        if (target instanceof ListPath) {

            @SuppressWarnings({ "rawtypes", "unchecked" })
            DetachedFetchFilter<E> detachedFetchFilter = new DetachedFetchFilter<E>(this, (ListPath) target);
            this.detachedCollectionFetchList.add(detachedFetchFilter);
            return detachedFetchFilter;

        }
        return null;
    }

    public List<DetachedFetchFilter<?>> getDetachedCollectionFetchList() {
        return detachedCollectionFetchList;
    }

    /**
     * @param property
     *            Nome da propriedade.
     * @return Uma expressão a partir do nome de uma propriedade.
     */
    public PathBuilder<Object> createExpression(String property) {

        // nome do alias associado ao "from" do filtro, para manter consistente
        // a query.
        String alias = getFrom().getMetadata().getName();
        PathBuilder<ENTITY> entityPath = new PathBuilder<ENTITY>(getFrom().getType(), alias);
        return entityPath.get(property);

    }

    public void addHint(String key, Object value) {
        hints.put(key, value);
    }

    /**
     * @return Alias utilizado na construção das consultas no JPA.
     */
    public String getAlias() {
        return getFrom().getMetadata().getName();
    }

    public Map<String, Object> getHints() {
        return hints;
    }

    /**
     * Configura novo objeto de exemplo. Equivalente ao método
     * {@link #setExample(Object)}
     * 
     * @param entity
     *            Objeto exemplo.
     * @return this.
     */
    public QueryFilter<ENTITY> newExample(ENTITY entity) {
        setExample(entity);
        return this;
    }

    /**
     * Realiza a configuração de paginação. Configura apenas se ambos os
     * parâmetros forem != null.
     * 
     * @param pageSize
     *            Tamanho da página. Caso não informado, usa o padrão
     *            {@link #PAGE_SIZE}
     * @param pageIndex
     *            Índice da página (1 based). Caso não informado, usa o padrão
     *            {@link #PAGE_INDEX};
     * @return this.
     */
    public QueryFilter<ENTITY> fetchPage(Long pageSize, Long pageIndex) {
        Long index = pageIndex != null && pageIndex > 0 ? pageIndex : PAGE_INDEX;
        Long size = pageSize != null && pageSize > 0? pageSize : PAGE_SIZE;
        return this.fetchRange((index - 1) * size, size);
    }

    /**
     * Configures this to load a range of elements in result set. This
     * configuration will be applied only if limit were informed.
     * 
     * @param offset
     *            Index of first element to fetch. Default is "0".
     * @param limit
     *            Max number of elements to fetch.
     * @return this.
     */
    public QueryFilter<ENTITY> fetchRange(Long offset, Long limit) {
        if (limit != null) {
            this.offset(offset != null ? offset : 0);
            this.limit(limit);
        }
        return this;
    }

    /**
     * @return O valor configurado para paginação em {@link #limit(long)}.
     */
    public Long getLimit() {
        return this.getMetadata().getModifiers().getLimit();
    }

    /**
     * @return O valor configurado para paginação em {@link #offset(long)}.
     */
    public Long getOffset() {
        return this.getMetadata().getModifiers().getOffset();
    }

}
