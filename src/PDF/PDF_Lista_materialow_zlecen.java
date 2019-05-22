package PDF;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import WB.DBConnection;

import java.io.File;
import java.io.FileOutputStream;


public class PDF_Lista_materialow_zlecen {
	
	//okreœlenie dwóch czcionek typu Times New Roman z szyfrowaniem CP1250 (polskie znaki) o wielkoœci 10 i 9
	public static Font ffont = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 10); 
	public static Font ffont2 = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 9); 
	
	
	
	//g³ówna metoda obs³uguj¹ca tworzenie PDFów
	public static void createPDFs() {
		
		try {
			Connection myConn = DBConnection.dbConnector();
			create(myConn, "Lista materialow");
			create(myConn, "Lista zlecen");
			myConn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	//metoda dodaj¹ca nag³ówek do dokumentu
	private static void addHeader(PdfPTable t){
		
		//zbiór nazw wszystkich komórek nag³ówka
		String komorki[] = new String[] {"Opoznienie", " ", "Zlecenie", "Artykul", "Opis", "Seria", "Optymalny koniec", "Docelowy koniec", "Pozostaly czas zlecenia", "Material", "Suma", "Ilosc do zamowienia", "Status", "UWAGI"};
		for(int i = 0; i<komorki.length; i++){
			//stworzenie instancji komórki zawieraj¹cej tekst ze zbioru nazw, sformatowane czcionk¹ ffont
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
			
			//dodanie komórki do tabeli
			t.addCell(cell);
		}
	}
	
	//metoda dodaj¹ca komórkê do tabeli 
	private static void addCell(PdfPTable t, String tresc){
		
		PdfPCell cell = new PdfPCell(new Phrase(tresc, ffont2));
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setFixedHeight(25f);
		cell.setBorderWidth(0.2f);
		
		//formatowanie koloru wype³nienia komórki w zale¿noœci od treœci (status dabegi 
		if(tresc.equals("NZ")) cell.setBackgroundColor(BaseColor.YELLOW);
		if(tresc.equals("O")) cell.setBackgroundColor(new BaseColor(244, 176, 132));
		if(tresc.equals("J")) cell.setBackgroundColor(new BaseColor(146, 208, 80));
		t.addCell(cell);
	}
	
	//metoda dodaj¹ca komórkê do tabeli z opcj¹ pogrubienia ramki komórki
	private static void addCell(PdfPTable t, String z, boolean pogrubic){
		PdfPCell cell = new PdfPCell(new Phrase(z, ffont2));
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setFixedHeight(25f);
		cell.setBorderWidth(0.2f);
		if(pogrubic){
			cell.setBorderWidthBottom(1f);
			cell.setBorderWidthLeft(0.2f);
			cell.setBorderWidthRight(0.2f);
		}
		if(z.equals("NZ")) cell.setBackgroundColor(BaseColor.YELLOW);
		if(z.equals("O")) cell.setBackgroundColor(new BaseColor(244, 176, 132));
		if(z.equals("J")) cell.setBackgroundColor(new BaseColor(146, 208, 80));
		t.addCell(cell);
	}
	
	
	//metoda tworzaca pdfa
	private static void create(Connection myConn, String Nazwa){
		System.out.println("Start listy materialow");
		//format dokumentu
		Document Lista = new Document(PageSize.A4.rotate());
		
		 SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
		 SimpleDateFormat doNazwy = new SimpleDateFormat("yyyy.MM.dd");
		 SimpleDateFormat godz = new SimpleDateFormat("HH;mm");
		 Calendar date = Calendar.getInstance();
	
		 //c.add(Calendar.DATE, 5); // Adding 5 days
		 //String output = sdf.format(c.getTime());
		try
		{	
			//obsluga dokumentu: wskazanie sciezki, nadanie nazwy
			String path = PDF.Parameters.getPathToSave()+"/"+doNazwy.format(date.getTime())+"/";
			File f = new File(path+Nazwa);
			if(f.exists() && !f.isDirectory())
				Nazwa = godz.format(date.getTime())+" "+Nazwa;
			PdfWriter writer = PdfWriter.getInstance(Lista, new FileOutputStream(path+Nazwa+".pdf"));
			Lista.open();
			writer.setPageEvent(new PDF_MyFooter());
			
			//obsluga glownej tabeli
			PdfPTable table = new PdfPTable(14);
			float widths[] = new float[] { 3, 4, 5, 9, 7, 2, 4, 4, 3, 8, 3, 3, 2, 9};
			//dodaj naglowek
			addHeader(table);
			//addRow(1, table);
			Statement takeMaterials = myConn.createStatement();
			ResultSet Materials;
			
			//obsluga listy o nazwie "Lista materialow.pdf" - tylko te pozycje, gdzie material jest niezamowiony
			if(Nazwa.equals("Lista materialow")){
				
				//to co wykonane dla listy materialowej
				//pobranie materialow
				
				//utworzenie listy materialow w celu wskazania calkowitych dlugosci potrzebnych do zamowienia
				List<String> listedMaterials = new ArrayList<String>();
				listedMaterials.clear();
				Materials = takeMaterials.executeQuery("SELECT material FROM MATERIALY WHERE DM = 'NZ' ORDER BY opoznienie");
				ResultSet smallList;
				ResultSet ile;
				ResultSet dlugosci;
				int k = 0;
				
				while(Materials.next()){
					int dlugosc = 0;
					String material = Materials.getString("material");
					
					//jesli material wystepuje pierwszy raz na liscie listedMaterials:
					if(!listedMaterials.contains(material)){
						listedMaterials.add(material);
						Statement myStmt = myConn.createStatement();
						//ile jest pozycji z tym materialem O STATUSIE NIEZAMOWIONY
						ile = myStmt.executeQuery("SELECT COUNT(*) FROM MATERIALY WHERE DM = 'NZ' AND material = '"+ material + "' ORDER BY opoznienie ASC");
						while(ile.next()){
							k=ile.getInt("COUNT(*)");
						}
						ile.close();
						
						//sprawdzenie sumarycznej dlugosci
						dlugosci = myStmt.executeQuery("SELECT iloscMaterialu FROM MATERIALY WHERE DM = 'NZ' AND material = '"+ material + "' ORDER BY opoznienie ASC");
						while(dlugosci.next()){
							dlugosc += dlugosci.getInt("iloscMaterialu");
						}
						dlugosci.close();
						
						//pobranie szczegolow listy z bazy:
						smallList = myStmt.executeQuery("SELECT Opoznienie, nr500, nrRys, opis, iloscSztuk, OptymalnyKoniec, DocelowyKoniec, ileGodzNaZlec, material, iloscMaterialu, DM FROM MATERIALY WHERE DM = 'NZ' AND material = '"+ material + "' ORDER BY opoznienie ASC");
						boolean scalone = false;
						int j=1;
						while(smallList.next()){
							for(int i = 1; i<=11; i++){
								//jeœli druga kolumna:
								if(i==2){
									//dodanie komorki z dat¹: dziœ + smallList.getDate(0) -> kiedy powinno byc wykonane
									int ileDni = (int) Math.floor(smallList.getDouble(1));
									date.add(Calendar.DAY_OF_MONTH, ileDni);
									if(j!=k) addCell(table, sdf.format(date.getTime()));
									else addCell(table, sdf.format(date.getTime()), true);
									date = Calendar.getInstance();
								}
								//jeœli dziewiata kolumna
								else if(i==9 && !scalone){
									PdfPCell cell = new PdfPCell(new Phrase(smallList.getString(i), ffont2));
									//parametr k - ile zlecen na dany material
									cell.setRowspan(k);
									cell.setHorizontalAlignment(Element.ALIGN_CENTER);
									cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
									cell.setFixedHeight(25f);
									cell.setBorderWidth(0.5f);
									cell.setBorderWidthBottom(1f);
									cell.setBorderWidthLeft(0.2f);
									cell.setBorderWidthRight(0.2f);
									table.addCell(cell);
								}
								
								//jesli dziesiata kolumna
								else if(i==10 && !scalone){
									PdfPCell cell = new PdfPCell(new Phrase(dlugosc+"", ffont2));
									cell.setRowspan(k);
									cell.setHorizontalAlignment(Element.ALIGN_CENTER);
									cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
									cell.setFixedHeight(25f);
									cell.setBorderWidth(0.5f);
									cell.setBorderWidthBottom(1f);
									cell.setBorderWidthLeft(0.2f);
									cell.setBorderWidthRight(0.2f);
									table.addCell(cell);
									scalone=true;
								}
								
								//jesli inna niz dziewiata kolumna
								if(i!=9){
									if(j!=k) addCell(table, smallList.getString(i));
									else addCell(table, smallList.getString(i), true);
								}
								
							}
							if(j!=k) addCell(table, " ");
							else addCell(table, " ", true);
							j++;
						}
						myStmt.close();
						scalone= false;
					}
				}
				listedMaterials.clear();
				k=0;
			}
			else{
				// to co wykonane dla listy zlecen
				Materials = takeMaterials.executeQuery("SELECT Opoznienie, nr500, nrRys, opis, iloscSztuk, OptymalnyKoniec, DocelowyKoniec, ileGodzNaZlec, material, iloscMaterialu, DM FROM MATERIALY  ORDER BY opoznienie ASC");
				while(Materials.next()){
					for(int i = 1; i<=11; i++){
						if(i==2){
							//dodanie komorki z dat¹: dziœ + smallList.getDate(0) -> kiedy powinno byc wykonane
							int ileDni = (int) Math.floor(Materials.getDouble(1));
							date.add(Calendar.DAY_OF_MONTH, ileDni);
							addCell(table, sdf.format(date.getTime()));
							date = Calendar.getInstance();
						}
						else if(i==10)
							addCell(table, " ");
						addCell(table, Materials.getString(i));
					}
					addCell(table," ");
				}
			}
			takeMaterials.close();
			table.setWidthPercentage(100);
			table.setWidths(widths);
			table.setHorizontalAlignment(Element.ALIGN_CENTER);
			table.setHeaderRows(1);
			if(table.size()==0 ){
				Paragraph a = new Paragraph("Document is empty", ffont2);
				Lista.add(a);
			}
			else
				Lista.add(table);
			Lista.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		System.out.println("Koniec listy materialow");
	}
}
