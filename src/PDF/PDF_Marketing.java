package PDF;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

public class PDF_Marketing {
	
	static Connection myConn;
	private static Font ffont = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 10); 
	private static Font ffont2 = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 8); 
	private static Font ffont3 = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 14); 
	private static Font ffont4 = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 12); 
		
	public static void create(){
		System.out.println("Start harmonogramu marketingowego");
		myConn = WB.DBConnection.dbConnector();
		//kolejnoúÊ typÛw maszyn
		String kolejnosc[] = new String[] {"", "KONW", "560-710", "800", "1350", "CONTUR", "FCT"};
		//kolejnoúÊ miesiÍcy
		String[] miesiace = {"STYCZE—", "LUTY", "MARZEC", "KWIECIE—", "MAJ", "CZERWIEC", "LIPIEC", "SIERPIE—", "WRZESIE—", "PAèDZIERNIK", "LISTOPAD", "GRUDZIE—"};
		SimpleDateFormat doNazwy = new SimpleDateFormat("yyyy.MM.dd");
		SimpleDateFormat godz = new SimpleDateFormat("HH;mm");
		Calendar date = Calendar.getInstance();
		Document ML = new Document(PageSize.A3.rotate(), 15, 15, 50, 50);
		ffont2.setColor(BaseColor.BLACK);
		try
		{
			String path = PDF.Parameters.getPathToSave()+"/"+doNazwy.format(date.getTime())+"/";
			String name = "Harmonogram MARKETING.pdf";
			File f = new File(path+name);
			if(f.exists() && !f.isDirectory())
				name = godz.format(date.getTime())+" "+name;
			PdfWriter writer = PdfWriter.getInstance(ML, new FileOutputStream(path+name));
			ML.open();
			writer.setPageEvent(new PDF_MyFooter());
			PdfPTable table = new PdfPTable(33);
			float widths[] = new float[] {25, 50, 35, 22, 35, 35, 7, 7, 7, 7, 7, 7, 7, 7, 15, 15, 15, 20, 15, 15, 7, 7, 7, 7, 20, 20, 15, 15, 15, 15, 15, 15, 35};
			addHeader(table);
			//addRow(1, table);
			SimpleDateFormat db = new SimpleDateFormat("yyyy-MM-dd");
			
			//ustawienie daty poczπtku kalendarza
			Calendar poczatekKalendarza = Calendar.getInstance();
			poczatekKalendarza.add(Calendar.MONTH, -1);
			poczatekKalendarza.add(Calendar.DAY_OF_MONTH, -poczatekKalendarza.get(Calendar.DAY_OF_MONTH)+1);
			
			//ustalenie daty koÒca kalendarza
			Calendar koniecKalendarza = Calendar.getInstance();
			koniecKalendarza.add(Calendar.MONTH, 4);
			koniecKalendarza.add(Calendar.DAY_OF_MONTH, (koniecKalendarza.getActualMaximum(Calendar.DAY_OF_MONTH)-koniecKalendarza.get(Calendar.DAY_OF_MONTH)));
			
			//pobieranie starych projektow: (starszych = data potwierdzona klientowi == wczeúniej niø miesiπc temu)
			boolean naglowek = false;
			boolean zakonczono = false;
			String sql = "Select nrMaszyny, opis, klient, dataKoniecMontazu, dataKontrakt, DataZadanaMarketing, typ from calendar where dataKontrakt < '"+db.format(poczatekKalendarza.getTime())+"' ";
			for(int k = 0; k<2; k++){
				//trzy typy wybierania danych - najpierw maszyny z klientem (k==0), nastepnie magazynowki (k==1) i po wszystkim te skonczone
				boolean ifFirst = true;
				
				for(int i = 0; i<kolejnosc.length; i++){
					String sql1 = "";
					if(k==0){
						ifFirst=false;
						sql1 = sql + "  and (nrMaszyny like '2/%' or nrMaszyny like '6/%' or nrMaszyny like '0/%') and klient <> 'MAGAZYN' and typ = '"+kolejnosc[i]+"' and Zakonczone = 0 and Wyslano = 0 order by dataKontrakt asc";
					}
					else if(k==1){
						if(i==0) ifFirst = true; 
						sql1 = sql + "  and (nrMaszyny like '2/%' or nrMaszyny like '6/%' or nrMaszyny like '0/%') and klient = 'MAGAZYN' and typ = '"+kolejnosc[i]+"' and Zakonczone = 0 and Wyslano = 0 order by dataKontrakt asc";
					}
					else{
						if(i==0) ifFirst = true;
						sql1 = sql+" and (nrMaszyny like '2/%') and typ = '"+kolejnosc[i]+"' and (zakonczone = 0 and wyslano = 1) order by dataKontrakt asc";
						zakonczono = true;
					}
					Statement stm1 = myConn.createStatement();
					ResultSet rs1 = stm1.executeQuery(sql1);
					while(rs1.next()){
						if(!naglowek){
							naglowek = true;
							addHeader(table, "OLD PROJECTS");
						}
						String calyNumer = rs1.getString("nrMaszyny");
						String [] p = calyNumer.split("/");
						String ProjectGroup = p[0];
						String ProjectNumber = p[1];
						String ShippingDate = rs1.getString("dataKontrakt");
						String MontageFinishDate = rs1.getString("dataKoniecMontazu");
						String ProjectName = rs1.getString("Opis");
						String klient = rs1.getString("klient");
						String typ = rs1.getString("typ");
						String DataZadanaMarketing = rs1.getString("DataZadanaMarketing");
						if(ProjectNumber.length()==6) {
						addProject(table, ProjectGroup, ProjectNumber, ProjectName, klient, MontageFinishDate, ShippingDate,  DataZadanaMarketing, typ, ifFirst, zakonczono);
						}
						if(ifFirst) ifFirst=false;
					}
					rs1.close();
					stm1.close();
				}
			}
			table.completeRow();
			table.setWidthPercentage(100);
			table.setWidths(widths);
			table.setHorizontalAlignment(Element.ALIGN_CENTER);
			table.setHeaderRows(1);
			ML.add(table);
			ML.newPage();
			table = new PdfPTable(33);
			addHeader(table);
			
			//pobieranie maszyn (project schedule)
			for(int i = 0; i<6; i++){
				zakonczono = false;
				naglowek=false;
				Calendar poczMies = Calendar.getInstance(), konMies = Calendar.getInstance();
				poczMies.setTime(poczatekKalendarza.getTime());
				konMies.setTime(poczatekKalendarza.getTime());
				poczMies.add(Calendar.MONTH, i);
				konMies.add(Calendar.MONTH, i+1);
				konMies.add(Calendar.DAY_OF_YEAR, -1);
				sql = "Select nrMaszyny, opis, klient, dataKoniecMontazu, dataKontrakt, DataZadanaMarketing, typ from calendar where dataKontrakt >= '"+db.format(poczMies.getTime())+"' and dataKontrakt <= '"+db.format(konMies.getTime())+"'  ";
				String podpis = "";
				
				//pobieranie maszyn z zakresu aktualnych dat
				int male = 0, osiemset = 0, konw = 0, m1350 = 0, contur = 0, FCT = 0, pozostale=0;
				int maleMAG = 0, osiemsetMAG = 0, konwMAG = 0, m1350MAG = 0, conturMAG = 0, FCTMAG = 0, pozostaleMAG=0;
				for(int k = 0; k<2; k++){
					boolean ifFirst = true;
					for(int j = 0; j<kolejnosc.length; j++){
						String sql2 = "";
						//najpierw niemagazynowe
						if(k==0){
							ifFirst = false;
							sql2 = sql+" and (nrMaszyny like '2/%' or nrMaszyny like '6/%' or nrMaszyny like '0/%') and klient <> 'MAGAZYN' and typ = '"+kolejnosc[j]+"' and Zakonczone = 0 and Wyslano = 0 order by dataKontrakt";
						}
						//nastÍpnie magazynowe
						else if (k==1){
							if(j==0) ifFirst = true;
							sql2 = sql+" and (nrMaszyny like '2/%' or nrMaszyny like '6/%' or nrMaszyny like '0/%') and klient = 'MAGAZYN' and typ = '"+kolejnosc[j]+"' and Zakonczone = 0 and Wyslano = 0 order by dataKontrakt";
						}
						//wyslane ale niezakonczone
						else{
							if(j==0) ifFirst = true;
							sql2 = sql+" and (nrMaszyny like '2/%') and typ = '"+kolejnosc[j]+"' and (zakonczone = 0 and wyslano = 1) order by dataKontrakt";
							zakonczono = true;
						}
						Statement stm2 = myConn.createStatement();
						ResultSet rs2 = stm2.executeQuery(sql2);
						while(rs2.next()){
							if(!naglowek){
								naglowek = true;
								int miesiac = poczMies.get(Calendar.MONTH);
								addHeader(table, miesiace[miesiac]);
							}
							String calyNumer = rs2.getString("nrMaszyny");
							String [] p = calyNumer.split("/");
							String ProjectGroup = p[0];
							String ProjectNumber = p[1];
							String DeliveryDate = rs2.getString("dataKontrakt");
							String MontageFinishDate = rs2.getString("dataKoniecMontazu");
							String ProjectName = rs2.getString("Opis");
							String klient = rs2.getString("klient");
							String typ = rs2.getString("typ");
							String wysylka = rs2.getString("DataZadanaMarketing");
							if(k!=1){
								//zliczanie ile maszyn danego typu
								if(typ.equals("560-710")) male ++;
								else if(typ.equals("KONW")) konw ++;
								else if(typ.equals("800")) osiemset ++;
								else if(typ.equals("1350")) m1350 ++;
								else if(typ.equals("CONTUR")) contur ++;
								else if(typ.equals("FCT")) FCT ++;
								else pozostale++;
							}
							else{
								if(typ.equals("560-710")) maleMAG ++;
								else if(typ.equals("KONW")) konwMAG ++;
								else if(typ.equals("800")) osiemsetMAG ++;
								else if(typ.equals("1350")) m1350MAG ++;
								else if(typ.equals("CONTUR")) conturMAG ++;
								else if(typ.equals("FCT")) FCTMAG ++;
								else pozostale++;
							}
							addProject(table, ProjectGroup, ProjectNumber, ProjectName, klient, MontageFinishDate, DeliveryDate,  wysylka, typ, ifFirst, zakonczono);
							
							if(ifFirst) ifFirst=false;
						}
						stm2.close();
					}
				}
				//podpis podliczajπcy ile jest danych projektÛw
				podpis = "PROJEKTY  konw: "+konw+",    560-710: "+male+",    800-1150: "+osiemset+",    1350-1550: "+m1350+",    CONTUR: "+contur+",    FCT/FTM: "+FCT+",    pozostale: "+pozostale+"\n";
				podpis += "MAGAZYN  konw: "+konwMAG+",    560-710: "+maleMAG+",    800-1150: "+osiemsetMAG+",    1350-1550: "+m1350MAG+",    CONTUR: "+conturMAG+",    FCT/FTM: "+FCTMAG+",    pozostale: "+pozostaleMAG;
		
				addHeader(table, podpis);
				
				table.completeRow();
				table.setWidthPercentage(100);
				table.setWidths(widths);
				table.setHorizontalAlignment(Element.ALIGN_CENTER);
				table.setHeaderRows(1);
				ML.add(table);
				ML.newPage();
				table = new PdfPTable(33);
				addHeader(table);
			}
			
			
			//pobieranie nastepnych projektow:
			 naglowek = false;
			 sql = "Select nrMaszyny, opis, klient, dataKoniecMontazu, dataKontrakt, DataZadanaMarketing, typ from calendar where dataKontrakt > '"+db.format(koniecKalendarza.getTime())+"' ";
			for(int k = 0; k<2; k++){
				boolean ifFirst = true;
				
				for(int i = 0; i<kolejnosc.length; i++){
					String sql1 = "";
					if(k==0){
						ifFirst=false;
						sql1 = sql + "  and (nrMaszyny like '2/%' or nrMaszyny like '6/%' or nrMaszyny like '0/%') and klient <> 'MAGAZYN' and typ = '"+kolejnosc[i]+"' and Zakonczone = 0 and Wyslano = 0 order by dataKontrakt";
					}
					else if(k==1){
						if(i==0) ifFirst = true;
						sql1 = sql + "  and (nrMaszyny like '2/%' or nrMaszyny like '6/%' or nrMaszyny like '0/%') and klient = 'MAGAZYN' and typ = '"+kolejnosc[i]+"' and Zakonczone = 0 and Wyslano = 0 order by dataKontrakt";
					}
					Statement stm3 = myConn.createStatement();
					ResultSet rs1 = stm3.executeQuery(sql1);
					while(rs1.next()){
						if(!naglowek){
							naglowek = true;
							addHeader(table, "NEXT PROJECTS");
						}
						String calyNumer = rs1.getString("nrMaszyny");
						String [] p = calyNumer.split("/");
						String ProjectGroup = p[0];
						String ProjectNumber = p[1];
						String ShippingDate = rs1.getString("dataKontrakt");
						String MontageFinishDate = rs1.getString("dataKoniecMontazu");
						String ProjectName = rs1.getString("Opis");
						String klient = rs1.getString("klient");
						String typ = rs1.getString("typ");
						String wysylka = rs1.getString("DataZadanaMarketing");
						
						addProject(table, ProjectGroup, ProjectNumber, ProjectName, klient, MontageFinishDate, ShippingDate, wysylka, typ, ifFirst, false);
						if(ifFirst) ifFirst=false;
					}
					stm3.close();
				}
			}
			table.completeRow();
			table.setWidthPercentage(100);
			table.setWidths(widths);
			table.setHorizontalAlignment(Element.ALIGN_CENTER);
			table.setHeaderRows(1);
			ML.add(table);
			myConn.close();
			ML.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		System.out.println("Koniec harmonogramu marketingowego");
	}
	
	private static void addHeader(PdfPTable t){
	
		String komorki[] = new String[] {"Projekt", "Opis", "Klient", "Typ", "Data wysy≥ki / potwierdzona", "Zadana data montaøu", "£oøe", "Korpus wrzec", "Wrzeciono", "Korpus konika", "Korpus skrz.pos.", "Korpus zamka", "Sanki", "Szuflada", "SkrÍcanie", "Malowanie", "Skrobanie", "Takt 1: wspornik silnika", "Takt 2: oú Z", "Takt 3: oú X", "Wrzeciennik", "Konik", "Skrz. posuwÛw", "Zamek", "Takt 4: geometria, skrobanie konika, wrzec", "Takt 5: przyg do montaøu blach, silnik g≥Ûwny", "Malowanie os≥on", "G≥owica", "Hydr", "Takt 6", "Montaø koÒcowy", "Za≥adunek", "Data koÒca montaøu"};
		for(int i = 0; i<33; i++){
			PdfPCell cell = new PdfPCell(new Phrase(komorki[i], ffont));
			if(i>5 && i<32) cell.setRotation(90);
			cell.setFixedHeight(80f);
			cell.setBackgroundColor(BaseColor.ORANGE);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			if((i>5&&i<13)||(i>19&&i<23))
				cell.setVerticalAlignment(Element.ALIGN_TOP);
			else
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			t.addCell(cell);
		}
	}
	
	private static void addHeader(PdfPTable t, String title){
		PdfPCell cell = new PdfPCell(new Phrase(title, ffont));
		cell.setMinimumHeight(20f);
		if(!title.startsWith("W produkcji"))
			cell.setBackgroundColor(new BaseColor(211, 180, 254));
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setColspan(33);
		t.addCell(cell);
	}
	private static void addHeader2(PdfPTable t){
		String komorki[] = new String[] {"Numer seryjny", "Opis", "Klient", "Procent", "Kwota", "Termin p≥atnoúci"};
		for(int i = 0; i<komorki.length; i++) {
			PdfPCell cell = new PdfPCell(new Phrase(komorki[i], ffont3));
			cell.setMinimumHeight(20f);
			cell.setBackgroundColor(BaseColor.ORANGE);
			cell.setBorder(Rectangle.BOTTOM | Rectangle.TOP);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			t.addCell(cell);
		}
	}
	
	
	private static void addProject(PdfPTable table, String ProjectGroup, String ProjectNumber, String ProjectName, String klient, String MontageFinishDate, String ShippingDate, String DataZadanaMarketing, String typ, boolean odkreslic, boolean zakonczono) throws SQLException{
		Statement pobierzProjectSchedule = myConn.createStatement();
		String sql = "Select Komentarz from calendar where nrMaszyny = '"+ProjectGroup+"/"+ProjectNumber+"'";
		ResultSet ProjectSchedule = pobierzProjectSchedule.executeQuery(sql);
		String komentarz = "";
		while(ProjectSchedule.next()){
			komentarz = ProjectSchedule.getString("Komentarz");
		}
		ProjectSchedule.close();
		pobierzProjectSchedule.close();
		String zmianaNumeru = "";
		boolean zmiana = false;
		if(komentarz.length()==8){
			zmianaNumeru = komentarz;
			zmiana = true;
		}
		String zmianaGrupy = "";
		String zmianaNr = "";
		if(zmiana){
		String [] p2 = zmianaNumeru.split("/");
		 zmianaGrupy = p2[0];
		 zmianaNr = p2[1];
		}
		
		if(odkreslic){
			PdfPCell cell = new PdfPCell();
			cell.setFixedHeight(1f);
			cell.setBorderWidth(1f);
			cell.setColspan(33);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			table.addCell(cell);
		}
		boolean alert = false;
		if(DataZadanaMarketing.length()>9){
			if(MontageFinishDate.compareTo(DataZadanaMarketing)>0 ||MontageFinishDate.compareTo(ShippingDate)>0){
				alert = true;
			}
		}
		
		PdfPCell cell1 = new PdfPCell(new Phrase(ProjectNumber, ffont2));
		cell1.setFixedHeight(25f);
		cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
		if(zakonczono)
			cell1.setBackgroundColor(new BaseColor(215, 240, 247));
		if(alert)
			cell1.setBackgroundColor(new BaseColor(254, 154, 154));
		table.addCell(cell1);
		
		PdfPCell cell2 = new PdfPCell(new Phrase(ProjectName, ffont2));
		cell2.setFixedHeight(25f);
		cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell2.setVerticalAlignment(Element.ALIGN_MIDDLE);
		if(alert)
			cell2.setBackgroundColor(new BaseColor(254, 154, 154));
		table.addCell(cell2);
		
		PdfPCell cell3 = new PdfPCell(new Phrase(klient, ffont2));
		cell3.setFixedHeight(25f);
		cell3.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell3.setVerticalAlignment(Element.ALIGN_MIDDLE);
		if(alert)
			cell3.setBackgroundColor(new BaseColor(254, 154, 154));
		table.addCell(cell3);
		
		PdfPCell cell4 = new PdfPCell(new Phrase(typ, ffont2));
		cell4.setFixedHeight(25f);
		cell4.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell4.setVerticalAlignment(Element.ALIGN_MIDDLE);
		if(alert)
			cell4.setBackgroundColor(new BaseColor(254, 154, 154));
		table.addCell(cell4);
		
		PdfPCell cell5 = new PdfPCell(new Phrase(ShippingDate, ffont2));
		cell5.setFixedHeight(25f);
		cell5.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell5.setVerticalAlignment(Element.ALIGN_MIDDLE);
		if(alert)
			cell5.setBackgroundColor(new BaseColor(254, 154, 154));
		table.addCell(cell5);
		
		PdfPCell komorka2 = new PdfPCell(new Phrase (DataZadanaMarketing, ffont2));
		komorka2.setFixedHeight(24f);
		komorka2.setHorizontalAlignment(Element.ALIGN_CENTER);
		komorka2.setVerticalAlignment(Element.ALIGN_MIDDLE);
		if(alert)
			komorka2.setBackgroundColor(new BaseColor(254, 154, 154));
		table.addCell(komorka2);
		
		if(zakonczono||ProjectGroup.equals("0")) table.completeRow();
		else{
		
		String columns1[] = new String[] {"L$", "W$", "WR$", "K$", "P$", "S$", "SSZ$", "SSX$"};
		String columns2[] = new String[] {"%W$", "%K$", "%P$", "%S$"};
		Statement TakeTechn123=myConn.createStatement();
		ResultSet technologia123 = TakeTechn123.executeQuery("SELECT count(*) from werkbon " +
				"where Afdeling = '"+ProjectGroup+"' and Afdelingseq='"+ProjectNumber+"'");
		int ileStanowisk = 0;
		while(technologia123.next())
			ileStanowisk = technologia123.getInt("COUNT(*)");
		if(ileStanowisk!=0){
			List<String> techn = new ArrayList<String>();
			List<String> statusy = new ArrayList<String>();
			List<String> colors = new ArrayList<String>();
			String calyNumer = ProjectGroup+"/"+ProjectNumber;
			
			List<Zlozenie> Zloz = new ArrayList<Zlozenie>();
			Statement myStmt4 = myConn.createStatement();
			String sql07 = "Select * from Zlozenia where projekt = '"+calyNumer+"'";
			//if(zmiana)	sql07 = "Select * from Zlozenia where projekt = '"+zmianaNumeru+"'";
			ResultSet a = myStmt4.executeQuery(sql07);
			while(a.next()){
				String t = a.getString("typ");
				String zam = a.getString("nrZamowienia");
				Zlozenie b = new Zlozenie(t, zam, a.getString("kodArtykulu"), a.getString("nazwa"), a.getInt("status"));
				Zloz.add(b);
			}
			myStmt4.close();
			
			//pobierz technologie dla projektu
			
			
			colors.clear();
			
			//pobieranie informacji o zlozeniach
			for(int i = 0; i<8; i++){
				int s = -1;
				for(Zlozenie a1 : Zloz){
					if(a1.getTyp().equals(columns1[i])){
						if(s==-1)
							s=a1.getStatus();
						else{
							if(a1.getStatus()==10){
								if(s==90)
									s=20;
							}
							else if(a1.getStatus()==20)
								s=20;
							else if(a1.getStatus()==90){
								if(s==10)
									s=20;
							}
						}
					}
				}
				if(s==0)
					s=90;
				PdfPCell komorka = new PdfPCell();
				komorka.setFixedHeight(24f);
				if(s==10) 
					komorka.setBackgroundColor(BaseColor.WHITE);
				if(s==20) komorka.setBackgroundColor(new BaseColor(255, 255, 0));
				if(s==90) komorka.setBackgroundColor(new BaseColor(18, 162, 25));
				if(s==-1){
					komorka.setCellEvent(new PDF_Diagonal());
					komorka.setBackgroundColor(new BaseColor(225, 225, 225));
				}
				table.addCell(komorka);
			}
			
			//dla 14 grup technologicznych pobieramyh stanowiska:
			for (int i = 1; i<=14; i++){
				ResultSet WorkplaceCodes;
				
				//dodajemy info o zlozeniach
				if(i==7){
					for(int j = 0; j<4; j++){
						int s = -1;
						for(Zlozenie a1 : Zloz){
							if(a1.getTyp().equals(columns2[j])){
								if(s==-1)
									s=a1.getStatus();
								else{
									if(a1.getStatus()==10){
										if(s==90)
											s=20;
									}
									else if(a1.getStatus()==20)
										s=20;
									else if(a1.getStatus()==90){
										if(s==10)
											s=20;
									}
								}
							}
						}
						if(s==0)
							s=90;
						PdfPCell komorka = new PdfPCell();
						komorka.setFixedHeight(24f);
						if(s==10) komorka.setBackgroundColor(BaseColor.WHITE);
						if(s==20){
							komorka.setBackgroundColor(new BaseColor(255, 255, 0));
						}
						if(s==90){
							
							komorka.setBackgroundColor(new BaseColor(18, 162, 25));
						}
						if(s==-1){
							komorka.setCellEvent(new PDF_Diagonal());
							komorka.setBackgroundColor(new BaseColor(225, 225, 225));
						}
						table.addCell(komorka);
					}
				}
				
				//pobieranie mozliwych workplace'ow w danej i-tej grupie taktow
				Statement myStmt = myConn.createStatement();
				WorkplaceCodes = myStmt.executeQuery("Select WorkplaceCode from montaz " +
						"where sequence = '"+i+"' group by workplacecode");
				
				while(WorkplaceCodes.next()){
					//patrzymy w technologii projektu czy dane stanowisko wystepuje
					String sql3 = "SELECT seq, status from werkbon " +
							"where Afdeling = '"+ProjectGroup+"' and Afdelingseq like '"+ProjectNumber+"%' and werkpost = '"+WorkplaceCodes.getString("WorkplaceCode")+"'";
					if(zmiana)
						sql3 = "SELECT seq, status from werkbon " +
								"where Afdeling = '"+zmianaGrupy+"' and Afdelingseq like '"+zmianaNr+"%' and werkpost = '"+WorkplaceCodes.getString("WorkplaceCode")+"'";
					Statement TakeTechn = myConn.createStatement();
					ResultSet technologia = TakeTechn.executeQuery(sql3);
					//jesli wystepuje to je pokazujemy
					while(technologia.next()){
						//dla przypadku kiedy mowimy o malarnii:
						if(WorkplaceCodes.getString("WorkplaceCode").equals("MM01")){
							if(i == 2){
								if(technologia.getInt("seq")<100){
									techn.add(WorkplaceCodes.getString("WorkplaceCode"));
									statusy.add(technologia.getString("status"));
								}	
							}
							else
								if(technologia.getInt("seq")>=100){
									techn.add(WorkplaceCodes.getString("WorkplaceCode"));
									statusy.add(technologia.getString("status"));
								}
						}
						//dla kazdego innego stanowiska:
						else{
							statusy.add(technologia.getString("status"));
							techn.add(WorkplaceCodes.getString("WorkplaceCode"));
						}
					}
					TakeTechn.close();
				}
				myStmt.close();
				//na tym poziomie mamy poukladana technologie w listach: techn i statusy
				int ileKomorek = techn.size();
				String status = "0";
				if(ileKomorek>1){
					for(int a1=0; a1<techn.size(); a1++){
						if(statusy.get(a1).equals("10")){
							if(status.equals("90")) status = "20";
						}
						if(statusy.get(a1).equals("20")){
							if(status.equals("90")||status.equals("0")) status = "20";
						}
						if(statusy.get(a1).equals("90")){
							if(status.equals("0")) status = "90";
						}
					}
				}
				else if(ileKomorek == 1)
				{
					if(statusy.get(0).equals("10"))	status = "10";
					if(statusy.get(0).equals("20")){
						status = "20";
					}
					if(statusy.get(0).equals("90")){
						status = "90";
					}
				}
				PdfPCell cell6 = new PdfPCell(new Phrase(" ", ffont2));
				cell6.setFixedHeight(14f);
				cell6.setHorizontalAlignment(Element.ALIGN_CENTER);
				cell6.setVerticalAlignment(Element.ALIGN_MIDDLE);
				if(status.equals("20")) cell6.setBackgroundColor(new BaseColor(255, 255, 0));
				else if (status.equals("90")) cell6.setBackgroundColor(new BaseColor(18, 162, 25));
				table.addCell(cell6);
				techn.clear();
				statusy.clear();
			}
			
			PdfPCell komorka = new PdfPCell(new Phrase (MontageFinishDate, ffont2));
			komorka.setFixedHeight(24f);
			komorka.setHorizontalAlignment(Element.ALIGN_CENTER);
			komorka.setVerticalAlignment(Element.ALIGN_MIDDLE);
			table.addCell(komorka);
			
		}
		else {
			//FINISH LINE 
			table.completeRow();
		}
		
		}
		
	}
	
	private static class Zlozenie{
		
		private String typ;
		private String nrZamowienia;
		private String kodArtykulu;
		private String nazwa;
		
		public String getTyp() {
			return typ;
		}
		public void setTyp(String typ) {
			this.typ = typ;
		}
		public void setNrZamowienia(String nrZamowienia) {
			this.nrZamowienia = nrZamowienia;
		}
		public void setKodArtykulu(String kodArtykulu) {
			this.kodArtykulu = kodArtykulu;
		}
		public void setNazwa(String nazwa) {
			this.nazwa = nazwa;
		}
		public int getStatus() {
			return status;
		}
		public void setStatus(int status) {
			this.status = status;
		}
		private int status;
		public  Zlozenie(String a, String b, String c, String d, int e){
			setTyp(a);
			setNrZamowienia(b);
			setKodArtykulu(c);
			setNazwa(d);
			setStatus(e);
		}
		public String getNrZamowienia() {
			return nrZamowienia;
		}
		public String getKodArtykulu() {
			return kodArtykulu;
		}
		public String getNazwa() {
			return nazwa;
		}
		
	}
	
	public static void createOnlyMagazine(){
		/*
		 * tworzenie listy tylko magazynÛwek - te projekty, ktÛre majπ w referencjach "MAGAZYN"
		 */
		System.out.println("Start harmonogramu marketingowego TYLKO MAGAZYN");
		myConn = WB.DBConnection.dbConnector();
		String kolejnosc[] = new String[] {"KONW", "560-710", "800", "1350", "CONTUR", "FCT"};
		SimpleDateFormat doNazwy = new SimpleDateFormat("yyyy.MM.dd");
		SimpleDateFormat godz = new SimpleDateFormat("HH;mm");
		Calendar date = Calendar.getInstance();
		Document ML = new Document(PageSize.A3.rotate(), 15, 15, 50, 50);
		ffont2.setColor(BaseColor.BLACK);
		try
		{
			String path = PDF.Parameters.getPathToSave()+"/"+doNazwy.format(date.getTime())+"/";
			String name = "Magazynowki MARKETING.pdf";
			File f = new File(path+name);
			if(f.exists() && !f.isDirectory())
				name = godz.format(date.getTime())+" "+name;
			PdfWriter writer = PdfWriter.getInstance(ML, new FileOutputStream(path+name));
			ML.open();
			writer.setPageEvent(new PDF_MyFooter());
			PdfPTable table = new PdfPTable(33);
			float widths[] = new float[] {25, 50, 35, 22, 35, 35, 7, 7, 7, 7, 7, 7, 7, 7, 15, 15, 15, 20, 15, 15, 7, 7, 7, 7, 20, 20, 15, 15, 15, 15, 15, 15, 35};
			addHeader(table);
			//addRow(1, table);
			
			SimpleDateFormat db = new SimpleDateFormat("yyyy-MM-dd");
			
			//POBIERANIE MASZYN ZE STANU MAGAZYNOWEGO:
			PdfPCell cell = new PdfPCell(new Phrase("Gotowe:", ffont));
			cell.setMinimumHeight(20f);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setColspan(33);
			cell.setBackgroundColor(new BaseColor(211, 180, 254));
			table.addCell(cell);
			
			String pobierz = "Select kodArtykulu, Artykul from Stock where typArtykulu = 'PROD' and length(kodArtykulu) = 6 and Ilosc > 0";
			Statement check = myConn.createStatement();
			ResultSet stan = check.executeQuery(pobierz);
			while(stan.next()){
				//sprawdzenie czy magazynowka
				boolean magazynowka = true;
				
				String sprawdz = "Select nrMaszyny, opis, klient, komentarz from calendar where nrmaszyny like '%"+stan.getString("kodArtykulu")+"'";
				Statement checkMachine = myConn.createStatement();
				ResultSet rs = checkMachine.executeQuery(sprawdz);
				while(rs.next()){
					String klient = rs.getString("Klient");
					if(klient == null) klient = "";
					String komentarz = rs.getString("Komentarz");
					if(komentarz==null) komentarz = "";
					if((!klient.equals("MAGAZYN")&&!klient.equals("")) || !komentarz.equals(""))
						magazynowka = false;
				}
				rs.close();
				checkMachine.close();
				if(magazynowka){
					PdfPCell cell2 = new PdfPCell(new Phrase(stan.getString("kodArtykulu"), ffont2));
					cell2.setMinimumHeight(20f);
					cell2.setVerticalAlignment(Element.ALIGN_MIDDLE);
					cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
					table.addCell(cell2);
					
					PdfPCell cell3 = new PdfPCell(new Phrase(stan.getString("Artykul"), ffont2));
					cell3.setMinimumHeight(20f);
					cell3.setVerticalAlignment(Element.ALIGN_MIDDLE);
					cell3.setHorizontalAlignment(Element.ALIGN_CENTER);
					table.addCell(cell3);
					
					PdfPCell cell4 = new PdfPCell(new Phrase("MAGAZYN", ffont2));
					cell4.setMinimumHeight(20f);
					cell4.setVerticalAlignment(Element.ALIGN_MIDDLE);
					cell4.setHorizontalAlignment(Element.ALIGN_CENTER);
					table.addCell(cell4);
					
					table.completeRow();
				}
			}
			stan.close();
			check.close();
			
			
			PdfPCell cell1 = new PdfPCell(new Phrase("W produkcji:", ffont));
			cell1.setMinimumHeight(20f);
			cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
			cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell1.setColspan(33);
			cell1.setBackgroundColor(new BaseColor(211, 180, 254));
			table.addCell(cell1);
			
			
			
			//pobieranie maszyn (project schedule)
			boolean zakonczono = false;
			String sql = "Select nrMaszyny, opis, klient, dataKoniecMontazu, dataKontrakt, DataZadanaMarketing, typ from calendar ";

			String podpis = "";
			int maleMAG = 0, osiemsetMAG = 0, konwMAG = 0, m1350MAG = 0, conturMAG = 0, FCTMAG = 0, pozostaleMAG=0;
			boolean ifFirst = true;
			for(int j = 0; j<kolejnosc.length; j++){
				String sql2 = "";
				if(j==0) ifFirst = true;
				sql2 = sql+" where (nrMaszyny like '2/%' or nrMaszyny like '6/%') and klient = 'MAGAZYN' and typ = '"+kolejnosc[j]+"' and Zakonczone = 0 order by dataKontrakt";
				
				Statement stm2 = myConn.createStatement();
				ResultSet rs2 = stm2.executeQuery(sql2);
				while(rs2.next()){
					String calyNumer = rs2.getString("nrMaszyny");
					String [] p = calyNumer.split("/");
					String ProjectGroup = p[0];
					String ProjectNumber = p[1];
					String DeliveryDate = rs2.getString("dataKontrakt");
					String MontageFinishDate = rs2.getString("dataKoniecMontazu");
					String ProjectName = rs2.getString("Opis");
					String klient = rs2.getString("klient");
					String typ = rs2.getString("typ");
					String wysylka = rs2.getString("DataZadanaMarketing");
					if(typ.equals("560-710")) maleMAG ++;
					else if(typ.equals("KONW")) konwMAG ++;
					else if(typ.equals("800")) osiemsetMAG ++;
					else if(typ.equals("1350")) m1350MAG ++;
					else if(typ.equals("CONTUR")) conturMAG ++;
					else if(typ.equals("FCT")) FCTMAG ++;
					addProject(table, ProjectGroup, ProjectNumber, ProjectName, klient, MontageFinishDate, DeliveryDate, wysylka, typ, ifFirst, zakonczono);
					
					if(ifFirst) ifFirst=false;
				}
				stm2.close();
			}
			//podpis = "PROJEKTY  konw: "+konw+",    560-710: "+male+",    800-1150: "+osiemset+",    1350-1550: "+m1350+",    CONTUR: "+contur+",    FCT/FTM: "+FCT+",    pozostale: "+pozostale+"\n";
			podpis = "W produkcji na MAGAZYN  konw: "+konwMAG+",    560-710: "+maleMAG+",    800-1150: "+osiemsetMAG+",    1350-1550: "+m1350MAG+",    CONTUR: "+conturMAG+",    FCT/FTM: "+FCTMAG;
			addHeader(table, podpis);
			table.completeRow();
			table.setWidthPercentage(100);
			table.setWidths(widths);
			table.setHorizontalAlignment(Element.ALIGN_CENTER);
			table.setHeaderRows(1);
			
			PdfPCell cell2 = new PdfPCell(new Phrase("W planie do uruchomienia:", ffont));
			cell2.setMinimumHeight(20f);
			cell2.setVerticalAlignment(Element.ALIGN_MIDDLE);
			cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell2.setColspan(33);
			cell2.setBackgroundColor(new BaseColor(211, 180, 254));
			table.addCell(cell2);
			
			//pobieranie maszyn z bazy sprzedazy (nieuruchomione):
			
			String sql1 = "SELECT bestellingdetail.leverancier, VERKOOP.SERIENUMMER,  VERKOOP.BESTELDATUM AS DATAZAMOWIENIA, VERKOOP.LEVERDATUM_GEVRAAGD AS DATAZADANA, VERKOOPDETAIL.ARTIKELCODE, VERKOOPDETAIL.ARTIKELOMSCHRIJVING AS OPIS FROM VERKOOP "
					+ "left join VERKOOPDETAIL ON VERKOOP.KLANTNR = VERKOOPDETAIL.KLANTNR AND VERKOOP.BESTELLINGNR = VERKOOPDETAIL.BESTELLINGNR "
					+ "left  join bestellingdetail on verkoop.serienummer = bestellingdetail.ARTIKELCODE "
					+ "WHERE verkoop.STATUSCODE = 'O' AND "
					+ "MACHINEBESTELLING = 1 AND "
					+ "LEVERNAAM = 'TOKARKI MAGAZYNOWE' AND "
					+ "VERKOOP.SERIENUMMER < 300000 AND "
					+ "VERKOOPDETAIL.SEQUENTIE IN (SELECT MIN(VERKOOPDETAIL.SEQUENTIE) FROM VERKOOPDETAIL WHERE VERKOOP.KLANTNR = VERKOOPDETAIL.KLANTNR AND VERKOOP.BESTELLINGNR = VERKOOPDETAIL.BESTELLINGNR) "
					+ "and bestellingdetail.leverancier is null order by serienummer";
			Statement st3 = myConn.createStatement();
			ResultSet rs3 = st3.executeQuery(sql1);
			ifFirst = true;
			while (rs3.next()) {
				//sprawdziÊ co moøna dodaÊ do addProject - jakie dane, øeby to siÍ ≥adnie doda≥o.
				//za≥oøyÊ, øe projectgroup jest rÛwny zero (bez wyszukiwania z≥oøeÒ i operacji)
				String [] opisDzielony = rs3.getString("OPIS").split("Z");
				String opis = opisDzielony[0];	
				String typ="";
				if((opis.contains("MN")||opis.contains("SC"))&&(opis.contains("560")||opis.contains("630")||opis.contains("710"))){
					typ = "560-710";
				}
				else if(opis.contains("CONTUR")){
					typ = "CONTUR";
				}
				else if(((opis.contains("800")&&!opis.contains("8000")) || opis.contains("930"))&&opis.contains("MN")){
					typ = "800";
				}
				else if(opis.contains("CONTUR")){
					typ = "CONTUR";
				}
				else if((!opis.contains("MN")&&!opis.contains("SC"))&&(opis.contains("560")||opis.contains("630")||opis.contains("710"))){
					typ = "KONW";
				}
				else if(opis.contains("FCT")||opis.contains("FTM")){
					typ = "FCT";
				}
				else if(opis.contains("1150")||opis.contains("1100") ||opis.contains("1350") ||opis.contains("1550")){
					typ = "1350";
				}
				addProject(table, "0", rs3.getString("serienummer"), opis, "MAGAZYN", rs3.getString("datazadana"), rs3.getString("datazadana"), rs3.getString("datazadana"), typ, ifFirst, zakonczono);
				if(ifFirst) ifFirst=false;
			}
			rs3.close();st3.close();
			
			
			ML.add(table);
			myConn.close();
			ML.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		System.out.println("Koniec harmonogramu marketingowego TYLKO MAGAZYN");
	}
	
	
	public static void checkInvoices() throws SQLException{
		/* 
		 * tworzenie listy wszystkich niezap≥aconych zaliczek za projekty
		 */
		System.out.println("Start listy zaliczek Marketing");
		myConn = WB.DBConnection.dbConnector();
		Document zal = new Document(PageSize.A4, 15, 15, 20, 20);
		SimpleDateFormat doNazwy = new SimpleDateFormat("yyyy.MM.dd");
		SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat godz = new SimpleDateFormat("HH;mm");
		Calendar today = Calendar.getInstance();
		Date today2 = today.getTime();
		String path = PDF.Parameters.getPathToSave()+"/"+doNazwy.format(today.getTime())+"/";
		String name = "Marketing zaliczki.pdf";
		File f = new File(path+name);
		if(f.exists() && !f.isDirectory())
			name = godz.format(today.getTime())+" "+name;
		PdfWriter writer;
		try {
			double SumaPoCzasie = 0;
			double SumaPrzedCzasem = 0;
			
			writer = PdfWriter.getInstance(zal, new FileOutputStream(path+name));
			writer.setPageEvent(new PDF_MyFooter());
			PdfPTable table = new PdfPTable(6);
			float widths[] = new float[] {10, 14, 10, 6, 10, 10};
			table.setWidths(widths);
			table.setWidthPercentage(100);
			table.setHorizontalAlignment(Element.ALIGN_CENTER);
			table.setHorizontalAlignment(Element.ALIGN_CENTER);
			
			Paragraph preface = new Paragraph();
			Paragraph a = new Paragraph("Wykaz nieop≥aconych zaliczek", ffont4); 
			a.setAlignment(Element.ALIGN_CENTER);
			preface.add(a);
			preface.add("\n");
			
			addHeader2(table);
			//wyciπganie wszystkich maszyn i zaplanowanych p≥atnoúci + sprawdzenie czy niezap≥acone
			String sql = "select substring(calendar.NrMaszyny from 3) as numerSeryjny, calendar.opis, klant.ALFACODE, betaalconditie.PERCENTAGE, betaalconditie.BEDRAG, betaalconditie.CFMUNT, betaalconditie.DATUMBETALING, betaalconditie.CFBOEKINGSNUMMER from calendar " + 
					"	INNER join verkoop on verkoop.SERIENUMMER = substring(calendar.NrMaszyny from 3) " + 
					"	left join klant on klant.KLANTNR = verkoop.KLANTNR " + 
					"	LEFT join betaalconditie on verkoop.KLANTNR = betaalconditie.KLANTNR and verkoop.BESTELLINGNR = betaalconditie.BESTELLINGNR " + 
					"	where (nrMaszyny like '0/%' or nrMaszyny like '2/%' or nrMaszyny like '6/%') and " + 
					"		klient <> 'MAGAZYN' and " + 
					"		verkoop.KLANTNR <> 220001 and" + 
					"		betaalconditie.VOLGNUMMER = 1 and " + 			// <- przed betaalconditie bylo fatdb - niepotrzebne
					"		Wyslano = 0 and " + 
					"		Zakonczone = 0 and " + 
					"		betaalconditie.CFBOEKINGSNUMMER is null " + 
					"	order by betaalconditie.DATUMBETALING ";
			Statement st = myConn.createStatement();
			ResultSet rs = st.executeQuery(sql);
			while(rs.next()) {
				Date deadline;
				try {
					deadline = format1.parse(rs.getString("datumbetaling"));
					if(deadline.before(today2)||deadline.equals(today2)) {
						if(rs.getString("cfmunt").equalsIgnoreCase("pln"))
							SumaPoCzasie += (rs.getDouble("bedrag")/4.2);
						else if(rs.getString("cfmunt").equalsIgnoreCase("eur"))
							SumaPoCzasie += rs.getDouble("bedrag");
					}
					else if (deadline.after(today2)) {
						if(rs.getString("cfmunt").equalsIgnoreCase("pln"))
							SumaPrzedCzasem += (rs.getDouble("bedrag")/4.2);
						else if(rs.getString("cfmunt").equalsIgnoreCase("eur"))
							SumaPrzedCzasem += rs.getDouble("bedrag");
					}
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					System.out.println("Problem in date parsing");
					e.printStackTrace();
				}				
				String komorki[] = new String [] {rs.getString("numerSeryjny"), rs.getString("opis"), rs.getString("alfacode"), rs.getString("percentage")+" %", rs.getString("bedrag")+" "+rs.getString("cfmunt"), rs.getString("datumbetaling")};
				for(int i = 0; i<komorki.length; i++) {
					PdfPCell cell = new PdfPCell(new Phrase(komorki[i], ffont));
					cell.setMinimumHeight(40f);
					cell.setBorderColor(BaseColor.GRAY);
					cell.setBorder(Rectangle.TOP); cell.setBorder(Rectangle.BOTTOM);
					cell.setHorizontalAlignment(Element.ALIGN_CENTER);
					cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
					table.addCell(cell);
				}
			}
			rs.close();st.close();
			zal.open();
			zal.add(preface);
			table.setHeaderRows(1);
			zal.add(table);
			writer.setPageEvent(new PDF_MyFooter());
			
			Paragraph ending = new Paragraph();
			ending.add("\n");
			Paragraph b = new Paragraph("Suma zaliczek po czasie:  "+String.format("%.2f", SumaPoCzasie)+" EUR", ffont4); 
			b.setAlignment(Element.ALIGN_CENTER);
			ending.add(b);
			ending.add("\n");
			Paragraph c = new Paragraph("Suma nadchodzπcych zaliczek:  "+ String.format("%.2f", SumaPrzedCzasem)+" EUR", ffont4); 
			c.setAlignment(Element.ALIGN_CENTER);
			ending.add(c);
			ending.add("\n");
			zal.add(ending);
			
			zal.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Koniec listy zaliczek Marketing");
	}
}
