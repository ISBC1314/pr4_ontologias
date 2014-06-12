package org.mindswap.pellet.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.io.BufferedReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.LinkedList;

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

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.graph.compose.MultiUnion;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.mem.GraphMem;
import com.hp.hpl.jena.rdf.arp.JenaReader;
import com.hp.hpl.jena.ontology.OntModel;
import org.mindswap.pellet.jena.PelletReasonerFactory;

// OK, so the plan is to create one jena union model, which will have
// all the rdf data.  We'll handle puts, deletes, and so on to this.
// We'll also have a pool of pellets that is reset every time the
// data changes.  We answer queries from that pool.  Eventually, we
// may try to serve up old results from the old pellet copies while
// we refill.

public class Spas2 extends HttpServlet {
    //Map contexts = null;
    Set pelletpool = null;
    BlockingQueue q = null;
    public void init(ServletConfig config) {
        //model = new OntModel();
        //contexts = new HashMap();
        pelletpool = new HashSet();
        q = new LinkedBlockingQueue();
        // does spigot need to know about the contexts also?
        Spigot spigot = new Spigot(pelletpool, q);
        spigot.start();
    }
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        OutputStream out = response.getOutputStream();
        //if (request.getParameterMap().containsKey("context")) {
        //    String context = request.getParameter("context");
        //    Model tempmodel = (Model)contexts.get(context);
        //    tempmodel.write(out);
        //} else {
        //    //model.writeAll(out, null, "");
        //}
    }
    public void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        BufferedReader body = request.getReader();
        if (request.getParameterMap().containsKey("context")) {
            String context = request.getParameter("context");
            Model tempmodel = ModelFactory.createDefaultModel();
            tempmodel.read(body, context);
            q.add(new Command("PUT", tempmodel, context));
            //contexts.put(context, tempmodel);
            //model.addSubModel(tempmodel);
        } else {
            //model.read(body, "");
        }
    }
    public void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getParameterMap().containsKey("context")) {
            String context = request.getParameter("context");
            q.add(new Command("DELETE", null, context));
            //Model tempmodel = (Model)contexts.get(context);
            //model.removeSubModel(tempmodel);
        } else {
            //model = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC );
        }
    }
}

class Spigot extends Thread {
    Map contexts = null;
    Set pelletpool = null;
    Queue q = null;
    public Spigot(Set pp, Queue commands) {
        contexts = new HashMap();
        pelletpool = pp;
        q = commands;
        Model tempmodel = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC );
        pelletpool.add(tempmodel);
    }
    public void run() {
        while(q.peek() == null) {
            if(pelletpool.size() >= 10) {
                sleep(1);
            }
        }
        OntModel model = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC );
        while(q.peek() != null) {
            Command command = (Command)q.remove();
            // do whatever we have to do to the context list
            if(command.command == "DELETE") {
                contexts.remove(command.context);
            } else if(command.command == "PUT") {
                contexts.set(command.context, command.model);
            }
        }
        // now need to iterate over the contexts, adding them to the
        // model
        i = contexts.iterator();
        while(i.hasNext()) {
            context = (String)i.next();
            model.addSubGraph(contexts.get(context), false);
        }
        // need to somehow tell the model to inference now
        model.rebind();
        pelletpool.add(model);
        
        // When commands appear, this needs to take a model out of the
        // pool, clear the pool, modify the model
        // Is the last statement true?  Maybe we should just start
        // over and create a new model.  
        // If no commands are waiting, this should create more models,
        // up to say 10 of them.
        //while(command = q.take()) {
        //    model = (OntModel)pelletpool.iterator.next();
        //    pelletpool.clear();
        //}
    }
}

// 3 possibilities:
// command="PUT", context="<uri>", model=<model>
// command="DELETE", context="<uri>", model=null
// command="RESET", context=null, model=null
class Command {
    public String command;
    public Model model;
    public String context;
    public Command(String cmd, Model m, String con) {
        command = cmd;
        model = m;
        context = con;
    }
}
//public class Spas2 extends HttpServlet {
//    MultiUnion model = null;
//    HashMap contexts = null;
//    public void init(ServletConfig config) {
//        model = new MultiUnion();
//        contexts = new HashMap();
//        //Model model = ModelFactory.createDefaultModel();
//    }
//    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        // still have to write this
//        //OutputStream out = response.getOutputStream();
//        //Model m = ModelFactory.createDefaultModel();
//        //RDFWriter writer = m.getWriter();
//        //writer.write(model, out, "");
//        //model.write(out);
//    }
//    public void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        BufferedReader body = request.getReader();
//        String context = request.getParameter("context");
//        JenaReader jr = new JenaReader();
//        if (request.getParameterMap().containsKey("context")) {
//            //Model tempmodel = ModelFactory.createDefaultModel();
//            Graph tempmodel = new GraphMem();
//            //tempmodel.read(body, context);
//            jr.read(tempmodel, body, context);
//            contexts.put(context, tempmodel);
//            model.addGraph((Graph)tempmodel);
//        } else {
//            //model.read(body, context);
//        }
//    }
//    public void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        if (request.getParameterMap().containsKey("context")) {
//            String context = request.getParameter("context");
//            //Model tempmodel = (Model)contexts.get(context);
//            Graph tempmodel = (Graph)contexts.get(context);
//            model.removeGraph((Graph)tempmodel);
//        } else {
//            //model = new MultiUnion();
//        }
//    }
//}
