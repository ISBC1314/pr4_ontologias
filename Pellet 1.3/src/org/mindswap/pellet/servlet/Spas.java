/*
 * Created on Jul 13, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.mindswap.pellet.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mindswap.pellet.servlet.functions.IsCanonical;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.query.function.FunctionRegistry;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * @author ronwalf
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Spas extends HttpServlet {

	DatasetManager dataManager = null;
	
	public void init(ServletConfig config) {
		int managerSize;
		try {
			managerSize = Integer.parseInt(config.getInitParameter("dataset.cachesize"));
		} catch (NumberFormatException e) {
			managerSize = 10;
		}
		dataManager = new DatasetManager(managerSize);
		
		String backgroundList = config.getInitParameter("dataset.default.graphs");
		String namedList = config.getInitParameter("dataset.default.named");
		Set backgrounds = new HashSet();
		Set named = new HashSet();
		
		if (backgroundList != null && backgroundList.trim().length() > 0) {
			backgrounds = new HashSet(Arrays.asList(backgroundList.split("\\s+")));
		}
		if (namedList != null && namedList.trim().length() > 0) {
			named = new HashSet(Arrays.asList(namedList.split(" ")));
		}
		System.out.println("Setting default dataset: "+backgrounds+", "+named);
		dataManager.setDefaultDataset(backgrounds, named);
		
		// Register some sparql functions...
		FunctionRegistry registry = FunctionRegistry.get();
		registry.put("http://www.mindswap.org/2005/sparql/reasoning#isCanonical",
				IsCanonical.class);
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (request.getParameterMap().containsKey("reset")) {
			dataManager.reset();
		}
		if (request.getParameterMap().containsKey("query")) {
			doQuery(request, response);
		}
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (request.getParameterMap().containsKey("reset")) {
			dataManager.reset();
		}
		if (request.getParameterMap().containsKey("query")) {
			doQuery(request, response);
		}
	}
	
	public void doQuery(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		String queryString = request.getParameter("query");
		Query query = QueryFactory.create(queryString);
		
		long startTime = System.currentTimeMillis();
		Dataset dataset = getDataset(request, query);
		QueryExecution qexec = QueryExecutionFactory.create(query, dataset) ;
		
		long dataTime = System.currentTimeMillis();
		setContentType(request, response, query);
		if (query.isAskType()) {
			execAsk(request, response, qexec);
		} else if (query.isConstructType()) {
			outputModel(request, response, qexec.execConstruct());
		} else if (query.isDescribeType()) {
			outputModel(request, response, qexec.execDescribe());
		} else if (query.isSelectType()) {
			execSelect(request, response, query, qexec);
		} else {
			response.setStatus(400);
			response.setContentType("text/plain");
			Writer out = response.getWriter();
			out.write("Unknown type for query:\n"+queryString);
		}
       	
		long endTime = System.currentTimeMillis();

		System.out.println("Total time: "+(endTime-startTime));
		System.out.println("Dataset time: "+(dataTime-startTime));
		System.out.println("Query time: "+(endTime-dataTime));
		System.out.println(""+IsCanonical.cacheHits+" cache hits, "+IsCanonical.cacheMisses+" cache misses");
		IsCanonical.cacheHits = 0;
		IsCanonical.cacheMisses = 0;
		
		dataManager.returnDataset(dataset);
	}
	
	public void execAsk(HttpServletRequest request, HttpServletResponse response, QueryExecution qexec) throws IOException {
		boolean result = qexec.execAsk();
		OutputStream out = response.getOutputStream();
		
		if (request.getParameterMap().containsKey("text")) {
			ResultSetFormatter.out(out, result);
		} else {
			ResultSetFormatter.outputAsXML(out, result);
		}
	}
	
	public void execSelect(HttpServletRequest request, HttpServletResponse response, Query query, QueryExecution qexec) throws IOException {
		ResultSet results = qexec.execSelect() ;

		OutputStream out = response.getOutputStream();
		
		if (request.getParameterMap().containsKey("text")) {
			ResultSetFormatter.out(out, results, query);
		} else {
			ResultSetFormatter.outputAsXML(out, results); 
		};
	}
	
	public Dataset getDataset(HttpServletRequest request, Query query) {
		
		Set defaultGraphs = new HashSet(query.getGraphURIs());
		Set namedGraphs = new HashSet(query.getNamedGraphURIs());
		
		String[] defaultList = request.getParameterValues("default-graph-uri");
		String[] namedList = request.getParameterValues("named-graph-uri");
			
		if (defaultList != null) {
			defaultGraphs = new HashSet(Arrays.asList(defaultList));
		}
		if (namedList != null) {
			namedGraphs = new HashSet(Arrays.asList(namedList));
		}
		
		return dataManager.getDataset(defaultGraphs, namedGraphs);
	}
	
	public void outputModel(HttpServletRequest request, HttpServletResponse response, Model model) throws IOException {
		OutputStream out = response.getOutputStream();
		model.write(out);
	}

	public void setContentType(HttpServletRequest request, HttpServletResponse response, Query query) throws IOException {
		if (request.getParameterMap().containsKey("text")) {
			response.setContentType("text/plain");
		} else if (query.isAskType() || query.isSelectType()) {
			response.setContentType("application/sparql-results+xml");
		} else if (query.isConstructType() || query.isDescribeType()) {
			response.setContentType("application/rdf+xml");
		}
	}
}
