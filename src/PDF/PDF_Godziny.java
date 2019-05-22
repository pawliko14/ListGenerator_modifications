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
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import WB.DBConnection;


public class PDF_Godziny {
static Connection connection=DBConnection.dbConnector();
	
	private static Font catFont = new Font(Font.FontFamily.TIMES_ROMAN, 18,
	        Font.BOLD);
	private static Font redFont = new Font(Font.FontFamily.TIMES_ROMAN, 12,
	        Font.NORMAL, BaseColor.RED);
	private static Font smallFont = new Font(Font.FontFamily.TIMES_ROMAN, 8,
	        Font.BOLD);
	private static Font smallFont2 = new Font(Font.FontFamily.TIMES_ROMAN, 8);
	private static Font smallBold = new Font(Font.FontFamily.TIMES_ROMAN, 12,
	        Font.BOLD);
	

	public static void createWeekRaport() throws SQLException{
		
		System.out.println("Start lista zarejestrowanych godzin");
		Document doc = new Document();
		SimpleDateFormat doNazwy = new SimpleDateFormat("yyyy.MM.dd");
		SimpleDateFormat doKalendarza = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat godz = new SimpleDateFormat("HH;mm");
		Calendar date = Calendar.getInstance();
		String path = PDF.Parameters.getPathToSave()+"/"+doNazwy.format(date.getTime())+"/";
		Calendar startDate = Calendar.getInstance();
		startDate.add(Calendar.DAY_OF_YEAR, -7);
		Calendar endDate = Calendar.getInstance();
		endDate.add(Calendar.DAY_OF_YEAR, -1);
		String start = doKalendarza.format(startDate.getTime());
		String end = doKalendarza.format(endDate.getTime());
		File theDir = new File(path);
		// if the directory does not exist, create it
		if (!theDir.exists()) {
		    try{
		        theDir.mkdir();
		    } 
		    catch(SecurityException se){
		        //handle it
		    }
		}
		int ilePracownikow=1;
		String [] headers = new String [] {"Nazwisko Imie", "Zarejestrowane [h]", "Zakonczonych serii",  "Stanowiska"};
		int ileKolumn = headers.length;
		
		String name = "Raport godzin od "+start+" do "+end+".pdf";
		File f = new File(path+name);
		if(f.exists() && !f.isDirectory())
			name = godz.format(date.getTime())+" "+name;
		PdfWriter writer;
		try {
			writer = PdfWriter.getInstance(doc, new FileOutputStream(path+name));
			writer.setPageEvent(new PDF_MyFooter());
		} catch (FileNotFoundException | DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		doc.open();
		//wybierz wszystkie gniazda
		String sql1 = "SELECT nest, cfnestoms from werkpost where nest<> 'KOP7' and nest <>\"\" and nest like 'W%' group by nest, cfnestoms order by nest";
		String [][] tab = new String [4][10];
		int ktoreGniazdo = -1;
		Statement st1 = connection.createStatement();
		ResultSet rs1 = st1.executeQuery(sql1);
		int zarejestrowanychMinutAll = 0;
		while(rs1.next()){
			ktoreGniazdo++;
			String nest = rs1.getString("nest");
			int minutyWydzial = 0;
			tab[0][ktoreGniazdo] = nest;
			tab[1][ktoreGniazdo] = rs1.getString("cfnestoms");
			
			//Zrobienie naglowka
			 Paragraph preface = new Paragraph();
			 preface.add("\n");
             preface.add(new Paragraph("Raport godzin pracowników", catFont));
             preface.add("\n");
             preface.add(new Paragraph("Od: "+start+" do:  "+end, smallBold));
             preface.add("\n");
             preface.add(new Paragraph(
                             "Gniazdo:  "+nest+"  "+tab[1][ktoreGniazdo],
                             smallBold));
             preface.add("\n");
             try {
				doc.add(preface);
			} catch (DocumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
             
             PdfPTable tabPDF = new PdfPTable(ileKolumn);
             float widths[] = new float[] { 10, 6, 6, 6 };
			
		//  ilosc pracownikow w okreœlonym przedziale czasowym
			String a = "select count(*) from (select cfnaam from Rejestracja where datum >= '"+start+"' and datum <= '"+end+"' and cfnaam not like 'AWARIA%' and GniazdoS = '"+nest+"' and verwerkt = 1 group by cfnaam) M";
			Statement a1 = connection.createStatement();
			ResultSet rs2 = a1.executeQuery(a);
			while(rs2.next()){
				ilePracownikow= rs2.getInt(1);
			}
			a1.close();
			String[][] t = new String[ileKolumn][2*(ilePracownikow+1)];
			String b = null;

			int p = 0;
			b = "select cfnaam from Rejestracja where datum >= '"+start+"' and datum <= '"+end+"' and cfnaam not like 'AWARIA%' and GniazdoS = '"+nest+"' and verwerkt = 1 group by cfnaam";
			Statement b1 = connection.createStatement();
			ResultSet rs3 = b1.executeQuery(b);
			//dla kazdego gniazda licz godziny (dla kazdego pracownika osobno)
			
			//DLA KAZDEGO PRACOWNIKA
			while(rs3.next()){
				String nazwisko = rs3.getString(1);
				if(nazwisko.equals("NAREGOWSKA MONIKA")) nazwisko = "KOREKTA BLEDOW";
				t[0][2*p] = nazwisko;
				t[0][(2*p)+1] = nazwisko;
				int MinutWykonano = 0;
				int ileSeriiUkonczono = 0;
				
				//z werkuren -> wszystkie rejestracje czasu pracownika w okreslonym przedziale czasowym W GNIEZDZIE
					//suma czasu dla jednego bonu 
				
				String sql2 = "select werkbon, sum(tijd), status from Rejestracja where datum >= '"+start+"' and datum <= '"+end+"' and CFNAAM = '"+nazwisko+"' and GniazdoS = '"+nest+"' and verwerkt = 1  group by werkbon ";
				Statement stm1 = connection.createStatement();
				ResultSet rs4 = stm1.executeQuery(sql2);
				while(rs4.next()){
					//int GodzinDlaBonu = rs4.getInt(4);
					int MinutDlaBonu = rs4.getInt("SUM(TIJD)");
					MinutWykonano += MinutDlaBonu;	
					String status = rs4.getString("status");
					if(status.equals("90")){
						ileSeriiUkonczono++;
					}
					
				}
				stm1.close();				
				minutyWydzial+=MinutWykonano;
				
				//Sprawdzenie ile maszyn
				String sql6 = "select werkpost from Rejestracja where datum >= '"+start+"' and datum <= '"+end+"' and CFNAAM = '"+nazwisko+"' and GniazdoS = '"+nest+"' and verwerkt = 1 group by werkpost";
				Statement stm6 = connection.createStatement();
				ResultSet rs6 = stm6.executeQuery(sql6);
				String ileMaszyn = "";
				while(rs6.next()){
					ileMaszyn = ileMaszyn + rs6.getString(1) + ", ";
				}
				stm6.close();
				t[1][2*p]="";
				t[2][2*p]="";
				if(MinutWykonano<0){
					t[1][2*p] = "-";
					MinutWykonano = MinutWykonano*(-1);
				}
				
				if(MinutWykonano%60>=10)
					t[1][2*p] += Integer.toString(MinutWykonano/60)+":"+Integer.toString(MinutWykonano%60);
				else
					t[1][2*p] += Integer.toString(MinutWykonano/60)+":0"+Integer.toString(MinutWykonano%60);
				
				
				t[2][2*p] = Integer.toString(ileSeriiUkonczono);
				t[3][2*p] = ileMaszyn;
				
				MinutWykonano = 0;
				ileSeriiUkonczono = 0;
				//z Rejestracji -> wszystkie rejestracje czasu pracownika w okreslonym przedziale czasowym
					//suma czasu dla jednego bonu 
				// na WSZYSTKICH HALACH
				
				String sql10 = "select werkbon, sum(tijd), status from Rejestracja where datum >= '"+start+"' and datum <= '"+end+"' and CFNAAM = '"+nazwisko+"' and verwerkt = 1 group by werkbon ";
				Statement stm10 = connection.createStatement();
				ResultSet rs10 = stm10.executeQuery(sql10);
				while(rs10.next()){
					int MinutDlaBonu = rs10.getInt(2);
					MinutWykonano += MinutDlaBonu;
					//wyszukanie czasu teoretycznego 	
				}
				stm10.close();
				
				//Sprawdzenie ile maszyn
				String sql13 = "select count(*) from (select werkpost from Rejestracja where datum >= '"+start+"' and datum <= '"+end+"' and CFNAAM = '"+nazwisko+"' and verwerkt = 1 group by werkpost) M";
				Statement stm13 = connection.createStatement();
				ResultSet rs13 = stm13.executeQuery(sql13);
				int ileMaszyn2 = 0;
				while(rs13.next()){
					ileMaszyn2 = rs13.getInt(1);
				}
				stm13.close();
				t[1][(2*p)+1] = "";
				if(MinutWykonano<0){
					t[1][(2*p)+1] = "-";
					MinutWykonano = MinutWykonano*(-1);
				}
				if(MinutWykonano%60>=10)
					t[1][(2*p)+1] += Integer.toString(MinutWykonano/60)+":"+Integer.toString(MinutWykonano%60);
				else
					t[1][(2*p)+1] += Integer.toString(MinutWykonano/60)+":0"+Integer.toString(MinutWykonano%60);
				t[2][(2*p)+1] = Integer.toString(ileSeriiUkonczono);
				t[3][(2*p)+1] = Integer.toString(ileMaszyn2);
				
				p++;
			}//koniec podsumowania pracownikow w gniezdzie
			
			//Zrobienie PDFa
			for(int i = 0; i<ileKolumn; i++){
				PdfPCell c1 = new PdfPCell(new Phrase(headers[i], smallFont));
				c1.setMinimumHeight(30);
				c1.setHorizontalAlignment(Element.ALIGN_CENTER);
				c1.setVerticalAlignment(Element.ALIGN_MIDDLE);
				c1.setBackgroundColor(BaseColor.ORANGE);
				tabPDF.addCell(c1);
			}
			for(int i = 0; i<((2*ilePracownikow)); i++){
				for(int j = 0; j<ileKolumn; j++){
					String zawartosc = t[j][i];
					if(zawartosc.equals("NaN%")||zawartosc.equals("Infinity%"))
						zawartosc = "";
					PdfPCell c2 = new PdfPCell(new Phrase(zawartosc, smallFont2));
					c2.setFixedHeight(25);
					if(i%2==1)
						c2.setFixedHeight(15);
					if((j==0 || j==2) && i%2==0)
						c2.setRowspan(2);
					else
						c2.setRowspan(1);
					
					c2.setHorizontalAlignment(Element.ALIGN_CENTER);
					c2.setVerticalAlignment(Element.ALIGN_MIDDLE);
					
					if(i%2==1 && (j==0 || j==2)){
						
					}
					else
						tabPDF.addCell(c2);
				}
			}
			tabPDF.deleteLastRow();
			tabPDF.deleteLastRow();
			try {
				tabPDF.setWidths(widths);
				tabPDF.setHeaderRows(1);
				tabPDF.setWidthPercentage(100);
				tabPDF.setHorizontalAlignment(Element.ALIGN_CENTER);
				tabPDF.setHorizontalAlignment(Element.ALIGN_CENTER);
				doc.add(tabPDF);
			} catch (DocumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			doc.newPage();
			zarejestrowanychMinutAll +=minutyWydzial;
			tab[2][ktoreGniazdo] = Integer.toString(minutyWydzial/60)+":"+Integer.toString(minutyWydzial%60);
			
		}//koniec gniazda
		
		String sql = "select count(*) from (select cfnaam from Rejestracja where datum >= '"+start+"' and datum <= '"+end+"' and cfnaam not like 'AWARIA%' and verwerkt = 1 and cfnest like 'W%' group by cfnaam) M";
		Statement st = connection.createStatement();
		ResultSet rs = st.executeQuery(sql);
		String ile = "";
		while(rs.next()){
			ile = rs.getString(1);
		}
		rs.close();
		st.close();
		
		Paragraph preface = new Paragraph();
        // We add one empty line
		 preface.add("\n");
        // Lets write a big header
        preface.add(new Paragraph("Raport godzin pracowników", catFont));

        preface.add("\n");
        // Will create: Report generated by: _name, _date
        preface.add(new Paragraph("Od: "+start+" do:  "+end, smallBold));
        preface.add("\n");
        preface.add(new Paragraph("Ilosc pracowników zarejestrowanych: "+ile, smallBold));
        preface.add("\n");
        preface.add(new Paragraph(
                        "Raport wg gniazd",
                        smallBold));

        preface.add("\n");
        try {
			doc.add(preface);
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        headers = new String[3];
		headers[0] = "Gniazdo";
		headers[1] = "Gniazdo - opis";
		headers[2] = "Czas zarejestrowany";
		
		PdfPTable tab2PDF = new PdfPTable(3);
		 float widths[] = new float[] { 5, 14, 10};
		for(int i = 0; i<3; i++){
			PdfPCell c1 = new PdfPCell(new Phrase(headers[i], smallFont));
			c1.setMinimumHeight(30);
			c1.setHorizontalAlignment(Element.ALIGN_CENTER);
			c1.setVerticalAlignment(Element.ALIGN_MIDDLE);
			c1.setBackgroundColor(BaseColor.ORANGE);
			tab2PDF.addCell(c1);
		}
		for(int i = 0; i<9; i++){
			for(int j = 0; j<3; j++){
				String zawartosc = tab[j][i];
				if(zawartosc.equals("NaN%")||zawartosc.equals("Infinity%"))
					zawartosc = "";
				PdfPCell c2 = new PdfPCell(new Phrase(zawartosc, smallFont2));
				c2.setMinimumHeight(30);
				c2.setHorizontalAlignment(Element.ALIGN_CENTER);
				c2.setVerticalAlignment(Element.ALIGN_MIDDLE);
				tab2PDF.addCell(c2);
			}
		}
		
		PdfPCell c1 = new PdfPCell(new Phrase("SUMA", smallFont2));
		c1.setMinimumHeight(30);
		c1.setColspan(2);
		c1.setHorizontalAlignment(Element.ALIGN_CENTER);
		c1.setVerticalAlignment(Element.ALIGN_MIDDLE);
		tab2PDF.addCell(c1);
		
		PdfPCell c2 = new PdfPCell(new Phrase(Integer.toString(zarejestrowanychMinutAll/60)+":"+Integer.toString(zarejestrowanychMinutAll%60), smallFont2));
		c2.setMinimumHeight(30);
		c2.setHorizontalAlignment(Element.ALIGN_CENTER);
		c2.setVerticalAlignment(Element.ALIGN_MIDDLE);
		tab2PDF.addCell(c2);
		
		tab2PDF.setWidthPercentage(100);
		try {
			tab2PDF.setWidths(widths);
			tab2PDF.setHorizontalAlignment(Element.ALIGN_CENTER);
			tab2PDF.setHorizontalAlignment(Element.ALIGN_CENTER);
			doc.add(tab2PDF);
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		doc.close();
		System.out.println("Koniec listy zarejestrowanych godzin");
		return;
	}
	
	
}