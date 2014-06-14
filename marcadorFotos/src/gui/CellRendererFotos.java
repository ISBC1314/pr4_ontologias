package gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;


@SuppressWarnings("serial")
public class CellRendererFotos extends DefaultListCellRenderer {
	
	public Component getListCellRendererComponent(JList<?> list, Object value,int index, boolean isSelected, boolean hasFocus) {
		JPanel panel = new JPanel();
		panel.setForeground(Color.white);
		ImageIcon icono = (ImageIcon) value;
		
		panel.setPreferredSize(new Dimension(100,50));
		panel.setBorder(BorderFactory.createLineBorder(Color.black));
		
		icono = new ImageIcon(icono.getImage().getScaledInstance(60, 60, Image.SCALE_DEFAULT));
		
		JLabel JLabel_foto = new JLabel ();
		JLabel_foto.setIcon(icono);
		
		panel.add(JLabel_foto);
		
		//DescripcionVivienda descripcion = (DescripcionVivienda)value;
		
		return panel;	
	}

}
