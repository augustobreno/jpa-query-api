package br.com.vcg.query.repository;

import java.util.List;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;

import br.com.vcg.query.api.QueryFilter;

public interface QbeRepository {

    /**
     * Consulta conjunto de valores específicos de uma entidade.
     * @param filter Filtro com configurações de restrições da consulta.
     * @param expressions Conjunto de valores a serem recuperados.
     * @return Lista de valores a serem recuperados
     */
    public List<Tuple> findAllValuesBy(QueryFilter<?> filter, Expression<?>...expressions);    
    
    /**
	 * Search for a single tuple of simple values (a projection).
	 * 
	 * @param filter      restrictions for search. It must be build to reduce result
	 *                    to a single result. If not, will be return the first
	 *                    element found.
	 * @param expressions expressions to set projection.
	 * @return Single tuple.
	 */
    public abstract Tuple findValuesBy(QueryFilter<?> filter, Expression<?>...expressions);
    
	/**
	 * Consulta todas as entidades equivalentes às configurações encontradas no
	 * filtro informado.
	 * 
	 * @param filter
	 *            Filtro com as configurações da consulta a ser realizada.
	 * @return Lista com as entidades equivalentes às configurações encontradas
	 *         no filtro informado
	 */
	public abstract <ENTITY> List<ENTITY> findAllBy(QueryFilter<ENTITY> filter);

	/**
	 * Conta o número de registros existentes de acordo com as configurações/restrições
	 * encontradas no filtro.
	 * @param queryFilter Filtro com restrições para a query.
	 * @return Número de registros que se adequam ao filtro.
	 */
	public abstract <ENTITY> long count(QueryFilter<ENTITY> queryFilter);	

}