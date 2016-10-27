package br.com.vcg.query.repository.operator;

import com.querydsl.jpa.HQLTemplates;

/**
 * Constantes para definições de novos templates QueryDsl.
 * @author augusto
 *
 */
public class CustomTemplates extends HQLTemplates {

	public static final CustomTemplates INSTANCE = new CustomTemplates();
	
	private CustomTemplates() {
		// precedence=18 (inspirada no operador EQ)
		add(CustomOperators.ALWAYS_TRUE, "1=1", 18);	
	}
	
	public static CustomTemplates getInstance() {
		return INSTANCE;
	}
	
}

