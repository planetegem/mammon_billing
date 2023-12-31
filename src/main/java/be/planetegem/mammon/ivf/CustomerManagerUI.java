package be.planetegem.mammon.ivf;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import be.planetegem.mammon.Mammon;
import be.planetegem.mammon.db.DbConsole;
import be.planetegem.mammon.statics.DocConstraints;
import be.planetegem.mammon.statics.LanguageFile;
import be.planetegem.mammon.statics.StyleSheet;
import be.planetegem.mammon.util.ui.JSmallButton;

public class CustomerManagerUI extends JPanel {

    // parent and grandparent are used to interact with other components
    protected InvoiceFactory parent;
    protected Mammon grandparent;
    protected DbConsole db;

    // data properties
    protected int customerIndex = -1;
    protected ArrayList<HashMap<String, String>> customers;
    protected HashMap<String, String> currentCustomer = new HashMap<String, String>();

    // buttons and variable components
    private JPanel customerButtons, customerPreview;
    private JLabel customerDetails, customerHeader;
    protected JSmallButton addCustomer, removeCustomer, editCustomer;
    protected JComboBox<String> selectCustomer;

    // Language logic
    public int lang = LanguageFile.NL;

    public void setLanguage(int lang){
        this.lang = lang;
        customerHeader.setText(LanguageFile.to[lang]);
    }

    // Clear 'canvas' when loading customer
    protected void clearPreview(){
        customerDetails.setText("");
    }

    // Create new customer preview: call whenever new customer is loaded/selected
    protected void setPreview(HashMap<String, String> selectedCustomer){
        String customerString = "";
        // Check if contact person was provided
        if (!selectedCustomer.get("FAMILYNAME").equals("")){
            customerString += selectedCustomer.get("FIRSTNAME") + " ";
            customerString += selectedCustomer.get("FAMILYNAME") + "<br>";
        }
        // Company name immediately beneath & empty line
        customerString += selectedCustomer.get("COMPANYNAME") + "<br>";
        customerString += "<br>";
        // Address
        customerString += selectedCustomer.get("STREETNAME") + " " + selectedCustomer.get("HOUSENUMBER");
        if (!selectedCustomer.get("BOXNUMBER").equals("")){
            customerString += " " + selectedCustomer.get("BOXNUMBER");
        } 
        customerString += "<br>";
        customerString += selectedCustomer.get("POSTALCODE") + " " + selectedCustomer.get("PLACENAME");
        customerString += " - " + selectedCustomer.get("COUNTRYNAME") + "<br>";
        customerString += "<br>";

        // Vat number: compare customertype, potentially add 2nd line
        if (!selectedCustomer.get("VATNUMBER").equals("")){
            customerString += LanguageFile.vat[lang] + " ";
            customerString += selectedCustomer.get("VATNUMBER");
            if (!selectedCustomer.get("VATNUMBER2").equals("")){
                customerString += "<br>" + selectedCustomer.get("VATNUMBER2");
            }
        }
        customerDetails.setText("<html>" + customerString + "</html>");
    }

    protected CustomerManagerUI(){
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.white);
        Dimension mainPanelSize = new Dimension(
            DocConstraints.a4Width*DocConstraints.previewRatio, 
            (DocConstraints.customerHeight + DocConstraints.lineHeight)*DocConstraints.previewRatio
        );
        setPreferredSize(mainPanelSize);
        setMaximumSize(mainPanelSize);
        setMinimumSize(mainPanelSize);

        customerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        Dimension buttonPanelSize = new Dimension(
            DocConstraints.lineWidth*DocConstraints.previewRatio, 
            DocConstraints.lineHeight*DocConstraints.previewRatio
        );
        customerButtons.setPreferredSize(buttonPanelSize);
        customerButtons.setMaximumSize(buttonPanelSize);
        customerButtons.setMinimumSize(buttonPanelSize);
        customerButtons.setBackground(Color.white);
        add(customerButtons);

        JLabel selectCustomerLabel = new JLabel("Huidige klant:");
        selectCustomerLabel.setFont(StyleSheet.wizardFont);
        selectCustomer = new JComboBox<String>();
        selectCustomer.setFont(StyleSheet.wizardFont);
        selectCustomer.setPrototypeDisplayValue("[0] Company Name goes here");
        customerButtons.add(selectCustomerLabel);
        customerButtons.add(selectCustomer);

        editCustomer = new JSmallButton("wijzig");
        editCustomer.setFont(StyleSheet.wizardFont);
        removeCustomer = new JSmallButton("verwijder");
        removeCustomer.setFont(StyleSheet.wizardFont);
        addCustomer = new JSmallButton("nieuw");
        addCustomer.setFont(StyleSheet.wizardFont);
        customerButtons.add(editCustomer);
        customerButtons.add(removeCustomer);
        customerButtons.add(addCustomer);

        customerPreview = new JPanel();
        customerPreview.setLayout(null);
        Dimension previewPanelSize = new Dimension(
            DocConstraints.lineWidth*DocConstraints.previewRatio, 
            DocConstraints.profileHeight*DocConstraints.previewRatio
        );
        customerPreview.setPreferredSize(previewPanelSize);
        customerPreview.setMaximumSize(previewPanelSize);
        customerPreview.setMinimumSize(previewPanelSize);
        customerPreview.setBackground(Color.white);
        add(customerPreview);

        customerHeader = new JLabel();
        customerHeader.setBounds(
            DocConstraints.customerLeadingWidth*DocConstraints.previewRatio, 
            0,
            DocConstraints.customerHeaderWidth*DocConstraints.previewRatio, 
            DocConstraints.customerHeight*DocConstraints.previewRatio
        );
        customerHeader.setHorizontalAlignment(SwingConstants.LEFT);
        customerHeader.setVerticalAlignment(SwingConstants.TOP);
        customerHeader.setFont(StyleSheet.documentFont);
        customerPreview.add(customerHeader);

        customerDetails = new JLabel();
        customerDetails.setBounds(
            (DocConstraints.customerLeadingWidth + DocConstraints.customerHeaderWidth)*DocConstraints.previewRatio, 
            0,
            DocConstraints.customerBodyWidth*DocConstraints.previewRatio, 
            DocConstraints.customerHeight*DocConstraints.previewRatio
        );
        customerDetails.setHorizontalAlignment(SwingConstants.LEFT);
        customerDetails.setVerticalAlignment(SwingConstants.TOP);
        customerDetails.setFont(StyleSheet.documentFont);
        customerPreview.add(customerDetails);
    }
}
