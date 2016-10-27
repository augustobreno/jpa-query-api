package br.com.vcg.query.repository.visitor;

import java.util.List;

import br.com.vcg.query.extension.ParamExample;
import br.com.vcg.query.repository.QbeContextProcessor;

import com.querydsl.core.types.Constant;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.FactoryExpression;
import com.querydsl.core.types.Operation;
import com.querydsl.core.types.ParamExpression;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.PredicateOperation;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.core.types.TemplateExpression;
import com.querydsl.core.types.Visitor;

/**
 * Extrai os parâmetros encontrados em uma (sub)árvore de expressões, armazenando-os no contexto.
 */
public final class ParamExtractorVisitor implements Visitor<Void, VisitorContext> {
      
    public static final ParamExtractorVisitor DEFAULT = new ParamExtractorVisitor();
    
    private ParamExtractorVisitor() {}

    @Override
    public Void visit(Constant<?> expr, VisitorContext context) {
        return null;
    }

    @Override
    public Void visit(FactoryExpression<?> expr, VisitorContext context) {
        return visit(expr.getArgs(), context);
    }

    @Override
    public Void visit(Operation<?> expr, VisitorContext context) {
    	// ordem de execução: 1
    	/*
    	 * Armazenado um Predicado para que possa ser identificado e descartado
    	 * caso o valor encontrado no objeto exemplo seja =null. 
    	 */
    	if (expr instanceof PredicateOperation) {
			context.setLastOperation((PredicateOperation) expr);
		}
        return visit(expr.getArgs(), context);
    }

    @Override
    public Void visit(ParamExpression<?> param, VisitorContext context) {
    	// ordem de execução: 3    	
   		context.set(param);
   		
    	/*
    	 * Caso este parâmetro seja do tipo ParamExample, e o path deste contexto 
    	 * se refira a um atributo cujo valor =null no objeto exemplo, 
    	 * esta última operação deverá ser despresada.
    	 */
   		if (param instanceof ParamExample) {
	    	Object value = QbeContextProcessor.getValueFromProperty(context, param);
	    	if (value == null) {
	    		context.scheduleLastOperationForDisposal();
	    	}   		
   		}	
   		
        return null;
    }

    @Override
    public Void visit(Path<?> path, VisitorContext context) {
    	// ordem de execução: 2    	
    	context.set(path);
        return null;
    }


	@Override
    public Void visit(SubQueryExpression<?> expr, VisitorContext context) {
        return null;
    }

    @Override
    public Void visit(TemplateExpression<?> expr, VisitorContext context) {
        return null;
    }

    private Void visit(List<?> exprs, VisitorContext context) {
        for (Object e : exprs) {
            if (e instanceof Expression) {
                ((Expression<?>)e).accept(this, context);
            }            
        }
        return null;
    }

}