package PDF;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import WB.DBConnection;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;


public class PDF_Serwis {

	private static Font ffont;
	private static Font ffont2;
	
	public static void create() throws SQLException{
		/*
		 * lista dla serwisu - taka ubo¿sza wersja listy produkcyjnej, tylko projekty serwisowe 14/... i bez technologii
		 */
		System.out.println("Start lista dla serwisu");
		Connection myConn = DBConnection.dbConnector();
		SimpleDateFormat doNazwy = new SimpleDateFormat("yyyy.MM.dd");
		SimpleDateFormat godz = new SimpleDateFormat("HH;mm");
		Calendar date = Calendar.getInstance();
		Document Serwis = new Document(PageSize.A4, 10, 10, 25, 25);
		String fName = "//192.168.90.203/Logistyka/Tosia/Projekty JAVA";
		FontFactory.register(fName);
		ffont = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 10);
		ffont2 = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 6); 
		String path = PDF.Parameters.getPathToSave()+"/"+doNazwy.format(date.getTime())+"/";
		String name = "Serwis.pdf";
		File f = new File(path+name);
		if(f.exists() && !f.isDirectory())
			name = godz.format(date.getTime())+" "+name;
		PdfWriter writer;
		try {
			writer = PdfWriter.getInstance(Serwis, new FileOutputStream(path+name));
			writer.setPageEvent(new PDF_MyFooter());
		} catch (FileNotFoundException | DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Serwis.open();
		PdfPTable table = new PdfPTable(8);
		float widths[] = new float[] { 18, 9, 28, 45, 16, 8, 15, 18};
		try {
			table.setWidths(widths);
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		table.setWidthPercentage(100);
		table.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.setHorizontalAlignment(Element.ALIGN_CENTER);
		addHeader(table);
		addRow(1,table);
		//creating a statement for project content
		
		Statement pobierzProjectSchedule = myConn.createStatement();
		String sql = "Select nrMaszyny, opis, klient, dataprodukcji, dataKoniecMontazu, komentarz from calendar where zakonczone = 0 and nrMaszyny like '14/%' order by dataProdukcji";
		ResultSet ProjectSchedule = pobierzProjectSchedule.executeQuery(sql);
		
		while(ProjectSchedule.next()){
			String calyNumer = ProjectSchedule.getString("nrMaszyny");
			String [] p = calyNumer.split("/");
			String ProjectGroup = p[0];
			String ProjectNumber = p[1];
			String MontageFinishDate = ProjectSchedule.getString("dataKoniecMontazu");
			String ProductionDate = ProjectSchedule.getString("dataProdukcji");
			String ProjectName = ProjectSchedule.getString("Opis");
			String klient = ProjectSchedule.getString("klient");
			Statement ListContent = myConn.createStatement();
			ResultSet ProjectContent;
			ProjectContent = ListContent.executeQuery("SELECT MatSource, `ItemNo`, `ItemDesc`, `ConsumerOrder`, Quantity, Slack FROM partsoverview " +
						"where `OrderNo` = '"+ProjectGroup+"/"+ProjectNumber+"' order by MatSource");
		
			boolean naglowek = false;
			
			while(ProjectContent.next())
			{
				if(!naglowek){
					addProjectHeader(new String [] {ProjectGroup+"/"+ProjectNumber, ProjectName, ProductionDate, MontageFinishDate, klient}, table);
					naglowek=true;
				}
				
				addCell(table, ProjectContent.getString("MatSource"));
				addCell(table, ProjectContent.getString("Slack"));
				addCell(table, ProjectContent.getString("ItemNo"));
				addCell(table, ProjectContent.getString("ItemDesc"));
				addCell(table, ProjectContent.getString("ConsumerOrder"));
				addCell(table, ProjectContent.getString("Quantity"));
				//Material
				Statement takeMaterial = myConn.createStatement();
				ResultSet Mat = takeMaterial.executeQuery("Select MaterialAssigned from ProjectMaterials where Order500='"+ProjectContent.getString("MatSource")+"'");
				String Material = "";
				while(Mat.next()){
						Material=Mat.getString("MaterialAssigned");
						if(Material==null)
							Material = "";
				}
				takeMaterial.close();
				addCell(table, Material);
				String batchNumber = ProjectContent.getString("MatSource");
				String NumberAbove = ProjectContent.getString("ConsumerOrder");
				if(batchNumber.startsWith("500/"))
				{	
					String number = batchNumber.substring(4, batchNumber.length());
					Statement takeTakt = myConn.createStatement();
					ResultSet T;
					T = takeTakt.executeQuery("SELECT MONTAGEOMSCHRIJVING FROM StoreNotesDetail where projectnummer = '"+NumberAbove+"' and ARTIKELCODE= '"+ProjectContent.getString("ItemNo")+"'");
					String Takt = " ";
					while(T.next()){
						Takt = T.getString("MONTAGEOMSCHRIJVING");
					}
					if(Takt==null) Takt = "";
					if(Takt.startsWith("FAT -")){
						Takt = Takt.substring(5, Takt.length());
					}
					takeTakt.close();
					PdfPCell cell2 = new PdfPCell(new Phrase(Takt, ffont2));
					cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
					cell2.setVerticalAlignment(Element.ALIGN_MIDDLE);
					cell2.setFixedHeight(10f);
					cell2.setNoWrap(false);
					table.addCell(cell2);
				}
				else
					table.completeRow();	
			}
			if(naglowek)
				addRow(2, table);
			ListContent.close();
		}
		table.completeRow();
		table.deleteLastRow();
		pobierzProjectSchedule.close();
		System.out.println("konczymy liste dla serwisu");
		try {
			if(table.size()==0 ){
				Paragraph a = new Paragraph("Document is empty", ffont2);
				Serwis.add(a);
			}
			else{
				table.setHeaderRows(1);
				Serwis.add(table);
			}
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Serwis.close();
		myConn.close();
	}
	private static void addHeader(PdfPTable t){
		String [] header = new String [] {"Numer serii", "OpóŸnienie", "Numer artyku³u", "Nazwa artyku³u",  "Projekt", "Ilosc", "Materia³", "Takt"};
		//adding header to our file
		for(int i = 0; i<header.length; i++) {
			PdfPCell cell1 = new PdfPCell(new Phrase(header[i], ffont));
			cell1.setMinimumHeight(30);
			cell1.setBackgroundColor(BaseColor.ORANGE);
			cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
			t.addCell(cell1);
		}
	}
	
	private static void addRow(int a, PdfPTable t){
		for (int i = 1; i<=a; i++){
			PdfPCell cell = new PdfPCell(new Phrase(" "));
			cell.setColspan(8);
			cell.setFixedHeight(10f);
			t.addCell(cell);
		}
	}
	
	private static void addCells(int a, PdfPTable t){
		for(int i = 1; i<=a; i++){
			PdfPCell cell = new PdfPCell(new Phrase(" ",ffont2));
			cell.setNoWrap(true);
			t.addCell(cell);
		}
	}
	
	private static void addCell(PdfPTable t, String z){
		PdfPCell cell = new PdfPCell(new Phrase(z, ffont2));

		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setFixedHeight(10f);
		if(z.startsWith("Nie u")) cell.setBackgroundColor(new BaseColor(255,127,39));
		if(z.startsWith("Na m")) cell.setBackgroundColor(new BaseColor(218, 211, 220));
		t.addCell(cell);
	}
	
	private static void addProjectHeader(String [] dane, PdfPTable t){
		
		String [] header = new String [] {"Numer projektu", "Nazwa projektu", "Data produkcji", "Data koñca monta¿u", "Klient"};
		
		for(int j=0; j<2; j++) {
			for(int i = 0; i<header.length; i++) {
				PdfPCell cell1;
				if(j==0) {
					cell1 = new PdfPCell(new Phrase(header[i], ffont));
				}
				else
					cell1 = new PdfPCell(new Phrase(dane[i], ffont));
				cell1.setFixedHeight(15f);
				if(i==4)
					cell1.setColspan(3);
				else if(i!=2) 
					cell1.setColspan(2);
				cell1.setBackgroundColor(BaseColor.ORANGE);
				cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
				cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
				t.addCell(cell1);
			}
		}
	}
	
}
