package controlador;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;

import es.ucm.fdi.gaia.ontobridge.OntoBridge;
import es.ucm.fdi.gaia.ontobridge.OntologyDocument;
import es.ucm.fdi.gaia.ontobridge.test.gui.PnlSelectInstance;

public class Controlador {

	private static final String URL_ONTOLOGIA = "http://www.owl-ontologies.com/Ontology1402150125.owl";
	
	private OntoBridge ob;
	
	public Controlador(){
		ob = new OntoBridge();
		ob.initWithPelletReasoner();
		
		OntologyDocument mainOnto = new OntologyDocument(URL_ONTOLOGIA,"file:Ejercicio2.owl");
		ArrayList<OntologyDocument> subOntologies = new ArrayList<OntologyDocument>();	
		ob.loadOntology(mainOnto, subOntologies, false);
		
	}
	
	public JPanel damePanelOntologia() {
		return new PnlSelectInstance(ob);
	}
	
	public List<String> damePersonas() {
		List<String> personas = new ArrayList<String>();
		Iterator<String> iterador = ob.listInstances("Persona");
		
		while (iterador.hasNext()){
			String nombre = iterador.next();
			personas.add(nombre);	
		}
		
		return personas;
	}
	
	public List<String> dameLugares(){
		List<String> personas = new ArrayList<String>();
		Iterator<String> iterador = ob.listInstances("Lugares");
		
		while (iterador.hasNext()){
			String nombre = iterador.next();
			personas.add(nombre);	
		}
		
		return personas;
	}
	
	public void marcaFoto(String relacion,String item) {
		//ob.createOntProperty("Foto"+(numFotoActual+1),relacion,item);
		//ob.save("Ejercicio2.owl");
		
	}
	
}