package br.com.vocegerente.vcgerente.query.repository;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EmbeddedId;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import org.hibernate.Hibernate;

import br.com.vocegerente.vcgerente.query.util.ReflectionUtil;
import br.com.vocegerente.vcgerente.query.util.StringUtil;

/**
 * Extrator das propriedades preenchidas e válidas de um objeto exemplo.
 * Geralmente todas as propriedades diferentes de null são consideradas válidas
 * e prováveis fontes de ser tornarem um predicado.
 * @author augusto
 */
public class ExamplePropertiesInspector {

	/** Logger */
	private static Logger log;
	
	/**
	 * Extrai as propriedades preenchidas do objeto. Por padrão verifica as propriedades
	 * pertencentes diretamente à entidade de consulta, mas o nível de profundidade pode ser configurado, caso
	 * seja desejado a checagem de associações.
	 * @param example Objeto preenchido para extração.
	 * @return Lista de propriedades preenchidas.
	 */
	public List<Property> extractFilledProperties(Object example, int deepLevel) throws Exception {
		return extractFilledProperties(new ArrayList<Property>(), example, null, deepLevel, 1);
	}

	/**
	 * Para recursividade. 
	 * @param filledProperties 
	 * @param nestedProperty Propriedade pai, de onde esta chamada foi originada 
	 */
	protected List<Property> extractFilledProperties(ArrayList<Property> filledProperties, 
			Object entity, Property nestedProperty, int maxNivel, int currentNivel) throws Exception {
		
		if (entity != null) {
			
			// verifica se a entidade é um Hibernate proxy, e extrai a verdadeira entidade
			entity = ReflectionUtil.unProxy(entity); 
			
			List<Field> propriedades = extractMainProperties(entity, currentNivel);
			for (Field field : propriedades) {
				
				Property novaPropriedade = new Property(field, nestedProperty, ReflectionUtil.getValue(entity, field));
				if (!isCollection(novaPropriedade.getField())) {
					
					// se é um relacionamento o tratamento é diferenciado
					if ((isRelationship(field) && isInitialized(novaPropriedade.getValue()))) {

						/*
						 * Quando um relacionamento (associação) é encontrado, seus atributos também podem
						 * ser utilizados como filtro, se o nível de profundidade configurado permitir.
						 * No entanto, se o objeto possuir a chave primária (simples) preenchida, os demais atributos são
						 * totalmente descartados, e a PK é utilizada como único filtro para identificação da associação. 
						 */
						if (ifPkFilled(novaPropriedade)) {
							filledProperties.add(novaPropriedade);
							
						} else if (currentNivel <= maxNivel) {
							// analisa os demais atributos apenas se o nível permitir
							extractFilledProperties(filledProperties, novaPropriedade.getValue(), novaPropriedade, maxNivel, currentNivel + 1);
							
						} else {
							// informa que ainda seria possível avançar mais na análise das propriedades
							logWarn("O relacionamento " + novaPropriedade.generateDotNotation() + " não foi analisado porque a propriedade 'deepLevel' " +
									"do QueryFilter está limitando a profundidade das propriedades aninhadas.");
						}
						
					// se é chave composta, extrai seus atributos internos	
					} else if (isCompositeKey(field)) {
						extractFilledProperties(filledProperties, novaPropriedade.getValue(), novaPropriedade, maxNivel, currentNivel + 1);
						
						// se é outra propriedade qualquer, adicionado às propriedades preenchidas
					} else if (isFilled(novaPropriedade)) {
						filledProperties.add(novaPropriedade);
					}
					
				}	
				
			}
			
		}	
		
		return filledProperties;  
	}	
	
	/**
	 * Considerando que este field sugere uma associação, verifica se esta está com a chave primária preenchida.
	 * @param fk Associação.
	 * @return true se a chave primária estiver preenchida.
	 */
	protected boolean ifPkFilled(Property fk) {
		return fk.getValue() != null && getSimplePrimaryKey(fk.getValue()) != null;
	}

	/**
	 * Extrai o valor encontrado no atributo que representa uma chave primária simples (se existir),
	 * na entidade informada. 
	 * @param entity Entidade que contém a chave primária simples.
	 * @return Valor da chave primária.
	 */
	protected Object getSimplePrimaryKey(Object entity) {
		
		// iterando sobre a estrutura da classe e das superclasses
		for (Class<?> clazz = entity.getClass(); clazz != Object.class; clazz = clazz
				.getSuperclass()) {

			// para cada classe, itera sobre os atributos
			for (Field field : clazz.getDeclaredFields()) {

				if (field.isAnnotationPresent(Id.class)) {
					return ReflectionUtil.getValue(entity, field);
				}
				
			}

		}    
		
		return null;
	}

	/**
	 * Este método inclui certa inteligencia para perceber quando o id da entidade foi informado. Neste caso,  
	 * se o id for diferente de null, e não se trata do objeto base da consulta (nível = 1), não verifica as demais propriedades, 
	 * visto que o id é o sufiente para identificar um registro.  
	 */
	protected List<Field> extractMainProperties(Object entidade, int currentNivel) throws Exception {
		
	      List<Field> fields = new ArrayList<Field>();

	      // iterando sobre a estrutura da classe e das superclasses
	      for (Class<?> clazz = entidade.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
	    	  
	    	  // para cada classe, itera sobre os atributos
	         for (Field field : clazz.getDeclaredFields()) {
	        	 
	        	 // descarta campos considerados padroes da api ou inválidos
	        	 if (isValidProperty(field)) {
        			 fields.add(field);
	        	 }
	         }
	      }
	      
	      return fields;
	}
	
	/**
	 * Verifica se a propriedade possuir valor preenchido na entidade.
	 * @param propriedade Propriedade devidamente preenchida
	 * @return true se for preenchida.
	 * @throws Exception 
	 */
	protected boolean isFilled(Property propriedade) throws Exception {
		Object valor = propriedade.getValue();
		
		boolean preenchido = true;
		if (valor == null 
				|| (valor instanceof String && StringUtil.isStringEmpty((String) valor))
				|| (isRelationship(propriedade.getField()) && !isIdFilled(propriedade.getValue()))) { //se é um relacionamento, mas o id não está preenchido, nao considera
			preenchido = false;
		}
		return preenchido;
	}

	/**
	 * Verifica se a propriedade representa uma chave composta.
	 * @param propriedade a ser avaliada.
	 * @return true se representa uma chave composta
	 */
	protected boolean isCompositeKey(Field propriedade) {
		return propriedade.isAnnotationPresent(EmbeddedId.class);
	}

	/**
	 * Verifica se uma propriedade possui anotacao de relacionamento com outra entidade
	 * @param propriedade Propriedade a ser vefificada.
	 * @return true se representar uma relacionamento.
	 */
	protected boolean isRelationship(Field propriedade) {
		return propriedade.isAnnotationPresent(OneToMany.class) 
			|| propriedade.isAnnotationPresent(OneToOne.class)
			|| propriedade.isAnnotationPresent(ManyToMany.class)
			|| propriedade.isAnnotationPresent(ManyToOne.class);
	}
	
	/**
	 * Verifica se o bean está inicializado ou é um Hibernate Proxy.
	 * @param objeto 
	 * @return true se estiver inicializado.
	 */
	protected boolean isInitialized(Object objeto) {
		return Hibernate.isInitialized(objeto); 
	}
	
	/**
	 * Verifica se uma propriedade é uma coleção
	 */
	protected boolean isCollection(Field field) {
		return Collection.class.isAssignableFrom(field.getType());
	}

	/**
	 * Verifica se a propriedade é transient. Como estamos utilizando anotações em propriedades,
	 * apenas as que são anotadas com @Transient é que não sao mapeadas.
	 * @param propriedade propriedade a ser checada.
	 * @return true se for transiente.
	 */
	protected boolean isTransient(Field propriedade) {
		return propriedade.isAnnotationPresent(Transient.class);
	}
	
	/**
	 * Verifica se a propriedade é static. Como estamos utilizando o padrão QBE,
	 * apenas as que não são static devem ser consideradas.
	 * @param propriedade propriedade a ser checada.
	 * @return true se for static.
	 */
	protected boolean isStatic(Field propriedade) {
		return Modifier.isStatic(propriedade.getModifiers());
	}
	
	/**
	 * Verifica se o campo faz parte de alguma obrigação padrão da API ou é desnecessário no contexto de busca
	 * através de QBE. Ex: serialVersionUID, class, transient, static
	 * @param field propriedade a ser checada.
	 * @return true se for um campo válido.
	 */
	protected boolean isValidProperty(Field field) {
		if (isTransient(field)
				|| isStatic(field)
				|| field.getName().matches("serialVersionUID|class")) {
			return false;
		}
		return true;
	}
	
	/**
	 * Verifica se o id está preenchido
	 */
	protected boolean isIdFilled(Object entidade) throws Exception {
		return entidade != null && ReflectionUtil.getValue(entidade, "id") != null;
	}

	protected void logWarn(String msg) {
		if (getLogger() != null) {
			getLogger().log(Level.WARNING, msg);
		}
	}

	/**
	 * Inicializa o logger
	 */
	protected Logger getLogger() {
		if (log == null) {
			log = Logger.getLogger(ExamplePropertiesInspector.class.getName());
		}
		return log;
	}
}
