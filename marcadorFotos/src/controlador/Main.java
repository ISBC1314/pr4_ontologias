package controlador;

import gui.VistaPrincipal;

public class Main {
	
	public static void main (String[] args)
	{

		//Crear el objeto que implementa la aplicacion CBR
		Controlador controlador = new Controlador();
		VistaPrincipal vistaPrincipal = new VistaPrincipal(controlador.damePanelOntologia(),controlador);

	}

}