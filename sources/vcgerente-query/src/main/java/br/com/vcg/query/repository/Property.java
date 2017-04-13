package br.com.vcg.query.repository;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.com.vcg.query.util.StringUtil;

/**
 * Classe auxiliar para representar as propriedades a serem consideradas durante a construção de uma consulta baseada
 * na solução QBE. Pode armazenar o seu caminho a partir da classe pai, possibilitando a construção de uma notação "dot notation".
 */
public class Property {

    private Logger log = Logger.getLogger(Property.class.getName());
	private Field field;
	private Object value;
	private Property nestedName;
	private String dotNotationCache;

	/**
	 * Construtor.
	 * @param field Campo que representa a propria propriedade.
	 * @param nestedProperty Campo que representa o campo pai de onde este (this.campo) derivou.
	 * @param value Valor da propriedade.
	 */
	public Property(Field field, Property nestedProperty, Object value) {
		this.field = field;
		this.nestedName = nestedProperty;
		this.value = value;
	}
	
	/**
	 * Cria a notação em "dot notation" para representar a propriedade. 
	 * @return notacao.
	 */
	public String generateDotNotation() {
		if (StringUtil.isStringEmpty(dotNotationCache)) {
			dotNotationCache = generate(field, nestedName);
		}
		return dotNotationCache;
	}
	
	private String generate(Field prop, Property nested) {
		String s = "";
		if (nested != null) {
			s += generate(nested.getField(), nested.getNestedName());
			s += ".";
		}
		s += prop.getName();
		
		return s;
	}

	public Field getField() {
		return field;
	}

	public Property getNestedName() {
		return nestedName;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object valor) {
		this.value = valor;
	}

	@Override
	public String toString() {
		return generateDotNotation();
	}
}
