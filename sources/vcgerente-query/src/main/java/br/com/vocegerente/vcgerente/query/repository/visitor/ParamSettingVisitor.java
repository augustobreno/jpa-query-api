package br.com.vocegerente.vcgerente.query.repository.visitor;

import java.util.List;

import br.com.vocegerente.vcgerente.query.extension.ParamExample;
import br.com.vocegerente.vcgerente.query.repository.QbeContextProcessor;

import com.mysema.query.types.Constant;
import com.mysema.query.types.Expression;
import com.mysema.query.types.FactoryExpression;
import com.mysema.query.types.Operation;
import com.mysema.query.types.ParamExpression;
import com.mysema.query.types.Path;
import com.mysema.query.types.SubQueryExpression;
import com.mysema.query.types.TemplateExpression;
import com.mysema.query.types.Visitor;

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