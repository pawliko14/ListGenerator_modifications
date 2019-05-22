package PDF;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import WB.DBConnection;


public class PDF_Braki_w_uruchomieniu {

	private static Font ffont;
	private static Font ffont2;
	
	public static void create() throws SQLException{
		
		/*
		 * lista ze wszystkimi pozycjami, które GTT nie potrafi po³¹czyæ z zamówieniami
		 * BRAKI - BRAK URUCHOMIENIA / ZAMÓWIENIA
		 */
		System.out.println("Start lista braków");
		Connection myConn = DBConnection.dbConnector();
		SimpleDateFormat doNazwy = new SimpleDateFormat("yyyy.MM.dd");
		SimpleDateFormat godz = new SimpleDateFormat("HH;mm");
		Calendar date = Calendar.getInstance();
		Document Braki = new Document(PageSize.A4, 10, 10, 25, 25);
			FontFactory.register(PDF_Braki_w_uruchomieniu.class.getClassLoader().getResource("times.ttf").toString(), "times");
			ffont = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 10);
			ffont2 = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 8); 
			String path = PDF.Parameters.getPathToSave()+"/"+doNazwy.format(date.getTime())+"/";
			String name = "Braki w uruchomieniu.pdf";
			File f = new File(path+name);
			if(f.exists() && !f.isDirectory())
				name = godz.format(date.getTime())+" "+name;
			PdfWriter writer;
			try {
				writer = PdfWriter.getInstance(Braki, new FileOutputStream(path+name));
				writer.setPageEvent(new PDF_MyFooter());
			} catch (FileNotFoundException | DocumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Braki.open();
			
			PdfPTable table = new PdfPTable(6);
			float widths[] = new float[] { 18, 9, 28, 45, 16, 16};
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
			String sql = "Select nrMaszyny, opis, klient, dataprodukcji, dataKoniecMontazu, komentarz from calendar where zakonczone = 0 order by dataProdukcji";
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
							"where `OrderNo` = '"+ProjectGroup+"/"+ProjectNumber+"' and MatSource = 'Nie uruchomione'");
			
				boolean naglowek = false;
				
				while(ProjectContent.next())
				{
					if(!naglowek){
						addProjectHeader(new String[] {ProjectGroup+"/"+ProjectNumber, ProjectName, klient, ProductionDate, MontageFinishDate}, table);
						naglowek=true;
					}
					
					addCell(table, ProjectContent.getString("MatSource"));
					addCell(table, ProjectContent.getString("Slack"));
					addCell(table, ProjectContent.getString("ItemNo"));
					addCell(table, ProjectContent.getString("ItemDesc"));
					addCell(table, ProjectContent.getString("ConsumerOrder"));
					addCell(table, ProjectContent.getString("Quantity"));
					table.completeRow();	
				}
				if(naglowek)
					addRow(2, table);
				ListContent.close();
			}
			table.completeRow();
			table.deleteLastRow();
			pobierzProjectSchedule.close();
			try {
				if(table.size()==0 ){
					Paragraph a = new Paragraph("Document is empty", ffont2);
					Braki.add(a);
				}
				else{
					table.setHeaderRows(1);
					Braki.add(table);
				}
			} catch (DocumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Braki.close();
			myConn.close();
			System.out.println("Koniec listy braków");
	}
	private static void addHeader(PdfPTable t){
		String [] header = new String [] {"Numer serii", "OpóŸnienie", "Numer artyku³u", "Nazwa artyku³u", "Projekt", "Ilosc"};
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
			cell.setColspan(6);
			cell.setFixedHeight(10f);
			t.addCell(cell);
		}
	}
	
	private static void addCell(PdfPTable t, String z){
		PdfPCell cell = new PdfPCell(new Phrase(z, ffont2));

		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setFixedHeight(15f);
		if(z.startsWith("Nie u")) cell.setBackgroundColor(new BaseColor(255,127,39));
		if(z.startsWith("Na m")) cell.setBackgroundColor(new BaseColor(218, 211, 220));
		t.addCell(cell);
	}
	
	private static void addProjectHeader(String [] dane, PdfPTable t){
		String [] header = new String[] {"Numer projektu", "Nazwa projektu", "Klient", "Data produkcji", "Data koñca monta¿u"};
		
		for(int j=0; j<2; j++) {
			for(int i = 0; i<header.length; i++) {
				PdfPCell cell1;
				if(j==0) {
					cell1 = new PdfPCell(new Phrase(header[i], ffont));
				}
				else
					cell1 = new PdfPCell(new Phrase(dane[i], ffont));
				cell1.setFixedHeight(15f);
				if(i==1)
					cell1.setColspan(2);
				cell1.setBackgroundColor(BaseColor.ORANGE);
				cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
				cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
				t.addCell(cell1);
			}
		}
	}
	
}
