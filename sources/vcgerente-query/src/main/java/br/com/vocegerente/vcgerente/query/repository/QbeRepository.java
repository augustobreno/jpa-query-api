package br.com.vocegerente.vcgerente.query.repository;

import java.util.List;

import br.com.vocegerente.vcgerente.query.api.QueryFilter;

public interface QbeRepository {

	/**
	 * Consulta todas as entidades equivalentes às configurações encontradas no
	 * filtro informado.
	 * 
	 * @param filter
	 *            Filtro com as configurações da consulta a ser realizada.
	 * @return Lista com as entidades equivalentes às configurações encontradas
	 *         no filtro informado
	 */
	public abstract <ENTITY> List<ENTITY> findAllBy(QueryFilter<? extends ENTITY> filter);

	/**
	 * Conta o número de registros existentes de acordo com as configurações/restrições
	 * encontradas no filtro.
	 * @param queryFilter Filtro com restrições para a query.
	 * @return Número de registros que se adequam ao filtro.
	 */
	public abstract <ENTITY> long count(QueryFilter<? extends ENTITY> queryFilter);

}