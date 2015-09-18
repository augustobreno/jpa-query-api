package br.com.vcg.query.repository.visitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.com.vcg.query.api.QueryFilter;
import br.com.vcg.query.extension.ParamExample;
import br.com.vcg.query.repository.operator.CustomOperators;

import com.mysema.query.support.Expressions;
import com.mysema.query.types.Operation;
import com.mysema.query.types.Ops;
import com.mysema.query.types.ParamExpression;
import com.mysema.query.types.Path;
import com.mysema.query.types.expr.BooleanExpression;
import com.mysema.query.types.expr.BooleanOperation;

/**
 * Armazena o path e o parâmetro relacionados em uma expressão. 
 * @author augusto
 */
public class VisitorContext {

	/**
	 * Mapa com todos os partes de Parâmetros e Paths encontrados nos nós da (sub)árvore visitada.
	 * Geralmente uma (sub)árvore tem mais de um par deste tipo quando a query foi construída com 
	 * predicados aninhados: nome.like(param1).and(cpf.eq(param2));
	 * 
	 */
	private Map<ParamExpression<?>, ParamPathPair> paramPaths = new HashMap<ParamExpression<?>, ParamPathPair>();
	
	/**
	 * Armazena o {@link ParamPathPair} que está sendo construído, visto que este objeto, quando construído em visitors, 
	 * ocorre em 2 passos (path e depois param). 
	 */
	private ParamPathPair buildingPpp;
	
	/**
	 * Cache de todos os paths processados.
	 */
	private Set<String> cacheCustomPredicates;

	/**
	 * Predicados cujo atributo no objeto exemplo possui valor =null, portanto devem ser "descartados".
	 */
	private List<Operation<?>> predicatesToDispose = new ArrayList<Operation<?>>();
	
	/**
	 * Armazena o útimo Predicado processado nas visitas à árvore.
	 */
	private Operation<?> lastOperation;
	
	/**
	 * Operação para substituir um nó (PredicateOperation) garantindo que
	 * esté não influenciará no resultado final da consulta. É utilizado quando o predicado
	 * dependente de um valor extraído do objeto exemplo e este valor é =null.
	 */
	private BooleanExpression alwaystrueReplaceOperation = createAlwaysTrue();

	/**
	 * Filtro de configuração da consulta.
	 */
	private QueryFilter<?> filter;

	private BooleanExpression createAlwaysTrue() {
		return Expressions.predicate(CustomOperators.ALWAYS_TRUE);
	}
	
	public VisitorContext(QueryFilter<?> filter, Set<String> cacheCustomPredicates) {
		this.filter = filter;
		this.cacheCustomPredicates = cacheCustomPredicates;
	}
	
	public Object getExample() {
		return getFilter().getExample();
	}
	public Set<String> getCacheCustomPredicates() {
		return cacheCustomPredicates;
	}
	private void setBuildingPpp(ParamPathPair buildPpp) {
		this.buildingPpp = buildPpp;
	}
	public Collection<ParamPathPair> getParamPaths() {
		return paramPaths.values();
	}
	public void add(ParamPathPair ppp) {
		this.paramPaths.put(ppp.getParam(), ppp);
		setBuildingPpp(null);
	}

	/**
	 * Registra o Path em {@link #lastPpp}. Caso o parâmetro já esteja preenchido, adiciona o
	 * {@link #lastPpp} à lista de parâmetros encontrados.
	 */
	public void set(Path<?> expr) {
		if (buildingPpp == null) {
			buildingPpp = new ParamPathPair();
		}
		
		buildingPpp.setPath(expr);
		
		if (buildingPpp.getParam() != null) {
			add(buildingPpp);
		}
	}
	
	/**
	 * Registra o Parâmetro em {@link #lastPpp}. Caso o path já esteja preenchido, adiciona o
	 * {@link #lastPpp} à lista de parâmetros encontrados.
	 */
	public void set(ParamExpression<?> expr) {
		if (buildingPpp == null) {
			buildingPpp = new ParamPathPair();
		}
		
		buildingPpp.setParam(expr);
		
		if (buildingPpp.getPath() != null) {
			add(buildingPpp);
		}
	}

	/**
	 * Substitui um {@link ParamExample} no conjunto de {@link ParamPathPair} amazenado para futuro processamento. 
	 */
	public void replace(ParamExample<?> paramExample) {
		ParamPathPair ppp = paramPaths.get(paramExample);
		if (ppp != null) {
			ppp.setParam(paramExample);
		}
	}
	
	/**
	 * @return O {@link Path} associado ao {@link ParamExpression}, caso este esteja armazenado no mapa de 
	 * {@link ParamPathPair} deste contexto.
	 */
	public Path<?> getPath(ParamExpression<?> param) {
		ParamPathPair paramPathPair = paramPaths.get(param);
		return paramPathPair == null ? null : paramPathPair.getPath();
	}

	public Operation<?> getLastOperation() {
		return lastOperation;
	}

	public void setLastOperation(Operation<?> currentOperation) {
		buildAlwaysTrue(currentOperation);
		this.lastOperation = currentOperation;
	}

	/**
	 * Constrói uma expressão equivalente à "dispensação" da operação correta. É utilizado quando
	 * uma operação está relacionada a um valore null em um objeto exemplo (QBE). Como não é simples
	 * remover um nó da árvore de expressões do Query DSL, então substituímos o nó por uma expressão
	 * neutra, que não afeta o resultado final da consulta.
	 */
	protected void buildAlwaysTrue(Operation<?> currentOperation) {
		
		if (currentOperation.getOperator().equals(Ops.AND)) {
			this.alwaystrueReplaceOperation = createAlwaysTrue();
			
		} else if (currentOperation.getOperator().equals(Ops.OR)) {
			this.alwaystrueReplaceOperation = createAlwaysTrue().not();
			
		} else if (currentOperation.getOperator().equals(Ops.NOT)) {
			if (getLastOperation() != null) {
				// Quando o NOT está abaixo de um outro nó booleano, é considerado o valor do pai e não do NOT 
				this.alwaystrueReplaceOperation = this.alwaystrueReplaceOperation.not();
			} else {
				this.alwaystrueReplaceOperation = createAlwaysTrue();
			}
		} else if (currentOperation instanceof BooleanOperation) {
			throw new UnsupportedOperationException("O operador " + currentOperation.getOperator() + " não é suportado em operações QBE.");
		}
		
	}

	public List<Operation<?>> getPredicatesToDispose() {
		return predicatesToDispose;
	}
	public void scheduleLastOperationForDisposal() {
		if (getLastOperation() != null) {
			this.predicatesToDispose.add(getLastOperation());
		}	
	}

	public BooleanExpression getAlwaystrueReplaceOperation() {
		return alwaystrueReplaceOperation;
	}

	public QueryFilter<?> getFilter() {
		return filter;
	}
	
}