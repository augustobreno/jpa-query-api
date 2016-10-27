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
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.core.types.TemplateExpression;
import com.querydsl.core.types.Visitor;

/**
 * Seta os parâmetros que estão no contexto no jpaQuery.
 */
public final class ParamSettingVisitor implements Visitor<Void, VisitorContext> {
      
    public static final ParamSettingVisitor DEFAULT = new ParamSettingVisitor();
    
    private ParamSettingVisitor() {}

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
        return visit(expr.getArgs(), context);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
    public Void visit(ParamExpression param, VisitorContext context) {
    	
    	if (ParamExample.class.isAssignableFrom(param.getClass())) {
			
			Object value = QbeContextProcessor.getValueFromProperty(context, param);
			if (value != null) {
				context.getFilter().set(param, value);
				context.getCacheCustomPredicates().add(context.getPath(param).getMetadata().getName());
			}
			
		}
        return null;
    }

    @Override
    public Void visit(Path<?> expr, VisitorContext context) {
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