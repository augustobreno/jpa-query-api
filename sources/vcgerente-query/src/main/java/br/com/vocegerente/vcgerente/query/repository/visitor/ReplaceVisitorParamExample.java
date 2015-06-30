package br.com.vocegerente.vcgerente.query.repository.visitor;

import javax.annotation.Nullable;

import br.com.vocegerente.vcgerente.query.extension.ParamExample;
import br.com.vocegerente.vcgerente.query.repository.QbeContextProcessor;

import com.mysema.query.support.ReplaceVisitor;
import com.mysema.query.types.Expression;
import com.mysema.query.types.Operation;
import com.mysema.query.types.ParamExpression;

/**
 * Visitor para substituir os ParamExample genéricos (com tipagem em Object) para ParamExample com a tipagem correspondente do atributo do exemplo.
 * 
 * @author augusto
 *
 */
public class ReplaceVisitorParamExample extends ReplaceVisitor {
		private final VisitorContext myContext;

		public ReplaceVisitorParamExample(VisitorContext myContext) {
			this.myContext = myContext;
		}

		@Override
		public Expression<?> visit(Operation<?> expr, Void context) {
			
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
		public Expression<?> visit(ParamExpression<?> param, @Nullable Void context) {
			
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