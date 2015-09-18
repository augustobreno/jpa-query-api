package br.com.vcg.query.repository.visitor;

import javax.persistence.criteria.ParameterExpression;

import br.com.vcg.query.api.QueryFilter;

import com.mysema.query.types.ParamExpression;
import com.mysema.query.types.Path;

/**
 * Armazena um par de {@link ParameterExpression} e {@link Path} relacionados.
 * Estas informações geralmente são definidas no momento da configuração do {@link QueryFilter}, na definição
 * de operações sobre os atributos de um objeto exemplo. 
 * @author augusto
 */
public class ParamPathPair {

	private Path<?> path;
	private ParamExpression<?> param;

	public Path<?> getPath() {
		return path;
	}
	public void setPath(Path<?> path) {
		this.path = path;
	}
	public ParamExpression<?> getParam() {
		return param;
	}
	public void setParam(ParamExpression<?> param) {
		this.param = param;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((param.getName() == null) ? 0 : param.getName().hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ParamPathPair other = (ParamPathPair) obj;
		if (param == null) {
			if (other.param != null)
				return false;
		} else if (!param.getName().equals(other.param.getName()))
			return false;
		return true;
	}

	
}
