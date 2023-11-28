package be.planetegem.mammon.invoice;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import be.planetegem.mammon.db.DbConsole;
import be.planetegem.mammon.statics.DocumentConstraints;
import be.planetegem.mammon.statics.LanguageFile;
import be.planetegem.mammon.statics.StyleSheet;
import be.planetegem.mammon.util.FormattedCell;
import be.planetegem.mammon.util.JSmallButton;

public class InvoiceTableUI extends JPanel {
    protected InvoiceFactory parent;
    protected DbConsole db;

    // Buttons and variable containers
    private JPanel tableBody, subtotalContainer, buttonContainer, vatContainer, finalTotalContainer, drawnTable;
    protected JSmallButton addButton, deleteButton;

    // vat selector
    protected JComboBox<String> vatSelector;
    private String[] vatOptions = {"21%", "6%", "0%"};
    private JPanel reverseChargeContainer;
    public String currentVat = "21%";

    // main table array; number of rows in table are tracked for formatting purposes
    public ArrayList<HashMap<String, String>> tableArray = new ArrayList<HashMap<String, String>>();
    public int tableLines = tableArray.size();
    public String direction = "POSITIVE";

    // formatted table columns: cells containing same value are fused
    protected ArrayList<FormattedCell> descriptionColumn;
    protected ArrayList<FormattedCell> dateColumn;
    protected ArrayList<FormattedCell> amountColumn;
    protected ArrayList<FormattedCell> priceColumn;
    protected ArrayList<Double> totalColumn;

    // to add new lines     
    protected boolean addingNewLine = false;
    protected LineInput lineCreator;
    
    // language logic
    public int lang = LanguageFile.NL;
    private JLabel headerDescription, headerDate, headerAmount, headerPrice, headerTotal;
    private JLabel subtotalHeader, vatHeader, finalTotalHeader, reverseChargeLabel;

    public void setLanguage(int lang){
        this.lang = lang;

        headerDescription.setText(LanguageFile.descr[lang]);
        headerDate.setText(LanguageFile.date[lang]);
        headerAmount.setText(LanguageFile.amount[lang]);
        headerPrice.setText(LanguageFile.price[lang]);
        headerTotal.setText(LanguageFile.lTotal[lang]);
        subtotalHeader.setText(LanguageFile.sTotal[lang]);
        setVatHeader(currentVat);
        reverseChargeLabel.setText(LanguageFile.reverseCharge[lang]);
        finalTotalHeader.setText(LanguageFile.fTotal[lang]);
    }
    public void setVatHeader(String selection){
        if (selection.equals("21%")){
            vatHeader.setText(LanguageFile.vat21[lang]);
        } else if (selection.equals("0%")){
            vatHeader.setText(LanguageFile.vat0[lang]);
        } else if (selection.equals("6%")){
            vatHeader.setText(LanguageFile.vat6[lang]);
        }
    }

    // calculate totals
    private double subtotal;
    private double subtotalWithVat;
    private double finalTotal;
    private JLabel subtotalLabel, vatLabel, finalTotalLabel;

    public void setTotals() {
        if (tableArray.size() > 0){
            subtotal = 0;
            for (Double number : totalColumn){
                subtotal += number;
            }
            String label = String.format("%.2f", subtotal).replace(".", ",") + " € ";
            subtotalLabel.setText(label);

            if (currentVat.equals("21%")){
                subtotalWithVat = subtotal*0.21;
            } else if (currentVat.equals("0%")){
                subtotalWithVat = subtotal*0;
            } else if (currentVat.equals("6%")){
                subtotalWithVat = subtotal*0.06;
            }
            label = String.format("%.2f", subtotalWithVat).replace(".", ",") + " € ";
            vatLabel.setText(label);

            finalTotal = subtotal + subtotalWithVat;
            label = String.format("%.2f", finalTotal).replace(".", ",") + " € ";
            finalTotalLabel.setText(label);
        } else {
            String label = "0,00 € ";
            subtotalLabel.setText(label);
            vatLabel.setText(label);
            finalTotalLabel.setText(label);
        }
    }

    // Calculmate size of table and position of subtables
    protected void placeTables(){
        db.logEvent("Positioning tables: currently counting " + tableLines + " lines");

        tableBody.setBounds(
            0, 
            DocumentConstraints.tHeaderHeight*DocumentConstraints.previewRatio,
            DocumentConstraints.lineWidth*DocumentConstraints.previewRatio,
            DocumentConstraints.softLine*DocumentConstraints.previewRatio*tableLines
        );

        // Keep track of current Y value beneath table
        int currentY = DocumentConstraints.tHeaderHeight + DocumentConstraints.softLine*tableLines + DocumentConstraints.halfLine;
        subtotalContainer.setBounds(
            (DocumentConstraints.lineWidth - DocumentConstraints.tSubtotalWidth)*DocumentConstraints.previewRatio, 
            currentY*DocumentConstraints.previewRatio,
            DocumentConstraints.tSubtotalWidth*DocumentConstraints.previewRatio,
            DocumentConstraints.softLine*DocumentConstraints.previewRatio
        );

        buttonContainer.setBounds(
            0, 
            currentY*DocumentConstraints.previewRatio,
            (DocumentConstraints.lineWidth - DocumentConstraints.tSubtotalWidth)*DocumentConstraints.previewRatio,
            DocumentConstraints.lineHeight*DocumentConstraints.previewRatio
        );

        currentY += DocumentConstraints.softLine;
        vatContainer.setBounds(
            (DocumentConstraints.lineWidth - DocumentConstraints.tSubtotalWidth)*DocumentConstraints.previewRatio, 
            currentY*DocumentConstraints.previewRatio,
            DocumentConstraints.tSubtotalWidth*DocumentConstraints.previewRatio,
            DocumentConstraints.softLine*DocumentConstraints.previewRatio
        );

        currentY += DocumentConstraints.softLine;

        if (vatSelector.getSelectedItem().toString().equals("0%")){
            reverseChargeContainer.setBounds(
                (DocumentConstraints.lineWidth - DocumentConstraints.tSubtotalWidth*2)*DocumentConstraints.previewRatio, 
                currentY*DocumentConstraints.previewRatio,
                DocumentConstraints.tSubtotalWidth*DocumentConstraints.previewRatio*2,
                DocumentConstraints.softLine*DocumentConstraints.previewRatio*3
            );
            reverseChargeContainer.setVisible(true);
            currentY += DocumentConstraints.previewRatio*3;
        } else {
            reverseChargeContainer.setVisible(false);
        }

        currentY += DocumentConstraints.softLine;
        finalTotalContainer.setBounds(
            (DocumentConstraints.lineWidth - DocumentConstraints.tSubtotalWidth)*DocumentConstraints.previewRatio, 
            currentY*DocumentConstraints.previewRatio,
            DocumentConstraints.tSubtotalWidth*DocumentConstraints.previewRatio,
            DocumentConstraints.softLine*DocumentConstraints.previewRatio
        );

        repaint();
        revalidate();

        // repaint parent to solve graphical glitch
        parent.repaint();
        parent.revalidate();

        // If adding line, request focus for descriptionField
        if (addingNewLine){
            lineCreator.setFocusToDescription();
        }
    }

    // add LineMaker to table body; if table body remains empty, draw empty field
    protected void setLineCreator(){
        this.tableLines = tableArray.size();
            
        // Add 2 table lines if in process of adding new line
        if (addingNewLine){
            tableBody.add(lineCreator);
            lineCreator.setBounds(
                0, 
                tableLines*DocumentConstraints.softLine*DocumentConstraints.previewRatio,
                DocumentConstraints.lineWidth*DocumentConstraints.previewRatio,
                DocumentConstraints.softLine*DocumentConstraints.previewRatio*2
            );
            this.tableLines += 2;
        }
        // If empty, build empty table of one line
        if (tableLines == 0){
            this.tableLines++;
            Border border = BorderFactory.createMatteBorder(0, 1, 1, 1, Color.black);
            tableBody.setBorder(border);
        }

        db.logEvent("Setting line creator: currently counting " + tableLines + " lines");

        tableBody.repaint();
        tableBody.revalidate();
        placeTables();
    }

    // draw table body
    protected void makeTableBody(){
        tableBody.removeAll();
        tableBody.setBorder(null);

        // Check number of table lines in array
        this.tableLines = tableArray.size();
        db.logEvent("Setting table body: currently counting " + tableLines + " lines");

        if (tableLines > 0){
            // Draw lines from memory
            drawnTable = new JPanel();

            drawnTable.setLayout(null);
            drawnTable.setBackground(Color.white);
            drawnTable.setBounds(
                0, 0,
                DocumentConstraints.lineWidth*DocumentConstraints.previewRatio,
                DocumentConstraints.softLine*DocumentConstraints.previewRatio*tableLines
            );

            for (FormattedCell cell : descriptionColumn){
                JLabel tempDescription = new JLabel("<html><p align=\"center\">" + cell.string + "</p></html>", SwingConstants.CENTER);
                tempDescription.setFont(StyleSheet.documentFont);
                tempDescription.setBounds(
                    0, 
                    cell.y*DocumentConstraints.softLine*DocumentConstraints.previewRatio,
                    DocumentConstraints.tDescription*DocumentConstraints.previewRatio,
                    cell.height*DocumentConstraints.softLine*DocumentConstraints.previewRatio
                );
                Border border = BorderFactory.createMatteBorder(0, 1, 1, 1, Color.black);
                tempDescription.setBorder(border);
                drawnTable.add(tempDescription);
            }

            int currentX = DocumentConstraints.tDescription*DocumentConstraints.previewRatio;
            for (FormattedCell cell : dateColumn){
                JLabel tempDate = new JLabel(cell.string, SwingConstants.CENTER);
                tempDate.setFont(StyleSheet.documentFont);
                tempDate.setBounds(
                    currentX, 
                    cell.y*DocumentConstraints.softLine*DocumentConstraints.previewRatio,
                    DocumentConstraints.tDate*DocumentConstraints.previewRatio,
                    cell.height*DocumentConstraints.softLine*DocumentConstraints.previewRatio
                );
                Border border = BorderFactory.createMatteBorder(0, 0, 1, 1, Color.black);
                tempDate.setBorder(border);
                drawnTable.add(tempDate);
            }

            currentX += DocumentConstraints.tDate*DocumentConstraints.previewRatio;
            for (FormattedCell cell : amountColumn){
                JLabel tempAmount = new JLabel(cell.string, SwingConstants.CENTER);
                tempAmount.setFont(StyleSheet.documentFont);
                tempAmount.setBounds(
                    currentX, 
                    cell.y*DocumentConstraints.softLine*DocumentConstraints.previewRatio,
                    DocumentConstraints.tNumber*DocumentConstraints.previewRatio,
                    cell.height*DocumentConstraints.softLine*DocumentConstraints.previewRatio
                );
                Border border = BorderFactory.createMatteBorder(0, 0, 1, 1, Color.black);
                tempAmount.setBorder(border);
                drawnTable.add(tempAmount);
            }

            currentX += DocumentConstraints.tNumber*DocumentConstraints.previewRatio;
            for (FormattedCell cell : priceColumn){
                JLabel tempPrice = new JLabel(cell.string + " €", SwingConstants.CENTER);
                tempPrice.setFont(StyleSheet.documentFont);
                tempPrice.setBounds(
                    currentX, 
                    cell.y*DocumentConstraints.softLine*DocumentConstraints.previewRatio,
                    DocumentConstraints.tPrice*DocumentConstraints.previewRatio,
                    cell.height*DocumentConstraints.softLine*DocumentConstraints.previewRatio
                );
                Border border = BorderFactory.createMatteBorder(0, 0, 1, 1, Color.black);
                tempPrice.setBorder(border);
                drawnTable.add(tempPrice);
            }

            currentX += DocumentConstraints.tPrice*DocumentConstraints.previewRatio;
            for (int i = 0; i < totalColumn.size(); i++){
                String content = String.format("%.2f",totalColumn.get(i)) + " € ";
                content = content.replace(".", ",");
                JLabel tempTotal = new JLabel(content, SwingConstants.RIGHT);
                tempTotal.setFont(StyleSheet.documentFont);
                tempTotal.setBounds(
                    currentX, 
                    i*DocumentConstraints.softLine*DocumentConstraints.previewRatio,
                    DocumentConstraints.tTotal*DocumentConstraints.previewRatio,
                    DocumentConstraints.softLine*DocumentConstraints.previewRatio
                );
                Border border = BorderFactory.createMatteBorder(0, 0, 1, 1, Color.black);
                tempTotal.setBorder(border);
                drawnTable.add(tempTotal);
            }
            tableBody.add(drawnTable);
        }
        setTotals();
        setLineCreator();        
    }

    // Constructor prepares table headers & containers for table body, buttons, subtotals & totals
    protected InvoiceTableUI(){
        super();

        Dimension sectionSize = new Dimension(
            DocumentConstraints.lineWidth*DocumentConstraints.previewRatio, 
            DocumentConstraints.invoiceTable*DocumentConstraints.previewRatio
        );
        setPreferredSize(sectionSize);
        setLayout(null);
        setBackground(Color.white);

        // table header
        JPanel tableHeader = new JPanel();
        tableHeader.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        tableHeader.setBounds(
            0, 0,
            DocumentConstraints.lineWidth*DocumentConstraints.previewRatio,
            DocumentConstraints.tHeaderHeight*DocumentConstraints.previewRatio
        );
        tableHeader.setBackground(Color.white);
        add(tableHeader);

        headerDescription = new JLabel(LanguageFile.descr[lang], SwingConstants.CENTER);
        headerDescription.setFont(StyleSheet.documentFont);
        Dimension headerSize = new Dimension(
            DocumentConstraints.tDescription*DocumentConstraints.previewRatio,
            DocumentConstraints.tHeaderHeight*DocumentConstraints.previewRatio
        );
        headerDescription.setPreferredSize(headerSize);
        Border border = BorderFactory.createMatteBorder(1, 1, 1, 1, Color.black);
        headerDescription.setBorder(border);
        tableHeader.add(headerDescription);

        headerDate = new JLabel(LanguageFile.date[lang], SwingConstants.CENTER);
        headerDate.setFont(StyleSheet.documentFont);
        headerSize = new Dimension(
            DocumentConstraints.tDate*DocumentConstraints.previewRatio,
            DocumentConstraints.tHeaderHeight*DocumentConstraints.previewRatio
        );
        headerDate.setPreferredSize(headerSize);
        border = BorderFactory.createMatteBorder(1, 0, 1, 1, Color.black);
        headerDate.setBorder(border);
        tableHeader.add(headerDate);

        headerAmount = new JLabel(LanguageFile.amount[lang], SwingConstants.CENTER);
        headerAmount.setFont(StyleSheet.documentFont);
        headerSize = new Dimension(
            DocumentConstraints.tNumber*DocumentConstraints.previewRatio,
            DocumentConstraints.tHeaderHeight*DocumentConstraints.previewRatio
        );
        headerAmount.setPreferredSize(headerSize);
        headerAmount.setBorder(border);
        tableHeader.add(headerAmount);

        headerPrice = new JLabel(LanguageFile.price[lang], SwingConstants.CENTER);
        headerPrice.setFont(StyleSheet.documentFont);
        headerSize = new Dimension(
            DocumentConstraints.tPrice*DocumentConstraints.previewRatio,
            DocumentConstraints.tHeaderHeight*DocumentConstraints.previewRatio
        );
        headerPrice.setPreferredSize(headerSize);
        headerPrice.setBorder(border);
        tableHeader.add(headerPrice);

        headerTotal = new JLabel(LanguageFile.lTotal[lang], SwingConstants.CENTER);
        headerTotal.setFont(StyleSheet.documentFont);
        headerSize = new Dimension(
            DocumentConstraints.tTotal*DocumentConstraints.previewRatio,
            DocumentConstraints.tHeaderHeight*DocumentConstraints.previewRatio
        );
        headerTotal.setPreferredSize(headerSize);
        headerTotal.setBorder(border);
        tableHeader.add(headerTotal);

        // Table body container
        tableBody = new JPanel();
        tableBody.setLayout(null);
        tableBody.setBackground(Color.white);
        add(tableBody);

        // Button container
        buttonContainer = new JPanel();
        buttonContainer.setBackground(Color.white);
        add(buttonContainer);
        addButton = new JSmallButton("+");
        addButton.setFont(StyleSheet.wizardFont);
        buttonContainer.add(addButton);
        deleteButton = new JSmallButton("-");
        deleteButton.setFont(StyleSheet.wizardFont);
        buttonContainer.add(deleteButton);

        JLabel vatSelectorLabel = new JLabel("BTW-tarief:", SwingConstants.RIGHT);
        Dimension labelPadding = new Dimension(
            DocumentConstraints.tSubtotalWidth*DocumentConstraints.previewRatio,
            DocumentConstraints.softLine*DocumentConstraints.previewRatio
        );
        vatSelectorLabel.setFont(StyleSheet.wizardFont);
        vatSelectorLabel.setPreferredSize(labelPadding);
        buttonContainer.add(vatSelectorLabel);

        vatSelector = new JComboBox<String>(vatOptions);
        vatSelector.setFont(StyleSheet.wizardFont);
        buttonContainer.add(vatSelector);

        // Subtotal
        subtotalContainer = new JPanel();
        subtotalContainer.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        subtotalContainer.setBackground(Color.white);
        add(subtotalContainer);

        subtotalHeader = new JLabel(LanguageFile.sTotal[lang], SwingConstants.LEFT);
        subtotalHeader.setFont(StyleSheet.documentFont);
        headerSize = new Dimension(
            DocumentConstraints.tSubtotalLeft*DocumentConstraints.previewRatio,
            DocumentConstraints.softLine*DocumentConstraints.previewRatio
        );
        subtotalHeader.setPreferredSize(headerSize);
        border = BorderFactory.createMatteBorder(1, 1, 1, 1, Color.black);
        subtotalHeader.setBorder(border);
        subtotalContainer.add(subtotalHeader);

        subtotalLabel = new JLabel("0,00 € ", SwingConstants.RIGHT);
        subtotalLabel.setFont(StyleSheet.documentFont);
        headerSize = new Dimension(
            DocumentConstraints.tSubtotalRight*DocumentConstraints.previewRatio,
            DocumentConstraints.softLine*DocumentConstraints.previewRatio
        );
        subtotalLabel.setPreferredSize(headerSize);
        border = BorderFactory.createMatteBorder(1, 0, 1, 1, Color.black);
        subtotalLabel.setBorder(border);
        subtotalContainer.add(subtotalLabel);

        // vat
        vatContainer = new JPanel();
        vatContainer.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        vatContainer.setBackground(Color.white);
        add(vatContainer);

        vatHeader = new JLabel(LanguageFile.vat21[lang], SwingConstants.LEFT);
        vatHeader.setFont(StyleSheet.documentFont);
        headerSize = new Dimension(
            DocumentConstraints.tSubtotalLeft*DocumentConstraints.previewRatio,
            DocumentConstraints.softLine*DocumentConstraints.previewRatio
        );
        vatHeader.setPreferredSize(headerSize);
        border = BorderFactory.createMatteBorder(0, 1, 1, 1, Color.black);
        vatHeader.setBorder(border);
        vatContainer.add(vatHeader);

        vatLabel = new JLabel("0,00 € ", SwingConstants.RIGHT);
        vatLabel.setFont(StyleSheet.documentFont);
        headerSize = new Dimension(
            DocumentConstraints.tSubtotalRight*DocumentConstraints.previewRatio,
            DocumentConstraints.softLine*DocumentConstraints.previewRatio
        );
        vatLabel.setPreferredSize(headerSize);
        border = BorderFactory.createMatteBorder(0, 0, 1, 1, Color.black);
        vatLabel.setBorder(border);
        vatContainer.add(vatLabel);

        // VAT reverse charge
        reverseChargeContainer = new JPanel();
        reverseChargeContainer.setLayout(new BoxLayout(reverseChargeContainer, BoxLayout.Y_AXIS));
        reverseChargeContainer.setBackground(Color.white);
        add(reverseChargeContainer);
        reverseChargeContainer.setVisible(false);

        Dimension topPadding = new Dimension(
            DocumentConstraints.tSubtotalWidth*DocumentConstraints.previewRatio,
            DocumentConstraints.quarterLine*DocumentConstraints.previewRatio
        );
        JLabel padLabel = new JLabel();
        padLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        padLabel.setMaximumSize(topPadding);
        padLabel.setMinimumSize(topPadding);
        padLabel.setPreferredSize(topPadding);
        reverseChargeContainer.add(padLabel);

        reverseChargeLabel = new JLabel(LanguageFile.reverseCharge[lang]);
        reverseChargeLabel.setFont(StyleSheet.documentFont);
        reverseChargeLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        reverseChargeContainer.add(reverseChargeLabel);

        String kbText = "<html><body align=\"right\">(Intracommunautaire dienstverrichting niet onderworpen ";
        kbText += "<br>aan Belgische btw art. 21, § 2 van het Wbtw)</body></html>";
        JLabel kbLabel = new JLabel(kbText, SwingConstants.RIGHT);
        kbLabel.setFont(StyleSheet.captionFont);
        kbLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        reverseChargeContainer.add(kbLabel);

        // total
        finalTotalContainer = new JPanel();
        finalTotalContainer.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        finalTotalContainer.setBackground(Color.white);
        add(finalTotalContainer);

        finalTotalHeader = new JLabel(LanguageFile.fTotal[lang], SwingConstants.LEFT);
        finalTotalHeader.setFont(StyleSheet.documentFont);
        headerSize = new Dimension(
            DocumentConstraints.tSubtotalLeft*DocumentConstraints.previewRatio,
            DocumentConstraints.softLine*DocumentConstraints.previewRatio
        );
        finalTotalHeader.setPreferredSize(headerSize);
        border = BorderFactory.createMatteBorder(1, 1, 1, 1, Color.black);
        finalTotalHeader.setBorder(border);
        finalTotalContainer.add(finalTotalHeader);

        finalTotalLabel = new JLabel("0,00 € ", SwingConstants.RIGHT);
        finalTotalLabel.setFont(StyleSheet.documentFont);
        headerSize = new Dimension(
            DocumentConstraints.tSubtotalRight*DocumentConstraints.previewRatio,
            DocumentConstraints.softLine*DocumentConstraints.previewRatio
        );
        finalTotalLabel.setPreferredSize(headerSize);
        border = BorderFactory.createMatteBorder(1, 0, 1, 1, Color.black);
        finalTotalLabel.setBorder(border);
        finalTotalContainer.add(finalTotalLabel);
    }
}
