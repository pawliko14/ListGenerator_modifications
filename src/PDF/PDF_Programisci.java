package PDF;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import WB.DBConnection;

public class PDF_Programisci {
	
	public static Font ffont = new Font(Font.FontFamily.TIMES_ROMAN, 10);
	// private static Connection conn;
	
	private static void addHeader(PdfPTable t){
		
		String komorki[] = new String[] {"PROJECT", "Opis maszyny", "Numer Serii", "Numer Rysunku", "Opis Rysunku", "Iloœæ", "OpóŸnienie", "Nr bonu pracy", "Status"};
		for(int i = 0; i<9; i++){
			PdfPCell cell = new PdfPCell(new Phrase(komorki[i], ffont));
			cell.setFixedHeight(30f);
			cell.setBackgroundColor(BaseColor.ORANGE);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			cell.setBorderWidth(0.2f);
			cell.setBorderWidthTop(1f);
			cell.setBorderWidthBottom(1f);
			cell.setBorderWidthLeft(0.2f);
			cell.setBorderWidthRight(0.2f);
			t.addCell(cell);
		}
	}
	
	private static void addCell (PdfPTable t, String a){
		
		PdfPCell cell = new PdfPCell(new Phrase(a, ffont));
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setFixedHeight(25f);
		cell.setBorderWidth(0.2f);
		// if(a.equals("coœ ciekawego")) cell.setBackgroundColor(BaseColor.YELLOW);
		t.addCell(cell);
	}

	public static void create() throws SQLException{
	
	Connection myConn = DBConnection.dbConnector();
	Document PDFProgramisci = new Document();
	SimpleDateFormat doNazwy = new SimpleDateFormat("yyyy.MM.dd");
	SimpleDateFormat godz = new SimpleDateFormat("HH;mm");
	 Calendar date = Calendar.getInstance();
	System.out.println("Start lista dla programistow");
		 
	try	 
	{ 
		String path = PDF.Parameters.getPathToOSN()+"/";
		String path2 = PDF.Parameters.getPathToDokumentacjaHacoSoft()+"/";
		String lista = "Lista dla Programistow"+" "+doNazwy.format(date.getTime());
		File plik = new File(path+lista);
		File plik2 = new File(path2+lista);
		if (plik.exists()){
				lista = godz.format(date.getTime())+" "+lista;
		};
		if (plik2.exists()){
			lista = godz.format(date.getTime())+" "+lista;
		};
		PdfWriter writer = PdfWriter.getInstance(PDFProgramisci, new FileOutputStream(path+lista+".pdf"));
		PdfWriter writer2 = PdfWriter.getInstance(PDFProgramisci, new FileOutputStream(path2+lista+".pdf"));
		PDFProgramisci.open();
		writer.setPageEvent(new PDF_MyFooter());
		writer2.setPageEvent(new PDF_MyFooter());
		
		for (int i = 1; i<30; i++){
			Calendar PoprzednieDni = Calendar.getInstance();
			PoprzednieDni.add(Calendar.DAY_OF_MONTH,-i);
			java.util.Date PoprzednieDniKrotko = PoprzednieDni.getTime();
			Path sciezka = FileSystems.getDefault().getPath(PDF.Parameters.getPathToOSN(), "Lista dla Programistow"+" "+doNazwy.format(PoprzednieDniKrotko.getTime())+".pdf");
			Path sciezka2 = FileSystems.getDefault().getPath(PDF.Parameters.getPathToDokumentacjaHacoSoft(), "Lista dla Programistow"+" "+doNazwy.format(PoprzednieDniKrotko.getTime())+".pdf");
			Files.deleteIfExists(sciezka);
			Files.deleteIfExists(sciezka2);
			//File PlikiDoUsuniecia = new File (path+"Lista dla Programistow"+" "+doNazwy.format(PoprzednieDniKrotko.getTime())+".pdf");

		}
		
		PdfPTable table = new PdfPTable(9);
		float widths[] = new float[] {15, 22, 15, 28, 22, 5, 8, 15, 5};
		addHeader(table);
		
		Statement takeProgramers = myConn.createStatement();
		ResultSet tabelaProgrammers;
		
		tabelaProgrammers = takeProgramers.executeQuery("SELECT * FROM ProgrammersTable");
		
		while (tabelaProgrammers.next()){
			for (int i=1; i<=9; i++){
				addCell (table, tabelaProgrammers.getString(i));	
			}
		}
		takeProgramers.close();
		table.setWidthPercentage(100);
		table.setWidths(widths);
		table.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.setHeaderRows(1);
		if(table.size()==0 ){
			Paragraph a = new Paragraph("Document is empty", ffont);
			PDFProgramisci.add(a);
		}
		else
			PDFProgramisci.add(table);
		PDFProgramisci.close();
	}
	catch(Exception e){
		e.printStackTrace();	 

			
		}
	System.out.println("Koniec listy dla programistow");
	
	}

}
