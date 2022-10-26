package rm4j.io.ui;

import javax.swing.JFrame;

import java.io.File;

import javax.swing.*;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class Frame extends JFrame implements ActionListener{
    JLabel label;
    String filepath = "./";

    public Frame(){
        JButton button = new JButton("file select");
        button.addActionListener(this);
    
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(button);
    
        this.label = new JLabel();
    
        JPanel labelPanel = new JPanel();
        labelPanel.add(label);
    
        getContentPane().add(labelPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.PAGE_END);
    }

    @Override
    public void actionPerformed(ActionEvent event){
        var filechooser = new JFileChooser(filepath);
        filechooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    
        int selected = filechooser.showSaveDialog(this);
        if (selected == JFileChooser.APPROVE_OPTION){
            File file = filechooser.getSelectedFile();
        }
    }
}
