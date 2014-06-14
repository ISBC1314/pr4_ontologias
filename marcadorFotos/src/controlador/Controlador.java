package controlador;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;

import es.ucm.fdi.gaia.ontobridge.OntoBridge;
import es.ucm.fdi.gaia.ontobridge.OntologyDocument;
import es.ucm.fdi.gaia.ontobridge.test.gui.PnlSelectInstance;

public class Controlador {

	private static final String URL_ONTOLOGIA = "http://www.owl-ontologies.com/Ontology1402519238.owl";
	
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
	
	public void addMarca(String foto, String relacion,String item) {
		ob.createOntProperty(foto,relacion,item);
		ob.save("Ejercicio2.owl");	
	}
	
	public void removeMarca(String foto, String relacion, String persona) {
		ob.deleteOntProperty(foto,relacion,persona);
		ob.save("Ejercicio2.owl");
	}
	
	public List<String> getPropiedad(String propiedad){
		List<String> lista = new ArrayList<String>();
		Iterator<String> it = ob.listInstances(propiedad);
		while (it.hasNext()){
			String nombre = it.next();
			lista.add(parser_nombre(nombre));	
		}
		return lista;
	}
	
	public List<String> getInfoFoto(String propiedad, String foto){
		List<String> personas = new ArrayList<String>();
		Iterator<String> iterador = ob.listPropertyValue(foto,propiedad);
		
		while (iterador.hasNext()){
			String nombre = iterador.next();
			nombre = parser_nombre(nombre);
			personas.add(nombre);	
		}
		return personas;
	}
	
	public List<String> getFotosFamilia(){
		List<String> fotos = new ArrayList<String>();
	    Iterator<String> iteradorFotos = ob.listInstances("Foto");
	    while (iteradorFotos.hasNext()){
	    	String foto = iteradorFotos.next();
	    	Iterator<String> iteradorPersonas =  ob.listPropertyValue(foto,"aparece_en");
	    	boolean esFamilia = false;
	    	int count = 0;
	    	while (iteradorPersonas.hasNext() && !esFamilia){
	    		String nombre_aux = parser_nombre(iteradorPersonas.next());
	    		if (ob.existsInstance(nombre_aux,"Familia"))
	    			count++;
	    		if (count >= 3)
	    			esFamilia = true;
	    	}
	    	
	    	if (esFamilia)
	    		fotos.add(parser_nombre(foto));
	    }	    
	    return fotos;
	}
	
	public List<String> getFotosRey(){
		List<String> fotos = new ArrayList<String>();
	    Iterator<String> iteradorFotos = ob.listInstances("Foto");
	    while (iteradorFotos.hasNext()){
	    	String foto = iteradorFotos.next();
	    	Iterator<String> iteradorPersonas =  ob.listPropertyValue(foto,"aparece	_en");
	    	boolean apereceRey = false;
	    	while (iteradorPersonas.hasNext() && !apereceRey){
	    		String nombre_aux = parser_nombre(iteradorPersonas.next());
	    		apereceRey = nombre_aux.equals("Juan_Carlos_I");
	    	}
	    	if (apereceRey)
	    		fotos.add(parser_nombre(foto));
	    }	    
	    return fotos;
	}
	
	public List<String> getFotosTrabajo(){
		List<String> fotos = new ArrayList<String>();
	    Iterator<String> iteradorFotos = ob.listInstances("Foto");
	    while (iteradorFotos.hasNext()){
	    	String foto = iteradorFotos.next();
	    	Iterator<String> iteradorPersonas =  ob.listPropertyValue(foto,"esta_en");
	    	boolean esTrabajo = false;
	    	while (iteradorPersonas.hasNext() && !esTrabajo){
	    		String lugar_aux = parser_nombre(iteradorPersonas.next());
	    		esTrabajo = (lugar_aux.equals("Despacho")) || (lugar_aux.equals("ActoOficial"));
	    	}
	    	if (esTrabajo)
	    		fotos.add(parser_nombre(foto));
	    }	    
	    return fotos;
	}
	
	public List<String> getFotosHermanos(){
		List<String> fotos = new ArrayList<String>();
	    Iterator<String> iteradorFotos = ob.listInstances("Foto");
	    while (iteradorFotos.hasNext()){
	    	String foto = iteradorFotos.next();
	    	Iterator<String> iteradorPersonas =  ob.listPropertyValue(foto,"aparece_en");
 	    	List<String> personasEnLaFoto = new ArrayList<String>();
 	    	while (iteradorPersonas.hasNext()){
 	    		String persona = parser_nombre(iteradorPersonas.next());
 	    		personasEnLaFoto.add(persona);
 	    		Iterator<String> iteradorHermanos = ob.listPropertyValue(persona,"es_hermanoa_de");
 	    		boolean hayHermano = false;
 	    		while (iteradorHermanos.hasNext() && !hayHermano){
 	    			String hermano = parser_nombre(iteradorHermanos.next());
 	    			hayHermano = hayHermano || personasEnLaFoto.contains(hermano);
 	    		}
 	    		if (hayHermano)
 	    			fotos.add(parser_nombre(foto));
 	    	}
 	    	
	    }
	    return fotos;
	}
	
	public List<String> getInfoBusqueda(int tipo){
		List<String> fotos = new ArrayList<String>();
		switch (tipo){
		case 0: return getFotosRey();
		case 1: return getFotosTrabajo();
		case 2: return getFotosHermanos();
		case 3: return getFotosFamilia();
		default: return fotos;
		}
	}
	
	private String parser_nombre(String string) {
		return string.substring(string.indexOf('#')+1);
	}
	
	public String getUrlFoto(String s) {
		String string = ob.listPropertyValue(s,"urlFoto").next();
		return string.substring(0,string.indexOf("^^"));
	}
	
}
