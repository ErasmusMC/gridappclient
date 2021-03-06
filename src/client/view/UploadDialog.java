/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.view;

import client.control.Controller;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author bram
 */
public class UploadDialog extends JDialog {

    private static final String TITLE = "New Data";
    private DefaultListModel<FormFactory> forms;

    /**
     * Creates new form AddGenomeDialog
     */
    public UploadDialog(Component parent, final Controller controller) {
        super(SwingUtilities.getWindowAncestor(parent), TITLE, ModalityType.APPLICATION_MODAL);
        initComponents();
        setLocationRelativeTo(parent);
        setResizable(false);
        setDefaultCloseOperation(HIDE_ON_CLOSE);

        forms = new DefaultListModel<>();
        forms.addElement(new FormFactory("Genome") {
            @Override
            Container newForm() {
                return new NewGenomePanel(controller);
            }
        });
        forms.addElement(new FormFactory("File") {
            @Override
            Container newForm() {
                return new NewFilePanel(controller);
            }
        });
        forms.addElement(new FormFactory("Application") {
            @Override
            Container newForm() {
                return new NewVersionPanel(controller);
            }
        });

        dataList.setModel(forms);
        dataList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (scrollPane.getViewport().getView() != null) {
                    remove(scrollPane.getViewport().getView());
                }
                scrollPane.setViewportView(((FormFactory) dataList.getSelectedValue()).newForm());
            }
        });

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                int index = dataList.getSelectedIndex();
                if (index != -1) {
                    dataList.setSelectedIndex(index);
                }
            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        dataTypePanel = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        dataList = new javax.swing.JList();
        dataPanel = new javax.swing.JPanel();
        scrollPane = new javax.swing.JScrollPane();

        setPreferredSize(new java.awt.Dimension(600, 400));

        dataList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(dataList);

        javax.swing.GroupLayout dataTypePanelLayout = new javax.swing.GroupLayout(dataTypePanel);
        dataTypePanel.setLayout(dataTypePanelLayout);
        dataTypePanelLayout.setHorizontalGroup(
            dataTypePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dataTypePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 189, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        dataTypePanelLayout.setVerticalGroup(
            dataTypePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dataTypePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 370, Short.MAX_VALUE)
                .addContainerGap())
        );

        getContentPane().add(dataTypePanel, java.awt.BorderLayout.WEST);

        dataPanel.setLayout(new javax.swing.BoxLayout(dataPanel, javax.swing.BoxLayout.LINE_AXIS));
        dataPanel.add(scrollPane);

        getContentPane().add(dataPanel, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JList dataList;
    private javax.swing.JPanel dataPanel;
    private javax.swing.JPanel dataTypePanel;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane scrollPane;
    // End of variables declaration//GEN-END:variables

    abstract class FormFactory {

        String name;

        public FormFactory(String displayName) {
            name = displayName;
        }

        abstract Container newForm();

        @Override
        public String toString() {
            return name;
        }
    }
}
