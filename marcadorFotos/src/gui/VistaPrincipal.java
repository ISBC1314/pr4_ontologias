package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import net.miginfocom.swing.MigLayout;

public class VistaPrincipal extends JFrame implements ActionListener{


	private static final int IMAGE_WIDTH = 565;
	private static final int IMAGE_HEIGHT = 340;
	
	private JButton JButton_fotosHermanos;
	private JButton JButton_fotosRománticas;
	private JButton JButton_fotosDeTrabajo;
	private JButton JButton_fotosFamiliares;
	private JButton JButton_fotosDelRey;
	private JButton JButton_marcarFoto;
	private JButton JButton_eliminarMarca;
	private JButton JButton_cargarFoto;
	
	private JPanel JPanel_marcar;
	private JPanel JPanel_buscar;
	private JPanel JPanel_infoFoto;
	private JPanel JPanel_foto;
	private JPanel JPanel_fotoImg;
	private JPanel JPanel_resultado1;
	private JPanel JPanel_infoResultado;
	private JPanel JPanel_OntoBrigde;
	private JPanel JPanel_marcarFoto;
	private JPanel JPanel_busqueda;
	private JPanel JPanel_resultado2;
	
	private JLabel JLabel_tituloFoto;
	private JLabel JLabel_foto;
	private JLabel JLabel_marcado;
	
	private JTabbedPane JTabbedPane_principal;
	private JScrollPane JScrollPane_resultado1;
	private JScrollPane JScrollPane_resultado2;
	
	private JComboBox<String> JComboBox_relacion;
	private JComboBox<String> JComboBox_item;
	
	public VistaPrincipal(JPanel panel) {
		this.setBounds(50,50,900,500);
		initialize(panel);
		this.setVisible(true);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	public void initialize(JPanel panelOnt){
		setTitle("Marcador y recuperador de fotos");
		getContentPane().setLayout(new MigLayout("","[300.00,grow]50[550.00,grow]","[]"));
		
		JPanel_OntoBrigde = panelOnt;
		JPanel_OntoBrigde.setBackground(Color.WHITE);
		JPanel_OntoBrigde.setBorder(new TitledBorder(new LineBorder(new Color(0, 0, 0)), "Ontology Structure", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		JPanel_OntoBrigde.setBounds(10, 11, 239, 420);
		getContentPane().add(JPanel_OntoBrigde,"cell 0 0");
		
		JTabbedPane_principal = new JTabbedPane(JTabbedPane.TOP);
		JTabbedPane_principal.setBounds(259, 11, 463, 344);
		getContentPane().add(JTabbedPane_principal,"cell 1 0");
		
		/******************* PANEL MARCAR *******************/
		
		JPanel_marcar = new JPanel();
		JTabbedPane_principal.addTab("Marcar", null, JPanel_marcar, null);
		JPanel_marcar.setLayout(new MigLayout("","[]","[300.00,grow]50[250.00,grow]"));
		
		JPanel_infoFoto = new JPanel();
		JPanel_marcar.add(JPanel_infoFoto,"cell 0 0");
		JPanel_infoFoto.setLayout(new MigLayout("","[300.00,grow]50[150.00,grow]","[]"));
		
		JPanel_foto = new JPanel();
		JPanel_infoFoto.add(JPanel_foto,"cell 0 0");
		JPanel_foto.setLayout(new MigLayout("","[]","[30.00,grow]10[200.00,grow]20[40.00,grow,center]"));
		
		JLabel_tituloFoto = new JLabel("Nombre foto");
		JPanel_foto.add(JLabel_tituloFoto,"cell 0 0");
		
		JPanel_fotoImg = new JPanel();
		JPanel_foto.add(JPanel_fotoImg,"cell 0 1");
		
		JButton_cargarFoto = new JButton("Cargar foto");
		JPanel_foto.add(JButton_cargarFoto,"cell 0 2");
		JButton_cargarFoto.addActionListener(this);
		
		JPanel_resultado1 = new JPanel();
		JPanel_infoFoto.add(JPanel_resultado1,"cell 1 0");
		JPanel_resultado1.setLayout(new MigLayout("","[]","[240.00,grow]20[40.00,grow,center]"));
		
		JPanel_infoResultado = new JPanel();
		JPanel_infoResultado.setBorder(new TitledBorder(null, "Resultados", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		JPanel_resultado1.add(JPanel_infoResultado,"cell 0 0");
		
		JScrollPane_resultado1 = new JScrollPane();
		JPanel_resultado1.add(JScrollPane_resultado1);
		
		JButton_eliminarMarca = new JButton("Eliminar");
		JPanel_resultado1.add(JButton_eliminarMarca,"cell 0 1");
		JButton_eliminarMarca.addActionListener(this);
		
		JPanel_marcarFoto = new JPanel();
		JPanel_marcar.add(JPanel_marcarFoto,"cell 0 1");
		JPanel_marcarFoto.setLayout(new MigLayout("","[]30[]20[]20[]","[]"));
		
		JComboBox_relacion = new JComboBox<String>();
		JPanel_marcarFoto.add(JComboBox_relacion,"cell 1 0");
		
		JComboBox_item = new JComboBox<String>();
		JPanel_marcarFoto.add(JComboBox_item,"cell 2 0");
		
		JButton_marcarFoto = new JButton("Marcar foto");
		JPanel_marcarFoto.add(JButton_marcarFoto,"cell 3 0");
		
		JLabel_marcado = new JLabel("Opciones marcado:");
		JPanel_marcarFoto.add(JLabel_marcado,"cell 0 0");
		
		/******************* PANEL BUSCAR ********************/
		
		JPanel_buscar = new JPanel();
		JTabbedPane_principal.addTab("Buscar", null, JPanel_buscar, null);
		JPanel_buscar.setLayout(new MigLayout("","[350.00,grow]50[0.00,grow]","[]"));
		
		JPanel_busqueda  = new JPanel();
		JPanel_buscar.add(JPanel_busqueda,"cell 0 0");
		JPanel_busqueda.setLayout(new MigLayout("","[150.00,grow]50[150.00,grow]","[100.00,grow]50[100.00,grow]"));
		
		JButton_fotosDelRey = new JButton("Fotos del rey");
		JButton_fotosDelRey.addActionListener(this);
		JPanel_busqueda.add(JButton_fotosDelRey,"cell 0 0");
		
		JButton_fotosHermanos = new JButton("Fotos de hermanos");
		JButton_fotosHermanos.addActionListener(this);
		JPanel_busqueda.add(JButton_fotosHermanos,"cell 0 1");
		
		JButton_fotosDeTrabajo = new JButton("Fotos de trabajo");
		JButton_fotosDeTrabajo.addActionListener(this);
		JPanel_busqueda.add(JButton_fotosDeTrabajo,"cell 1 0");
		
		JButton_fotosFamiliares = new JButton("Fotos familiares");
		JButton_fotosFamiliares.addActionListener(this);
		JPanel_busqueda.add(JButton_fotosFamiliares,"cell 1 1");
		
		JPanel_resultado2 = new JPanel();
		JPanel_buscar.add(JPanel_resultado2,"cell 1 0");
		JScrollPane_resultado2 = new JScrollPane();
		JPanel_resultado2.add(JScrollPane_resultado2);
	}
	
	public void mostrarFoto(String url){
			ImageIcon icono = new ImageIcon(url);
			icono = new ImageIcon(icono.getImage().getScaledInstance(IMAGE_WIDTH, IMAGE_HEIGHT, Image.SCALE_DEFAULT));
			JLabel_foto.setIcon(icono);
	}
	/*
	public static void main(String[] args){
				try {
					
					VistaPrincipal window = new VistaPrincipal();
					window.setVisible(true);
					
				} catch (Exception e) {
					e.printStackTrace();
				}
	}
	*/
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

}
