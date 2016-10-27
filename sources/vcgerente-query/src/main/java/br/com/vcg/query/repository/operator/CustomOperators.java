package br.com.vcg.query.repository.operator;

import com.querydsl.core.types.Operator;


/**
 * Constantes para definições de novos operadores QueryDsl.
 * @author augusto
 *
 */
public class CustomOperators {

	/**
	 * Operação que permite implementar um predicado do tipo "1=1"
	 */
	@SuppressWarnings("serial")
    public static final Operator ALWAYS_TRUE = new Operator() {
        @Override
        public String name() {
            return "always_true";
        }
        @Override
        public Class<?> getType() {
            return Boolean.class;
        }
    };
    
}
