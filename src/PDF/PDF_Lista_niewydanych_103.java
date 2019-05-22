package PDF;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
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

public class PDF_Lista_niewydanych_103 {

	public static Font ffont = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 12); 
	public static Font ffont2 = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 10); 
	public static Font ffont3 = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 8); 
	public static void create(){
		System.out.println("Start taktowa 103");
		Connection myConn = DBConnection.dbConnector();
		Document TL = new Document(PageSize.A3.rotate(), 10, 10, 25, 25);
		TL.setPageSize(PageSize.A3.rotate());
		SimpleDateFormat doNazwy = new SimpleDateFormat("yyyy.MM.dd");
		SimpleDateFormat godz = new SimpleDateFormat("HH;mm");
		Calendar date = Calendar.getInstance();
		String Columns = "LEVERANCIER, ORDERNUMMER, ARTIKELCODE, ARTIKELOMSCHRIJVING, BESTELD, BESTELEENHEID, GELEVERD, BOSTDEH, CFSTOCK, MONTAGE, MONTAGEOMSCHRIJVING, CFMAGAZIJN1, afdeling, afdelingseq ";
		try
		{
			String path = PDF.Parameters.getPathToSave()+"/"+doNazwy.format(date.getTime())+"/";
			String name = "Lista niewydanych 103.pdf";
			
			File f = new File(path+name);
			if(f.exists() && !f.isDirectory())
				name = godz.format(date.getTime())+" "+name;
			PdfWriter writer=null;
			writer = PdfWriter.getInstance(TL, new FileOutputStream(path+name));
			TL.open();
			writer.setPageEvent(new PDF_MyFooter());

			//dla kazdego projektu
			Statement pobierzProjectSchedule = myConn.createStatement();
			String sql = "Select nrMaszyny, opis, klient, dataKoniecMontazu, komentarz from calendar where Zakonczone = 0 order by dataProdukcji";
			ResultSet ProjectSchedule = pobierzProjectSchedule.executeQuery(sql);
			
			while(ProjectSchedule.next()){
				
				String calyNumer = ProjectSchedule.getString("nrMaszyny");
				String [] p = calyNumer.split("/");
				String ProjectGroup = p[0];
				String ProjectNumber = p[1];
				
				String innyNumer = ProjectSchedule.getString("komentarz");
				String[] zmiana = new String [2];
				boolean dodatkowy = false;
				if(innyNumer!=null){
					if(innyNumer.length()>1 && innyNumer.length()<10){
						zmiana = innyNumer.split("/");
						dodatkowy = true;
					}
				}
				
				String klient = ProjectSchedule.getString("klient");
				String nazwa = ProjectSchedule.getString("opis");
				String MontageFinishDate = ProjectSchedule.getString("dataKoniecMontazu");
				boolean ifPrinted = false;
				PdfPTable table = new PdfPTable(13);
				float widths[] = new float[] {10, 18, 18, 36, 72, 15, 12, 15, 15, 15, 18, 36, 22};
				table.setWidthPercentage(100);
				table.setWidths(widths);
				table.setHorizontalAlignment(Element.ALIGN_CENTER);
				table.setHorizontalAlignment(Element.ALIGN_CENTER);	
				
				if(!ProjectGroup.equals("6")&& !ProjectGroup.equals("30") )
				{
					//sprawdziæ bony wszystkich podprojektów 103
					Queue<String> Lista500 = new LinkedList<String>();
					String sql2 = "Select MatSource from PartsOverview where OrderNo = '"+calyNumer+"' and MatSource like '500/%'";
					Statement pobierz500 = myConn.createStatement();
					ResultSet piecsetki = pobierz500.executeQuery(sql2);
					while(piecsetki.next()){
						Lista500.add(piecsetki.getString(1));
					}
					pobierz500.close();
					piecsetki.close();
					
					while(!Lista500.isEmpty()){
						String art = Lista500.remove();
						Statement st2 = myConn.createStatement();
						ResultSet rs2 = st2.executeQuery("SELECT "+Columns+" from storenotesdetail " +
								"where projectnummer =  '500/"+art.substring(4, art.length())+"' and BOSTDEH <> 0  and leverancier = '103' " +
								"order by MONTAGE asc, leverancier asc, ordernummer asc, CFMAGAZIJN1 ASC");
						String subproject = art;
						while(rs2.next()){
							
							boolean naZielono = false;
							if(rs2.getInt("CFSTOCK")>=rs2.getInt("BOSTDEH"))
								naZielono=true;
							if(!ifPrinted){
								String nazwaProjektu = ProjectGroup+"/"+ProjectNumber;
								addHeader(table, new String [] {nazwaProjektu, MontageFinishDate, nazwa, klient});
								ifPrinted=true;
							}
							addFirstRow(true, naZielono, table, 1, rs2.getString("LEVERANCIER"), rs2.getString("ORDERNUMMER"), rs2.getString("ARTIKELCODE"), rs2.getString("ARTIKELOMSCHRIJVING"), rs2.getString("BESTELD"), rs2.getString("BESTELEENHEID"), rs2.getString("GELEVERD"), rs2.getString("BOSTDEH"), rs2.getString("CFSTOCK"), rs2.getString("MONTAGE"), rs2.getString("MONTAGEOMSCHRIJVING"), rs2.getString("CFMAGAZIJN1"));
							addCell(table, subproject);
						}
						st2.close();
					}
					
					//ile wierszy w kazdym z storenote'ow
					Statement takeRowList = myConn.createStatement();
					String sql0 = "SELECT LEVERANCIER, ORDERNUMMER, COUNT(*) FROM STORENOTESDETAIL " +
							"where (PROJECTNUMMER LIKE '"+calyNumer+"%' and BOSTDEH <> 0 and LEVERANCIER = '103')  ";
					if(dodatkowy){
						sql0 += " or (PROJECTNUMMER like '"+innyNumer+"%' and BOSTDEH <> 0 and LEVERANCIER = '103') ";
					}
					sql0+= " group by ORDERNUMMER order by montage asc,  leverancier asc, ordernummer asc ";
					ResultSet RowList = takeRowList.executeQuery(sql0);
					
					List<Integer> RL = new ArrayList<Integer>();
					boolean ifExist = false;
					int iter = 0;
					while(RowList.next()){
						ifExist=true;
						RL.add(RowList.getInt("COUNT(*)"));
					}
					takeRowList.close();
					int RowNumber = 0;
					if(ifExist){
						RowNumber = RL.get(0);
					}
					//pobieranie danych o niewydanych artykulach na bonach
					Statement takeTaktList = myConn.createStatement();
					String sql01 = "SELECT "+Columns+" from storenotesdetail " +
							"where (PROJECTNUMMER LIKE '"+calyNumer+"%' and BOSTDEH <> 0 and LEVERANCIER = '103') ";
					if(dodatkowy){
						sql01+= " or (PROJECTNUMMER like '"+innyNumer+"%' and BOSTDEH <> 0 and LEVERANCIER = '103') ";
					}
					sql01+=" order by MONTAGE asc, leverancier asc, ordernummer asc, CFMAGAZIJN1 ASC, artikelcode asc";
					ResultSet TaktsList = takeTaktList.executeQuery(sql01);
					
					while(TaktsList.next())
					{	
						String afd = TaktsList.getString("Afdeling");
						String afdseq = TaktsList.getString("Afdelingseq");
						if(RowNumber==0){
							iter++;
							RowNumber = RL.get(iter);
							
						}
						//dodawanie lewej czesci wiersza dla artykulu: 'naglowek'
						
						if(!ifPrinted){
							String nazwaProjektu = ProjectGroup+"/"+ProjectNumber;
							addHeader(table, new String [] {nazwaProjektu, MontageFinishDate, nazwa, klient});
							ifPrinted=true;
						}
						boolean naZielono = false;
						if(TaktsList.getInt("CFSTOCK")>=TaktsList.getInt("BOSTDEH"))
							naZielono = true;
						if(RowNumber == RL.get(iter)){
							addFirstRow(false, naZielono, table, RowNumber, TaktsList.getString("LEVERANCIER"), TaktsList.getString("ORDERNUMMER"), TaktsList.getString("ARTIKELCODE"), TaktsList.getString("ARTIKELOMSCHRIJVING"), TaktsList.getString("BESTELD"), TaktsList.getString("BESTELEENHEID"), TaktsList.getString("GELEVERD"), TaktsList.getString("BOSTDEH"), TaktsList.getString("CFSTOCK"), TaktsList.getString("MONTAGE"), TaktsList.getString("MONTAGEOMSCHRIJVING"), TaktsList.getString("CFMAGAZIJN1"));
						}
						else{
							addNextRow(table, naZielono, TaktsList.getString("ARTIKELCODE"), TaktsList.getString("ARTIKELOMSCHRIJVING"), TaktsList.getString("BESTELD"), TaktsList.getString("BESTELEENHEID"), TaktsList.getString("GELEVERD"), TaktsList.getString("BOSTDEH"), TaktsList.getString("CFSTOCK"), TaktsList.getString("CFMAGAZIJN1"));
						}
						addCell(table, afd+"/"+afdseq);
						RowNumber--;
					}
					takeTaktList.close();					
				}
				if(ifPrinted)
					TL.add(table);
			}
			pobierzProjectSchedule.close();
			Paragraph a = new Paragraph("Koniec dokumentu", ffont3);
			TL.add(a);
			TL.close();
			myConn.close();
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		System.out.println("Koniec taktowa 103");
	}
	

public static void addHeader(PdfPTable t, String [] dane){
	dane[3] = "Data ukoñczenia monta¿u: "+dane[3];
	for(int i = 0; i< dane.length; i++) {
		PdfPCell cell1 = new PdfPCell(new Phrase(dane[i], ffont));
		cell1.setMinimumHeight(30);
		if(i==0||i==2)
			cell1.setColspan(3);
		if(i==1)
			cell1.setColspan(2);
		if(i==3)
			cell1.setColspan(4);
		cell1.setBackgroundColor(BaseColor.ORANGE);
		cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
		t.addCell(cell1);
	}
	String [] header = new String [] {"Zlozenie nadrzedne", "Magazyn", "Nr bonu", "Lokalizacja", "Nr artykulu", "Nazwa artykulu", "Zamowiono", "Jednostka", "Dostarczono", "Brakujace", "Stan mag", "Numer taktu", "Nazwa taktu"};
	
	for(int i = 0; i< header.length; i++) {
		PdfPCell cell3 = new PdfPCell(new Phrase(header[i], ffont));
		cell3.setMinimumHeight(30);
		if(i==0)
			cell3.setRowspan(2);
		cell3.setBackgroundColor(BaseColor.ORANGE);
		cell3.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell3.setVerticalAlignment(Element.ALIGN_MIDDLE);
		t.addCell(cell3);
	}
	
}
//METODA DODAJ¥CA PIERWSZ¥ POZYCJÊ Z BONU MAGAZYNOWEGO - WTEDY KIEDY DODAWANE S¥ KOMÓRKI SCALONE
	
	public static void addFirstRow(boolean ifgrey, boolean doWydania, PdfPTable t, int ile, String Magazyn, String BonNo, String ArticleNo, String ArticleName, String Ordered, String Unit, String Delivered, String Missing, String Stock, String TaktNo, String TaktName, String lokalizacja){
		String rows[] = new String[] {Magazyn, BonNo, lokalizacja, ArticleNo, ArticleName, Ordered, Unit, Delivered, Missing, Stock, TaktNo, TaktName};
		for(int i = 0; i<12; i++){
			PdfPCell cell = new PdfPCell(new Phrase(rows[i], ffont2));
			if(ifgrey)
				cell.setBackgroundColor(new BaseColor(210, 210, 210));
			if( i==0 || i==1 || i==10 || i==11)
				cell.setRowspan(ile);
			else{
				cell.setFixedHeight(20f);
				if(doWydania)
					cell.setBackgroundColor(new BaseColor(34, 177, 76));//do dobrania
			}
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			t.addCell(cell);
		}
		
	}
	
	//DODAWANIE KA¯DEJ KOLEJNEJ POZYCJI Z BONU MAGAZYNOWEGO - OMIJANIE KOMÓREK SCALONYCH
	public static void addNextRow(PdfPTable t, boolean doWydania, String ArticleNo, String ArticleName, String Ordered, String Unit, String Delivered, String Missing, String Stock, String lokalizacja){
		String rows[] = new String[] {lokalizacja, ArticleNo, ArticleName, Ordered, Unit, Delivered, Missing, Stock};
		for(int i = 0; i<8; i++){
			PdfPCell cell = new PdfPCell(new Phrase(rows[i], ffont2));
			cell.setFixedHeight(20f);
			if(doWydania)
				cell.setBackgroundColor(new BaseColor(34, 177, 76));//do dobrania
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			t.addCell(cell);
		}
	}
	
	//metoda dodaj¹ca puste komórki
	public static void addCells(int a, PdfPTable t){
		for(int i = 1; i<=a; i++){
			PdfPCell cell = new PdfPCell(new Phrase(" ",ffont2));
			t.addCell(cell);
		}
	}
	
	//dodanie komórki z zawartoœci¹ Stringa z
	public static void addCell(PdfPTable t, String z){
		PdfPCell cell = new PdfPCell(new Phrase(z, ffont2));

		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setFixedHeight(20f);
		t.addCell(cell);
	}
	
	//dodanie komórki z technologi¹ + formatowanie t³a
	public static void addTechnCell(PdfPTable t, String stan, String status){
		PdfPCell cell = new PdfPCell(new Phrase(stan, ffont3));
		if(status.equals("10"))	cell.setBackgroundColor(BaseColor.WHITE);
		if(status.equals("20")) cell.setBackgroundColor(new BaseColor(255, 255, 0));
		if(status.equals("90")) cell.setBackgroundColor(new BaseColor(18, 162, 25));
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setFixedHeight(20f);
		t.addCell(cell);
	}
	

}

