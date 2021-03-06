package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import net.miginfocom.swing.MigLayout;
import controlador.Controlador;

@SuppressWarnings("serial")
public class VistaPrincipal extends JFrame implements ActionListener{

	private Controlador controlador;
	
	private static final int IMAGE_WIDTH = 280;
	private static final int IMAGE_HEIGHT = 250;
	
	private JButton JButton_fotosHermanos;
	private JButton JButton_fotosDeTrabajo;
	private JButton JButton_fotosFamiliares;
	private JButton JButton_fotosDelRey;
	private JButton JButton_marcarFoto;
	private JButton JButton_eliminarMarca;
	private JButton JButton_busquedaPersonal;
	
	private JPanel JPanel_marcar;
	private JPanel JPanel_buscar;
	private JPanel JPanel_infoFoto;
	private JPanel JPanel_foto;
	private JPanel JPanel_resultado1;
	private JPanel JPanel_infoResultado;
	private JPanel JPanel_OntoBrigde;
	private JPanel JPanel_marcarFoto;
	private JPanel JPanel_busqueda;
	private JPanel JPanel_resultado2;
	private JPanel JPanel_busquedaPersonal;
	
	private JLabel JLabel_tituloFoto;
	private JLabel JLabel_foto;
	private JLabel JLabel_marcado;
	private JLabel JLabel_cargoReal;
	private JLabel JLabel_persona;
	private JLabel JLabel_lugar;
	
	private JTabbedPane JTabbedPane_principal;
	private JScrollPane JScrollPane_resultado1;
	private JScrollPane JScrollPane_resultado2;
	
	private JComboBox<String> JComboBox_relacion;
	private JComboBox<String> JComboBox_item;
	private JComboBox<String> JComboBox_fotos;
	private JComboBox<String> JComboBox_tipoMarca;
	private JComboBox<String> JComboBox_cargoReal;
	private JComboBox<String> JComboBox_lugar;
	private JComboBox<String> JComboBox_persona;
	
	
	private JList<String> JList_resultado1;
	private JList<ImageIcon> JList_resultado2;
	
	public VistaPrincipal(JPanel panel,Controlador controlador) {
		this.controlador = controlador;
		this.setBounds(50,50,900,500);
		initialize(panel);
		cargarComboBoxFotos();
		cargarComboBoxItems();
		cargarComboBoxBusqueda();
		cargarResultado(JTabbedPane_principal.getSelectedIndex(), JComboBox_tipoMarca.getSelectedIndex());
		this.setVisible(true);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setResizable(false);
	}
	
	public void initialize(JPanel panelOnt){
		setTitle("Marcador y recuperador de fotos");
		getContentPane().setLayout(new MigLayout("","[300.00,grow]10[600.00,grow]","[]"));
		
		JPanel_OntoBrigde = panelOnt;
		JPanel_OntoBrigde.setBackground(Color.WHITE);
		JPanel_OntoBrigde.setBorder(new TitledBorder(new LineBorder(new Color(0, 0, 0)), "Ontology Structure", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		//JPanel_OntoBrigde.setBounds(10, 11, 239, 420);
		getContentPane().add(JPanel_OntoBrigde,"cell 0 0");
		
		JTabbedPane_principal = new JTabbedPane(JTabbedPane.TOP);
		JTabbedPane_principal.setPreferredSize(new Dimension(600,400));
		getContentPane().add(JTabbedPane_principal,"cell 1 0");
		
		/******************* PANEL MARCAR *******************/
		
		JPanel_marcar = new JPanel();
		JTabbedPane_principal.addTab("Marcar", null, JPanel_marcar, null);
		JPanel_marcar.setLayout(new MigLayout("","[]","[300.00,grow]20[250.00,grow]"));
		
		JPanel_infoFoto = new JPanel();
		JPanel_marcar.add(JPanel_infoFoto,"cell 0 0");
		JPanel_infoFoto.setLayout(new MigLayout("","[300.00,grow]50[150.00,grow]","[]"));
		
		JPanel_foto = new JPanel();
		JPanel_infoFoto.add(JPanel_foto,"cell 0 0");
		JPanel_foto.setLayout(new MigLayout("","[]","[30.00,grow]10[200.00,grow]20[40.00,grow,center]"));
		
		JLabel_tituloFoto = new JLabel("Nombre foto");
		JPanel_foto.add(JLabel_tituloFoto,"cell 0 0");
		
		JLabel_foto = new JLabel();
		JPanel_foto.add(JLabel_foto,"cell 0 1");
		
		JComboBox_fotos = new JComboBox<String>();
		JPanel_foto.add(JComboBox_fotos,"cell 0 2");
		JComboBox_fotos.addActionListener(this);
		
		//Panel resultado
		JPanel_resultado1 = new JPanel();
		JPanel_infoFoto.add(JPanel_resultado1,"cell 1 0");
		JPanel_resultado1.setLayout(new MigLayout("","[]","[200.00,grow]20[30.00,grow,center]10[40.00,grow,center]"));
		
		JPanel_infoResultado = new JPanel();
		JPanel_infoResultado.setBorder(new TitledBorder(null, "Resultados", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		JPanel_resultado1.add(JPanel_infoResultado,"cell 0 0");
		
		JScrollPane_resultado1 = new JScrollPane();
		JPanel_infoResultado.add(JScrollPane_resultado1);
		JScrollPane_resultado1.setPreferredSize(new Dimension(100,150));
		
		JList_resultado1 = new JList<String>();
		JList_resultado1.setForeground(Color.BLACK);
		JList_resultado1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JList_resultado1.setModel(new DefaultListModel<String>());
		JScrollPane_resultado1.setViewportView(JList_resultado1);
		
		JComboBox_tipoMarca = new JComboBox<String>(new String[] {"aparece_en" ,"esta_en"});
		JPanel_resultado1.add(JComboBox_tipoMarca,"cell 0 1");
		JComboBox_tipoMarca.addActionListener(this);
		
		JButton_eliminarMarca = new JButton("Eliminar");
		JPanel_resultado1.add(JButton_eliminarMarca,"cell 0 2");
		JButton_eliminarMarca.addActionListener(this);
		
		//Panal marcar
		JPanel_marcarFoto = new JPanel();
		JPanel_marcar.add(JPanel_marcarFoto,"cell 0 1");
		JPanel_marcarFoto.setLayout(new MigLayout("","[]30[]20[]20[]","[]"));
		
		JComboBox_relacion = new JComboBox<String>(new String[]{"aparece_en","esta_en"});
		JPanel_marcarFoto.add(JComboBox_relacion,"cell 1 0");
		JComboBox_relacion.addActionListener(this);
		
		JComboBox_item = new JComboBox<String>();
		JPanel_marcarFoto.add(JComboBox_item,"cell 2 0");
		
		JButton_marcarFoto = new JButton("Marcar foto");
		JPanel_marcarFoto.add(JButton_marcarFoto,"cell 3 0");
		JButton_marcarFoto.addActionListener(this);
		
		JLabel_marcado = new JLabel("Opciones marcado:");
		JPanel_marcarFoto.add(JLabel_marcado,"cell 0 0");
		
		/******************* PANEL BUSCAR ********************/
		
		JPanel_buscar = new JPanel();
		JTabbedPane_principal.addTab("Buscar", null, JPanel_buscar, null);
		JPanel_buscar.setLayout(new MigLayout("","[200.00,grow]20[180.00,grow]","[]"));
		
		JPanel_busqueda  = new JPanel();
		JPanel_buscar.add(JPanel_busqueda,"cell 0 0");
		JPanel_busqueda.setLayout(new MigLayout("","[200.00,grow]","[50.00,grow]10[50.00,grow]10[50.00,grow]10[50.00,grow]10[100.00,grow][]"));
		JPanel_busqueda.setPreferredSize(new Dimension(170,400));
		
		JButton_fotosDelRey = new JButton("Fotos del rey");
		JButton_fotosDelRey.addActionListener(this);
		JPanel_busqueda.add(JButton_fotosDelRey,"cell 0 0");
		
		JButton_fotosHermanos = new JButton("Fotos de hermanos");
		JButton_fotosHermanos.addActionListener(this);
		JPanel_busqueda.add(JButton_fotosHermanos,"cell 0 1");
		
		JButton_fotosDeTrabajo = new JButton("Fotos de trabajo");
		JButton_fotosDeTrabajo.addActionListener(this);
		JPanel_busqueda.add(JButton_fotosDeTrabajo,"cell 0 2");
		
		JButton_fotosFamiliares = new JButton("Fotos familiares");
		JButton_fotosFamiliares.addActionListener(this);
		JPanel_busqueda.add(JButton_fotosFamiliares,"cell 0 3");
		
		JPanel_busquedaPersonal = new JPanel();
		JPanel_busqueda.add(JPanel_busquedaPersonal,"cell 0 4");
		JPanel_busquedaPersonal.setBorder(new TitledBorder(new LineBorder(new Color(0, 0, 0)), "Busqueda personal:", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		JPanel_busquedaPersonal.setLayout(new MigLayout("","[100.00,grow]10[40.00,grow]","[30.00,grow]10[30.00,grow]10[30.00,grow]"));
		
		JPanel JPanel_cargoReal = new JPanel();
		JPanel_cargoReal.setLayout(new MigLayout("","[]","[]5[]"));
		JPanel_busquedaPersonal.add(JPanel_cargoReal, "cell 0 0");
		JLabel_cargoReal = new JLabel("Cargo real:");
		JLabel_cargoReal.setHorizontalAlignment(SwingConstants.CENTER);
		JLabel_cargoReal.setFont(new Font("Tahoma", Font.BOLD, 12));
		//JLabel_cargoReal.setForeground(new Color(202,255,112));
		JPanel_cargoReal.add(JLabel_cargoReal,"cell 0 0");
		JComboBox_cargoReal = new JComboBox<String>();
		JPanel_cargoReal.add(JComboBox_cargoReal,"cell 0 1");
		
		JPanel JPanel_persona = new JPanel();
		JPanel_persona.setLayout(new MigLayout("","[]","[]5[]"));
		JPanel_busquedaPersonal.add(JPanel_persona, "cell 0 1");
		JLabel_persona = new JLabel("Nombre persona:");
		JLabel_persona.setHorizontalAlignment(SwingConstants.CENTER);
		JLabel_persona.setFont(new Font("Tahoma", Font.BOLD, 12));
		//JLabel_persona.setForeground(new Color(202,255,112));
		JPanel_persona.add(JLabel_persona,"cell 0 0");
		JComboBox_persona = new JComboBox<String>();
		JPanel_persona.add(JComboBox_persona,"cell 0 1");
		
		JPanel JPanel_lugar = new JPanel();
		JPanel_lugar.setLayout(new MigLayout("","[]","[]5[]"));
		JPanel_busquedaPersonal.add(JPanel_lugar, "cell 0 2");
		JLabel_lugar = new JLabel("Lugar:");
		JLabel_lugar.setHorizontalAlignment(SwingConstants.CENTER);
		JLabel_lugar.setFont(new Font("Tahoma", Font.BOLD, 12));
		//JLabel_lugar.setForeground(new Color(202,255,112));
		JPanel_lugar.add(JLabel_lugar,"cell 0 0");
		JComboBox_lugar = new JComboBox<String>();
		JPanel_lugar.add(JComboBox_lugar,"cell 0 1");
		
		JButton_busquedaPersonal = new JButton("Buscar");
		JPanel_busquedaPersonal.add(JButton_busquedaPersonal,"cell 1 0 1 2");
		JButton_busquedaPersonal.addActionListener(this);
		
		JPanel_resultado2 = new JPanel();
		JPanel_buscar.add(JPanel_resultado2,"cell 1 0");
		JScrollPane_resultado2 = new JScrollPane();
		JScrollPane_resultado2.setPreferredSize(new Dimension(200,400));
		JPanel_resultado2.add(JScrollPane_resultado2);
		
		JList_resultado2 = new JList<ImageIcon>();
		JList_resultado2.setForeground(Color.BLACK);
		JList_resultado2.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JList_resultado2.setModel(new DefaultListModel<ImageIcon>());
		JList_resultado2.setCellRenderer(new CellRendererFotos());
		JScrollPane_resultado2.setViewportView(JList_resultado2);
	}
	
	public void cargarComboBoxFotos(){
		List<String> items;
		items = controlador.getPropiedad("Foto");
		for (int i=0;i<items.size();i++)
			JComboBox_fotos.addItem(items.get(i));
	}
	
	public void cargarComboBoxItems(){
		List<String> items;
		if (JComboBox_relacion.getSelectedIndex() == 0)
			items = controlador.getPropiedad("Familia");
		else
			items = controlador.getPropiedad("Lugar");
		
		JComboBox_item.removeAllItems();
		for (int i=0;i<items.size();i++)
			JComboBox_item.addItem(items.get(i));
		
		SwingUtilities.updateComponentTreeUI(this);
	}
	
	public void cargarComboBoxBusqueda(){
		List<String> items_personas = controlador.getPropiedad("Familia");
		List<String> items_lugares = controlador.getPropiedad("Lugar");
		List<String> items_cargos = controlador.getPropiedad("TituloReal");
		
		JComboBox_persona.addItem("Indiferente");
		JComboBox_lugar.addItem("Indiferente");
		JComboBox_cargoReal.addItem("Indiferente");
		
		for (int i=0;i<items_personas.size();i++)
			JComboBox_persona.addItem(items_personas.get(i));
		for (int i=0;i<items_lugares.size();i++)
			JComboBox_lugar.addItem(items_lugares.get(i));
		for (int i=0;i<items_cargos.size();i++)
			JComboBox_cargoReal.addItem(items_cargos.get(i));
		
	}
	
	public void busquedaPersonal(){
		String lugar = "";
		String persona = "";
		String cargo = "";
		if (JComboBox_cargoReal.getSelectedIndex() != 0)
			cargo = (String)JComboBox_cargoReal.getSelectedItem();
		if (JComboBox_lugar.getSelectedIndex() != 0)
			lugar = (String)JComboBox_lugar.getSelectedItem();
		if (JComboBox_persona.getSelectedIndex() != 0)
			persona = (String)JComboBox_persona.getSelectedItem();
		
		((DefaultListModel<ImageIcon>)JList_resultado2.getModel()).removeAllElements();
		List<String> items = controlador.getInfoPersonal(cargo,persona,lugar);
		for (int i=0;i<items.size();i++){	
			String nombre = items.get(i);
			ImageIcon icono = new ImageIcon(controlador.getUrlFoto(nombre));
			((DefaultListModel<ImageIcon>)JList_resultado2.getModel()).addElement(icono);
		}
	}
	
	public void cargarResultado(int panel, int tipo){
		if (panel == 0){
			((DefaultListModel<String>)JList_resultado1.getModel()).removeAllElements();
			List<String> items = controlador.getInfoFoto((String)JComboBox_tipoMarca.getSelectedItem(), (String)JComboBox_fotos.getSelectedItem());
			for (int i=0;i<items.size();i++){	
				String nombre = items.get(i);
				((DefaultListModel<String>)JList_resultado1.getModel()).addElement(nombre);
			}
		}
		else{
			((DefaultListModel<ImageIcon>)JList_resultado2.getModel()).removeAllElements();
			List<String> items = controlador.getInfoBusqueda(tipo);
			for (int i=0;i<items.size();i++){	
				String nombre = items.get(i);
				ImageIcon icono = new ImageIcon(controlador.getUrlFoto(nombre));
				((DefaultListModel<ImageIcon>)JList_resultado2.getModel()).addElement(icono);
			}
		}
		SwingUtilities.updateComponentTreeUI(this);
	}
	
	public void marcarFoto(){
		controlador.addMarca((String)JComboBox_fotos.getSelectedItem(),(String)JComboBox_relacion.getSelectedItem(),(String)JComboBox_item.getSelectedItem());
		cargarResultado(JTabbedPane_principal.getSelectedIndex(),JComboBox_tipoMarca.getSelectedIndex());
	}
	
	public void eliminarMarca(){
		int index = JList_resultado1.getSelectedIndex();
		if (index > -1)
			controlador.removeMarca((String)JComboBox_fotos.getSelectedItem(),(String)JComboBox_tipoMarca.getSelectedItem(),(String)JList_resultado1.getSelectedValue());
		cargarResultado(JTabbedPane_principal.getSelectedIndex(),JComboBox_tipoMarca.getSelectedIndex());
	}
	
	public void mostrarFoto(String url){
		System.out.println(controlador.getUrlFoto(url));
		ImageIcon icono = new ImageIcon(controlador.getUrlFoto(url));
		icono = new ImageIcon(icono.getImage().getScaledInstance(IMAGE_WIDTH, IMAGE_HEIGHT, Image.SCALE_DEFAULT));
		JLabel_foto.setIcon(icono);
		cargarResultado(JTabbedPane_principal.getSelectedIndex(),JComboBox_tipoMarca.getSelectedIndex());
	}
	
	public void actionPerformed(ActionEvent arg0) {
		Object fuente = arg0.getSource();
		if (fuente.equals(JComboBox_fotos))
			mostrarFoto((String)JComboBox_fotos.getSelectedItem());
		else if (fuente.equals(JComboBox_relacion))
			cargarComboBoxItems();
		else if (fuente.equals(JComboBox_tipoMarca))
			cargarResultado(JTabbedPane_principal.getSelectedIndex(),JComboBox_tipoMarca.getSelectedIndex());
		else if (fuente.equals(JButton_marcarFoto))
			marcarFoto();
		else if (fuente.equals(JButton_eliminarMarca))
			eliminarMarca();
		else if (fuente.equals(JButton_fotosDelRey))
			cargarResultado(JTabbedPane_principal.getSelectedIndex(),0);
		else if (fuente.equals(JButton_fotosDeTrabajo))
			cargarResultado(JTabbedPane_principal.getSelectedIndex(),1);
		else if (fuente.equals(JButton_fotosHermanos))
			cargarResultado(JTabbedPane_principal.getSelectedIndex(),2);
		else if (fuente.equals(JButton_fotosFamiliares))
			cargarResultado(JTabbedPane_principal.getSelectedIndex(),3);
		else if (fuente.equals(JButton_busquedaPersonal))
			busquedaPersonal();
	}
	
}
