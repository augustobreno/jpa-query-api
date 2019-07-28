package br.com.vcg.query;

import java.util.List;

import org.junit.Test;

import br.com.vcg.query.api.QueryFilter;
import br.com.vcg.query.domain.QUf;
import br.com.vcg.query.domain.Uf;
import br.com.vcg.query.repository.QbeRepositoryImpl;

/**
 * Tests operations using pagination features of QueryFilter
 * @author augusto
 */
public class PaginationTest extends QbeTestBase {

	/**
	 * Try to fetch a page with null page parameter. Pagination must use default values.
	 */
	@SuppressWarnings("unchecked")
    @Test
	public void fetchPageWithNullParamsTest() {
		
		QbeRepositoryImpl qbe = new QbeRepositoryImpl(getEntityManager());
		String hql = "from " + Uf.class.getSimpleName();

		// both parameter are null
		List<Uf> ufsHQL = getQuerier().executeQuery(hql, 0, QueryFilter.PAGE_SIZE.intValue(), null);		

		QueryFilter<Uf> filter = new QueryFilter<Uf>(QUf.uf);
		filter.fetchPage(null, null);
		List<Uf> ufs = qbe.findAllBy(filter);
		
		assertContentEqual(ufsHQL, ufs);;
		
		// pageSize is null
		ufsHQL = getQuerier().executeQuery(hql, 10, QueryFilter.PAGE_SIZE.intValue(), null);		
		
		filter = new QueryFilter<Uf>(QUf.uf);
        filter.fetchPage(null, 2L);
        ufs = qbe.findAllBy(filter);
        assertContentEqual(ufsHQL, ufs);	
		
        // pageIndex is null
        ufsHQL = getQuerier().executeQuery(hql, 0, 5, null);
        
        filter = new QueryFilter<Uf>(QUf.uf);
        filter.fetchPage(5L, null);
        ufs = qbe.findAllBy(filter);
        assertContentEqual(ufsHQL, ufs);;   
        
	}
	
    /**
     * Try to fetch a page.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void fetchPageTest() {
        
        QbeRepositoryImpl qbe = new QbeRepositoryImpl(getEntityManager());
        String hql = "from " + Uf.class.getSimpleName();

        // both parameter are null
        List<Uf> ufsHQL = getQuerier().executeQuery(hql, 5, 5, null);        

        QueryFilter<Uf> filter = new QueryFilter<Uf>(QUf.uf);
        filter.fetchPage(5L, 2L);
        List<Uf> ufs = qbe.findAllBy(filter);
        
        assertContentEqual(ufsHQL, ufs);;
        
    }
    
    /**
     * Try to fetch a range with null page parameters.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void fetchRangeWithNullParamsTest() {
        
        QbeRepositoryImpl qbe = new QbeRepositoryImpl(getEntityManager());
        String hql = "from " + Uf.class.getSimpleName();

        // both parameter are null, range must not be applied
        List<Uf> ufsHQL = getQuerier().executeQuery(hql);        

        QueryFilter<Uf> filter = new QueryFilter<Uf>(QUf.uf);
        filter.fetchRange(null, null);
        List<Uf> ufs = qbe.findAllBy(filter);
        
        assertContentEqual(ufsHQL, ufs);;
        
        // limit is null, range must not me applied
        ufsHQL = getQuerier().executeQuery(hql);        
        
        filter = new QueryFilter<Uf>(QUf.uf);
        filter.fetchRange(0L, null);
        ufs = qbe.findAllBy(filter);
        assertContentEqual(ufsHQL, ufs);    
        
        // offset is null, 0 must be used as default
        ufsHQL = getQuerier().executeQuery(hql, 0, 5, null);
        
        filter = new QueryFilter<Uf>(QUf.uf);
        filter.fetchRange(null, 5L);
        ufs = qbe.findAllBy(filter);
        assertContentEqual(ufsHQL, ufs);;   
        
    }
    
    /**
     * Try to fetch a range of elements.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void fetchRangeTest() {
        
        QbeRepositoryImpl qbe = new QbeRepositoryImpl(getEntityManager());
        String hql = "from " + Uf.class.getSimpleName();

        // both parameter are null
        List<Uf> ufsHQL = getQuerier().executeQuery(hql, 8, 5, null);        

        QueryFilter<Uf> filter = new QueryFilter<Uf>(QUf.uf);
        filter.fetchRange(8L, 5L);
        List<Uf> ufs = qbe.findAllBy(filter);
        
        assertContentEqual(ufsHQL, ufs);;
        
    }	
    
}