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

public class PDF_Lista_niewydanych_102 {

	public static Font ffont = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 12); 
	public static Font ffont2 = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 10); 
	public static Font ffont3 = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 8); 
	public static void create(){
		System.out.println("Start taktowa 102");
		Connection connection = DBConnection.dbConnector();
		Document TL = new Document(PageSize.A3.rotate(), 10, 10, 25, 25);
		TL.setPageSize(PageSize.A3.rotate());
		SimpleDateFormat doNazwy = new SimpleDateFormat("yyyy.MM.dd");
		SimpleDateFormat godz = new SimpleDateFormat("HH;mm");
		Calendar date = Calendar.getInstance();
		try
		{
			String path = PDF.Parameters.getPathToSave()+"/"+doNazwy.format(date.getTime())+"/";
			String name = "Lista niewydanych 102.pdf";
			
			File f = new File(path+name);
			if(f.exists() && !f.isDirectory())
				name = godz.format(date.getTime())+" "+name;
			PdfWriter writer=null;
			writer = PdfWriter.getInstance(TL, new FileOutputStream(path+name));
			TL.open();
			writer.setPageEvent(new PDF_MyFooter());

			//dla ka¿dego projektu
			Statement pobierzProjectSchedule = connection.createStatement();
			String sql = "Select nrMaszyny, opis, klient, dataprodukcji, dataKoniecMontazu, komentarz from calendar where Zakonczone = 0 and nrMaszyny not like '6/%' and nrMaszyny not like '30/%' order by dataKoniecMontazu";
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
				String MontageFinishDate = ProjectSchedule.getString("dataKoniecMontazu");
				String nazwa = ProjectSchedule.getString("opis");
				String klient = ProjectSchedule.getString("klient");
				
				Statement takeTaktList = connection.createStatement();
				Statement takeRowList = connection.createStatement();
				ResultSet TaktsList;
				ResultSet RowList;
				//System.out.println("");
				boolean ifPrinted = false;
				
				//ile wierszy w kazdym ze storenote'ow
				String sql0 = "SELECT LEVERANCIER, ORDERNUMMER, COUNT(*) FROM STORENOTESDETAIL " +
						"where (PROJECTNUMMER LIKE '"+calyNumer+"%' and BOSTDEH <> 0 and LEVERANCIER = '102')  ";
				if(dodatkowy){
					sql0 += " or (PROJECTNUMMER like '"+innyNumer+"%' and BOSTDEH <> 0 and LEVERANCIER = '102') ";
				}
				sql0+= " group by ORDERNUMMER order by montage asc,  leverancier asc, ordernummer asc ";
				RowList = takeRowList.executeQuery(sql0);
				
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
				String Columns = "LEVERANCIER, ORDERNUMMER, ARTIKELCODE, ARTIKELOMSCHRIJVING, BESTELD, BESTELEENHEID, GELEVERD, BOSTDEH, CFSTOCK, MONTAGE, MONTAGEOMSCHRIJVING, CFMAGAZIJN1, AFDELING, AFDELINGSEQ ";
				
				String sql01 = "SELECT "+Columns+" from storenotesdetail " +
						"where (PROJECTNUMMER LIKE '"+calyNumer+"%' and BOSTDEH <> 0 and LEVERANCIER = '102') ";
				if(dodatkowy){
					sql01+= " or (PROJECTNUMMER like '"+innyNumer+"%' and BOSTDEH <> 0 and LEVERANCIER = '102') ";
				}
				sql01+=" order by MONTAGE asc, leverancier asc, ordernummer asc, CFMAGAZIJN1 ASC, artikelcode asc";
				TaktsList = takeTaktList.executeQuery(sql01);
				
				PdfPTable table = new PdfPTable(14);
				float widths[] = new float[] {10, 18, 18, 36, 72, 15, 12, 15, 15, 15, 18, 36, 22, 22};
				table.setWidthPercentage(100);
				table.setWidths(widths);
				table.setHorizontalAlignment(Element.ALIGN_CENTER);
				table.setHorizontalAlignment(Element.ALIGN_CENTER);	
				
				//stworzenie listy wszystkich zamowien piecset podrzednych!
				Queue<String> Lista500 = new LinkedList<String>();
				while(TaktsList.next())
				{	
					if(RowNumber==0){
						iter++;
						RowNumber = RL.get(iter);
						TL.add(table);
						table = new PdfPTable(14);
						table.setWidthPercentage(100);
						table.setWidths(widths);
						table.setHorizontalAlignment(Element.ALIGN_CENTER);
						table.setHorizontalAlignment(Element.ALIGN_CENTER);
					}
					int StoreNoteType = TaktsList.getInt("LEVERANCIER");
					//dodawanie lewej czesci wiersza dla artykulu: 'naglowek'
					
					boolean naZielono = false;
					if(TaktsList.getInt("CFSTOCK")>=TaktsList.getInt("BOSTDEH"))
						naZielono=true;
					if(!ifPrinted){
						String nazwaProjektu = ProjectGroup+"/"+ProjectNumber;
						addHeader(table, new String [] {nazwaProjektu, MontageFinishDate, nazwa, klient});
						ifPrinted=true;
					}
					// sprawdzenie czy 500 + technologia
					String Piecsetka = "";
					String subproject = TaktsList.getString("AFDELING")+"/"+TaktsList.getString("AFDELINGSEQ");
					Statement znajdz500 = connection.createStatement();
					ResultSet Piecset = znajdz500.executeQuery("SELECT MatSource, `ConsumerOrder` FROM partsoverview WHERE `OrderNo`= '"+ProjectGroup+"/"+ProjectNumber+"' AND `ItemNo` = '"+TaktsList.getString("ARTIKELCODE")+"'");
					if(Piecset.next()){
						Piecsetka=Piecset.getString("MatSource");
						//System.out.println(Piecsetka);
						subproject = Piecset.getString(2);
						Lista500.add(Piecsetka);
						if(Piecsetka.startsWith("500/")){
							naZielono=false;
						}
						else{
							Piecsetka = "";
						}
					}
					znajdz500.close();
					
					if(RowNumber == RL.get(iter)){
						addFirstRow(false, naZielono, table, RowNumber, TaktsList.getString("LEVERANCIER"), TaktsList.getString("ORDERNUMMER"), TaktsList.getString("ARTIKELCODE"), TaktsList.getString("ARTIKELOMSCHRIJVING"), TaktsList.getString("BESTELD"), TaktsList.getString("BESTELEENHEID"), TaktsList.getString("GELEVERD"), TaktsList.getString("BOSTDEH"), TaktsList.getString("CFSTOCK"), TaktsList.getString("MONTAGE"), TaktsList.getString("MONTAGEOMSCHRIJVING"), TaktsList.getString("CFMAGAZIJN1"));
					}
					else{
						addNextRow(table, naZielono, TaktsList.getString("ARTIKELCODE"), TaktsList.getString("ARTIKELOMSCHRIJVING"), TaktsList.getString("BESTELD"), TaktsList.getString("BESTELEENHEID"), TaktsList.getString("GELEVERD"), TaktsList.getString("BOSTDEH"), TaktsList.getString("CFSTOCK"), TaktsList.getString("CFMAGAZIJN1"));
					}
					
					addCell(table, Piecsetka);
					addCell(table, subproject);
					
					// wyciaganie podprojektow dla marcina
					
					if(RowNumber == 1){
						while(!Lista500.isEmpty()){
							String art = Lista500.remove();
							//System.out.println(art);
							Statement st = connection.createStatement();
							Statement st2 = connection.createStatement();
							ResultSet rs = st.executeQuery("SELECT MatSource FROM PARTSOVERVIEW WHERE `ConsumerOrder` = '"+art+"' and `OrderNo`= '"+ProjectGroup+"/"+ProjectNumber+"'");
							
							while(rs.next()){
								Lista500.add(rs.getString("MatSource"));
							}
							st.close();
							ResultSet rs2 = st2.executeQuery("SELECT "+Columns+" from storenotesdetail " +
									"where projectnummer =  '500/"+art.substring(4, art.length())+"' and BOSTDEH <> 0  and leverancier = '102' " +
									"order by MONTAGE asc, leverancier asc, ordernummer asc, CFMAGAZIJN1 ASC");
							
							while(rs2.next()){
								
								 naZielono = false;
								if(rs2.getInt("CFSTOCK")>=rs2.getInt("BOSTDEH"))
									naZielono=true;
								Piecsetka = "";
								subproject = art;
								//pobieranie 500 i technologii
								Statement znajdz500v2 = connection.createStatement();
								ResultSet Piecsetv2 = znajdz500v2.executeQuery("SELECT MatSource, `ConsumerOrder` FROM partsoverview WHERE `OrderNo`= '"+ProjectGroup+"/"+ProjectNumber+"' AND `ItemNo` = '"+rs2.getString("ARTIKELCODE")+"'");
								
								if(Piecsetv2.next()){
									if(Piecsetv2.getString("MatSource").startsWith("500/")){
										Piecsetka=Piecsetv2.getString("MatSource");
										subproject = Piecsetv2.getString(2);
									}
								}
								else{
									if(StoreNoteType!=103){
										Piecsetka="";
									}
								}
								znajdz500v2.close();
								addFirstRow(true, naZielono, table, 1, rs2.getString("LEVERANCIER"), rs2.getString("ORDERNUMMER"), rs2.getString("ARTIKELCODE"), rs2.getString("ARTIKELOMSCHRIJVING"), rs2.getString("BESTELD"), rs2.getString("BESTELEENHEID"), rs2.getString("GELEVERD"), rs2.getString("BOSTDEH"), rs2.getString("CFSTOCK"), rs2.getString("MONTAGE"), rs2.getString("MONTAGEOMSCHRIJVING"), rs2.getString("CFMAGAZIJN1"));
								addCell(table, Piecsetka);
								addCell(table, subproject);
							}
							st2.close();
						}
					}
					
					RowNumber--;
				}
				TL.add(table);
				takeTaktList.close();
				//System.out.print("\n");
				//System.out.print("\n");
			}
			
			pobierzProjectSchedule.close();
			System.out.println("Koniec taktowej 102");
			Paragraph a = new Paragraph("Koniec dokumentu", ffont3);
			TL.add(a);
			TL.close();
			connection.close();
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		System.out.println("Koniec taktowa 102");
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
		String [] header = new String [] {"Nr serii 500/", "Zlozenie nadrzedne", "Magazyn", "Nr bonu", "Lokalizacja", "Nr artykulu", "Nazwa artykulu", "Zamowiono", "Jednostka", "Dostarczono", "Brakujace", "Stan mag", "Numer taktu", "Nazwa taktu"};
		
		for(int i = 0; i< header.length; i++) {
			PdfPCell cell3 = new PdfPCell(new Phrase(header[i], ffont));
			cell3.setMinimumHeight(30);
			if(i<=1)
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


