package be.planetegem.mammon.ivf;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JOptionPane;

import be.planetegem.mammon.statics.DocConstraints;
import be.planetegem.mammon.statics.LanguageFile;
import be.planetegem.mammon.util.FormattedCell;

public class InvoiceTable extends InvoiceTableUI implements ActionListener {

    

    // getters for table and vat (used when saving invoice to db)
    public ArrayList<HashMap<String, String>> getTable(){
        return tableArray;
    }
    public String getVat(){
        return currentVat;
    }

    // getter for formatted table (used when generating pdf)
    public void getFormattedTable(FormattedInvoice formalInvoice){

        formalInvoice.setDescriptionColumn(this.descriptionColumn);
        formalInvoice.setDateColumn(this.dateColumn);
        formalInvoice.setAmountColumn(this.amountColumn);
        formalInvoice.setPriceColumn(this.priceColumn);
        formalInvoice.setTotalsColumn(this.totalColumn);

        formalInvoice.setLineCount(this.tableLines);
        formalInvoice.setTableHeaders();

        ArrayList<String> subtotals = new ArrayList<String>();
        subtotals.add(LanguageFile.sTotal[lang]);
        subtotals.add(String.format("%.2f", subtotal).replace(".", ",") + " € ");

        String selectedVat = vatSelector.getSelectedItem().toString();
        if (selectedVat.equals("21%")){
            subtotals.add(LanguageFile.vat21[lang]);
        } else if (selectedVat.equals("0%")){
            subtotals.add(LanguageFile.vat0[lang]);
        } else if (selectedVat.equals("6%")){
            subtotals.add(LanguageFile.vat6[lang]);
        }
        subtotals.add(String.format("%.2f", subtotalWithVat).replace(".", ",") + " € ");
        subtotals.add(LanguageFile.fTotal[lang]);
        subtotals.add(String.format("%.2f", finalTotal).replace(".", ",") + " € ");
        formalInvoice.setSubtotals(subtotals);
    }

    // setter for table and vat (used when loading invoice from db)
    public void setTable(ArrayList<HashMap<String, String>> loadedTable, String direction, String currentVat){
        this.tableArray = loadedTable;
        this.direction = direction;
        this.currentVat = currentVat;
        this.addingNewLine = false;

        // adjust vatselector
        vatSelector.removeActionListener(this);
        vatSelector.setSelectedItem(this.currentVat);
        vatSelector.addActionListener(this);

        setVatHeader(this.currentVat);

        simplifyTable();
        makeTableBody();
    }

    // Push tableArray to SQL DB
    public void pushTableArray(int invoiceId) {
        // Add invoiceId to every row
        for (HashMap<String, String> line : tableArray){
            line.put("invoiceId", Integer.toString(invoiceId));
        }
        // for credit note: clean up minuses
        if (direction.equals("NEGATIVE")){
            for (HashMap<String, String> entry : tableArray){
                String newPrice = entry.get("price").replaceAll("-", "");
                entry.put("price", newPrice);
            }
        }
        // Delete all lines from db
        db.clearInvoiceLines(tableArray.get(0));
        // Add new lines
        for (HashMap<String, String> line : tableArray){
            db.addInvoiceLine(line);
        }
    }

    // Format tableArray into arraylist of table cells with coordinates (for drawing purposes);
    // Called every time the table body is changed
    public void simplifyTable(){
        // if credit note, reverse price in table
        if (direction.equals("NEGATIVE")){
            for (HashMap<String, String> entry : tableArray){
                String newPrice = "-" + entry.get("price");
                entry.put("price", newPrice);
            }
        }

        // prepare columns
        descriptionColumn = new ArrayList<FormattedCell>();
        dateColumn = new ArrayList<FormattedCell>();
        amountColumn = new ArrayList<FormattedCell>();
        priceColumn = new ArrayList<FormattedCell>();
        totalColumn = new ArrayList<FormattedCell>();

        // Step 1: prepare descriptionColumn; combine cells if previous cell was the same
        for (int i = 0; i < tableArray.size(); i++){
            // Convert first line faithfully
            if (i == 0){
                String content = tableArray.get(i).get("description");
                descriptionColumn.add(new FormattedCell(i, 1, content));

            } else {
                // Compare against previous values
                String content;
                if (tableArray.get(i).get("description").equals(tableArray.get(i - 1).get("description"))){
                    descriptionColumn.get(descriptionColumn.size() - 1).height++;
                } else {
                    content = tableArray.get(i).get("description");
                    descriptionColumn.add(new FormattedCell(i, 1, content));
                }
            }
        }
        // Step 2: check if descriptions fit in their container; keep track of offsets
        int lineOffset = 0;
        HashMap<Integer, Integer> offsets = new HashMap<Integer, Integer>();
        
        for (int i = 0; i < descriptionColumn.size(); i++){
            int lineCount = descriptionColumn.get(i).lineCount(DocConstraints.tDescription*DocConstraints.previewRatio);
            int currentLine = descriptionColumn.get(i).y;
            
            descriptionColumn.get(i).y += lineOffset;

            if (lineCount > descriptionColumn.get(i).height){
                int diff = lineCount - descriptionColumn.get(i).height;
                offsets.put(currentLine + descriptionColumn.get(i).height, diff);

                descriptionColumn.get(i).height += diff;
                lineOffset += diff;
            }
        }
        tableLines = tableArray.size() + lineOffset;

        // loop through tableArray: combine cells if previous one was the same
        lineOffset = 0;

        for (int i = 0; i < tableArray.size(); i++){
            // check offsets
            int heightOffset = 0;
            if (offsets.get(i + 1) != null){
                heightOffset = offsets.get(i + 1);
            }

            // Convert first line faithfully
            if (i == 0){
                String content = tableArray.get(i).get("date");
                dateColumn.add(new FormattedCell(i, 1 + heightOffset, content));

                content = tableArray.get(i).get("amount");
                if (!content.equals("/") && tableArray.get(i).get("unit") != null &&
                    !tableArray.get(i).get("unit").equals("")){
                    content += " " + tableArray.get(i).get("unit");
                }
                amountColumn.add(new FormattedCell(i, 1 + heightOffset, content));

                content = tableArray.get(i).get("price");
                priceColumn.add(new FormattedCell(i, 1 + heightOffset, content + " €"));
            } else {
                // Compare against previous values
                String content;
                if (tableArray.get(i).get("date").equals(tableArray.get(i - 1).get("date"))){
                    dateColumn.get(dateColumn.size() - 1).height += 1 + heightOffset;
                } else {
                    content = tableArray.get(i).get("date");
                    dateColumn.add(new FormattedCell(i + lineOffset, 1 + heightOffset, content));
                }
                
                // Amount is always specified, except when NA
                if (tableArray.get(i).get("amount").equals("/")){
                    if (tableArray.get(i - 1).get("amount").equals("/")){
                        amountColumn.get(amountColumn.size() - 1).height += 1 + heightOffset;
                    } else {
                        amountColumn.add(new FormattedCell(i + lineOffset, 1 + heightOffset, "/"));
                    }
                } else {
                    content = tableArray.get(i).get("amount");
                    if (!tableArray.get(i).get("unit").equals("")){
                        content += " " + tableArray.get(i).get("unit");
                    }
                    amountColumn.add(new FormattedCell(i + lineOffset, 1 + heightOffset, content));
                }
                if (heightOffset > 0){
                    amountColumn.get(amountColumn.size() - 1).mergedCell = true;
                }

                if (tableArray.get(i).get("price").equals(tableArray.get(i - 1).get("price"))){
                    priceColumn.get(priceColumn.size() - 1).height += 1 + heightOffset;
                } else {
                    content = tableArray.get(i).get("price");
                    priceColumn.add(new FormattedCell(i + lineOffset, 1 + heightOffset, content + " €"));
                }
            }

            // Calculate totals
            String cleanAmount = tableArray.get(i).get("amount").replace(",", ".");
            float amountFloat;
            if (cleanAmount.equals("/")){
                amountFloat = 1;
            } else {
                amountFloat = Float.parseFloat(cleanAmount);
            }
            String cleanPrice = tableArray.get(i).get("price").replace(",", ".");
            float priceFloat = Float.parseFloat(cleanPrice);
            totalColumn.add(new FormattedCell(i + lineOffset, 1 + heightOffset, amountFloat*priceFloat));
            if (heightOffset > 0){
                totalColumn.get(totalColumn.size() - 1).mergedCell = true;
            }

            lineOffset += heightOffset;
        }
    }
        
    // called from factory when selecting invoice type
    public void setDirection(String direction){
        this.direction = direction;
        if (addingNewLine){
            this.addingNewLine = false;
        }
        if (tableArray.size() > 0){
            simplifyTable();
        }
        makeTableBody();
    }

    // to interact with LineCreator
    public void addLine(HashMap<String, String> lineData){
        tableArray.add(lineData);
        this.addingNewLine = false;

        simplifyTable();
        makeTableBody();
    }
    public void endLineInput(){
        this.addingNewLine = false;
        makeTableBody();
    }

    // event handlers
    public void actionPerformed(ActionEvent e){
        if (direction.equals("NEGATIVE")){
            String message = "Selecteer een bestaande factuur om automatisch een creditfactuur op te stellen.";
            JOptionPane.showMessageDialog(parent, message);

            // reset vatselector
            vatSelector.removeActionListener(this);
            vatSelector.setSelectedItem(currentVat);
            vatSelector.addActionListener(this);

        } else {
            if (e.getSource() == addButton){
                db.logEvent("Event triggered: adding line to table");

                if (!addingNewLine){
                    addingNewLine = true;
                    lineCreator = new LineInput(this);
                    setLineCreator();
                }
            }
            if (e.getSource() == deleteButton){
                db.logEvent("Event triggered: removing line from table");
                if (addingNewLine){
                    addingNewLine = false;
                }
                if (tableArray.size() > 0){
                    tableArray.remove(tableArray.size() - 1);
                }
                // if table is empty after deletion, skip simplify step
                simplifyTable();
                makeTableBody();
            }
            if (e.getSource() == vatSelector){
                db.logEvent("Event triggered: changing vat");

                // first check if vat has changed
                String selection = vatSelector.getSelectedItem().toString();
                if (selection.equals(currentVat)){
                    db.logEvent("No change in vat: event skipped");
                } else {
                    this.currentVat = selection;
                    setVatHeader(selection);
                    setTotals();
                    placeTables(); 
                }
            }
        }
    }
    
    public InvoiceTable(InvoiceFactory parent){
        super();
        this.parent = parent;
        this.db = parent.getDb();

        makeTableBody();
        setLanguage(this.lang);

        // add listeners
        addButton.addActionListener(this);
        deleteButton.addActionListener(this);
        vatSelector.addActionListener(this);
    }
}
