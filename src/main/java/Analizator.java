import org.apache.mina.filter.codec.ProtocolCodecException;
import quickfix.*;
import quickfix.mina.message.FIXMessageDecoder;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by bogdan on 17.07.18.
 */
public class Analizator {
    private String fileInput;
    private String fileOutput;
    private ReportTable rt=null;

    public Analizator() throws ProtocolCodecException {
        fileInput=getFilePath("Choose log File");
        System.out.println(fileInput);
        unZipIt();
        messageListDecoder(new File(fileOutput));


    }

    public void unZipIt(){

        byte[] buffer = new byte[1024];

        try{

            //create output directory is not exists
            File folder = new File(fileInput.substring(0,fileInput.lastIndexOf("/")+1)+"MarketData");
            if(!folder.exists()){
                folder.mkdir();
            }

            //get the zip file content
            ZipInputStream zis =
                    new ZipInputStream(new FileInputStream(fileInput));
            //get the zipped file list entry
            ZipEntry ze = zis.getNextEntry();

            while(ze!=null){

                String fileName = ze.getName();
                File newFile = new File(folder + File.separator + fileName);
                this.fileOutput=newFile.getAbsoluteFile().getAbsolutePath();
                System.out.println("file unzip : "+ newFile.getAbsoluteFile());

                //create all non exists folders
                //else you will hit FileNotFoundException for compressed folder
                new File(newFile.getParent()).mkdirs();

                FileOutputStream fos = new FileOutputStream(newFile);

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();

            System.out.println("Done");

        }catch(IOException ex){
            ex.printStackTrace();
        }
    }

    private void messageListDecoder(File file) throws ProtocolCodecException {
        FIXMessageDecoder fmd;
        try{
           fmd = new FIXMessageDecoder();
            FIXMessageDecoder.MessageListener listener = new FIXMessageDecoder.MessageListener() {
                @Override
                public void onMessage(String message) {
                    try {
                        getMessage(message);
                    } catch (InvalidMessage invalidMessage) {
                        invalidMessage.printStackTrace();
                    }
                }
            };
                fmd.extractMessages(file,listener);
        }catch (UnsupportedEncodingException e){
            e.getMessage();
        }catch (IOException e){
            e.getMessage();
        }
    }

    private  String getFilePath(String dialogTitle){
        String filePath="";
        String[] filters = {"zip"};
        JFileChooser dialog = new JFileChooser();
        dialog.setDialogTitle(dialogTitle);
        dialog.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("zip", filters);
        dialog.setFileFilter(filter);
        try{
            dialog.showOpenDialog(null);
            filePath =dialog.getSelectedFile().getAbsolutePath();}
        catch(NullPointerException e){
            System.out.println("Canceled");
            System.exit(0);
        }
        return filePath;

    }

    private void getMessage(String s) throws InvalidMessage {
        String[] mar = s.split("\001");
        arrayAnalizer(mar);
    }

    private void arrayAnalizer(String[] array){
        ArrayList<String> list = new ArrayList<>(Arrays.asList(array));
        String typeOfMeth="";
        String bool="";
        String id="";
        String pair="";
        String price="";
        String size="";

        ArrayList<MessageActionNew> newActions=new ArrayList<>();
        ArrayList<MessageActionNew> dellActions = new ArrayList<>();
        for (String s : list) {
            if(s.contains("279")){
                typeOfMeth=s.substring(s.length()-1,s.length());
            }

            if(s.contains("269")){
                String type = s.substring(s.length()-1,s.length());
                if(type.equals("0")){
                    bool="bid";
                }

                if(type.equals("1")){
                    bool="offer";
                }
            }

            if(s.contains("278")){
                id=s.substring(4);
            }

            if(s.contains("55")){
                pair=s.substring(3);
            }

            if(s.contains("270")){
                price=s.substring(4);
            }

            if(s.contains("271")){
                size=s.substring(4);
            }

            if(typeOfMeth.equals("0") && !id.equals("") && !pair.equals("") && !price.equals("") && !size.equals("")){
                newActions.add(new MessageActionNew(id,pair,bool,price,size));
                id=pair=price=size="";
            }

            if(typeOfMeth.equals("2") && !id.equals("") && !pair.equals("")){
              //  rt.deleteRow(id);
                dellActions.add(new MessageActionNew(id,pair,"Delete","0","0"));
                id="";
                pair="";
            }
        }

        if (newActions.size() > 0) {
            newActions.addAll(dellActions);
            rt = new ReportTable(newActions);
            try {
                rt.writeToFileInHTML(fileInput.substring(0,fileOutput.lastIndexOf("/")+1)+"_report.html");
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

}

class MessageActionNew{
    private String id;
    private String pair;
    private String bid;
    private String price;
    private String size;

    MessageActionNew(String id, String pair, String bool, String price, String size){
        this.id=id;
        this.pair=pair;
        this.bid=bool;
        this.price=price;
        this.size=size;
    }

    public String getId(){return id;}
    public String getPrice(){return price;}
    public String getSize(){return size;}
    public String getBid(){return  bid;}
    public String getPair(){return pair;}
}

class ReportTable{
    private JTable jTable;
    private JFrame jfrm;
    private ReportTableModel rt;
    private ArrayList<MessageActionNew> messageActionNewArrayList = new ArrayList<>();
    ReportTable(ArrayList<MessageActionNew> messageActionNewArrayList){
        this.messageActionNewArrayList=messageActionNewArrayList;
        jfrm = new JFrame("Report");
        jfrm.getContentPane().setLayout(new FlowLayout());
        jfrm.setSize(300, 170);
        jfrm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        rt= new ReportTableModel(messageActionNewArrayList);
        this.jTable = new JTable(rt);
    }

    public void writeToFileInHTML(String filepath) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(filepath, true));
        TableModel model = jTable.getModel();
        bw.append("<h1>New message income</h1>");
        bw.append("<table>");
        bw.append("<tr>");
        for (int i = 0; i <model.getColumnCount() ; i++) {
            bw.append("<td>");
            bw.append(jTable.getColumnName(i));
            bw.append("</td>");
        }
        for(int r=0;r<model.getRowCount();++r) {
            bw.append("<tr>");
            for(int c=0;c<model.getColumnCount();++c) {
                bw.append("<td>");
                bw.append(model.getValueAt(r,c).toString());
                bw.append("</td>");
            }
        }
        bw.append("</table>");
        bw.close();
    }

    public void addDynamicRow(MessageActionNew messageActionNew){
        messageActionNewArrayList.add(messageActionNew);
        rt.fireTableDataChanged();
    }

    public void deleteRow(String value){
            for (int i = rt.getRowCount() - 1; i >= 0; --i) {
                for (int j = rt.getColumnCount() - 1; j >= 0; --j) {
                    if (rt.getValueAt(i, j).equals(value)) {
                        // what if value is not unique?
                        DefaultTableModel model = (DefaultTableModel) jTable.getModel();
                        model.removeRow(i);
                    }
                }
            }
        }


    public void showTable(){
        JScrollPane jscrlp = new JScrollPane(jTable);
        jTable.setPreferredScrollableViewportSize(new Dimension(250, 100));
        jfrm.getContentPane().add(jscrlp);
        jfrm.setVisible(true);
    }

}

class ReportTableModel extends AbstractTableModel {
    private ArrayList<MessageActionNew> messageActionNewArrayList;
    ReportTableModel(ArrayList<MessageActionNew> messageActionNewArrayList){
        this.messageActionNewArrayList=messageActionNewArrayList;
    }

    @Override
    public int getRowCount() {
        return messageActionNewArrayList.size();
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex){
            case 0:
                return messageActionNewArrayList.get(rowIndex).getId();
            case 1:
                return messageActionNewArrayList.get(rowIndex).getBid();
            case 2:
                return messageActionNewArrayList.get(rowIndex).getPair();
            case 3:
                return messageActionNewArrayList.get(rowIndex).getPrice();
            case 4:
                return messageActionNewArrayList.get(rowIndex).getSize();
            default:
                return "";
        }
    }

    @Override
    public String getColumnName(int c){
        switch (c){
            case 0:
                return "ID";
            case 1:
                return "Side";
            case 2:
                return "Pair";
            case 3:
                return "Price";
            case 4:
                return "Size";
            default:
                return "Other";
        }
    }

}