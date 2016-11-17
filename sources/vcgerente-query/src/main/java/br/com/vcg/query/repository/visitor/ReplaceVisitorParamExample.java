package br.com.vcg.query.repository.visitor;

import javax.annotation.Nullable;

import br.com.vcg.query.extension.ParamExample;
import br.com.vcg.query.repository.QbeContextProcessor;

import com.querydsl.core.support.ReplaceVisitor;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Operation;
import com.querydsl.core.types.ParamExpression;

/**
 * Visitor para substituir os ParamExample genéricos (com tipagem em Object) para ParamExample com a tipagem correspondente do atributo do exemplo.
 * 
 * @author augusto
 *
 */
public class ReplaceVisitorParamExample<CONTEXT> extends ReplaceVisitor<CONTEXT> {
		private final VisitorContext myContext;

		public ReplaceVisitorParamExample(VisitorContext myContext) {
			this.myContext = myContext;
		}

		@Override
		public Expression<?> visit(Operation<?> expr, CONTEXT context) {
			
			/*
			 * Se a operação corrente foi agendada para ser descartada no visitor anterior,
			 * esta deverá ser substituída por uma operação genérica que não deverá afetar
			 * o resultado final da query.
			 */
			if (myContext.getPredicatesToDispose().contains(expr)) {
				return myContext.getAlwaystrueReplaceOperation();
			}
			
			// Para ter acesso ao ancestral do operador atual.
			myContext.setLastOperation(expr);
			
			return super.visit(expr, context);
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public Expression<?> visit(ParamExpression<?> param, @Nullable CONTEXT context) {
			
			/*
			 * Se o parâmetro for do tipo ParamExample, indica que o valor deverá ser extraído do objeto exemplo,
			 * e o parâmetro substituído por uma instância equivalente ao tipo encontrado neste valor.
			 * 
			 * Está considerado que se este Visitor chegou neste ponto, o atributo relativo no objeto exemplo
			 * estará preenchido (garantido pelo método anterior)
			 */
			if (param instanceof ParamExample) {
				Object value = QbeContextProcessor.getValueFromProperty(myContext, param);
				ParamExample paramExample = new ParamExample(value.getClass(), param);
				
				// substituindo o paramExample do contexto
				myContext.replace(paramExample);
				return paramExample;
			}
			return param;
		}
	}