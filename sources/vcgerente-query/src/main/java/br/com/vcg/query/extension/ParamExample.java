package br.com.vcg.query.extension;

import com.mysema.query.types.ParamExpression;
import com.mysema.query.types.expr.Param;

/**
 * Demarcador de parâmetro que será substituído pelo atributo correspondente no exemplo.
 * 
 * @author David
 *
 * @param <T> Tipo do atributo da entidade
 */
public class ParamExample<T> extends Param<T> {

	private static final long serialVersionUID = 1L;

	public ParamExample(Class<? extends T> type) {
		super(type);
	}

	public ParamExample(Class<? extends T> type, ParamExpression<T> param) {
		super(type, param.getName());
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ParamExample<?> other = (ParamExample<?>) obj;
		if (getName() == null) {
			if (other.getName() != null)
				return false;
		} else if (!getName().equals(other.getName()))
			return false;
		return true;
	}
}
