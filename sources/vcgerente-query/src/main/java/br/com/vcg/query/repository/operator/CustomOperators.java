package br.com.vcg.query.repository.operator;

import com.mysema.query.types.Operator;
import com.mysema.query.types.OperatorImpl;

/**
 * Constantes para definições de novos operadores QueryDsl.
 * @author augusto
 *
 */
public class CustomOperators {

	/**
	 * Operação que permite implementar um predicado do tipo "1=1"
	 */
	public static final Operator<Boolean> ALWAYS_TRUE = new OperatorImpl<Boolean>("always_true", "qbe");	
}
